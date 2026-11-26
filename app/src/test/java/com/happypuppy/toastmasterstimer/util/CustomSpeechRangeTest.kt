package com.happypuppy.toastmasterstimer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomSpeechRangeTest {

    @Test
    fun startRange_isOneThroughFiftyNine() {
        assertEquals(1..59, CustomSpeechRange.startRange)
    }

    @Test
    fun endRange_isNeverEmptyEvenAtMaxStart() {
        val maxEndRange = CustomSpeechRange.endRange(59)
        assertFalse(maxEndRange.isEmpty())
        assertEquals(60..60, maxEndRange)

        // Out-of-bounds start still produces a valid non-empty range
        val clampedEndRange = CustomSpeechRange.endRange(60)
        assertFalse(clampedEndRange.isEmpty())
        assertEquals(60..60, clampedEndRange)
    }

    @Test
    fun updateStart_pushesEndUpToMaintainStrictInequality() {
        val (start, end) = CustomSpeechRange.updateStart(newStart = 10, currentEnd = 10)
        assertEquals(10, start)
        assertEquals(11, end)
        assertTrue(end > start)
    }

    @Test
    fun updateStart_clampsOutOfBoundsValues() {
        assertEquals(1 to 5, CustomSpeechRange.updateStart(newStart = 0, currentEnd = 5))
        assertEquals(59 to 60, CustomSpeechRange.updateStart(newStart = 60, currentEnd = 60))
        assertEquals(59 to 60, CustomSpeechRange.updateStart(newStart = 99, currentEnd = 10))
    }

    @Test
    fun updateEnd_clampsToStrictlyGreaterThanStartAndMaxSixty() {
        assertEquals(8 to 9, CustomSpeechRange.updateEnd(currentStart = 8, newEnd = 8))
        assertEquals(8 to 9, CustomSpeechRange.updateEnd(currentStart = 8, newEnd = 3))
        assertEquals(8 to 60, CustomSpeechRange.updateEnd(currentStart = 8, newEnd = 65))
        assertEquals(8 to 15, CustomSpeechRange.updateEnd(currentStart = 8, newEnd = 15))
    }

    @Test
    fun defaultRange_isEightToTenMinutes() {
        assertEquals(8, CustomSpeechRange.DEFAULT_MIN_MINUTES)
        assertEquals(10, CustomSpeechRange.DEFAULT_MAX_MINUTES)
        assertEquals(CustomRange(8, 10), CustomSpeechRange.defaultRange)
    }

    @Test
    fun coerceRange_normalizesValidEqualInvertedAndOutOfBoundsInputs() {
        assertEquals(CustomRange(3, 5), CustomSpeechRange.coerceRange(3, 5))
        assertEquals(CustomRange(10, 11), CustomSpeechRange.coerceRange(10, 10))
        assertEquals(CustomRange(15, 16), CustomSpeechRange.coerceRange(15, 10))
        assertEquals(CustomRange(1, 2), CustomSpeechRange.coerceRange(0, 0))
        assertEquals(CustomRange(59, 60), CustomSpeechRange.coerceRange(60, 60))
        assertEquals(CustomRange(12, 60), CustomSpeechRange.coerceRange(12, 90))
    }
}
