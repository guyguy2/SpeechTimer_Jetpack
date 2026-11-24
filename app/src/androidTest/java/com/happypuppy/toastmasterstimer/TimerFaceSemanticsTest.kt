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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.ui.screens.TimerFace
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import com.happypuppy.toastmasterstimer.util.formatTime
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

    private fun show(initialPhase: TimerPhase = TimerPhase.GREEN, initialTicks: Int = 64, visible: Boolean = true) {
        phase = initialPhase
        ticks = initialTicks
        uiVisible = visible
        rule.setContent {
            MaterialTheme {
                TimerFace(
                    phase = phase,
                    ticks = ticks,
                    timeText = formatTime(ticks),
                    isTimerRunning = true,
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
}
