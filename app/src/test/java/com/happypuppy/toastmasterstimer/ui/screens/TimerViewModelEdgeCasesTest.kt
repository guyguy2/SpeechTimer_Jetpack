package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Edge cases around [TimerViewModel] that the main [TimerViewModelTest] does not pin:
 * idempotent start/pause, clock regressions, alert de-duplication across pause/resume,
 * phase skipping, and input coercion in constructor thresholds.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelEdgeCasesTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val created = mutableListOf<TimerViewModel>()

    private fun newVm(
        clock: () -> Long,
        thresholds: TimerThresholds? = TimerThresholds(1, 2)
    ): TimerViewModel = TimerViewModel(
        savedStateHandle = SavedStateHandle(),
        elapsedRealtimeMs = clock,
        initialThresholds = thresholds
    ).also { created += it }

    /**
     * Wraps runTest so every ViewModel created via [newVm] is reset in a finally block. Without
     * this, a failed assertion after start() leaves the unbounded tick loop running and runTest
     * would drain virtual time forever (hang instead of fail).
     */
    private fun runTimerTest(body: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            body()
        } finally {
            created.forEach { it.reset() }
            created.clear()
        }
    }

    private fun TestScope.collectAlerts(vm: TimerViewModel): Pair<MutableList<TimerPhase>, kotlinx.coroutines.Job> {
        val alerts = mutableListOf<TimerPhase>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.phaseAlerts.collect { alerts.add(it) }
        }
        return alerts to job
    }

    private fun TestScope.collectCatchUps(vm: TimerViewModel): Pair<MutableList<TimerPhase>, kotlinx.coroutines.Job> {
        val cues = mutableListOf<TimerPhase>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.catchUpCues.collect { cues.add(it) }
        }
        return cues to job
    }

    @Test
    fun startWhileRunning_isANoOpAndDoesNotRestartTheClock() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })

        vm.start()
        advanceTimeBy(5_000L)
        runCurrent()
        vm.start()
        advanceTimeBy(1_000L)
        runCurrent()

        assertTrue(vm.uiState.value.isRunning)
        assertEquals(6, vm.uiState.value.elapsedSeconds)

        vm.reset()
    }

    @Test
    fun pauseWhileIdle_isANoOp() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })

        vm.pause()

        assertFalse(vm.uiState.value.isRunning)
        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.BEFORE_GREEN, vm.uiState.value.phase)
    }

    @Test
    fun toggleRunning_startsThenPausesAndKeepsElapsed() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })

        vm.toggleRunning()
        assertTrue(vm.uiState.value.isRunning)
        advanceTimeBy(3_000L)
        runCurrent()

        vm.toggleRunning()
        assertFalse(vm.uiState.value.isRunning)
        assertEquals(3, vm.uiState.value.elapsedSeconds)

        vm.reset()
    }

    @Test
    fun clockGoingBackwards_neverProducesNegativeElapsed() = runTimerTest {
        var nowMs = 10_000L
        val vm = newVm({ nowMs })

        vm.start()
        nowMs = 5_000L
        vm.syncElapsed()

        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.BEFORE_GREEN, vm.uiState.value.phase)

        vm.reset()
    }

    @Test
    fun pauseAndResumeInsideAPhase_doesNotReAlertThatPhase() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })
        val (alerts, job) = collectAlerts(vm)

        vm.start()
        advanceTimeBy(95_000L) // GREEN at 60s, now inside YELLOW
        runCurrent()
        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW), alerts)

        vm.pause()
        advanceTimeBy(30_000L)
        runCurrent()
        vm.start()
        advanceTimeBy(10_000L)
        runCurrent()

        // Resumed still inside YELLOW (105s): no duplicate YELLOW alert.
        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW), alerts)

        advanceTimeBy(20_000L) // crosses red at 120s
        runCurrent()
        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW, TimerPhase.RED), alerts)

        vm.reset()
        job.cancel()
    }

    @Test
    fun runningPastRed_emitsRedOnlyOnce() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })
        val (alerts, job) = collectAlerts(vm)

        vm.start()
        advanceTimeBy(600_000L)
        runCurrent()

        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW, TimerPhase.RED), alerts)
        assertEquals(600, vm.uiState.value.elapsedSeconds)

        vm.reset()
        job.cancel()
    }

    @Test
    fun jumpStraightToRed_alertsOnlyTheLivePhaseAndCatchesUpOnReturn() = runTimerTest {
        // FLIPPED in Wave 1 (P0-1): this used to pin "a long stall only alerts for the phase it wakes
        // up in" with no catch-up. The live alert still skips GREEN and YELLOW (only the current
        // phase matters), but returning to the app now fires one catch-up cue for that phase.
        var nowMs = 0L
        val vm = newVm({ nowMs })
        val (alerts, job) = collectAlerts(vm)
        val (catchUps, catchUpJob) = collectCatchUps(vm)

        vm.start()
        vm.onAppBackgrounded()
        nowMs = 200_000L
        vm.syncElapsed()

        assertEquals(listOf(TimerPhase.RED), alerts)
        assertEquals(TimerPhase.RED, vm.uiState.value.phase)
        assertEquals(emptyList<TimerPhase>(), catchUps)

        // The user comes back well after the live alert (screen was off, process was alive).
        nowMs = 230_000L
        vm.onAppForegrounded()

        assertEquals(listOf(TimerPhase.RED), catchUps)
        assertEquals(listOf(TimerPhase.RED), alerts)

        vm.reset()
        job.cancel()
        catchUpJob.cancel()
    }

    @Test
    fun returnWithoutCrossingAPhase_hasNoCatchUpCue() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })
        val (catchUps, job) = collectCatchUps(vm)

        vm.start()
        nowMs = 10_000L
        vm.syncElapsed()
        vm.onAppBackgrounded()
        nowMs = 40_000L
        vm.onAppForegrounded()

        assertEquals(emptyList<TimerPhase>(), catchUps)

        vm.reset()
        job.cancel()
    }

    @Test
    fun catchUpCue_namesTheCurrentPhaseAndFiresOnlyOnce() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })
        val (catchUps, job) = collectCatchUps(vm)

        vm.start()
        vm.onAppBackgrounded()
        nowMs = 65_000L
        vm.syncElapsed() // crosses GREEN while away
        nowMs = 95_000L
        vm.syncElapsed() // crosses YELLOW while away
        nowMs = 99_000L // back a few seconds later, after the live alert
        vm.onAppForegrounded()
        vm.onAppForegrounded()

        assertEquals(listOf(TimerPhase.YELLOW), catchUps)

        vm.onAppBackgrounded()
        vm.onAppForegrounded()
        assertEquals(listOf(TimerPhase.YELLOW), catchUps)

        vm.reset()
        job.cancel()
    }

    @Test
    fun foregroundSyncThatCrossesAPhase_alertsOnceWithoutADuplicateCatchUp() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })
        val (alerts, job) = collectAlerts(vm)
        val (catchUps, catchUpJob) = collectCatchUps(vm)

        vm.start()
        vm.onAppBackgrounded()
        nowMs = 70_000L // no tick ran while away; the foreground sync discovers GREEN
        vm.onAppForegrounded()

        assertEquals(listOf(TimerPhase.GREEN), alerts)
        assertEquals(emptyList<TimerPhase>(), catchUps)

        vm.reset()
        job.cancel()
        catchUpJob.cancel()
    }

    @Test
    fun frozenProcessThawingOnReturn_cuesOnceNotTwice() = runTimerTest {
        // Seen on device: a cached, frozen process wakes up on return, its tick fires the live alert
        // an instant before ON_START, and the catch-up used to fire a second time.
        var nowMs = 0L
        val vm = newVm({ nowMs })
        val (alerts, job) = collectAlerts(vm)
        val (catchUps, catchUpJob) = collectCatchUps(vm)

        vm.start()
        vm.onAppBackgrounded()
        nowMs = 95_000L
        vm.syncElapsed() // the thawed tick
        vm.onAppForegrounded()

        assertEquals(listOf(TimerPhase.YELLOW), alerts)
        assertEquals(emptyList<TimerPhase>(), catchUps)

        vm.reset()
        job.cancel()
        catchUpJob.cancel()
    }

    @Test
    fun pausedTimer_hasNoCatchUpCueOnReturn() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })
        val (catchUps, job) = collectCatchUps(vm)

        vm.start()
        nowMs = 70_000L
        vm.syncElapsed()
        vm.pause()
        vm.onAppBackgrounded()
        nowMs = 500_000L
        vm.onAppForegrounded()

        assertEquals(emptyList<TimerPhase>(), catchUps)

        vm.reset()
        job.cancel()
    }

    @Test
    fun syncElapsedWhilePaused_doesNotAdvanceTime() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })

        vm.start()
        nowMs = 4_000L
        vm.pause()
        nowMs = 500_000L
        vm.syncElapsed()

        assertEquals(4, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun initialThresholds_coercesNegativeAndInvertedInput() = runTimerTest {
        val negative = newVm({ 0L }, thresholds = TimerThresholds(minMinutes = -3, maxMinutes = -1))
        assertEquals(0, negative.uiState.value.thresholds.minMinutes)
        assertEquals(0, negative.uiState.value.thresholds.maxMinutes)

        val inverted = newVm({ 0L }, thresholds = TimerThresholds(minMinutes = 5, maxMinutes = 2))
        assertEquals(5, inverted.uiState.value.thresholds.minMinutes)
        assertEquals(5, inverted.uiState.value.thresholds.maxMinutes)
    }

    @Test
    fun resetWhilePaused_clearsElapsedAndAllowsAFreshRun() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })

        vm.start()
        advanceTimeBy(10_000L)
        runCurrent()
        vm.pause()
        vm.reset()

        assertEquals(0, vm.uiState.value.elapsedSeconds)

        vm.start()
        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(2, vm.uiState.value.elapsedSeconds)

        vm.reset()
    }

    @Test
    fun uiVisibilityToggle_survivesReset() = runTimerTest {
        val vm = newVm({ 0L })

        vm.toggleUiVisibility()
        vm.reset()

        // Characterization: hiding the controls is a view preference, not timer state.
        assertFalse(vm.uiState.value.isUiVisible)
    }
}
