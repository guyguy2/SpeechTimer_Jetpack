package com.happypuppy.toastmasterstimer

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Black-box UI flows through the real app (real Koin graph, real Room DB). They deliberately
 * assert on user-visible text and content descriptions only, so they keep passing when the
 * string-path timer route and label regex are replaced by typed routes.
 */
@RunWith(AndroidJUnit4::class)
class TimerFlowsTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val repository: SpeakerRepository
        get() = GlobalContext.get().get()

    @Before
    fun clearDatabase() = runBlocking {
        repository.deleteAllSpeakers()
    }

    private fun AndroidComposeTestRule<*, *>.waitForText(text: String, timeoutMs: Long = 10_000) {
        try {
            waitUntil(timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: androidx.compose.ui.test.ComposeTimeoutException) {
            throw AssertionError("Text '$text' not found. Tree:\n" + onRoot().printToString(maxDepth = 12), e)
        }
    }

    private fun awaitMainScreen() = rule.waitForText("TABLE TOPICS (1 TO 2 MIN)")

    private fun openTableTopicsTimer() {
        awaitMainScreen()
        rule.onNodeWithText("TABLE TOPICS (1 TO 2 MIN)").performClick()
        rule.waitForText("00:00")
    }

    @Test
    fun mainScreen_listsAllFourPresets() {
        awaitMainScreen()
        rule.onNodeWithText("EVALUATION (2 TO 3 MIN)").assertExists()
        rule.onNodeWithText("ICE BREAKER (4 TO 6 MIN)").assertExists()
        rule.onNodeWithText("SPEECH (5 TO 7 MIN)").assertExists()
    }

    @Test
    fun presetTap_opensTimerWithThatTitleAtZero() {
        awaitMainScreen()
        rule.onNodeWithText("ICE BREAKER (4 TO 6 MIN)").performClick()

        rule.waitForText("00:00")
        rule.onNodeWithText("ICE BREAKER (4 TO 6 MIN)").assertExists()
        rule.onNodeWithContentDescription("play").assertExists()
    }

    @Test
    fun timerScreen_showsForegroundOnlyHint() {
        openTableTopicsTimer()
        rule.onNodeWithText("Keep this screen open for alerts").assertExists()
    }

    @Test
    fun settings_statesThatAlertsNeedTheAppOpen() {
        awaitMainScreen()
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitForText(
            "Alerts only play while this app is open. Keep the timer screen open and the screen on while timing."
        )
    }

    @Test
    fun startThenPause_timeAdvancesAndControlsSwap() {
        openTableTopicsTimer()

        rule.onNodeWithContentDescription("play").performClick()
        rule.waitForText("00:02")
        rule.onNodeWithContentDescription("pause").performClick()

        rule.onNodeWithContentDescription("play").assertExists()
        rule.onNodeWithContentDescription("save").assertExists()
        rule.onNodeWithContentDescription("restart").assertExists()
    }

    @Test
    fun restart_returnsTimerToZeroAndHidesSaveAndRestart() {
        openTableTopicsTimer()
        rule.onNodeWithContentDescription("play").performClick()
        rule.waitForText("00:01")
        rule.onNodeWithContentDescription("pause").performClick()

        rule.onNodeWithContentDescription("restart").performClick()

        rule.waitForText("00:00")
        rule.onAllNodesWithText("00:01").assertCountEquals(0)
    }

    @Test
    fun runningTimer_survivesActivityRecreation() {
        openTableTopicsTimer()
        rule.onNodeWithContentDescription("play").performClick()
        rule.waitForText("00:02")

        rule.activityRule.scenario.recreate()

        rule.waitForText("TABLE TOPICS (1 TO 2 MIN)")
        rule.onNodeWithContentDescription("pause").assertExists()
        rule.onAllNodesWithText("00:00").assertCountEquals(0)
    }

    @Test
    fun saveResult_appearsInSavedDataAndCanBeDeleted() {
        openTableTopicsTimer()
        rule.onNodeWithContentDescription("play").performClick()
        rule.waitForText("00:02")
        rule.onNodeWithContentDescription("pause").performClick()

        rule.onNodeWithContentDescription("save").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Alex Morgan")
        rule.onNodeWithText("Save").performClick()

        // FLIPPED in Wave 1 (P1-9): the VIEW DATA action used to be skipped because a tap on it did
        // nothing (the content overlay in TimerScreen sat above the snackbar host). It must navigate.
        rule.waitForText("Saved Alex Morgan's speech data")
        rule.onNodeWithText("VIEW DATA").performClick()

        rule.waitForText("Alex Morgan")
        rule.onNodeWithText("TABLE TOPICS").assertExists()

        rule.onNodeWithContentDescription("Delete").performClick()
        rule.waitForText("No Saved Speech Data")
        check(runBlocking { repository.getAllSpeakers().first() }.isEmpty())
    }

    @Test
    fun customRange_stepsClampAndStartTimerWithThatTitle() {
        awaitMainScreen()
        rule.onNodeWithText("Custom").performClick()
        rule.waitForText("Speech will last between 8 and 10 minutes")

        // "From" up: 9 (end stays 10), then 10 (end pushed to 11).
        rule.onAllNodesWithText("▲")[0].performClick()
        rule.waitForText("Speech will last between 9 and 10 minutes")
        rule.onAllNodesWithText("▲")[0].performClick()
        rule.waitForText("Speech will last between 10 and 11 minutes")

        rule.onNodeWithText("Start Timer").performClick()

        rule.waitForText("Custom Speech (10 - 11 min)")
        rule.waitForText("00:00")
    }

    @Test
    fun clearAll_confirmDialogEmptiesTheSavedList() {
        runBlocking {
            repository.insertSpeaker(Speaker(name = "Ann", speechType = "SPEECH", duration = "05:10"))
            repository.insertSpeaker(Speaker(name = "Bob", speechType = "EVALUATION", duration = "02:20"))
        }
        awaitMainScreen()
        rule.onNodeWithText("Saved Data").performClick()
        rule.waitForText("Ann")
        rule.onNodeWithText("Bob").assertExists()

        rule.onNodeWithContentDescription("Clear All Data").performClick()
        rule.onNodeWithText("Delete All").performClick()

        rule.waitForText("No Saved Speech Data")
    }

    @Test
    fun emptySavedList_offersAWayBackToThePresets() {
        awaitMainScreen()
        rule.onNodeWithText("Saved Data").performClick()
        rule.waitForText("No Saved Speech Data")

        rule.onNodeWithText("Add New Speech").performClick()

        rule.waitForText("TABLE TOPICS (1 TO 2 MIN)")
    }
}
