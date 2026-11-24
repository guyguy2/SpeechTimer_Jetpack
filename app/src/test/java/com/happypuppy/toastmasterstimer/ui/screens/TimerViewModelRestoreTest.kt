package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.util.TimerPhase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
 * Process-death restore (Wave 1, P0-2): the ViewModel mirrors its clock state into a
 * [SavedStateHandle] and rebuilds from it, plus the snapshot used by the Restart Undo.
 * Tests pre-fill a handle (what the system hands back after process death) and use the fake clock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelRestoreTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val created = mutableListOf<TimerViewModel>()

    private fun newVm(handle: SavedStateHandle = SavedStateHandle(), clock: () -> Long): TimerViewModel =
        TimerViewModel(savedStateHandle = handle, elapsedRealtimeMs = clock).also { created += it }

    /** Resets every ViewModel in a finally block so a failed assertion cannot hang the tick loop. */
    private fun runTimerTest(body: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            body()
        } finally {
            created.forEach { it.reset() }
            created.clear()
        }
    }

    private fun TestScope.collectCatchUps(vm: TimerViewModel): Pair<MutableList<TimerPhase>, Job> {
        val cues = mutableListOf<TimerPhase>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.catchUpCues.collect { cues.add(it) }
        }
        return cues to job
    }

    private fun savedHandle(
        min: Int = 1,
        max: Int = 2,
        accumulatedMs: Long = 0L,
        startedAtMs: Long = -1L,
        phase: String = TimerPhase.BEFORE_GREEN.name
    ) = SavedStateHandle(
        mapOf(
            "timer_min_minutes" to min,
            "timer_max_minutes" to max,
            "timer_accumulated_ms" to accumulatedMs,
            "timer_started_at_ms" to startedAtMs,
            "timer_phase" to phase
        )
    )

    @Test
    fun emptyHandle_startsFreshAndNotRestored() = runTimerTest {
        val vm = newVm { testScheduler.currentTime }

        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRunning)
        assertFalse(vm.uiState.value.isRestored)
    }

    @Test
    fun pausedHandle_restoresElapsedTimeAndMarksRestored() = runTimerTest {
        val vm = newVm(savedHandle(accumulatedMs = 45_500L)) { testScheduler.currentTime }

        assertEquals(45, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRunning)
        assertTrue(vm.uiState.value.isRestored)
        assertEquals(60, vm.uiState.value.thresholds.greenSeconds)
    }

    @Test
    fun restoredThresholds_makeTheScreensConfigureCallANoOp() = runTimerTest {
        val vm = newVm(savedHandle(accumulatedMs = 45_000L)) { testScheduler.currentTime }

        vm.configure(1, 2)

        assertEquals(45, vm.uiState.value.elapsedSeconds)
        assertTrue(vm.uiState.value.isRestored)
    }

    @Test
    fun restoredThenConfiguredWithDifferentThresholds_resets() = runTimerTest {
        val vm = newVm(savedHandle(accumulatedMs = 45_000L)) { testScheduler.currentTime }

        vm.configure(5, 7)

        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRestored)
        assertEquals(300, vm.uiState.value.thresholds.greenSeconds)
    }

    @Test
    fun runningHandle_keepsCountingFromTheOriginalStartAndKeepsTicking() = runTimerTest {
        // Process died at t=10 s on the monotonic clock with 5 s accumulated; it is now t=70 s.
        advanceTimeBy(70_000L)
        val vm = newVm(savedHandle(accumulatedMs = 5_000L, startedAtMs = 10_000L)) { testScheduler.currentTime }

        assertEquals(65, vm.uiState.value.elapsedSeconds)
        assertTrue(vm.uiState.value.isRunning)
        assertEquals(TimerPhase.GREEN, vm.uiState.value.phase)

        advanceTimeBy(2_000L)
        runCurrent()
        assertEquals(67, vm.uiState.value.elapsedSeconds)
    }

    @Test
    fun runningHandle_crossingAPhaseWhileDead_firesOneCatchUpCueOnReturn() = runTimerTest {
        advanceTimeBy(70_000L)
        val vm = newVm(savedHandle(accumulatedMs = 0L, startedAtMs = 10_000L, phase = "BEFORE_GREEN")) {
            testScheduler.currentTime
        }
        val (catchUps, job) = collectCatchUps(vm)

        vm.onAppForegrounded()
        vm.onAppForegrounded()

        assertEquals(listOf(TimerPhase.GREEN), catchUps)
        job.cancel()
    }

    @Test
    fun pausedHandle_hasNoCatchUpCue() = runTimerTest {
        val vm = newVm(savedHandle(accumulatedMs = 70_000L, phase = "GREEN")) { testScheduler.currentTime }
        val (catchUps, job) = collectCatchUps(vm)

        vm.onAppForegrounded()

        assertEquals(emptyList<TimerPhase>(), catchUps)
        job.cancel()
    }

    @Test
    fun startedAtInTheFuture_afterAReboot_fallsBackToPausedAccumulatedTime() = runTimerTest {
        // elapsedRealtime restarts from zero after a reboot, so a saved start in the future is stale.
        val vm = newVm(savedHandle(accumulatedMs = 30_000L, startedAtMs = 900_000L)) { testScheduler.currentTime }

        assertEquals(30, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRunning)
    }

    @Test
    fun runningTimer_roundTripsThroughTheHandleIntoANewViewModel() = runTimerTest {
        val handle = SavedStateHandle()
        val first = newVm(handle) { testScheduler.currentTime }
        first.configure(1, 2)
        first.start()
        advanceTimeBy(30_000L)
        runCurrent()

        // "Process death": a new ViewModel is built from the same handle contents.
        advanceTimeBy(40_000L)
        val second = newVm(SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })) {
            testScheduler.currentTime
        }

        assertEquals(70, second.uiState.value.elapsedSeconds)
        assertTrue(second.uiState.value.isRunning)
        assertTrue(second.uiState.value.isRestored)
    }

    @Test
    fun pausedTimer_roundTripsItsAccumulatedTime() = runTimerTest {
        val handle = SavedStateHandle()
        val first = newVm(handle) { testScheduler.currentTime }
        first.configure(1, 2)
        first.start()
        advanceTimeBy(12_300L)
        runCurrent()
        first.pause()

        val second = newVm(SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })) {
            testScheduler.currentTime
        }

        assertEquals(12, second.uiState.value.elapsedSeconds)
        assertFalse(second.uiState.value.isRunning)
    }

    @Test
    fun reset_clearsTheSavedStateSoNothingIsRestored() = runTimerTest {
        val handle = SavedStateHandle()
        val first = newVm(handle) { testScheduler.currentTime }
        first.configure(1, 2)
        first.start()
        advanceTimeBy(20_000L)
        runCurrent()
        first.reset()

        val second = newVm(SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })) {
            testScheduler.currentTime
        }

        assertEquals(0, second.uiState.value.elapsedSeconds)
        assertFalse(second.uiState.value.isRestored)
    }

    @Test
    fun restoredFlag_clearsWhenTheUserActs() = runTimerTest {
        val paused = newVm(savedHandle(accumulatedMs = 20_000L)) { testScheduler.currentTime }
        paused.start()
        assertFalse(paused.uiState.value.isRestored)

        val running = newVm(savedHandle(accumulatedMs = 20_000L, startedAtMs = 0L)) { testScheduler.currentTime }
        assertTrue(running.uiState.value.isRestored)
        running.pause()
        assertFalse(running.uiState.value.isRestored)

        val toReset = newVm(savedHandle(accumulatedMs = 20_000L)) { testScheduler.currentTime }
        toReset.reset()
        assertFalse(toReset.uiState.value.isRestored)
    }

    @Test
    fun undoReset_restoresTheElapsedTimePausedWithoutReAlerting() = runTimerTest {
        val vm = newVm { testScheduler.currentTime }
        vm.configure(1, 2)
        val alerts = mutableListOf<TimerPhase>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { vm.phaseAlerts.collect { alerts.add(it) } }

        vm.start()
        advanceTimeBy(95_300L)
        runCurrent()
        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW), alerts)

        val snapshot = vm.snapshot()
        vm.reset()
        assertEquals(0, vm.uiState.value.elapsedSeconds)

        vm.undoReset(snapshot)

        assertEquals(95, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.YELLOW, vm.uiState.value.phase)
        assertFalse(vm.uiState.value.isRunning)
        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW), alerts)

        // Resuming continues from the restored time and still alerts RED once.
        vm.start()
        advanceTimeBy(30_000L)
        runCurrent()
        assertEquals(listOf(TimerPhase.GREEN, TimerPhase.YELLOW, TimerPhase.RED), alerts)
        job.cancel()
    }

    @Test
    fun undoReset_isIgnoredOnceTheTimerHasMovedOn() = runTimerTest {
        val vm = newVm { testScheduler.currentTime }
        vm.configure(1, 2)
        vm.start()
        advanceTimeBy(10_000L)
        runCurrent()
        val snapshot = vm.snapshot()
        vm.reset()
        vm.start()
        advanceTimeBy(3_000L)
        runCurrent()

        vm.undoReset(snapshot)

        assertEquals(3, vm.uiState.value.elapsedSeconds)
        assertTrue(vm.uiState.value.isRunning)
    }
}
