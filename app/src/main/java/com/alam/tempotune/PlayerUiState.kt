package com.alam.tempotune

import androidx.compose.ui.graphics.ImageBitmap
import com.alam.tempotune.audio.PlaybackMath
import com.alam.tempotune.data.ThemePreference
import com.alam.tempotune.data.Track

data class PlayerUiState(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isReady: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val tempoPercent: Int = PlaybackMath.DEFAULT_TEMPO_PERCENT,
    val pitchSemitones: Int = PlaybackMath.DEFAULT_PITCH_SEMITONES,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val artwork: ImageBitmap? = null,
) {
    val currentTrack: Track?
        get() = tracks.getOrNull(currentIndex)

    val hasNext: Boolean
        get() = currentIndex in 0 until tracks.lastIndex

    val hasPrevious: Boolean
        get() = currentIndex > 0

    val hasTrack: Boolean
        get() = currentTrack != null
}
