package com.alam.tempotune

import android.app.Application
import android.content.ComponentName
import android.os.SystemClock
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.alam.tempotune.audio.PlaybackMath
import com.alam.tempotune.data.AudioSettings
import com.alam.tempotune.data.ThemePreference
import com.alam.tempotune.data.Track
import com.alam.tempotune.data.ArtworkRepository
import com.alam.tempotune.data.UserPreferences
import com.alam.tempotune.data.toTrack
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val preferences = UserPreferences(app)
    private val initialAudioSettings = preferences.loadAudioSettings()

    private val _uiState = MutableStateFlow(
        PlayerUiState(
            tempoPercent = initialAudioSettings.tempoPercent,
            pitchSemitones = initialAudioSettings.pitchSemitones,
            themePreference = preferences.loadTheme(),
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val messageChannel = Channel<String>(Channel.BUFFERED)
    val messages = messageChannel.receiveAsFlow()

    private val mainExecutor = ContextCompat.getMainExecutor(app)
    private val mutableTracks = mutableListOf<Track>()
    private var pendingQueue: Pair<List<Track>, Int>? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var controllerReady = false
    private var cleared = false

    private var artworkJob: Job? = null
    private var settingsSaveJob: Job? = null
    private var artworkTrackUri: String? = null
    private var lastKnownIndex = -1
    private var lastPositionSavedAt = 0L

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncFromPlayer(player)
        }

        override fun onPlayerError(error: PlaybackException) {
            messageChannel.trySend(app.getString(R.string.playback_error))
            controller?.let(::syncFromPlayer)
        }
    }

    init {
        connectToPlaybackService()
        startPositionTicker()
    }

    /**
     * Replaces the current playback queue with a snapshot of the visible
     * library list. The snapshot is intentional: next/previous follow what the
     * listener was looking at when they chose a song.
     */
    fun playQueue(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        val queue = tracks.toList()
        val safeIndex = startIndex.coerceIn(queue.indices)

        if (!controllerReady) {
            pendingQueue = queue to safeIndex
            _uiState.value = _uiState.value.copy(
                tracks = queue,
                currentIndex = safeIndex,
                positionMs = 0L,
                durationMs = queue[safeIndex].durationMs,
            )
            return
        }

        val player = controller ?: return
        mutableTracks.clear()
        mutableTracks.addAll(queue)
        artworkTrackUri = null
        lastKnownIndex = safeIndex
        _uiState.value = _uiState.value.copy(
            tracks = queue,
            currentIndex = safeIndex,
            isPlaying = false,
            isBuffering = true,
            positionMs = 0L,
            durationMs = queue[safeIndex].durationMs,
            artwork = null,
        )
        player.setMediaItems(queue.map { it.toMediaItem() }, safeIndex, 0L)
        player.prepare()
        player.play()
        loadArtwork(queue[safeIndex])
        persistQueue(0L)
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.mediaItemCount == 0) return

        when {
            player.playbackState == Player.STATE_ENDED -> {
                player.seekTo(0L)
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
            }

            player.playWhenReady -> player.pause()
            else -> {
                if (player.playbackState == Player.STATE_IDLE) player.prepare()
                player.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        val duration = _uiState.value.durationMs
        val max = if (duration > 0) duration else Long.MAX_VALUE
        player.seekTo(positionMs.coerceIn(0L, max))
    }

    fun seekBy(deltaMs: Long) {
        val player = controller ?: return
        seekTo(player.currentPosition + deltaMs)
    }

    fun skipNext() {
        val player = controller ?: return
        if (_uiState.value.hasNext) player.seekToNextMediaItem()
    }

    fun skipPrevious() {
        val player = controller ?: return
        if (player.mediaItemCount > 0) player.seekToPrevious()
    }

    fun playTrack(index: Int) {
        val player = controller ?: return
        if (index !in _uiState.value.tracks.indices) return
        player.seekToDefaultPosition(index)
        player.play()
    }

    fun setTempoPercent(percent: Int) {
        val safeValue = percent.coerceIn(
            PlaybackMath.MIN_TEMPO_PERCENT,
            PlaybackMath.MAX_TEMPO_PERCENT,
        )
        if (safeValue == _uiState.value.tempoPercent) return
        _uiState.value = _uiState.value.copy(tempoPercent = safeValue)
        applyPlaybackParameters()
        scheduleAudioSettingsSave()
    }

    fun setPitchSemitones(semitones: Int) {
        val safeValue = semitones.coerceIn(
            PlaybackMath.MIN_PITCH_SEMITONES,
            PlaybackMath.MAX_PITCH_SEMITONES,
        )
        if (safeValue == _uiState.value.pitchSemitones) return
        _uiState.value = _uiState.value.copy(pitchSemitones = safeValue)
        applyPlaybackParameters()
        scheduleAudioSettingsSave()
    }

    fun resetTempo() = setTempoPercent(PlaybackMath.DEFAULT_TEMPO_PERCENT)

    fun resetPitch() = setPitchSemitones(PlaybackMath.DEFAULT_PITCH_SEMITONES)

    fun setThemePreference(theme: ThemePreference) {
        _uiState.value = _uiState.value.copy(themePreference = theme)
        preferences.saveTheme(theme)
    }

    /** Called when the app leaves the foreground so progress survives process death. */
    fun persistNow() {
        persistAudioSettings()
        val player = controller ?: return
        val state = _uiState.value
        if (player.mediaItemCount > 0 && state.currentIndex >= 0) {
            val position = player.currentPosition.coerceAtLeast(0L)
            preferences.saveQueue(mutableTracks.toList(), state.currentIndex, position)
            preferences.savePosition(position)
        }
    }

    private fun connectToPlaybackService() {
        val token = SessionToken(
            app,
            ComponentName(app, PlaybackService::class.java),
        )
        val future = MediaController.Builder(app, token).buildAsync()
        controllerFuture = future
        future.addListener(
            Runnable {
                val mediaController = try {
                    future.get()
                } catch (_: Exception) {
                    if (!cleared) {
                        messageChannel.trySend(app.getString(R.string.service_unavailable))
                    }
                    return@Runnable
                }

                if (cleared) {
                    mediaController.release()
                    return@Runnable
                }

                controller = mediaController
                controllerReady = true
                mediaController.addListener(playerListener)
                applyPlaybackParameters()
                restoreOrAdoptQueue(mediaController)
                _uiState.value = _uiState.value.copy(isReady = true)
                pendingQueue?.let { (tracks, startIndex) ->
                    pendingQueue = null
                    playQueue(tracks, startIndex)
                }
            },
            mainExecutor,
        )
    }

    private fun restoreOrAdoptQueue(player: MediaController) {
        if (player.mediaItemCount > 0) {
            // The service is already running (for example the activity was
            // recreated). Prefer its live queue over the persisted copy.
            val persistedTracks = preferences.loadQueue()
                ?.tracks
                ?.associateBy { it.uri }
                .orEmpty()
            val adopted = buildList {
                for (index in 0 until player.mediaItemCount) {
                    val mediaItem = player.getMediaItemAt(index)
                    val fallback = mediaItem.toTrack() ?: continue
                    add(persistedTracks[fallback.uri] ?: fallback)
                }
            }
            if (adopted.isEmpty()) return

            mutableTracks.clear()
            mutableTracks.addAll(adopted)
            val currentIndex = player.currentMediaItemIndex
                .takeIf { it in adopted.indices }
                ?: 0
            lastKnownIndex = currentIndex
            _uiState.value = _uiState.value.copy(
                tracks = mutableTracks.toList(),
                currentIndex = currentIndex,
                durationMs = adopted[currentIndex].durationMs,
            )
            syncFromPlayer(player)
            return
        }

        val stored = preferences.loadQueue() ?: return
        if (stored.tracks.isEmpty()) return

        mutableTracks.clear()
        mutableTracks.addAll(stored.tracks)

        val startIndex = stored.index.coerceIn(stored.tracks.indices)
        val startTrack = stored.tracks[startIndex]
        val nearEnd = startTrack.durationMs > 0 &&
            stored.positionMs >= startTrack.durationMs - END_OF_TRACK_THRESHOLD_MS
        val startPosition = if (nearEnd) 0L else stored.positionMs

        lastKnownIndex = startIndex
        _uiState.value = _uiState.value.copy(
            tracks = mutableTracks.toList(),
            currentIndex = startIndex,
            durationMs = startTrack.durationMs,
            positionMs = startPosition,
        )
        player.setMediaItems(
            mutableTracks.map { it.toMediaItem() },
            startIndex,
            startPosition,
        )
        player.prepare()
        syncFromPlayer(player)
        loadArtwork(startTrack)
    }

    private fun syncFromPlayer(player: Player) {
        val state = _uiState.value
        if (state.tracks.isEmpty()) {
            _uiState.value = state.copy(
                currentIndex = -1,
                positionMs = 0L,
                durationMs = 0L,
                isPlaying = false,
                isBuffering = false,
            )
            return
        }

        val index = if (player.mediaItemCount == 0) {
            -1
        } else {
            player.currentMediaItemIndex
                .takeIf { it in state.tracks.indices }
                ?: 0
        }

        if (index < 0) {
            _uiState.value = state.copy(
                currentIndex = -1,
                positionMs = 0L,
                durationMs = 0L,
                isPlaying = false,
                isBuffering = false,
            )
            return
        }

        val track = state.tracks[index]
        val rawDuration = player.duration
        val duration = if (rawDuration != C.TIME_UNSET && rawDuration > 0L) {
            rawDuration
        } else {
            track.durationMs
        }
        val rawPosition = player.currentPosition.takeIf { it != C.TIME_UNSET } ?: 0L
        val position = if (duration > 0L) {
            rawPosition.coerceIn(0L, duration)
        } else {
            rawPosition.coerceAtLeast(0L)
        }
        val ended = player.playbackState == Player.STATE_ENDED
        val isPlaying = player.playWhenReady && !ended &&
            player.playbackState != Player.STATE_IDLE

        _uiState.value = state.copy(
            currentIndex = index,
            isPlaying = isPlaying,
            isBuffering = player.playbackState == Player.STATE_BUFFERING,
            positionMs = position,
            durationMs = duration.coerceAtLeast(0L),
        )

        loadArtwork(track)

        if (index != lastKnownIndex) {
            lastKnownIndex = index
            persistQueue(position)
        }
    }

    private fun loadArtwork(track: Track?) {
        if (artworkTrackUri == track?.uri) return
        artworkTrackUri = track?.uri
        artworkJob?.cancel()
        _uiState.value = _uiState.value.copy(artwork = null)
        if (track == null) return

        val requestedUri = track.uri
        artworkJob = viewModelScope.launch {
            val bitmap = ArtworkRepository.load(app, track, LARGE_ARTWORK_SIZE_PX)
            if (!cleared && artworkTrackUri == requestedUri) {
                _uiState.value = _uiState.value.copy(artwork = bitmap?.asImageBitmap())
            }
        }
    }

    private fun applyPlaybackParameters() {
        val state = _uiState.value
        controller?.setPlaybackParameters(
            PlaybackParameters(
                PlaybackMath.tempoPercentToSpeed(state.tempoPercent),
                PlaybackMath.semitonesToPitch(state.pitchSemitones),
            ),
        )
    }

    private fun scheduleAudioSettingsSave() {
        settingsSaveJob?.cancel()
        settingsSaveJob = viewModelScope.launch {
            delay(SETTINGS_SAVE_DEBOUNCE_MS)
            persistAudioSettings()
        }
    }

    private fun persistAudioSettings() {
        val state = _uiState.value
        preferences.saveAudioSettings(
            AudioSettings(
                tempoPercent = state.tempoPercent,
                pitchSemitones = state.pitchSemitones,
            ),
        )
    }

    private fun persistQueue(positionMs: Long) {
        val state = _uiState.value
        if (state.tracks.isEmpty() || state.currentIndex < 0) return
        preferences.saveQueue(mutableTracks.toList(), state.currentIndex, positionMs)
    }

    private fun startPositionTicker() {
        viewModelScope.launch {
            while (isActive) {
                delay(POSITION_TICK_MS)
                val player = controller ?: continue
                val state = _uiState.value
                if (player.mediaItemCount == 0 || state.currentIndex < 0) continue

                val duration = state.durationMs
                val position = player.currentPosition
                    .takeIf { it != C.TIME_UNSET }
                    ?.coerceAtLeast(0L)
                    ?: 0L
                val displayPosition = if (duration > 0L) {
                    position.coerceIn(0L, duration)
                } else {
                    position
                }
                if (displayPosition != state.positionMs) {
                    _uiState.value = state.copy(positionMs = displayPosition)
                }

                if (player.playWhenReady) {
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastPositionSavedAt >= POSITION_SAVE_INTERVAL_MS) {
                        lastPositionSavedAt = now
                        preferences.savePosition(displayPosition)
                    }
                }
            }
        }
    }

    override fun onCleared() {
        cleared = true
        persistNow()
        settingsSaveJob?.cancel()
        artworkJob?.cancel()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        controllerFuture?.cancel(true)
        controllerFuture = null
        super.onCleared()
    }

    private companion object {
        const val END_OF_TRACK_THRESHOLD_MS = 1_000L
        const val SETTINGS_SAVE_DEBOUNCE_MS = 250L
        const val POSITION_TICK_MS = 350L
        const val POSITION_SAVE_INTERVAL_MS = 5_000L
        const val LARGE_ARTWORK_SIZE_PX = 1_200
    }
}
