package com.happypuppy.toastmasterstimer.util

/** Which colour band the timer is in for a given elapsed time. */
enum class TimerPhase { BEFORE_GREEN, GREEN, YELLOW, RED }

/**
 * Pure threshold arithmetic for a speech timed against [minMinutes]..[maxMinutes].
 * Green starts at the minimum time, red at the maximum, yellow halfway between.
 */
data class TimerThresholds(val minMinutes: Int, val maxMinutes: Int) {
    val greenSeconds: Int = minMinutes * 60
    val redSeconds: Int = maxMinutes * 60
    val yellowSeconds: Int = (greenSeconds + redSeconds) / 2

    /** Full-scale value for the progress bar: two seconds past red. */
    val maxDurationSeconds: Int = redSeconds + 2

    fun phaseAt(elapsedSeconds: Int): TimerPhase = when {
        elapsedSeconds < greenSeconds -> TimerPhase.BEFORE_GREEN
        elapsedSeconds < yellowSeconds -> TimerPhase.GREEN
        elapsedSeconds < redSeconds -> TimerPhase.YELLOW
        else -> TimerPhase.RED
    }
}
