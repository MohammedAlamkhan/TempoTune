package com.alam.tempotune.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alam.tempotune.PlayerUiState
import com.alam.tempotune.R
import com.alam.tempotune.audio.PlaybackMath
import com.alam.tempotune.data.ThemePreference
import com.alam.tempotune.data.Track
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onTempoChange: (Int) -> Unit,
    onPitchChange: (Int) -> Unit,
    onResetTempo: () -> Unit,
    onResetPitch: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showQueue by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
        ) {
            PlayerHeader(
                trackCount = state.tracks.size,
                onBack = onBack,
                onOpenQueue = { showQueue = true },
                onThemeChange = onThemeChange,
                currentTheme = state.themePreference,
            )

            if (state.currentTrack == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 96.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LogoMark()
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(R.string.no_track_playing),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            } else {
                PlayerContent(
                    state = state,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onSeekBy = onSeekBy,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onTempoChange = onTempoChange,
                    onPitchChange = onPitchChange,
                    onResetTempo = onResetTempo,
                    onResetPitch = onResetPitch,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = sheetState,
            containerColor = colorScheme.surfaceContainerLow,
            contentColor = colorScheme.onSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = colorScheme.outline.copy(alpha = 0.5f),
                )
            },
        ) {
            QueueSheetContent(
                tracks = state.tracks,
                currentIndex = state.currentIndex,
                onTrackClick = { index ->
                    showQueue = false
                    if (index != state.currentIndex) onPlayTrack(index)
                },
            )
        }
    }
}

@Composable
private fun PlayerHeader(
    trackCount: Int,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    currentTheme: ThemePreference,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.back),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.now_playing_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.weight(1f))
        if (trackCount > 0) {
            IconButton(onClick = onOpenQueue) {
                BadgedBox(
                    badge = {
                        if (trackCount > 1) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ) {
                                Text(trackCount.toString())
                            }
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.queue),
                    )
                }
            }
        }
        ThemeMenu(
            current = currentTheme,
            onThemeChange = onThemeChange,
        )
    }
}

@Composable
private fun PlayerContent(
    state: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekBy: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onTempoChange: (Int) -> Unit,
    onPitchChange: (Int) -> Unit,
    onResetTempo: () -> Unit,
    onResetPitch: () -> Unit,
) {
    val track = state.currentTrack ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumArtwork(
                artwork = state.artwork,
                track = track,
                modifier = Modifier.size(144.dp),
            )
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (state.isPlaying) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                            ),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = when {
                            state.isBuffering -> stringResource(R.string.buffering_state)
                            state.isPlaying -> stringResource(R.string.now_playing_state)
                            else -> stringResource(R.string.paused_state)
                        }.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    if (state.tracks.size > 1) {
                        Text(
                            text = "${state.currentIndex + 1} / ${state.tracks.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                FormatBadge(track.format)
            }
        }

        Spacer(Modifier.height(14.dp))
        SeekControl(
            trackKey = track.uri,
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            onSeek = onSeek,
        )

        TransportControls(
            isPlaying = state.isPlaying,
            isBuffering = state.isBuffering,
            hasTrack = state.hasTrack,
            hasNext = state.hasNext,
            onTogglePlayPause = onTogglePlayPause,
            onPrevious = onPrevious,
            onNext = onNext,
            onSeekBy = onSeekBy,
        )

        Spacer(Modifier.height(20.dp))
        PracticeControls(
            tempoPercent = state.tempoPercent,
            pitchSemitones = state.pitchSemitones,
            onTempoChange = onTempoChange,
            onPitchChange = onPitchChange,
            onResetTempo = onResetTempo,
            onResetPitch = onResetPitch,
        )
    }
}

@Composable
private fun AlbumArtwork(
    artwork: ImageBitmap?,
    track: Track,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = colorScheme.primary.copy(alpha = 0.20f),
                spotColor = colorScheme.primary.copy(alpha = 0.24f),
            )
            .clip(shape)
            .background(colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = shape,
            ),
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = stringResource(R.string.artwork_description),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            FallbackArtwork(track)
        }
    }
}

@Composable
private fun FallbackArtwork(track: Track) {
    val colorScheme = MaterialTheme.colorScheme
    val palettes = listOf(
        listOf(colorScheme.primaryContainer, colorScheme.tertiaryContainer),
        listOf(colorScheme.secondaryContainer, colorScheme.primaryContainer),
        listOf(colorScheme.tertiaryContainer, colorScheme.secondaryContainer),
    )
    val palette = palettes[abs(track.uri.hashCode()) % palettes.size]

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = palette)),
    ) {
        val labelSize = maxWidth * 0.42f
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.07f),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.88f, size.height * 0.16f),
            )
            drawCircle(
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.06f),
                radius = size.minDimension * 0.60f,
                center = Offset(size.width * 0.10f, size.height * 0.92f),
            )
            drawCircle(
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
                radius = size.minDimension * 0.30f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = colorScheme.onPrimaryContainer.copy(alpha = 0.08f),
                radius = size.minDimension * 0.42f,
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(labelSize)
                .clip(CircleShape)
                .background(colorScheme.surface.copy(alpha = 0.76f))
                .border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(labelSize * 0.42f),
                tint = colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
internal fun FormatBadge(format: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = format,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                letterSpacing = 0.8.sp,
            ),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SeekControl(
    trackKey: String,
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
) {
    var isDragging by remember(trackKey) { mutableStateOf(false) }
    var draftPosition by remember(trackKey) { mutableFloatStateOf(0f) }

    val max = durationMs.coerceAtLeast(1L).toFloat()
    val sliderValue = (if (isDragging) draftPosition else positionMs.toFloat())
        .coerceIn(0f, max)
    val displayedPosition = if (isDragging) draftPosition.toLong() else positionMs

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = sliderValue,
            onValueChange = { value ->
                isDragging = true
                draftPosition = value
            },
            onValueChangeFinished = {
                if (isDragging) onSeek(draftPosition.toLong())
                isDragging = false
            },
            valueRange = 0f..max,
            enabled = durationMs > 0,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                disabledThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                disabledActiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledInactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
        ) {
            Text(
                text = PlaybackMath.formatTime(displayedPosition),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = PlaybackMath.formatTime(durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    hasTrack: Boolean,
    hasNext: Boolean,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekBy: (Long) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val playPauseDescription = stringResource(
        if (isPlaying) R.string.pause else R.string.play,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportIconButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = stringResource(R.string.previous_track),
            onClick = onPrevious,
            enabled = hasTrack,
        )
        Spacer(Modifier.width(2.dp))
        TransportIconButton(
            icon = Icons.Rounded.Replay10,
            contentDescription = stringResource(R.string.rewind_10),
            onClick = { onSeekBy(-10_000L) },
            enabled = hasTrack,
        )
        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(76.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = colorScheme.primary.copy(alpha = 0.30f),
                    spotColor = colorScheme.primary.copy(alpha = 0.36f),
                )
                .clip(CircleShape)
                .background(colorScheme.primary)
                .clickable(
                    enabled = hasTrack,
                    role = Role.Button,
                    onClick = onTogglePlayPause,
                )
                .semantics {
                    contentDescription = playPauseDescription
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.5.dp,
                    color = colorScheme.onPrimary,
                )
            } else {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                    },
                    label = "play-pause",
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = colorScheme.onPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))
        TransportIconButton(
            icon = Icons.Rounded.Forward10,
            contentDescription = stringResource(R.string.forward_10),
            onClick = { onSeekBy(10_000L) },
            enabled = hasTrack,
        )
        Spacer(Modifier.width(2.dp))
        TransportIconButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = stringResource(R.string.next_track),
            onClick = onNext,
            enabled = hasNext,
        )
    }
}

@Composable
private fun TransportIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val tint = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(26.dp),
            tint = tint,
        )
    }
}

@Composable
private fun PracticeControls(
    tempoPercent: Int,
    pitchSemitones: Int,
    onTempoChange: (Int) -> Unit,
    onPitchChange: (Int) -> Unit,
    onResetTempo: () -> Unit,
    onResetPitch: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val pitchValue = when {
        pitchSemitones > 0 -> stringResource(R.string.pitch_positive, pitchSemitones)
        pitchSemitones < 0 -> stringResource(R.string.pitch_negative, -pitchSemitones)
        else -> stringResource(R.string.pitch_zero)
    }
    val tempoValue = stringResource(R.string.percent_value, tempoPercent)
    val tempoRange = PlaybackMath.MIN_TEMPO_PERCENT.toFloat()..PlaybackMath.MAX_TEMPO_PERCENT.toFloat()
    val pitchRange = PlaybackMath.MIN_PITCH_SEMITONES.toFloat()..PlaybackMath.MAX_PITCH_SEMITONES.toFloat()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            ParameterControl(
                icon = Icons.Rounded.Speed,
                title = stringResource(R.string.tempo),
                hint = stringResource(R.string.tempo_hint),
                value = tempoValue,
                isDefault = tempoPercent == PlaybackMath.DEFAULT_TEMPO_PERCENT,
                onReset = onResetTempo,
                startLabel = stringResource(R.string.tempo_range_start),
                endLabel = stringResource(R.string.tempo_range_end),
                range = tempoRange,
                sliderValue = tempoPercent.toFloat(),
                onValueChange = { onTempoChange(it.roundToInt()) },
                sliderContentDescription = "${stringResource(R.string.tempo)}, $tempoValue",
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.45f),
            )

            ParameterControl(
                icon = Icons.Rounded.MusicNote,
                title = stringResource(R.string.pitch),
                hint = stringResource(R.string.pitch_hint),
                value = pitchValue,
                isDefault = pitchSemitones == PlaybackMath.DEFAULT_PITCH_SEMITONES,
                onReset = onResetPitch,
                startLabel = stringResource(R.string.pitch_range_start),
                endLabel = stringResource(R.string.pitch_range_end),
                range = pitchRange,
                sliderValue = pitchSemitones.toFloat(),
                onValueChange = { onPitchChange(it.roundToInt()) },
                sliderContentDescription = "${stringResource(R.string.pitch)}, $pitchValue",
            )
        }
    }
}

@Composable
private fun ParameterControl(
    icon: ImageVector,
    title: String,
    hint: String,
    value: String,
    isDefault: Boolean,
    onReset: () -> Unit,
    startLabel: String,
    endLabel: String,
    range: ClosedFloatingPointRange<Float>,
    sliderValue: Float,
    onValueChange: (Float) -> Unit,
    sliderContentDescription: String,
) {
    val colorScheme = MaterialTheme.colorScheme
    val accent = if (isDefault) colorScheme.onSurfaceVariant else colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colorScheme.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!isDefault) {
            IconButton(
                onClick = onReset,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.reset),
                    modifier = Modifier.size(18.dp),
                    tint = colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(2.dp))
        } else {
            Spacer(Modifier.width(8.dp))
        }
        Surface(
            shape = CircleShape,
            color = if (isDefault) {
                colorScheme.surfaceContainerHigh
            } else {
                colorScheme.primary.copy(alpha = 0.12f)
            },
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = 56.dp)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                    maxLines = 1,
                )
            }
        }
    }

    Spacer(Modifier.height(6.dp))
    Slider(
        value = sliderValue.coerceIn(range.start, range.endInclusive),
        onValueChange = onValueChange,
        valueRange = range,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = sliderContentDescription },
        colors = SliderDefaults.colors(
            thumbColor = colorScheme.primary,
            activeTrackColor = colorScheme.primary,
            inactiveTrackColor = colorScheme.onSurface.copy(alpha = 0.08f),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
        ),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text = startLabel,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.outline,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = endLabel,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.outline,
        )
    }
}

@Composable
private fun QueueSheetContent(
    tracks: List<Track>,
    currentIndex: Int,
    onTrackClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 2.dp),
        ) {
            Text(
                text = stringResource(R.string.up_next),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.track_count, tracks.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(10.dp))

        val queueListState = rememberLazyListState(
            initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0),
        )
        LazyColumn(
            state = queueListState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            itemsIndexed(tracks, key = { _, track -> track.uri }) { index, track ->
                QueueRow(
                    index = index,
                    track = track,
                    isCurrent = index == currentIndex,
                    onClick = { onTrackClick(index) },
                )
                if (index < tracks.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    index: Int,
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isCurrent) colorScheme.primaryContainer.copy(alpha = 0.55f) else Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCurrent) colorScheme.primary else colorScheme.surfaceContainerHigh,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (isCurrent) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = stringResource(R.string.current_track_marker),
                        modifier = Modifier.size(21.dp),
                        tint = colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = (index + 1).toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${track.artist} · ${track.album ?: stringResource(R.string.unknown_album)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            FormatBadge(track.format)
        }
    }
}
