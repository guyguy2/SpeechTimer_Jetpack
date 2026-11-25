package com.happypuppy.toastmasterstimer

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.ui.screens.TimerFace
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import com.happypuppy.toastmasterstimer.util.TimerPhase
import com.happypuppy.toastmasterstimer.util.TimerThresholds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies [TimerFace] layout across phone portrait, phone landscape (side-by-side), tablet,
 * and speaker-facing mode using [DeviceConfigurationOverride.ForcedSize].
 */
@RunWith(AndroidJUnit4::class)
class TimerLayoutInstrumentedTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun digitsLayout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(text).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]
            .action!!.invoke(results)
        return results.single()
    }

    @Test
    fun phonePortrait_stacksControlsBelowDigitsOnSingleLine() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))
            ) {
                SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                    TimerFace(
                        phase = TimerPhase.GREEN,
                        ticks = 75,
                        isTimerRunning = false,
                        isUiVisible = true,
                        isRestored = false,
                        thresholds = TimerThresholds(1, 2),
                        innerPadding = PaddingValues(),
                        onRestart = {},
                        onToggleRunning = {},
                        onSave = {},
                        onToggleUiVisibility = {}
                    )
                }
            }
        }
        rule.waitForIdle()

        val digitsLayout = digitsLayout("01:15")
        assertEquals(1, digitsLayout.lineCount)
        assertTrue(
            "Portrait digits overflowed: hasVisualOverflow=${digitsLayout.hasVisualOverflow}, " +
                "didOverflowWidth=${digitsLayout.didOverflowWidth}, didOverflowHeight=${digitsLayout.didOverflowHeight}, " +
                "lineRight=${digitsLayout.getLineRight(0)}, size=${digitsLayout.size}",
            !digitsLayout.hasVisualOverflow && digitsLayout.getLineRight(0) <= digitsLayout.size.width + 1f
        )

        val digitsBounds = rule.onNodeWithText("01:15").fetchSemanticsNode().boundsInRoot
        val startBounds = rule.onNodeWithContentDescription("Start timer").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Portrait controls should be below digits: digitsBottom=${digitsBounds.bottom}, startTop=${startBounds.top}",
            startBounds.top >= digitsBounds.bottom
        )
    }

    @Test
    fun phoneLandscape_placesControlsBesideDigitsWithoutOverlap() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(800.dp, 360.dp))
            ) {
                SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                    TimerFace(
                        phase = TimerPhase.YELLOW,
                        ticks = 95,
                        isTimerRunning = false,
                        isUiVisible = true,
                        isRestored = false,
                        thresholds = TimerThresholds(1, 2),
                        innerPadding = PaddingValues(),
                        onRestart = {},
                        onToggleRunning = {},
                        onSave = {},
                        onToggleUiVisibility = {}
                    )
                }
            }
        }
        rule.waitForIdle()

        val digitsLayout = digitsLayout("01:35")
        assertEquals(1, digitsLayout.lineCount)
        assertTrue(
            "Landscape digits overflowed: hasVisualOverflow=${digitsLayout.hasVisualOverflow}, " +
                "didOverflowWidth=${digitsLayout.didOverflowWidth}, didOverflowHeight=${digitsLayout.didOverflowHeight}, " +
                "lineRight=${digitsLayout.getLineRight(0)}, size=${digitsLayout.size}",
            !digitsLayout.hasVisualOverflow && digitsLayout.getLineRight(0) <= digitsLayout.size.width + 1f
        )

        val digitsBounds = rule.onNodeWithText("01:35").fetchSemanticsNode().boundsInRoot
        val startBounds = rule.onNodeWithContentDescription("Start timer").fetchSemanticsNode().boundsInRoot
        val restartBounds = rule.onNodeWithContentDescription("Restart timer").fetchSemanticsNode().boundsInRoot
        val saveBounds = rule.onNodeWithContentDescription("Save result").fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Landscape controls should sit to the right of digits: digitsRight=${digitsBounds.right}, startLeft=${startBounds.left}",
            startBounds.left >= digitsBounds.right
        )
        assertTrue(
            "Landscape restart button should not overlap digits: digitsRight=${digitsBounds.right}, restartLeft=${restartBounds.left}",
            restartBounds.left >= digitsBounds.right
        )
        assertTrue(
            "Landscape save button should not overlap restart button: restartRight=${restartBounds.right}, saveLeft=${saveBounds.left}",
            saveBounds.left >= restartBounds.right
        )
    }

    @Test
    fun tablet_scalesDigitsLargerThanPhoneWithoutClipping() {
        var viewportSize by mutableStateOf(DpSize(360.dp, 800.dp))

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(viewportSize)
            ) {
                SimpleSpeechTimerTheme(themePreference = AppTheme.DARK, dynamicColor = false) {
                    TimerFace(
                        phase = TimerPhase.RED,
                        ticks = 125,
                        isTimerRunning = true,
                        isUiVisible = true,
                        isRestored = false,
                        thresholds = TimerThresholds(1, 2),
                        innerPadding = PaddingValues(),
                        onRestart = {},
                        onToggleRunning = {},
                        onSave = {},
                        onToggleUiVisibility = {}
                    )
                }
            }
        }
        rule.waitForIdle()
        val phoneLayout = digitsLayout("02:05")
        val phoneHeightDp = phoneLayout.size.height / phoneLayout.layoutInput.density.density
        val phoneFontSp = phoneLayout.layoutInput.style.fontSize.value

        rule.runOnIdle { viewportSize = DpSize(840.dp, 1000.dp) }
        rule.waitForIdle()
        val tabletLayout = digitsLayout("02:05")
        val tabletHeightDp = tabletLayout.size.height / tabletLayout.layoutInput.density.density
        val tabletFontSp = tabletLayout.layoutInput.style.fontSize.value

        assertEquals(1, tabletLayout.lineCount)
        assertTrue(
            "Tablet digits overflowed: hasVisualOverflow=${tabletLayout.hasVisualOverflow}, " +
                "didOverflowWidth=${tabletLayout.didOverflowWidth}, didOverflowHeight=${tabletLayout.didOverflowHeight}, " +
                "lineRight=${tabletLayout.getLineRight(0)}, size=${tabletLayout.size}",
            !tabletLayout.hasVisualOverflow && tabletLayout.getLineRight(0) <= tabletLayout.size.width + 1f
        )
        assertTrue(
            "Expected tablet fontSize ($tabletFontSp sp) > phone fontSize ($phoneFontSp sp)",
            tabletFontSp > phoneFontSp
        )
        assertTrue(
            "Expected tablet digit height ($tabletHeightDp dp) > phone digit height ($phoneHeightDp dp)",
            tabletHeightDp > phoneHeightDp
        )
    }

    @Test
    fun speakerFacingMode_keepsPoliteLiveRegionAndClickableExitToggle() {
        var uiVisible by mutableStateOf(false)

        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))
            ) {
                SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                    TimerFace(
                        phase = TimerPhase.GREEN,
                        ticks = 64,
                        isTimerRunning = true,
                        isUiVisible = uiVisible,
                        isRestored = false,
                        thresholds = TimerThresholds(1, 2),
                        innerPadding = PaddingValues(),
                        onRestart = {},
                        onToggleRunning = {},
                        onSave = {},
                        onToggleUiVisibility = { uiVisible = !uiVisible }
                    )
                }
            }
        }
        rule.waitForIdle()

        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onAllNodesWithText("01:04").assertCountEquals(0)
        rule.onAllNodes(polite).assertCountEquals(1)
        rule.onAllNodes(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Green: minimum reached")
            ) and polite
        ).assertCountEquals(1)

        val exitButton = rule.onNodeWithContentDescription("Show controls")
        exitButton.assertExists()
        val exitNode = exitButton.fetchSemanticsNode()
        val exitDensity = exitNode.layoutInfo.density.density
        val exitWidthDp = exitNode.boundsInRoot.width / exitDensity
        val exitHeightDp = exitNode.boundsInRoot.height / exitDensity
        assertTrue("Expected exit button width >= 48dp, got $exitWidthDp", exitWidthDp >= 47.5f)
        assertTrue("Expected exit button height >= 48dp, got $exitHeightDp", exitHeightDp >= 47.5f)
        exitButton.performClick()
        rule.waitForIdle()

        rule.onNodeWithText("01:04").assertExists()
    }
}
