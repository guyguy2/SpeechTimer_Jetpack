package com.happypuppy.toastmasterstimer

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.screens.SortOption
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/**
 * Accessibility-check (ATF) tests for the screens other than the timer, one per screen or state,
 * through the real app. Dialogs live in their own window, so the checks run on every root.
 */
@RunWith(AndroidJUnit4::class)
class ScreenAccessibilityTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val repository: SpeakerRepository
        get() = GlobalContext.get().get()

    private val dataStoreManager: DataStoreManager
        get() = GlobalContext.get().get()

    @Before
    fun prepare() {
        runBlocking {
            repository.deleteAllSpeakers()
            dataStoreManager.setSavedSortOption(SortOption.DATE_NEWEST)
        }
        rule.enableAccessibilityChecks()
    }

    private fun waitForText(text: String) =
        rule.waitUntil(10_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun checkAllWindows() {
        rule.waitForIdle()
        rule.onAllNodes(isRoot()).tryPerformAccessibilityChecks()
    }

    @Test
    fun main_passesAccessibilityChecks() {
        waitForText("Table Topics")
        checkAllWindows()
    }

    @Test
    fun customSpeech_passesAccessibilityChecks() {
        waitForText("Table Topics")
        rule.onNodeWithText("Custom").performClick()
        waitForText("Speech will last between 8 and 10 minutes")
        checkAllWindows()
    }

    @Test
    fun savedDataEmpty_passesAccessibilityChecks() {
        waitForText("Table Topics")
        rule.onNodeWithText("Saved Data").performClick()
        waitForText("No Saved Speech Data")
        checkAllWindows()
    }

    @Test
    fun savedDataWithARow_passesAccessibilityChecks() {
        runBlocking { repository.insertSpeaker(Speaker(name = "Ann", speechType = "SPEECH", duration = "05:10")) }
        waitForText("Table Topics")
        rule.onNodeWithText("Saved Data").performClick()
        waitForText("Ann")
        checkAllWindows()
    }

    @Test
    fun settings_passesAccessibilityChecks() {
        waitForText("Table Topics")
        rule.onNodeWithContentDescription("Settings").performClick()
        waitForText("Theme")
        checkAllWindows()
    }

    @Test
    fun saveDialog_passesAccessibilityChecks() {
        waitForText("Table Topics")
        rule.onNodeWithText("Table Topics").performClick()
        waitForText("00:00")
        rule.onNodeWithContentDescription("Start timer").performClick()
        waitForText("00:01")
        rule.onNodeWithContentDescription("Pause timer").performClick()
        rule.onNodeWithContentDescription("Save result").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Alex Morgan")
        waitForText("Save Time")
        checkAllWindows()
    }

    @Test
    fun backConfirmDialog_passesAccessibilityChecks() {
        waitForText("Table Topics")
        rule.onNodeWithText("Table Topics").performClick()
        waitForText("00:00")
        rule.onNodeWithContentDescription("Start timer").performClick()
        waitForText("00:01")
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        waitForText("Leave timer?")
        checkAllWindows()
    }
}
