package com.alam.tempotune.audio

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Pure conversions shared by the player, the UI and tests.
 *
 * ExoPlayer's PlaybackParameters takes a speed multiplier and an independent
 * pitch multiplier. Keeping these mappings here makes the musical units used
 * by the UI explicit and easy to test.
 */
object PlaybackMath {
    const val MIN_TEMPO_PERCENT = 50
    const val MAX_TEMPO_PERCENT = 200
    const val DEFAULT_TEMPO_PERCENT = 100

    const val MIN_PITCH_SEMITONES = -12
    const val MAX_PITCH_SEMITONES = 12
    const val DEFAULT_PITCH_SEMITONES = 0

    fun tempoPercentToSpeed(percent: Int): Float =
        percent.coerceIn(MIN_TEMPO_PERCENT, MAX_TEMPO_PERCENT) / 100f

    fun semitonesToPitch(semitones: Int): Float {
        val safeSemitones = semitones.coerceIn(MIN_PITCH_SEMITONES, MAX_PITCH_SEMITONES)
        return 2.0.pow(safeSemitones / 12.0).toFloat()
    }

    fun sliderValueToTempoPercent(value: Float): Int =
        (value * 100f).roundToInt().coerceIn(MIN_TEMPO_PERCENT, MAX_TEMPO_PERCENT)

    fun sliderValueToSemitones(value: Float): Int =
        value.roundToInt().coerceIn(MIN_PITCH_SEMITONES, MAX_PITCH_SEMITONES)

    fun formatTime(positionMs: Long): String {
        if (positionMs <= 0L) return "0:00"
        val totalSeconds = positionMs / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}
