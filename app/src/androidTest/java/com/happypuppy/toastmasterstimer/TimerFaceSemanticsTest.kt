package com.happypuppy.toastmasterstimer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.ui.screens.TimerFace
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Shows the timer face at chosen phases without waiting minutes. Pins what a screen reader gets:
 * a visible phase label with the only live region, also when the controls are hidden.
 */
@RunWith(AndroidJUnit4::class)
class TimerFaceSemanticsTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private var phase by mutableStateOf(TimerPhase.GREEN)
    private var ticks by mutableStateOf(64)
    private var uiVisible by mutableStateOf(true)
    private var running by mutableStateOf(true)

    private fun show(
        initialPhase: TimerPhase = TimerPhase.GREEN,
        initialTicks: Int = 64,
        visible: Boolean = true,
        isRunning: Boolean = true
    ) {
        phase = initialPhase
        ticks = initialTicks
        uiVisible = visible
        running = isRunning
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

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
    private val noLiveRegion = SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion)

    private fun label(phase: TimerPhase): String = when (phase) {
        TimerPhase.BEFORE_GREEN -> "Before green"
        TimerPhase.GREEN -> "Green: minimum reached"
        TimerPhase.YELLOW -> "Yellow: wrap up soon"
        TimerPhase.RED -> "Red: maximum reached"
    }

    @Test
    fun everyPhaseShowsItsLabelAsText() {
        show(TimerPhase.BEFORE_GREEN, 5)
        TimerPhase.entries.forEach { p ->
            rule.runOnIdle { phase = p }
            rule.waitForIdle()
            rule.onNodeWithText(label(p)).assertExists()
            TimerPhase.entries.filter { it != p }.forEach { other ->
                rule.onAllNodesWithText(label(other)).assertCountEquals(0)
            }
        }
    }

    @Test
    fun phaseLabelIsTheOnlyPoliteLiveRegion_andTheDigitsHaveNone() {
        show()

        rule.onAllNodes(polite).assertCountEquals(1)
        rule.onNodeWithText(label(TimerPhase.GREEN)).assert(polite)
        rule.onNodeWithText("01:04").assert(noLiveRegion)
    }

    @Test
    fun phaseLabelDoesNotChangeWhenOnlyTheTimeTicks() {
        show(TimerPhase.GREEN, 64)
        rule.onNodeWithText(label(TimerPhase.GREEN)).assertTextEquals(label(TimerPhase.GREEN))

        rule.runOnIdle { ticks = 65 }
        rule.waitForIdle()

        rule.onNodeWithText("01:05").assert(noLiveRegion)
        rule.onNodeWithText(label(TimerPhase.GREEN)).assertTextEquals(label(TimerPhase.GREEN)).assert(polite)
        rule.onAllNodes(polite).assertCountEquals(1)
    }

    @Test
    fun hiddenControls_keepThePhaseAsAnInvisibleLiveRegion() {
        show(visible = false)

        // Pure-color mode draws no label and no digits...
        rule.onAllNodesWithText(label(TimerPhase.GREEN)).assertCountEquals(0)
        rule.onAllNodesWithText("01:04").assertCountEquals(0)
        // ...but a semantics-only node still carries the phase with the same polite live region.
        rule.onAllNodes(polite).assertCountEquals(1)
        rule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(label(TimerPhase.GREEN))) and polite
        ).assertCountEquals(1)
    }

    @Test
    fun hiddenControls_phaseChangeUpdatesTheLiveRegionDescription() {
        show(TimerPhase.GREEN, visible = false)

        rule.runOnIdle { phase = TimerPhase.YELLOW }
        rule.waitForIdle()

        rule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(label(TimerPhase.YELLOW))) and polite
        ).assertCountEquals(1)
    }

    @Test
    fun digitsAreSpokenAsOneDurationWithThePhase() {
        show(TimerPhase.GREEN, 64)
        rule.onNodeWithText("01:04").assertContentDescriptionEquals("1 minute 4 seconds, green phase")

        rule.runOnIdle { phase = TimerPhase.YELLOW; ticks = 90 }
        rule.waitForIdle()
        rule.onNodeWithText("01:30").assertContentDescriptionEquals("1 minute 30 seconds, yellow phase")

        rule.runOnIdle { phase = TimerPhase.RED; ticks = 120 }
        rule.waitForIdle()
        rule.onNodeWithText("02:00").assertContentDescriptionEquals("2 minutes, red phase")

        rule.runOnIdle { phase = TimerPhase.BEFORE_GREEN; ticks = 1 }
        rule.waitForIdle()
        rule.onNodeWithText("00:01").assertContentDescriptionEquals("1 second, before green")
    }

    @Test
    fun pausedTimer_showsASteadyPausedLabelAndKeepsTheDigits() {
        show(TimerPhase.GREEN, 64, isRunning = false)

        rule.onNodeWithText("Paused").assertExists()
        rule.onNodeWithText("01:04").assertExists()
        // No blinking: the digits stay put across what used to be several blink periods.
        repeat(8) {
            rule.mainClock.advanceTimeBy(500)
            rule.waitForIdle()
            rule.onNodeWithText("01:04").assertExists()
            rule.onNodeWithText("Paused").assertExists()
        }
    }

    @Test
    fun runningOrIdleTimer_hasNoPausedLabel() {
        show(TimerPhase.GREEN, 64, isRunning = true)
        rule.onAllNodesWithText("Paused").assertCountEquals(0)

        rule.runOnIdle { running = false; ticks = 0; phase = TimerPhase.BEFORE_GREEN }
        rule.waitForIdle()
        rule.onAllNodesWithText("Paused").assertCountEquals(0)
    }

    @Test
    fun pausing_doesNotMoveTheControls() {
        show(TimerPhase.GREEN, 64, isRunning = true)
        rule.waitForIdle()
        val runningToggle = rule.onNodeWithContentDescription("pause").fetchSemanticsNode().boundsInRoot
        val runningRestart = rule.onNodeWithContentDescription("restart").fetchSemanticsNode().boundsInRoot
        val runningDigits = rule.onNodeWithText("01:04").fetchSemanticsNode().boundsInRoot

        rule.runOnIdle { running = false }
        rule.waitForIdle()
        val pausedToggle = rule.onNodeWithContentDescription("play").fetchSemanticsNode().boundsInRoot
        val pausedRestart = rule.onNodeWithContentDescription("restart").fetchSemanticsNode().boundsInRoot
        val pausedDigits = rule.onNodeWithText("01:04").fetchSemanticsNode().boundsInRoot

        // The play/pause button changes width on purpose, so compare its vertical extent and center.
        assertEquals(runningToggle.top, pausedToggle.top, 0.5f)
        assertEquals(runningToggle.bottom, pausedToggle.bottom, 0.5f)
        assertEquals(runningToggle.center.x, pausedToggle.center.x, 0.5f)
        assertEquals(runningRestart, pausedRestart)
        assertEquals(runningDigits, pausedDigits)
    }
}
