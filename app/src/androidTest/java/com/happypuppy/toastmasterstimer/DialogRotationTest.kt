package com.happypuppy.toastmasterstimer

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Dialogs and the typed name must survive a rotation. `recreate()` is the rotation proxy used by
 * the rest of the instrumented suite.
 */
@RunWith(AndroidJUnit4::class)
class DialogRotationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val repository: SpeakerRepository
        get() = GlobalContext.get().get()

    @Before
    fun clearDatabase() = runBlocking { repository.deleteAllSpeakers() }

    private fun AndroidComposeTestRule<*, *>.waitForText(text: String, timeoutMs: Long = 10_000) {
        try {
            waitUntil(timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Text '$text' not found. Tree:\n" + onRoot().printToString(maxDepth = 12), e)
        }
    }

    private fun recreate() {
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
    }

    @Test
    fun saveDialog_andTheTypedName_surviveRotation() {
        rule.waitForText("TABLE TOPICS (1 TO 2 MIN)")
        rule.onNodeWithText("TABLE TOPICS (1 TO 2 MIN)").performClick()
        rule.waitForText("00:00")
        rule.onNodeWithContentDescription("Start timer").performClick()
        rule.waitForText("00:01")
        rule.onNodeWithContentDescription("Pause timer").performClick()
        rule.onNodeWithContentDescription("Save result").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Alex Morgan")

        recreate()

        rule.waitForText("Save Time")
        rule.onNode(hasSetTextAction()).assertTextContains("Alex Morgan")
    }

    @Test
    fun sortDialog_survivesRotation() {
        runBlocking { repository.insertSpeaker(Speaker(name = "Ann", speechType = "SPEECH", duration = "05:10")) }
        rule.waitForText("TABLE TOPICS (1 TO 2 MIN)")
        rule.onNodeWithText("Saved Data").performClick()
        rule.waitForText("Ann")
        rule.onNodeWithContentDescription("Sort Options").performClick()
        rule.waitForText("Sort By")

        recreate()

        rule.waitForText("Sort By")
    }

    @Test
    fun clearAllDialog_survivesRotation() {
        runBlocking { repository.insertSpeaker(Speaker(name = "Ann", speechType = "SPEECH", duration = "05:10")) }
        rule.waitForText("TABLE TOPICS (1 TO 2 MIN)")
        rule.onNodeWithText("Saved Data").performClick()
        rule.waitForText("Ann")
        rule.onNodeWithContentDescription("Clear All Data").performClick()
        rule.onNodeWithText("Delete All").assertExists()

        recreate()

        rule.waitForText("Delete All")
        rule.onAllNodesWithText("Delete All").assertCountEquals(1)
    }

    @Test
    fun aboutDialog_survivesRotation() {
        rule.waitForText("TABLE TOPICS (1 TO 2 MIN)")
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitForText("About")
        rule.onNodeWithText("About").performClick()
        rule.waitForText("About Simple Speech Timer")

        recreate()

        rule.waitForText("About Simple Speech Timer")
    }
}
