package com.happypuppy.toastmasterstimer.ui.screens

import androidx.lifecycle.SavedStateHandle
import com.happypuppy.toastmasterstimer.MainDispatcherRule
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Wave 3: the typed timer route's arguments land in the ViewModel's [SavedStateHandle] (under the
 * route property names), and the ViewModel reads its range directly in `init`. `toRoute()` cannot
 * run on the JVM, so the keys are read directly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelRouteArgsTest {

    private val testDispatcher = StandardTestDispatcher()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val created = mutableListOf<TimerViewModel>()

    private fun newVm(
        handle: SavedStateHandle,
        initialThresholds: TimerThresholds? = null,
        clock: () -> Long
    ): TimerViewModel = TimerViewModel(
        savedStateHandle = handle,
        elapsedRealtimeMs = clock,
        initialThresholds = initialThresholds
    ).also { created += it }

    /** Resets every ViewModel in a finally block so a failed assertion cannot hang the tick loop. */
    private fun runTimerTest(body: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            body()
        } finally {
            created.forEach { it.reset() }
            created.clear()
        }
    }

    private fun routeHandle(id: String, min: Int, max: Int) = SavedStateHandle(
        mapOf("speechTypeId" to id, "minMinutes" to min, "maxMinutes" to max)
    )

    @Test
    fun routeArgs_setTheRangeWithoutAConfigureCall() = runTimerTest {
        val vm = newVm(routeHandle("evaluation", 2, 3)) { testScheduler.currentTime }

        assertEquals(TimerThresholds(2, 3), vm.uiState.value.thresholds)
        assertEquals(0, vm.uiState.value.elapsedSeconds)
        assertFalse(vm.uiState.value.isRestored)
    }

    @Test
    fun routeArgs_coverTheCustomExtremes() = runTimerTest {
        val vm = newVm(routeHandle("custom", 59, 60)) { testScheduler.currentTime }

        assertEquals(TimerThresholds(59, 60), vm.uiState.value.thresholds)
    }

    @Test
    fun routeArgs_areCoercedLikeConfigure() = runTimerTest {
        val vm = newVm(routeHandle("custom", 3, 1)) { testScheduler.currentTime }

        assertEquals(TimerThresholds(3, 3), vm.uiState.value.thresholds)
    }

    @Test
    fun withoutRouteArgs_theDefaultRangeStays() = runTimerTest {
        val vm = newVm(SavedStateHandle()) { testScheduler.currentTime }

        assertEquals(TimerThresholds(1, 2), vm.uiState.value.thresholds)
    }

    @Test
    fun routeArgs_winOverInitialThresholds() = runTimerTest {
        val vm = newVm(
            handle = routeHandle("evaluation", 2, 3),
            initialThresholds = TimerThresholds(5, 7)
        ) { testScheduler.currentTime }

        assertEquals(TimerThresholds(2, 3), vm.uiState.value.thresholds)
    }

    @Test
    fun restoredClockState_winsOverTheRouteArgsAndInitialThresholds() = runTimerTest {
        val handle = SavedStateHandle(
            mapOf(
                "speechTypeId" to "evaluation",
                "minMinutes" to 2,
                "maxMinutes" to 3,
                "timer_min_minutes" to 5,
                "timer_max_minutes" to 7,
                "timer_accumulated_ms" to 45_000L,
                "timer_started_at_ms" to -1L,
                "timer_phase" to "BEFORE_GREEN"
            )
        )
        val vm = newVm(handle, initialThresholds = TimerThresholds(4, 6)) { testScheduler.currentTime }

        assertEquals(TimerThresholds(5, 7), vm.uiState.value.thresholds)
        assertEquals(45, vm.uiState.value.elapsedSeconds)
        assertTrue(vm.uiState.value.isRestored)
    }
}
