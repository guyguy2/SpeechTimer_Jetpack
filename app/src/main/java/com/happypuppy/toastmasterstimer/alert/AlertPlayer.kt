package com.happypuppy.toastmasterstimer.alert

import com.happypuppy.toastmasterstimer.util.TimerPhase

/** Vibration patterns the timer can request. */
enum class AlertVibration {
    /** Single short pulse for GREEN and YELLOW. */
    PHASE_CHANGE,

    /** Double pulse for RED. */
    RED_PHASE
}

/**
 * Plays the haptic and audible cues for the timer. Implementations own platform details so the
 * decision logic in [dispatchPhaseAlert] can be unit tested on the JVM with a fake.
 */
interface AlertPlayer {
    fun vibrate(vibration: AlertVibration)

    /** Plays the alert sound, stopping any sound this player started earlier. */
    fun playSound()

    /** Stops any sound this player started. */
    fun stopSound()
}

/** Fires the cues for one phase change according to the user's vibration and sound settings. */
fun dispatchPhaseAlert(
    player: AlertPlayer,
    phase: TimerPhase,
    vibrationEnabled: Boolean,
    soundEnabled: Boolean
) {
    if (vibrationEnabled) {
        player.vibrate(
            if (phase == TimerPhase.RED) AlertVibration.RED_PHASE else AlertVibration.PHASE_CHANGE
        )
    }
    if (soundEnabled) {
        player.playSound()
    }
}
