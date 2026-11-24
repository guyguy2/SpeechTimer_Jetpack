package com.happypuppy.toastmasterstimer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Characterization tests that tie [CustomSpeechRange] to [TimerThresholds] and pin the
 * Toastmasters presets, so later refactors (typed routes, a SpeechType model, etc.)
 * cannot silently change the green/yellow/red contract.
 */
class TimerContractTest {

    private data class Preset(val min: Int, val max: Int, val green: Int, val yellow: Int, val red: Int)

    private val presets = listOf(
        Preset(min = 1, max = 2, green = 60, yellow = 90, red = 120), // Table Topics
        Preset(min = 2, max = 3, green = 120, yellow = 150, red = 180), // Evaluation
        Preset(min = 4, max = 6, green = 240, yellow = 300, red = 360), // Ice Breaker
        Preset(min = 5, max = 7, green = 300, yellow = 360, red = 420) // Speech
    )

    @Test
    fun presets_haveExpectedThresholdsInSeconds() {
        presets.forEach { p ->
            val t = TimerThresholds(p.min, p.max)
            assertEquals("green ${p.min}-${p.max}", p.green, t.greenSeconds)
            assertEquals("yellow ${p.min}-${p.max}", p.yellow, t.yellowSeconds)
            assertEquals("red ${p.min}-${p.max}", p.red, t.redSeconds)
            assertEquals("bar ${p.min}-${p.max}", p.red + 2, t.maxDurationSeconds)
        }
    }

    @Test
    fun everyValidCustomRange_producesStrictlyIncreasingThresholds() {
        for (start in CustomSpeechRange.startRange) {
            for (end in CustomSpeechRange.endRange(start)) {
                val t = TimerThresholds(start, end)
                assertTrue("green<yellow for $start-$end", t.greenSeconds < t.yellowSeconds)
                assertTrue("yellow<red for $start-$end", t.yellowSeconds < t.redSeconds)
            }
        }
    }

    @Test
    fun everyValidCustomRange_switchesPhaseExactlyOnEachBoundary() {
        for (start in CustomSpeechRange.startRange) {
            for (end in CustomSpeechRange.endRange(start)) {
                val t = TimerThresholds(start, end)
                val label = "$start-$end"
                assertEquals(label, TimerPhase.BEFORE_GREEN, t.phaseAt(t.greenSeconds - 1))
                assertEquals(label, TimerPhase.GREEN, t.phaseAt(t.greenSeconds))
                assertEquals(label, TimerPhase.GREEN, t.phaseAt(t.yellowSeconds - 1))
                assertEquals(label, TimerPhase.YELLOW, t.phaseAt(t.yellowSeconds))
                assertEquals(label, TimerPhase.YELLOW, t.phaseAt(t.redSeconds - 1))
                assertEquals(label, TimerPhase.RED, t.phaseAt(t.redSeconds))
            }
        }
    }

    @Test
    fun phaseAt_isMonotonicOverTimeForEveryPreset() {
        presets.forEach { p ->
            val t = TimerThresholds(p.min, p.max)
            var previous = TimerPhase.BEFORE_GREEN
            for (second in 0..(t.redSeconds + 30)) {
                val phase = t.phaseAt(second)
                assertTrue(
                    "phase went backwards at ${second}s for ${p.min}-${p.max}",
                    phase.ordinal >= previous.ordinal
                )
                previous = phase
            }
            assertEquals(TimerPhase.RED, previous)
        }
    }

    @Test
    fun phaseAt_negativeElapsedIsBeforeGreen() {
        assertEquals(TimerPhase.BEFORE_GREEN, TimerThresholds(1, 2).phaseAt(-1))
        assertEquals(TimerPhase.BEFORE_GREEN, TimerThresholds(1, 2).phaseAt(Int.MIN_VALUE))
    }

    @Test
    fun extremeCustomRange_fiftyNineToSixty_hasExpectedBounds() {
        val t = TimerThresholds(CustomSpeechRange.MAX_START_MINUTES, CustomSpeechRange.MAX_END_MINUTES)
        assertEquals(3540, t.greenSeconds)
        assertEquals(3570, t.yellowSeconds)
        assertEquals(3600, t.redSeconds)
        assertEquals(3602, t.maxDurationSeconds)
    }

    @Test
    fun updateStartAndEnd_alwaysReturnAValidRangeForWildInput() {
        for (a in -5..70) {
            for (b in -5..70) {
                val (s1, e1) = CustomSpeechRange.updateStart(newStart = a, currentEnd = b)
                assertValid("updateStart($a,$b)", s1, e1)
                val (s2, e2) = CustomSpeechRange.updateEnd(currentStart = a, newEnd = b)
                assertValid("updateEnd($a,$b)", s2, e2)
            }
        }
    }

    @Test
    fun updates_areIdempotent() {
        for (a in -5..70) {
            for (b in -5..70) {
                val once = CustomSpeechRange.updateStart(a, b)
                assertEquals(once, CustomSpeechRange.updateStart(once.first, once.second))
                val onceEnd = CustomSpeechRange.updateEnd(a, b)
                assertEquals(onceEnd, CustomSpeechRange.updateEnd(onceEnd.first, onceEnd.second))
            }
        }
    }

    @Test
    fun endRange_matchesWhatUpdateStartAcceptsAsEnd() {
        for (start in CustomSpeechRange.startRange) {
            val range = CustomSpeechRange.endRange(start)
            assertEquals(start + 1, range.first)
            assertEquals(CustomSpeechRange.MAX_END_MINUTES, range.last)
            // Every end the picker can offer survives updateEnd unchanged.
            for (end in range) {
                assertEquals(start to end, CustomSpeechRange.updateEnd(start, end))
            }
        }
    }

    @Test
    fun formatTime_matchesTimerRangeAtBoundaries() {
        assertEquals("59:59", formatTime(59 * 60 + 59))
        assertEquals("60:00", formatTime(TimerThresholds(59, 60).redSeconds))
        // Int.MAX_VALUE must not overflow or throw.
        assertEquals("35791394:07", formatTime(Int.MAX_VALUE))
    }

    private fun assertValid(label: String, start: Int, end: Int) {
        assertTrue("$label start>=1 ($start)", start >= CustomSpeechRange.MIN_START_MINUTES)
        assertTrue("$label start<=59 ($start)", start <= CustomSpeechRange.MAX_START_MINUTES)
        assertTrue("$label end>start ($start,$end)", end > start)
        assertTrue("$label end<=60 ($end)", end <= CustomSpeechRange.MAX_END_MINUTES)
    }
}
