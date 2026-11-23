package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.util.TimerPhase
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
 * phase skipping, and input coercion in `configure`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelEdgeCasesTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val created = mutableListOf<TimerViewModel>()

    private fun newVm(clock: () -> Long): TimerViewModel =
        TimerViewModel(elapsedRealtimeMs = clock).also { created += it }

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

    @Test
    fun startWhileRunning_isANoOpAndDoesNotRestartTheClock() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })
        vm.configure(1, 2)

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
        vm.configure(1, 2)

        vm.pause()

        assertFalse(vm.uiState.value.isRunning)
        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.BEFORE_GREEN, vm.uiState.value.phase)
    }

    @Test
    fun toggleRunning_startsThenPausesAndKeepsElapsed() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })
        vm.configure(1, 2)

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
        vm.configure(1, 2)

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
        vm.configure(1, 2)
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
        vm.configure(1, 2)
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
    fun jumpStraightToRed_skipsEarlierPhaseAlerts() = runTimerTest {
        // Characterization: a long stall (Doze, process freeze) only alerts for the phase it
        // wakes up in. Pinning this so any change to "catch-up" alerts is a deliberate decision.
        var nowMs = 0L
        val vm = newVm({ nowMs })
        vm.configure(1, 2)
        val (alerts, job) = collectAlerts(vm)

        vm.start()
        nowMs = 200_000L
        vm.syncElapsed()

        assertEquals(listOf(TimerPhase.RED), alerts)
        assertEquals(TimerPhase.RED, vm.uiState.value.phase)

        vm.reset()
        job.cancel()
    }

    @Test
    fun syncElapsedWhilePaused_doesNotAdvanceTime() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })
        vm.configure(1, 2)

        vm.start()
        nowMs = 4_000L
        vm.pause()
        nowMs = 500_000L
        vm.syncElapsed()

        assertEquals(4, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun pausedElapsedSurvivesReconfigureWithIdenticalThresholds() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })
        vm.configure(1, 2)

        vm.start()
        nowMs = 42_000L
        vm.pause()
        vm.configure(1, 2)

        assertEquals(42, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun configure_coercesNegativeAndInvertedInput() = runTimerTest {
        val vm = newVm({ 0L })

        vm.configure(minMinutes = -3, maxMinutes = -1)
        assertEquals(0, vm.uiState.value.thresholds.minMinutes)
        assertEquals(0, vm.uiState.value.thresholds.maxMinutes)

        vm.configure(minMinutes = 5, maxMinutes = 2)
        assertEquals(5, vm.uiState.value.thresholds.minMinutes)
        assertEquals(5, vm.uiState.value.thresholds.maxMinutes)
    }

    @Test
    fun resetWhilePaused_clearsElapsedAndAllowsAFreshRun() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })
        vm.configure(1, 2)

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
    fun uiVisibilityToggle_survivesResetAndReconfigure() = runTimerTest {
        val vm = newVm({ 0L })
        vm.configure(1, 2)

        vm.toggleUiVisibility()
        vm.reset()
        vm.configure(5, 7)

        // Characterization: hiding the controls is a view preference, not timer state.
        assertFalse(vm.uiState.value.isUiVisible)
    }
}
