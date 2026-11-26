package com.happypuppy.toastmasterstimer.util

import androidx.compose.runtime.Immutable

/**
 * Validated custom speech duration range where 1 <= [minMinutes] <= 59 and
 * [minMinutes] < [maxMinutes] <= 60.
 */
@Immutable
data class CustomRange(
    val minMinutes: Int,
    val maxMinutes: Int
)

/**
 * Pure range-clamping helpers for custom speech durations.
 * Guarantees 1 <= startMinutes <= 59 and startMinutes < endMinutes <= 60 so
 * phase thresholds (green -> yellow -> red) are always distinct and valid.
 */
object CustomSpeechRange {
    const val MIN_START_MINUTES = 1
    const val MAX_START_MINUTES = 59
    const val MAX_END_MINUTES = 60
    const val DEFAULT_MIN_MINUTES = 8
    const val DEFAULT_MAX_MINUTES = 10

    val defaultRange: CustomRange = CustomRange(DEFAULT_MIN_MINUTES, DEFAULT_MAX_MINUTES)

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

    fun coerceRange(minMinutes: Int, maxMinutes: Int): CustomRange {
        val (start, end) = updateStart(newStart = minMinutes, currentEnd = maxMinutes)
        return CustomRange(minMinutes = start, maxMinutes = end)
    }
}
