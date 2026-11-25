package com.happypuppy.toastmasterstimer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.WindowSize
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerRepository
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.components.SWIPE_DELETE_BACKGROUND_TAG
import com.happypuppy.toastmasterstimer.ui.components.SWIPE_DELETE_ICON_TAG
import com.happypuppy.toastmasterstimer.ui.components.SwipeDeleteBackgroundColorKey
import com.happypuppy.toastmasterstimer.ui.components.SwipeToDeleteSpeakerItem
import com.happypuppy.toastmasterstimer.ui.layout.rememberAdaptiveLayoutSpec
import com.happypuppy.toastmasterstimer.ui.screens.SAVED_GRID_TAG
import com.happypuppy.toastmasterstimer.ui.screens.SAVED_LIST_TAG
import com.happypuppy.toastmasterstimer.ui.screens.SortOption
import com.happypuppy.toastmasterstimer.ui.screens.SpeechDataList
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import java.util.Date
import kotlin.math.abs

/**
 * Instrumented tests for Wave 7 Saved Data swipe-to-delete (EndToStart-only), multi-undo,
 * Clear-All Undo, and adaptive grid mode.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SavedDataSwipeAndUndoInstrumentedTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private val repository: SpeakerRepository
        get() = GlobalContext.get().get()

    private val dataStoreManager: DataStoreManager
        get() = GlobalContext.get().get()

    @Before
    fun cleanState() = runBlocking {
        repository.deleteAllSpeakers()
        dataStoreManager.setSavedSortOption(SortOption.DATE_NEWEST)
    }

    private fun waitForText(text: String, timeoutMs: Long = 10_000) =
        rule.waitUntil(timeoutMs) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun openSavedData() {
        waitForText("Table Topics")
        rule.onNodeWithText("Saved Data").performClick()
    }

    @Test
    fun endToStartSwipeDeletesAndUndoRestoresRowWithResetSwipeState() {
        runBlocking {
            repository.insertSpeaker(
                Speaker(name = "Alex Morgan", speechType = "TABLE TOPICS", duration = "01:30", createdAt = Date(2_000L))
            )
            repository.insertSpeaker(
                Speaker(name = "Sam Lee", speechType = "SPEECH", duration = "05:45", createdAt = Date(1_000L))
            )
        }

        openSavedData()
        waitForText("Alex Morgan")
        waitForText("Sam Lee")

        rule.onNodeWithText("Alex Morgan").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Alex Morgan").fetchSemanticsNodes().isEmpty() }
        waitForText("Undo")

        rule.onNodeWithText("Undo").performClick()
        waitForText("Alex Morgan")
        rule.waitForIdle()

        val backgrounds = rule.onAllNodesWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue(backgrounds.isNotEmpty())
        backgrounds.forEach { node ->
            assertEquals(
                "Restored card must reset swipe background to transparent",
                Color.Transparent,
                node.config.getOrNull(SwipeDeleteBackgroundColorKey)
            )
        }
    }

    @Test
    fun startToEndSwipeDoesNotDismiss() {
        runBlocking {
            repository.insertSpeaker(
                Speaker(name = "Alex Morgan", speechType = "TABLE TOPICS", duration = "01:30")
            )
        }

        openSavedData()
        waitForText("Alex Morgan")

        rule.onNodeWithText("Alex Morgan").performTouchInput { swipeRight() }
        rule.waitForIdle()

        rule.onNodeWithText("Alex Morgan").assertExists()
        assertEquals(1, runBlocking { repository.getAllSpeakers().first() }.size)
    }

    @Test
    fun consecutiveDeletesSupportLifoMultiUndo() {
        runBlocking {
            repository.insertSpeaker(
                Speaker(name = "Alex Morgan", speechType = "TABLE TOPICS", duration = "01:30", createdAt = Date(2_000L))
            )
            repository.insertSpeaker(
                Speaker(name = "Sam Lee", speechType = "SPEECH", duration = "05:45", createdAt = Date(1_000L))
            )
        }

        openSavedData()
        waitForText("Alex Morgan")
        waitForText("Sam Lee")

        rule.onNodeWithContentDescription("Delete Alex Morgan, TABLE TOPICS").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Alex Morgan").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithContentDescription("Delete Sam Lee, SPEECH").performClick()
        waitForText("No Saved Speech Data")

        // First Undo restores the most recently deleted row (Sam Lee) and surfaces the earlier Undo
        waitForText("Deleted Sam Lee's speech data")
        rule.onNodeWithText("Undo").performClick()
        waitForText("Sam Lee")

        // Second Undo restores Alex Morgan
        waitForText("Deleted Alex Morgan's speech data")
        rule.onNodeWithText("Undo").performClick()
        waitForText("Alex Morgan")
        waitForText("Sam Lee")
        assertEquals(2, runBlocking { repository.getAllSpeakers().first() }.size)
    }

    @Test
    fun clearAllConfirmAndUndoRestoresAllRows() {
        runBlocking {
            repository.insertSpeaker(Speaker(name = "Ann", speechType = "SPEECH", duration = "05:10"))
            repository.insertSpeaker(Speaker(name = "Bob", speechType = "EVALUATION", duration = "02:20"))
        }

        openSavedData()
        waitForText("Ann")
        waitForText("Bob")

        rule.onNodeWithContentDescription("Clear All Data").performClick()
        waitForText("Delete All")
        rule.onNodeWithText("Delete All").performClick()

        waitForText("No Saved Speech Data")
        waitForText("Undo")
        rule.onNodeWithText("Undo").performClick()

        waitForText("Ann")
        waitForText("Bob")
        assertEquals(2, runBlocking { repository.getAllSpeakers().first() }.size)
    }

    @Test
    fun threeRapidSwipeDeletes_restoreInLifoOrderWithResetSwipeState() {
        runBlocking {
            repository.insertSpeaker(
                Speaker(name = "Alex Morgan", speechType = "TABLE TOPICS", duration = "01:30", createdAt = Date(3_000L))
            )
            repository.insertSpeaker(
                Speaker(name = "Sam Lee", speechType = "SPEECH", duration = "05:45", createdAt = Date(2_000L))
            )
            repository.insertSpeaker(
                Speaker(name = "Jordan Taylor", speechType = "EVALUATION", duration = "02:30", createdAt = Date(1_000L))
            )
        }

        openSavedData()
        waitForText("Alex Morgan")
        waitForText("Sam Lee")
        waitForText("Jordan Taylor")

        rule.onNodeWithText("Alex Morgan").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Alex Morgan").fetchSemanticsNodes().isEmpty() }

        rule.onNodeWithText("Sam Lee").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Sam Lee").fetchSemanticsNodes().isEmpty() }

        rule.onNodeWithText("Jordan Taylor").performTouchInput { swipeLeft() }
        waitForText("No Saved Speech Data")

        // LIFO Undo: Jordan Taylor -> Sam Lee -> Alex Morgan
        waitForText("Deleted Jordan Taylor's speech data")
        rule.onNodeWithText("Undo").performClick()
        waitForText("Jordan Taylor")

        waitForText("Deleted Sam Lee's speech data")
        rule.onNodeWithText("Undo").performClick()
        waitForText("Sam Lee")

        waitForText("Deleted Alex Morgan's speech data")
        rule.onNodeWithText("Undo").performClick()
        waitForText("Alex Morgan")
        rule.waitForIdle()

        rule.onNodeWithText("Alex Morgan").assertExists()
        rule.onNodeWithText("Sam Lee").assertExists()
        rule.onNodeWithText("Jordan Taylor").assertExists()
        assertEquals(3, runBlocking { repository.getAllSpeakers().first() }.size)

        val backgrounds = rule.onAllNodesWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue(backgrounds.isNotEmpty())
        backgrounds.forEach { node ->
            assertEquals(
                "Restored cards must reset swipe background to transparent",
                Color.Transparent,
                node.config.getOrNull(SwipeDeleteBackgroundColorKey)
            )
        }
    }

    @Test
    fun editAndShareRemainFunctionalWhileAnotherRowIsPendingUndo() {
        runBlocking {
            repository.insertSpeaker(
                Speaker(name = "Alex Morgan", speechType = "TABLE TOPICS", duration = "01:30", createdAt = Date(2_000L))
            )
            repository.insertSpeaker(
                Speaker(name = "Sam Lee", speechType = "SPEECH", duration = "05:45", createdAt = Date(1_000L))
            )
        }

        openSavedData()
        waitForText("Alex Morgan")
        waitForText("Sam Lee")

        // Swipe-delete Alex Morgan so it sits on the undo stack with Undo snackbar active
        rule.onNodeWithText("Alex Morgan").performTouchInput { swipeLeft() }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Alex Morgan").fetchSemanticsNodes().isEmpty() }
        waitForText("Deleted Alex Morgan's speech data")

        // Edit Sam Lee while Alex Morgan is pending undo
        rule.onNodeWithContentDescription("Edit Sam Lee, SPEECH").performClick()
        waitForText("Edit Speaker Name")
        rule.onNode(hasSetTextAction()).performTextReplacement("Sam Updated")
        rule.onNodeWithText("Save").performClick()
        waitForText("Sam Updated")

        // Share action on the edited row remains interactive alongside pending undo
        rule.onNodeWithContentDescription("Share Sam Updated, SPEECH")
            .assertIsEnabled()
            .assertHasClickAction()

        // Undo restores Alex Morgan without clobbering Sam Updated
        waitForText("Undo")
        rule.onNodeWithText("Undo").performClick()
        waitForText("Alex Morgan")
        waitForText("Sam Updated")

        val names = runBlocking { repository.getAllSpeakers().first() }.map { it.name }.toSet()
        assertEquals(setOf("Alex Morgan", "Sam Updated"), names)
    }
}

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class SavedDataSwipeVisualAndGridInstrumentedTest {

    @get:Rule
    val rule = androidx.compose.ui.test.junit4.v2.createComposeRule()

    private val sampleSpeakers = listOf(
        Speaker(id = 1, name = "Alpha", speechType = "TABLE TOPICS", duration = "01:30"),
        Speaker(id = 2, name = "Bravo", speechType = "SPEECH", duration = "05:45")
    )

    @Test
    fun partialEndToStartDragRevealsEndAlignedIconAndDestructiveTintWithoutDismissing() {
        var deleteCount = 0
        rule.setContent {
            SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                Box(modifier = Modifier.width(360.dp).padding(16.dp)) {
                    SwipeToDeleteSpeakerItem(
                        speaker = sampleSpeakers.first(),
                        isVibrationEnabled = false,
                        onEditClick = {},
                        onShareClick = {},
                        onDelete = { deleteCount++ }
                    )
                }
            }
        }

        rule.onNodeWithTag(SWIPE_DELETE_ICON_TAG, useUnmergedTree = true).assertDoesNotExist()

        rule.onNodeWithText("Alpha").performTouchInput {
            down(center)
            moveBy(Offset(-width * 0.25f, 0f))
        }
        rule.waitForIdle()

        val bgNode = rule.onNodeWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true).fetchSemanticsNode()
        val iconBounds = rule.onNodeWithTag(SWIPE_DELETE_ICON_TAG, useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        val color = bgNode.config.getOrNull(SwipeDeleteBackgroundColorKey)

        assertNotNull(color)
        assertNotEquals(Color.Transparent, color)
        assertTrue("EndToStart icon must sit on end half", iconBounds.center.x > bgNode.boundsInRoot.center.x)
        assertEquals(0, deleteCount)

        rule.onNodeWithText("Alpha").performTouchInput { cancel() }
    }

    @Test
    fun wideGridDisablesSwipeWhileKeepingButtonsAndWidthCap() {
        var density = 1f
        val deleted = mutableListOf<String>()
        rule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.WindowSize(DpSize(840.dp, 900.dp))) {
                density = LocalDensity.current.density
                SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                    val spec = rememberAdaptiveLayoutSpec()
                    SpeechDataList(
                        speakers = sampleSpeakers,
                        onEditClick = {},
                        onShareClick = {},
                        onDeleteClick = { deleted += it.name },
                        useGrid = spec.useSavedDataGrid,
                        enableSwipeToDelete = spec.enableSwipeToDelete,
                        isVibrationEnabled = false
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithTag(SAVED_GRID_TAG).assertExists()
        rule.onNodeWithTag(SAVED_LIST_TAG).assertDoesNotExist()
        assertEquals(
            0,
            rule.onAllNodesWithTag(SWIPE_DELETE_BACKGROUND_TAG, useUnmergedTree = true)
                .fetchSemanticsNodes().size
        )

        val alphaBounds = rule.onNodeWithText("Alpha").fetchSemanticsNode().boundsInRoot
        val bravoBounds = rule.onNodeWithText("Bravo").fetchSemanticsNode().boundsInRoot
        assertTrue("Grid items should share row", abs(alphaBounds.top - bravoBounds.top) < 2f)
        assertTrue("Second card should sit right of first", bravoBounds.left > alphaBounds.right)

        val gridWidthDp = rule.onNodeWithTag(SAVED_GRID_TAG).fetchSemanticsNode().boundsInRoot.width / density
        assertTrue("Grid width ($gridWidthDp dp) must be capped at <= 840dp", gridWidthDp <= 841f)

        rule.onNodeWithContentDescription("Delete Alpha, TABLE TOPICS").performClick()
        assertEquals(listOf("Alpha"), deleted)
    }

    @Test
    fun restoreVersionIncrement_resetsDeleteDispatchedAndAllowsRetry() {
        var restoreVersion by mutableIntStateOf(0)
        var deleteCalls = 0
        var editCalls = 0
        var shareCalls = 0
        rule.setContent {
            SimpleSpeechTimerTheme(themePreference = AppTheme.LIGHT, dynamicColor = false) {
                Box(modifier = Modifier.width(360.dp).padding(16.dp)) {
                    SwipeToDeleteSpeakerItem(
                        speaker = sampleSpeakers.first(),
                        isVibrationEnabled = false,
                        onEditClick = { editCalls++ },
                        onShareClick = { shareCalls++ },
                        onDelete = { deleteCalls++ },
                        restoreVersion = restoreVersion
                    )
                }
            }
        }
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Edit Alpha, TABLE TOPICS").performClick()
        rule.onNodeWithContentDescription("Share Alpha, TABLE TOPICS").performClick()
        assertEquals(1, editCalls)
        assertEquals(1, shareCalls)

        rule.onNodeWithContentDescription("Delete Alpha, TABLE TOPICS").performClick()
        assertEquals(1, deleteCalls)

        // Subsequent tap before restoreVersion increments is suppressed by deleteDispatched
        rule.onNodeWithContentDescription("Delete Alpha, TABLE TOPICS").performClick()
        assertEquals(1, deleteCalls)

        // Incrementing restoreVersion (e.g. failed delete or undo restore) unlocks delete again
        rule.runOnIdle { restoreVersion = 1 }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Delete Alpha, TABLE TOPICS").performClick()
        assertEquals(2, deleteCalls)
    }
}
