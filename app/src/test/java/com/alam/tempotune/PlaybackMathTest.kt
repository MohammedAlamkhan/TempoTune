package com.alam.tempotune

import com.alam.tempotune.audio.PlaybackMath
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackMathTest {

    @Test
    fun tempoPercentMapsToSpeed() {
        assertEquals(0.5f, PlaybackMath.tempoPercentToSpeed(50), 0.0001f)
        assertEquals(1.0f, PlaybackMath.tempoPercentToSpeed(100), 0.0001f)
        assertEquals(2.0f, PlaybackMath.tempoPercentToSpeed(200), 0.0001f)
    }

    @Test
    fun tempoIsClampedToSupportedRange() {
        assertEquals(0.5f, PlaybackMath.tempoPercentToSpeed(1), 0.0001f)
        assertEquals(2.0f, PlaybackMath.tempoPercentToSpeed(999), 0.0001f)
    }

    @Test
    fun oneOctaveIsTwelveSemitones() {
        assertEquals(0.5f, PlaybackMath.semitonesToPitch(-12), 0.0001f)
        assertEquals(1.0f, PlaybackMath.semitonesToPitch(0), 0.0001f)
        assertEquals(2.0f, PlaybackMath.semitonesToPitch(12), 0.0001f)
    }

    @Test
    fun semitoneRatiosFollowEqualTemperament() {
        assertEquals(1.059463f, PlaybackMath.semitonesToPitch(1), 0.00001f)
        assertEquals(1.189207f, PlaybackMath.semitonesToPitch(3), 0.00001f)
        assertEquals(0.840896f, PlaybackMath.semitonesToPitch(-3), 0.00001f)
    }

    @Test
    fun semitonesAreClampedToSupportedRange() {
        assertEquals(0.5f, PlaybackMath.semitonesToPitch(-99), 0.0001f)
        assertEquals(2.0f, PlaybackMath.semitonesToPitch(99), 0.0001f)
    }

    @Test
    fun timeFormattingHandlesMinutesAndHours() {
        assertEquals("0:00", PlaybackMath.formatTime(0L))
        assertEquals("0:05", PlaybackMath.formatTime(5_000L))
        assertEquals("1:05", PlaybackMath.formatTime(65_000L))
        assertEquals("1:01:01", PlaybackMath.formatTime(3_661_000L))
    }
}
