package com.happypuppy.toastmasterstimer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.screens.SAVED_GRID_TAG
import com.happypuppy.toastmasterstimer.ui.screens.SavedDataScreen
import com.happypuppy.toastmasterstimer.ui.screens.SortOption
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.util.Date

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
    fun mainPopulated_passesAccessibilityChecks() {
        runBlocking {
            repository.insertSpeaker(
                Speaker(
                    name = "Ann",
                    speechType = "SPEECH",
                    duration = "05:10",
                    phaseReached = "GREEN",
                    createdAt = Date(2_000L)
                )
            )
            repository.insertSpeaker(
                Speaker(
                    name = "Bob",
                    speechType = "TABLE TOPICS",
                    duration = "01:40",
                    phaseReached = "YELLOW",
                    createdAt = Date(1_000L)
                )
            )
        }
        waitForText("Recent")
        waitForText("Ann")
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

/**
 * Adaptive wide-screen accessibility checks (ATF) in real landscape orientation so Compose renders
 * the 2-column Saved Data grid at true 1:1 physical display density without density downscaling.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class AdaptiveScreenAccessibilityTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val repository: SpeakerRepository
        get() = GlobalContext.get().get()

    private val dataStoreManager: DataStoreManager
        get() = GlobalContext.get().get()

    @Before
    fun prepare() {
        runBlocking {
            repository.deleteAllSpeakers()
            dataStoreManager.setSavedSortOption(SortOption.DATE_NEWEST)
            repository.insertSpeaker(
                Speaker(
                    name = "Ann",
                    speechType = "SPEECH",
                    duration = "05:10",
                    phaseReached = "GREEN",
                    createdAt = Date(2_000L)
                )
            )
            repository.insertSpeaker(
                Speaker(
                    name = "Bob",
                    speechType = "TABLE TOPICS",
                    duration = "01:40",
                    phaseReached = "YELLOW",
                    createdAt = Date(1_000L)
                )
            )
        }
        rule.enableAccessibilityChecks()
    }

    @After
    fun restoreOrientation() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<ComponentActivity>()
                .forEach { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun switchToWideLandscape() {
        rule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val deadlineMs = SystemClock.elapsedRealtime() + 10_000L
        var isWideLandscape = false
        while (!isWideLandscape && SystemClock.elapsedRealtime() < deadlineMs) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { it is ComponentActivity }
                if (resumed != null) {
                    val config = resumed.resources.configuration
                    isWideLandscape =
                        config.orientation == Configuration.ORIENTATION_LANDSCAPE &&
                            config.screenWidthDp >= 600
                }
            }
            if (!isWideLandscape) {
                SystemClock.sleep(50L)
            }
        }
        assertTrue("Expected landscape width >= 600dp for wide Saved Data grid", isWideLandscape)
    }

    @Test
    fun savedDataWideGrid_passesAccessibilityChecks() {
        switchToWideLandscape()
        rule.setContent {
            SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                SavedDataScreen(navController = rememberNavController())
            }
        }
        rule.waitUntilExactlyOneExists(hasTestTag(SAVED_GRID_TAG), timeoutMillis = 10_000)
        rule.waitUntilAtLeastOneExists(hasText("Ann"), timeoutMillis = 10_000)
        rule.waitForIdle()
        rule.onNodeWithTag(SAVED_GRID_TAG).assertExists()
        rule.onAllNodes(isRoot()).tryPerformAccessibilityChecks()
    }
}
