package com.happypuppy.toastmasterstimer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerThresholdsTest {

    private val tableTopics = TimerThresholds(minMinutes = 1, maxMinutes = 2)
    private val speech = TimerThresholds(minMinutes = 5, maxMinutes = 7)

    @Test
    fun thresholds_areDerivedFromMinutes() {
        assertEquals(60, tableTopics.greenSeconds)
        assertEquals(90, tableTopics.yellowSeconds)
        assertEquals(120, tableTopics.redSeconds)
        assertEquals(122, tableTopics.maxDurationSeconds)

        assertEquals(300, speech.greenSeconds)
        assertEquals(360, speech.yellowSeconds)
        assertEquals(420, speech.redSeconds)
        assertEquals(422, speech.maxDurationSeconds)
    }

    @Test
    fun phaseAt_beforeMinimumTime_isBeforeGreen() {
        assertEquals(TimerPhase.BEFORE_GREEN, tableTopics.phaseAt(0))
        assertEquals(TimerPhase.BEFORE_GREEN, tableTopics.phaseAt(59))
    }

    @Test
    fun phaseAt_switchesExactlyOnEachThreshold() {
        assertEquals(TimerPhase.GREEN, tableTopics.phaseAt(60))
        assertEquals(TimerPhase.GREEN, tableTopics.phaseAt(89))
        assertEquals(TimerPhase.YELLOW, tableTopics.phaseAt(90))
        assertEquals(TimerPhase.YELLOW, tableTopics.phaseAt(119))
        assertEquals(TimerPhase.RED, tableTopics.phaseAt(120))
    }

    @Test
    fun phaseAt_staysRedPastMaximum() {
        assertEquals(TimerPhase.RED, tableTopics.phaseAt(121))
        assertEquals(TimerPhase.RED, tableTopics.phaseAt(3600))
    }

    @Test
    fun equalMinAndMax_skipsGreenAndYellow() {
        val fixed = TimerThresholds(minMinutes = 3, maxMinutes = 3)

        assertEquals(180, fixed.greenSeconds)
        assertEquals(180, fixed.yellowSeconds)
        assertEquals(180, fixed.redSeconds)
        assertEquals(TimerPhase.BEFORE_GREEN, fixed.phaseAt(179))
        assertEquals(TimerPhase.RED, fixed.phaseAt(180))
    }
}
