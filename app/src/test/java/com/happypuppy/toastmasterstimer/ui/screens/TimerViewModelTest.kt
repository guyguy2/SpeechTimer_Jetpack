package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
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

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

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
     * would drain virtual time forever (hang instead of fail). A runTest timeout does not help:
     * the drain loop never yields to it.
     */
    private fun runTimerTest(body: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            body()
        } finally {
            created.forEach { it.reset() }
            created.clear()
        }
    }

    @Test
    fun initialState_isZeroPausedBeforeGreenAndUiVisible() {
        val vm = newVm({ 0L })
        val state = vm.uiState.value

        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isRunning)
        assertEquals(TimerPhase.BEFORE_GREEN, state.phase)
        assertTrue(state.isUiVisible)
    }

    @Test
    fun start_advancesMonotonicElapsedTimeAndTransitionsPhases() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })

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
    fun pauseAndResume_preservesSubSecondProgressWithoutDrift() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })

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
    fun phaseAlerts_fireOncePerPhaseTransitionWithoutDuplicates() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })

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
    fun reset_clearsPhaseStateSoAlertsFireAgainOnNextRun() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })

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
    fun resetWhileRunning_stopsTickingAndResetsState() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime })

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
    fun initialThresholds_setRangeInConstructor() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime }, thresholds = TimerThresholds(5, 7))

        assertEquals(300, vm.uiState.value.thresholds.greenSeconds)
        assertEquals(420, vm.uiState.value.thresholds.redSeconds)
    }

    @Test
    fun injectedTicker_drivesElapsedAndCancelsOnPauseResetAndOnCleared() = runTimerTest {
        var nowMs = 0L
        var activeTickJob: Job? = null
        var onTickCallback: (() -> Unit)? = null
        val ticker = TimerTicker { _, _, onTick ->
            onTickCallback = onTick
            Job().also { activeTickJob = it }
        }
        val store = ViewModelStore()
        val provider = ViewModelProvider(
            store,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    TimerViewModel(
                        savedStateHandle = SavedStateHandle(),
                        elapsedRealtimeMs = { nowMs },
                        initialThresholds = TimerThresholds(1, 2),
                        ticker = ticker
                    ) as T
            }
        )
        val vm = provider[TimerViewModel::class.java]

        vm.start()
        assertFalse(activeTickJob!!.isCancelled)
        nowMs = 60_000L
        onTickCallback!!.invoke()
        assertEquals(60, vm.uiState.value.elapsedSeconds)
        assertEquals(TimerPhase.GREEN, vm.uiState.value.phase)

        val pauseJob = checkNotNull(activeTickJob)
        vm.pause()
        assertTrue(pauseJob.isCancelled)

        vm.start()
        val resetJob = checkNotNull(activeTickJob)
        assertFalse(resetJob.isCancelled)
        vm.reset()
        assertTrue(resetJob.isCancelled)

        vm.start()
        val clearJob = checkNotNull(activeTickJob)
        assertFalse(clearJob.isCancelled)
        store.clear()
        assertTrue(clearJob.isCancelled)
    }

    @Test
    fun syncElapsed_catchesUpAfterDozeOrBackgroundJump() = runTimerTest {
        var nowMs = 0L
        val vm = newVm({ nowMs })

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
    fun zeroMinuteDuration_handlesEdgeCaseAndAlertsAgainAfterReset() = runTimerTest {
        val vm = newVm({ testScheduler.currentTime }, thresholds = TimerThresholds(0, 0))

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
        val vm = newVm({ 0L })
        assertTrue(vm.uiState.value.isUiVisible)

        vm.toggleUiVisibility()
        assertFalse(vm.uiState.value.isUiVisible)

        vm.toggleUiVisibility()
        assertTrue(vm.uiState.value.isUiVisible)
    }
}
