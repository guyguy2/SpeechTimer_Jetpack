package com.happypuppy.toastmasterstimer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.ui.screens.TimerFace
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility-check (ATF) tests for the timer screen through the real app: idle, running and
 * paused. Checks run on every action and explicitly via [tryPerformAccessibilityChecks].
 */
@RunWith(AndroidJUnit4::class)
class TimerAccessibilityTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun enableChecks() {
        rule.enableAccessibilityChecks()
    }

    private fun waitForText(text: String) =
        rule.waitUntil(10_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun openTimer() {
        waitForText("TABLE TOPICS (1 TO 2 MIN)")
        rule.onNodeWithText("TABLE TOPICS (1 TO 2 MIN)").performClick()
        waitForText("00:00")
    }

    @Test
    fun timerIdle_passesAccessibilityChecks() {
        openTimer()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun timerRunning_passesAccessibilityChecks() {
        openTimer()
        rule.onNodeWithContentDescription("play").performClick()
        waitForText("00:01")
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun timerPaused_passesAccessibilityChecks() {
        openTimer()
        rule.onNodeWithContentDescription("play").performClick()
        waitForText("00:01")
        rule.onNodeWithContentDescription("pause").performClick()
        waitForText("Paused")
        rule.onRoot().tryPerformAccessibilityChecks()
    }
}

/** The same checks at each phase and in pure-color mode, using the stateless [TimerFace]. */
@RunWith(AndroidJUnit4::class)
class TimerFaceAccessibilityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var phase by mutableStateOf(TimerPhase.GREEN)
    private var ticks by mutableStateOf(64)
    private var running by mutableStateOf(true)
    private var uiVisible by mutableStateOf(true)

    @Before
    fun enableChecksAndShowFace() {
        rule.enableAccessibilityChecks()
        rule.setContent {
            MaterialTheme {
                TimerFace(
                    phase = phase,
                    ticks = ticks,
                    isTimerRunning = running,
                    isUiVisible = uiVisible,
                    isRestored = false,
                    thresholds = TimerThresholds(1, 2),
                    innerPadding = PaddingValues(),
                    onRestart = {},
                    onToggleRunning = {},
                    onSave = {}
                )
            }
        }
    }

    private fun check(newPhase: TimerPhase, newTicks: Int, isRunning: Boolean = true, visible: Boolean = true) {
        rule.runOnIdle {
            phase = newPhase
            ticks = newTicks
            running = isRunning
            uiVisible = visible
        }
        rule.waitForIdle()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun beforeGreen_passesAccessibilityChecks() = check(TimerPhase.BEFORE_GREEN, 5)

    @Test
    fun green_passesAccessibilityChecks() = check(TimerPhase.GREEN, 64)

    @Test
    fun yellow_passesAccessibilityChecks() = check(TimerPhase.YELLOW, 95)

    @Test
    fun red_passesAccessibilityChecks() = check(TimerPhase.RED, 125)

    @Test
    fun pausedInRed_passesAccessibilityChecks() = check(TimerPhase.RED, 125, isRunning = false)

    @Test
    fun pureColorMode_passesAccessibilityChecks() = check(TimerPhase.GREEN, 64, visible = false)
}
