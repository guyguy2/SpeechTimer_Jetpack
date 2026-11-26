package com.happypuppy.toastmasterstimer.alert

import com.happypuppy.toastmasterstimer.util.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Characterizes what TimerScreen did for each phase alert before the AlertPlayer extraction:
 * a 300 ms pulse for GREEN and YELLOW, a double pulse for RED, each gated by its own setting,
 * and the sound path independent of the vibration path.
 */
class PhaseAlertDispatchTest {

    private fun dispatch(phase: TimerPhase, vibration: Boolean, sound: Boolean): List<String> {
        val player = FakeAlertPlayer()
        dispatchPhaseAlert(player, phase, vibration, sound)
        return player.calls
    }

    @Test
    fun green_vibratesShortPulse() {
        assertEquals(listOf("vibrate:PHASE_CHANGE"), dispatch(TimerPhase.GREEN, vibration = true, sound = false))
    }

    @Test
    fun yellow_vibratesShortPulse() {
        assertEquals(listOf("vibrate:PHASE_CHANGE"), dispatch(TimerPhase.YELLOW, vibration = true, sound = false))
    }

    @Test
    fun red_vibratesDoublePulse() {
        assertEquals(listOf("vibrate:RED_PHASE"), dispatch(TimerPhase.RED, vibration = true, sound = false))
    }

    @Test
    fun soundOnly_playsSoundWithoutVibrating() {
        assertEquals(listOf("playSound"), dispatch(TimerPhase.GREEN, vibration = false, sound = true))
    }

    @Test
    fun bothEnabled_vibratesThenPlaysSound() {
        assertEquals(
            listOf("vibrate:RED_PHASE", "playSound"),
            dispatch(TimerPhase.RED, vibration = true, sound = true)
        )
    }

    @Test
    fun bothDisabled_doesNothing() {
        assertEquals(emptyList<String>(), dispatch(TimerPhase.YELLOW, vibration = false, sound = false))
    }

    @Test
    fun fakeAlertPlayer_tracksIsSoundPlayingAcrossPlayAndStop() {
        val player = FakeAlertPlayer()
        assertEquals(false, player.isSoundPlaying.value)

        player.playSound()
        assertEquals(true, player.isSoundPlaying.value)

        player.stopSound()
        assertEquals(false, player.isSoundPlaying.value)
        assertEquals(listOf("playSound", "stopSound"), player.calls)
    }
}
