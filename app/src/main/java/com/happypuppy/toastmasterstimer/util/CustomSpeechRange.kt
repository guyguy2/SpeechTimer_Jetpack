package com.happypuppy.toastmasterstimer.util

/**
 * Pure range-clamping helpers for custom speech durations.
 * Guarantees 1 <= startMinutes <= 59 and startMinutes < endMinutes <= 60 so
 * phase thresholds (green -> yellow -> red) are always distinct and valid.
 */
object CustomSpeechRange {
    const val MIN_START_MINUTES = 1
    const val MAX_START_MINUTES = 59
    const val MAX_END_MINUTES = 60

    val startRange: IntRange = MIN_START_MINUTES..MAX_START_MINUTES

    fun endRange(startMinutes: Int): IntRange {
        val clampedStart = startMinutes.coerceIn(MIN_START_MINUTES, MAX_START_MINUTES)
        return (clampedStart + 1)..MAX_END_MINUTES
    }

    fun updateStart(newStart: Int, currentEnd: Int): Pair<Int, Int> {
        val start = newStart.coerceIn(MIN_START_MINUTES, MAX_START_MINUTES)
        val end = currentEnd.coerceIn(start + 1, MAX_END_MINUTES)
        return start to end
    }

    fun updateEnd(currentStart: Int, newEnd: Int): Pair<Int, Int> {
        val start = currentStart.coerceIn(MIN_START_MINUTES, MAX_START_MINUTES)
        val end = newEnd.coerceIn(start + 1, MAX_END_MINUTES)
        return start to end
    }
}
