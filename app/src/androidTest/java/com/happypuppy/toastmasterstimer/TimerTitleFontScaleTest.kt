package com.happypuppy.toastmasterstimer

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The timer app bar title may use two lines instead of truncating at large font scales (P1-5, layout
 * part). Changes the system font scale for the duration of each test and resets it afterwards, so it
 * runs on the emulator only like the rest of the suite.
 */
@RunWith(AndroidJUnit4::class)
class TimerTitleFontScaleTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val automation get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private fun setFontScale(scale: String) {
        automation.executeShellCommand("settings put system font_scale $scale").close()
        Thread.sleep(500)
    }

    @After
    fun resetFontScale() = setFontScale("1.0")

    private fun waitForText(text: String) =
        rule.waitUntil(15_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun assertTitleFullyShown(preset: String) {
        waitForText(preset)
        rule.onNodeWithText(preset).performClick()
        waitForText("00:00")
        rule.waitForIdle()

        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(preset).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]
            .action!!.invoke(results)
        val layout = results.single()
        check(!layout.didOverflowHeight) { "Title is clipped vertically: lines=${layout.lineCount} size=${layout.size}" }
        check(layout.lineCount <= 2) { "Title needs ${layout.lineCount} lines" }
        check((0 until layout.lineCount).none { layout.isLineEllipsized(it) }) { "Title is ellipsized" }
    }

    private fun checkAtScale(scale: String, preset: String) {
        setFontScale(scale)
        rule.activityRule.scenario.recreate()
        assertTitleFullyShown(preset)
    }

    @Test
    fun title_isNotTruncatedAtFontScale1_0() = checkAtScale("1.0", "TABLE TOPICS (1 TO 2 MIN)")

    @Test
    fun title_isNotTruncatedAtFontScale1_3() = checkAtScale("1.3", "TABLE TOPICS (1 TO 2 MIN)")

    @Test
    fun title_isNotTruncatedAtFontScale2_0() = checkAtScale("2.0", "ICE BREAKER (4 TO 6 MIN)")
}
