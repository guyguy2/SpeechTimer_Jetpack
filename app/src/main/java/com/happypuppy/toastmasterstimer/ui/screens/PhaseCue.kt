package com.happypuppy.toastmasterstimer.ui.screens

import androidx.annotation.StringRes
import com.happypuppy.toastmasterstimer.R
import com.happypuppy.toastmasterstimer.util.TimerPhase

/** Which icon a phase shows next to its label. Each key is a different shape, not only a different color. */
enum class PhaseIcon { HOURGLASS, CHECK, WARNING, STOP }

/** What the timer shows and says for one phase, as resource ids so the mapping stays JVM-testable. */
data class PhaseCue(
    @StringRes val labelRes: Int,
    @StringRes val spokenRes: Int,
    val icon: PhaseIcon
)

fun phaseCue(phase: TimerPhase): PhaseCue = when (phase) {
    TimerPhase.BEFORE_GREEN ->
        PhaseCue(R.string.phase_label_before_green, R.string.phase_spoken_before_green, PhaseIcon.HOURGLASS)
    TimerPhase.GREEN ->
        PhaseCue(R.string.phase_label_green, R.string.phase_spoken_green, PhaseIcon.CHECK)
    TimerPhase.YELLOW ->
        PhaseCue(R.string.phase_label_yellow, R.string.phase_spoken_yellow, PhaseIcon.WARNING)
    TimerPhase.RED ->
        PhaseCue(R.string.phase_label_red, R.string.phase_spoken_red, PhaseIcon.STOP)
}

/**
 * Spoken form of an elapsed time such as "1 minute 4 seconds". Zero parts are left out ("1 minute"),
 * except that 0 seconds is spoken as "0 seconds". [minutesText] and [secondsText] supply the
 * localized, pluralized parts so this stays pure.
 */
fun spokenDuration(
    totalSeconds: Int,
    minutesText: (Int) -> String,
    secondsText: (Int) -> String
): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val minutes = safe / 60
    val seconds = safe % 60
    return buildList {
        if (minutes > 0) add(minutesText(minutes))
        if (seconds > 0 || minutes == 0) add(secondsText(seconds))
    }.joinToString(" ")
}
