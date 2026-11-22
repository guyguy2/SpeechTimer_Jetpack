package com.happypuppy.toastmasterstimer.ui.screens

import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.util.TimerPhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @Test
    fun initialState_isZeroPausedBeforeGreenAndUiVisible() {
        val vm = TimerViewModel(elapsedRealtimeMs = { 0L })
        val state = vm.uiState.value

        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isRunning)
        assertEquals(TimerPhase.BEFORE_GREEN, state.phase)
        assertTrue(state.isUiVisible)
    }

    @Test
    fun start_advancesMonotonicElapsedTimeAndTransitionsPhases() = runTest(testDispatcher) {
        val vm = TimerViewModel(elapsedRealtimeMs = { testScheduler.currentTime })
        vm.configure(minMinutes = 1, maxMinutes = 2)

        vm.start()
        assertTrue(vm.uiState.value.isRunning)

        advanceTimeBy(59_000L)
        runCurrent()
        assertEquals(59, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.BEFORE_GREEN, vm.uiState.value.phase)

        advanceTimeBy(1_000L)
        runCurrent()
        assertEquals(60, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.GREEN, vm.uiState.value.phase)

        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(90, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.YELLOW, vm.uiState.value.phase)

        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(120, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.RED, vm.uiState.value.phase)

        vm.reset()
    }

    @Test
    fun pauseAndResume_preservesSubSecondProgressWithoutDrift() = runTest(testDispatcher) {
        var nowMs = 0L
        val vm = TimerViewModel(elapsedRealtimeMs = { nowMs })
        vm.configure(minMinutes = 1, maxMinutes = 2)

        vm.start()
        nowMs = 1_700L
        advanceTimeBy(1_700L)
        runCurrent()
        assertEquals(1, vm.uiState.value.elapsedSeconds)

        vm.pause()
        assertFalse(vm.uiState.value.isRunning)
        assertEquals(1, vm.uiState.value.elapsedSeconds)

        // Wall clock advances 10 seconds while paused; elapsed time must not change
        nowMs += 10_000L
        advanceTimeBy(10_000L)
        runCurrent()
        assertEquals(1, vm.uiState.value.elapsedSeconds)

        // Resume: only 300ms left until crossing 2.0s
        vm.start()
        nowMs += 299L
        advanceTimeBy(299L)
        runCurrent()
        assertEquals(1, vm.uiState.value.elapsedSeconds)

        nowMs += 1L
        advanceTimeBy(1L)
        runCurrent()
        assertEquals(2, vm.uiState.value.elapsedSeconds)

        vm.reset()
    }

    @Test
    fun phaseAlerts_fireOncePerPhaseTransitionWithoutDuplicates() = runTest(testDispatcher) {
        val vm = TimerViewModel(elapsedRealtimeMs = { testScheduler.currentTime })
        vm.configure(minMinutes = 1, maxMinutes = 2)

        val alerts = mutableListOf<TimerPhase>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.phaseAlerts.collect { alerts.add(it) }
        }

        vm.start()
        advanceTimeBy(180_000L)
        runCurrent()

        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW, TimerPhase.RED), alerts)

        vm.reset()
        collectJob.cancel()
    }

    @Test
    fun reset_clearsPhaseStateSoAlertsFireAgainOnNextRun() = runTest(testDispatcher) {
        val vm = TimerViewModel(elapsedRealtimeMs = { testScheduler.currentTime })
        vm.configure(minMinutes = 1, maxMinutes = 2)

        val alerts = mutableListOf<TimerPhase>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.phaseAlerts.collect { alerts.add(it) }
        }

        // First speech: reaches RED
        vm.start()
        advanceTimeBy(125_000L)
        runCurrent()
        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW, TimerPhase.RED), alerts)

        // Reset and run second speech
        vm.reset()
        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRunning)
        assertEquals(TimerPhase.BEFORE_GREEN, vm.uiState.value.phase)

        vm.start()
        advanceTimeBy(125_000L)
        runCurrent()

        assertEquals(
            listOf(
                TimerPhase.GREEN,
                TimerPhase.YELLOW,
                TimerPhase.RED,
                TimerPhase.GREEN,
                TimerPhase.YELLOW,
                TimerPhase.RED
            ),
            alerts
        )

        vm.reset()
        collectJob.cancel()
    }

    @Test
    fun resetWhileRunning_stopsTickingAndResetsState() = runTest(testDispatcher) {
        val vm = TimerViewModel(elapsedRealtimeMs = { testScheduler.currentTime })
        vm.configure(minMinutes = 1, maxMinutes = 2)

        vm.start()
        advanceTimeBy(65_000L)
        runCurrent()
        assertEquals(65, vm.uiState.value.elapsedSeconds)

        vm.reset()
        advanceTimeBy(10_000L)
        runCurrent()

        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRunning)
        assertEquals(TimerPhase.BEFORE_GREEN, vm.uiState.value.phase)
    }

    @Test
    fun configureWithSameThresholds_preservesRunningTimerAcrossRotation() = runTest(testDispatcher) {
        val vm = TimerViewModel(elapsedRealtimeMs = { testScheduler.currentTime })
        vm.configure(minMinutes = 1, maxMinutes = 2)

        vm.start()
        advanceTimeBy(45_000L)
        runCurrent()
        assertEquals(45, vm.uiState.value.elapsedSeconds)

        // Re-configuring with identical values simulates rotation/recomposition
        vm.configure(minMinutes = 1, maxMinutes = 2)

        assertTrue(vm.uiState.value.isRunning)
        assertEquals(45, vm.uiState.value.elapsedSeconds)

        vm.reset()
    }

    @Test
    fun configureWithDifferentThresholds_resetsTimer() = runTest(testDispatcher) {
        val vm = TimerViewModel(elapsedRealtimeMs = { testScheduler.currentTime })
        vm.configure(minMinutes = 1, maxMinutes = 2)

        vm.start()
        advanceTimeBy(30_000L)
        runCurrent()

        vm.configure(minMinutes = 5, maxMinutes = 7)

        assertFalse(vm.uiState.value.isRunning)
        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertEquals(300, vm.uiState.value.thresholds.greenSeconds)
        assertEquals(420, vm.uiState.value.thresholds.redSeconds)
    }

    @Test
    fun syncElapsed_catchesUpAfterDozeOrBackgroundJump() = runTest(testDispatcher) {
        var nowMs = 0L
        val vm = TimerViewModel(elapsedRealtimeMs = { nowMs })
        vm.configure(minMinutes = 1, maxMinutes = 2)

        val alerts = mutableListOf<TimerPhase>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.phaseAlerts.collect { alerts.add(it) }
        }

        vm.start()
        // Simulate process wakeup after 95 seconds of monotonic wall clock
        nowMs = 95_000L
        vm.syncElapsed()

        assertEquals(95, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.YELLOW, vm.uiState.value.phase)
        assertEquals(listOf(TimerPhase.YELLOW), alerts)

        vm.reset()
        collectJob.cancel()
    }

    @Test
    fun zeroMinuteDuration_handlesEdgeCaseAndAlertsAgainAfterReset() = runTest(testDispatcher) {
        val vm = TimerViewModel(elapsedRealtimeMs = { testScheduler.currentTime })
        vm.configure(minMinutes = 0, maxMinutes = 0)

        val alerts = mutableListOf<TimerPhase>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.phaseAlerts.collect { alerts.add(it) }
        }

        vm.start()
        assertEquals(TimerPhase.RED, vm.uiState.value.phase)
        assertEquals(listOf(TimerPhase.RED), alerts)

        vm.reset()
        vm.start()
        assertEquals(listOf(TimerPhase.RED, TimerPhase.RED), alerts)

        vm.reset()
        collectJob.cancel()
    }

    @Test
    fun toggleUiVisibility_flipsVisibilityFlag() {
        val vm = TimerViewModel(elapsedRealtimeMs = { 0L })
        assertTrue(vm.uiState.value.isUiVisible)

        vm.toggleUiVisibility()
        assertFalse(vm.uiState.value.isUiVisible)

        vm.toggleUiVisibility()
        assertTrue(vm.uiState.value.isUiVisible)
    }
}
