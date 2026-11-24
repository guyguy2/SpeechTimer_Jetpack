package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.util.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Test

class PhaseCueTest {

    private fun minutes(n: Int) = if (n == 1) "1 minute" else "$n minutes"
    private fun seconds(n: Int) = if (n == 1) "1 second" else "$n seconds"
    private fun spoken(totalSeconds: Int) = spokenDuration(totalSeconds, ::minutes, ::seconds)

    @Test
    fun everyPhaseHasItsOwnLabelSpokenTextAndIcon() {
        val cues = TimerPhase.entries.map { phaseCue(it) }
        assertEquals(TimerPhase.entries.size, cues.map { it.labelRes }.toSet().size)
        assertEquals(TimerPhase.entries.size, cues.map { it.spokenRes }.toSet().size)
        assertEquals(TimerPhase.entries.size, cues.map { it.icon }.toSet().size)
    }

    @Test
    fun phasesMapToTheirStrings() {
        assertEquals(R.string.phase_label_before_green, phaseCue(TimerPhase.BEFORE_GREEN).labelRes)
        assertEquals(R.string.phase_label_green, phaseCue(TimerPhase.GREEN).labelRes)
        assertEquals(R.string.phase_label_yellow, phaseCue(TimerPhase.YELLOW).labelRes)
        assertEquals(R.string.phase_label_red, phaseCue(TimerPhase.RED).labelRes)
        assertEquals(R.string.phase_spoken_before_green, phaseCue(TimerPhase.BEFORE_GREEN).spokenRes)
        assertEquals(R.string.phase_spoken_green, phaseCue(TimerPhase.GREEN).spokenRes)
        assertEquals(R.string.phase_spoken_yellow, phaseCue(TimerPhase.YELLOW).spokenRes)
        assertEquals(R.string.phase_spoken_red, phaseCue(TimerPhase.RED).spokenRes)
    }

    @Test
    fun beforeGreenIsNeutralAndTheColoredPhasesHaveDistinctShapes() {
        assertEquals(PhaseIcon.HOURGLASS, phaseCue(TimerPhase.BEFORE_GREEN).icon)
        assertEquals(PhaseIcon.CHECK, phaseCue(TimerPhase.GREEN).icon)
        assertEquals(PhaseIcon.WARNING, phaseCue(TimerPhase.YELLOW).icon)
        assertEquals(PhaseIcon.STOP, phaseCue(TimerPhase.RED).icon)
    }

    @Test
    fun spokenDuration_minutesAndSeconds() {
        assertEquals("1 minute 4 seconds", spoken(64))
        assertEquals("2 minutes 5 seconds", spoken(125))
        assertEquals("1 minute 1 second", spoken(61))
    }

    @Test
    fun spokenDuration_leavesOutZeroParts() {
        assertEquals("1 minute", spoken(60))
        assertEquals("5 minutes", spoken(300))
        assertEquals("59 seconds", spoken(59))
        assertEquals("1 second", spoken(1))
    }

    @Test
    fun spokenDuration_zeroAndNegativeAreZeroSeconds() {
        assertEquals("0 seconds", spoken(0))
        assertEquals("0 seconds", spoken(-5))
    }
}
