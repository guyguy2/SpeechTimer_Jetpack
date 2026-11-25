package com.happypuppy.toastmasterstimer

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.ui.layout.rememberAdaptiveLayoutSpec
import com.happypuppy.toastmasterstimer.ui.screens.MAIN_CONTENT_COLUMN_TAG
import com.happypuppy.toastmasterstimer.ui.screens.MainScreenContent
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import com.happypuppy.toastmasterstimer.util.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.math.abs

/**
 * Instrumented layout and content checks for the Wave 7 MainScreen redesign (UX-ST-2, P1-3).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainScreenLayoutInstrumentedTest {

    @get:Rule
    val rule = createComposeRule()

    private fun sampleSpeakers(): List<Speaker> = listOf(
        Speaker(id = 1, name = "Oldest Speaker", speechType = "SPEECH", duration = "05:00", createdAt = Date(1_000L)),
        Speaker(id = 2, name = "Third Recent", speechType = "EVALUATION", duration = "02:30", createdAt = Date(2_000L), phaseReached = TimerPhase.GREEN.name),
        Speaker(id = 3, name = "Second Recent", speechType = "ICE BREAKER", duration = "04:45", createdAt = Date(3_000L), phaseReached = TimerPhase.YELLOW.name),
        Speaker(id = 4, name = "Newest Speaker", speechType = "TABLE TOPICS", duration = "01:40", createdAt = Date(4_000L), phaseReached = TimerPhase.RED.name)
    )

    @Test
    fun presetCards_showTitleCaseNamesRangesAndCueTimesWithoutLastCustom() {
        val clicked = mutableListOf<SpeechType>()
        rule.setContent {
            SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                MainScreenContent(
                    speakers = emptyList(),
                    useTwoColumnPresets = false,
                    onPresetClick = { clicked += it },
                    onOpenSavedData = {},
                    onOpenSettings = {}
                )
            }
        }
        rule.waitForIdle()

        val expectedCards = listOf(
            listOf("Table Topics", "1\u20132 min", "1:00", "1:30", "2:00"),
            listOf("Evaluation", "2\u20133 min", "2:00", "2:30", "3:00"),
            listOf("Ice Breaker", "4\u20136 min", "4:00", "5:00", "6:00"),
            listOf("Prepared Speech", "5\u20137 min", "5:00", "6:00", "7:00")
        )
        expectedCards.forEach { cardTexts ->
            val cardMatcher = cardTexts
                .map { hasText(it) }
                .reduce { acc, matcher -> acc and matcher }
            rule.onNode(cardMatcher).assertExists()
        }
        rule.onAllNodesWithText("Last Custom").assertCountEquals(0)

        SpeechType.presets.forEachIndexed { index, type ->
            val label = when (type) {
                SpeechType.TABLE_TOPICS -> "Table Topics"
                SpeechType.EVALUATION -> "Evaluation"
                SpeechType.ICE_BREAKER -> "Ice Breaker"
                SpeechType.SPEECH -> "Prepared Speech"
                SpeechType.CUSTOM -> error("Unexpected")
            }
            rule.onNodeWithText(label).performClick()
            assertEquals(index + 1, clicked.size)
            assertEquals(type, clicked.last())
        }
    }

    @Test
    fun emptyState_showsTimingColorGuideAndHidesRecent() {
        rule.setContent {
            SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                MainScreenContent(
                    speakers = emptyList(),
                    useTwoColumnPresets = false,
                    onPresetClick = {},
                    onOpenSavedData = {},
                    onOpenSettings = {}
                )
            }
        }

        rule.onNodeWithText("Timing color guide").assertExists()
        rule.onAllNodesWithText("Recent").assertCountEquals(0)
    }

    @Test
    fun populatedState_showsLatestThreeRecentAndViewAllNavigates() {
        var openedSavedData = 0
        rule.setContent {
            SimpleSpeechTimerTheme(themePreference = AppTheme.DARK, dynamicColor = false) {
                MainScreenContent(
                    speakers = sampleSpeakers(),
                    useTwoColumnPresets = false,
                    onPresetClick = {},
                    onOpenSavedData = { openedSavedData++ },
                    onOpenSettings = {}
                )
            }
        }

        rule.onNodeWithText("Recent").assertExists()
        rule.onAllNodesWithText("Timing color guide").assertCountEquals(0)
        rule.onNodeWithText("Newest Speaker").assertExists()
        rule.onNodeWithText("Second Recent").assertExists()
        rule.onNodeWithText("Third Recent").assertExists()
        rule.onAllNodesWithText("Oldest Speaker").assertCountEquals(0)

        rule.onNodeWithText("View all").performClick()
        assertEquals(1, openedSavedData)
    }

    @Test
    fun compactWindow_stacksPresetsSingleColumn() {
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(360.dp, 800.dp))) {
                SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                    val spec = rememberAdaptiveLayoutSpec()
                    MainScreenContent(
                        speakers = emptyList(),
                        useTwoColumnPresets = spec.useTwoColumnPresets,
                        onPresetClick = {},
                        onOpenSavedData = {},
                        onOpenSettings = {}
                    )
                }
            }
        }

        val ttBounds = rule.onNodeWithText("Table Topics").fetchSemanticsNode().boundsInRoot
        val evalBounds = rule.onNodeWithText("Evaluation").fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Compact portrait should stack presets vertically",
            evalBounds.top > ttBounds.bottom
        )
    }

    @Test
    fun expandedWindow_usesTwoByTwoGridAndCapsContentAt840Dp() {
        var density = 1f
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(1200.dp, 800.dp))) {
                density = LocalDensity.current.density
                SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                    val spec = rememberAdaptiveLayoutSpec()
                    MainScreenContent(
                        speakers = emptyList(),
                        useTwoColumnPresets = spec.useTwoColumnPresets,
                        onPresetClick = {},
                        onOpenSavedData = {},
                        onOpenSettings = {}
                    )
                }
            }
        }

        val ttBounds = rule.onNodeWithText("Table Topics").fetchSemanticsNode().boundsInRoot
        val evalBounds = rule.onNodeWithText("Evaluation").fetchSemanticsNode().boundsInRoot
        val iceBounds = rule.onNodeWithText("Ice Breaker").fetchSemanticsNode().boundsInRoot
        assertTrue("First row should align Table Topics and Evaluation", abs(ttBounds.top - evalBounds.top) < 2f)
        assertTrue("Evaluation should be right of Table Topics", evalBounds.left > ttBounds.right)
        assertTrue("Ice Breaker should be on second row", iceBounds.top > ttBounds.bottom)

        val contentWidthDp = rule.onNodeWithTag(MAIN_CONTENT_COLUMN_TAG).fetchSemanticsNode().boundsInRoot.width / density
        assertTrue("Content width ($contentWidthDp dp) must be capped at <= 840dp", contentWidthDp <= 841f)
    }

    @Test
    fun fontScale2_0_presetCardsDoNotClipOrEllipsize() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.WindowSize(DpSize(360.dp, 800.dp)) then
                    DeviceConfigurationOverride.FontScale(2.0f)
            ) {
                SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                    MainScreenContent(
                        speakers = emptyList(),
                        useTwoColumnPresets = false,
                        onPresetClick = {},
                        onOpenSavedData = {},
                        onOpenSettings = {}
                    )
                }
            }
        }
        rule.waitForIdle()

        listOf("Table Topics", "Evaluation", "Ice Breaker", "Prepared Speech").forEach { title ->
            val results = mutableListOf<TextLayoutResult>()
            rule.onNodeWithText(title).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]
                .action!!.invoke(results)
            val layout = results.first()
            assertTrue("$title clipped vertically", !layout.didOverflowHeight)
            assertTrue(
                "$title ellipsized at 2.0x font scale",
                (0 until layout.lineCount).none { layout.isLineEllipsized(it) }
            )
        }
        rule.onNodeWithText("Timing color guide").assertExists()
    }
}
