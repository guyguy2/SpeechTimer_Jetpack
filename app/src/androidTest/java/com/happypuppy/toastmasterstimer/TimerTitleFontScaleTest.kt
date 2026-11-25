package com.happypuppy.toastmasterstimer

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
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

    private fun assertTitleFullyShown(title: String) {
        waitForText("00:00")
        rule.waitForIdle()

        val results = mutableListOf<TextLayoutResult>()
        rule.onNodeWithText(title).fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]
            .action!!.invoke(results)
        val layout = results.single()
        check(!layout.didOverflowHeight) { "Title is clipped vertically: lines=${layout.lineCount} size=${layout.size}" }
        check(layout.lineCount <= 2) { "Title needs ${layout.lineCount} lines" }
        check((0 until layout.lineCount).none { layout.isLineEllipsized(it) }) { "Title is ellipsized" }
    }

    private fun openPreset(preset: String) {
        waitForText(preset)
        rule.onNodeWithText(preset).performClick()
    }

    /** Opens the custom range screen, steps From up to 59 (which pushes To to 60) and starts the timer. */
    private fun openWidestCustom(): String {
        waitForText("TABLE TOPICS (1 TO 2 MIN)")
        rule.onNodeWithText("Custom").performClick()
        waitForText("Speech will last between 8 and 10 minutes")
        repeat(51) { rule.onNodeWithContentDescription("Increase minimum minutes").performClick() }
        waitForText("Speech will last between 59 and 60 minutes")
        rule.onNodeWithText("Start Timer").performClick()
        return "Custom Speech (59 - 60 min)"
    }

    private fun checkPresetAtScale(scale: String, preset: String) {
        setFontScale(scale)
        rule.activityRule.scenario.recreate()
        openPreset(preset)
        assertTitleFullyShown(preset)
    }

    @Test
    fun title_isNotTruncatedAtFontScale1_0() = checkPresetAtScale("1.0", "TABLE TOPICS (1 TO 2 MIN)")

    @Test
    fun title_isNotTruncatedAtFontScale1_3() = checkPresetAtScale("1.3", "TABLE TOPICS (1 TO 2 MIN)")

    @Test
    fun title_isNotTruncatedAtFontScale2_0() = checkPresetAtScale("2.0", "ICE BREAKER (4 TO 6 MIN)")

    /** TABLE TOPICS is the longest preset title (25 characters). */
    @Test
    fun longestPresetTitle_isNotTruncatedAtFontScale2_0() = checkPresetAtScale("2.0", "TABLE TOPICS (1 TO 2 MIN)")

    @Test
    fun widestCustomTitle_isNotTruncatedAtFontScale2_0() {
        setFontScale("2.0")
        rule.activityRule.scenario.recreate()
        assertTitleFullyShown(openWidestCustom())
    }

    @Test
    fun widestCustomTitle_isNotTruncatedAtFontScale1_3() {
        setFontScale("1.3")
        rule.activityRule.scenario.recreate()
        assertTitleFullyShown(openWidestCustom())
    }
}
