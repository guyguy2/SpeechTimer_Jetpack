package com.happypuppy.toastmasterstimer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.navigation.TimerRoute
import com.happypuppy.toastmasterstimer.ui.screens.TimerViewModel
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.compose.koinViewModel

/**
 * Real navigation round trip for [TimerRoute]: navigate with a typed route, read it back from the
 * back stack entry arguments and from the entry's own SavedStateHandle, with text that breaks
 * hand-built string routes. It also creates the real [TimerViewModel] the way the NavGraph does
 * (Koin, scoped to the entry) and checks its range came from the route, which proves the key names
 * the ViewModel reads out of its SavedStateHandle match the route.
 */
@RunWith(AndroidJUnit4::class)
class TimerRouteNavigationTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var navController: NavHostController
    private var fromArguments: TimerRoute? = null
    private var fromHandle: TimerRoute? = null
    private var viewModelThresholds: TimerThresholds? = null

    @Composable
    private fun Host() {
        navController = rememberNavController()
        NavHost(navController = navController, startDestination = "start") {
            composable("start") { Text("start") }
            composable<TimerRoute> { entry ->
                fromArguments = entry.toRoute<TimerRoute>()
                fromHandle = entry.savedStateHandle.toRoute<TimerRoute>()
                viewModelThresholds =
                    koinViewModel<TimerViewModel>(viewModelStoreOwner = entry).uiState.value.thresholds
                Text("timer")
            }
        }
    }

    private fun roundTrip(route: TimerRoute) {
        fromArguments = null
        fromHandle = null
        viewModelThresholds = null
        rule.runOnUiThread { navController.navigate(route) }
        rule.waitForIdle()
        assertEquals("from arguments", route, fromArguments)
        assertEquals("from SavedStateHandle", route, fromHandle)
        assertEquals(
            "ViewModel range",
            TimerThresholds(route.minMinutes, route.maxMinutes),
            viewModelThresholds
        )
        rule.runOnUiThread { navController.popBackStack() }
        rule.waitForIdle()
    }

    @Test
    fun presets_roundTrip() {
        rule.setContent { Host() }
        SpeechType.presets.forEach {
            roundTrip(TimerRoute(it.id, it.presetMinMinutes!!, it.presetMaxMinutes!!))
        }
    }

    @Test
    fun customRange_roundTripsWithoutALabel() {
        rule.setContent { Host() }
        roundTrip(TimerRoute(SpeechType.CUSTOM.id, 59, 60))
    }

    @Test
    fun awkwardCustomLabels_roundTrip() {
        rule.setContent { Host() }
        val labels = listOf(
            "Evaluation / Table Topics",
            "two  spaces and a tab\t",
            "café 日本語 مرحبا",
            "100% sure %2F %",
            "\"quoted\" and 'single' and \\ backslash",
            "a?b=c&d=e#f",
            "{customLabel}",
            "x".repeat(2_000)
        )
        labels.forEach { roundTrip(TimerRoute(SpeechType.CUSTOM.id, 3, 8, customLabel = it)) }
    }

    @Test
    fun emptyCustomLabel_roundTrips() {
        rule.setContent { Host() }
        roundTrip(TimerRoute(SpeechType.CUSTOM.id, 3, 8, customLabel = ""))
    }
}
