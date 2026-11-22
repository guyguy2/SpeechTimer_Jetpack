package com.happypuppy.toastmasterstimer.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormatTimeTest {
    @Test
    fun zeroSeconds_formatsAsZeroMinutesZeroSeconds() {
        assertEquals("00:00", formatTime(0))
    }

    @Test
    fun negativeSeconds_clampsToZero() {
        assertEquals("00:00", formatTime(-10))
    }

    @Test
    fun underOneMinute_padsSecondsToTwoDigits() {
        assertEquals("00:05", formatTime(5))
        assertEquals("00:59", formatTime(59))
    }

    @Test
    fun exactMinutes_haveZeroSeconds() {
        assertEquals("01:00", formatTime(60))
        assertEquals("07:00", formatTime(420))
    }

    @Test
    fun minutesAndSeconds_areSplitCorrectly() {
        assertEquals("02:05", formatTime(125))
        assertEquals("04:23", formatTime(263))
    }

    @Test
    fun overOneHour_keepsCountingMinutesWithoutRollingOver() {
        assertEquals("60:00", formatTime(3600))
        assertEquals("61:01", formatTime(3661))
    }

    @Test
    fun nonUsDefaultLocale_alwaysProducesAsciiDigits() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar-EG"))
            assertEquals("00:00", formatTime(0))
            assertEquals("05:30", formatTime(330))
            assertEquals("12:09", formatTime(729))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
