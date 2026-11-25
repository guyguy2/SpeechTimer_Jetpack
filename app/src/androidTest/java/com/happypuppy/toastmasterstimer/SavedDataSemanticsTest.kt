package com.happypuppy.toastmasterstimer

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.ui.screens.SpeechDataList
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Per-row Share and Delete must say which speaker and speech they act on. */
@RunWith(AndroidJUnit4::class)
class SavedDataSemanticsTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val alex = Speaker(id = 1, name = "Alex Morgan", speechType = "TABLE TOPICS", duration = "01:30")
    private val sam = Speaker(id = 2, name = "Sam Lee", speechType = "SPEECH", duration = "05:10")

    private val edited = mutableListOf<String>()
    private val shared = mutableListOf<String>()
    private val deleted = mutableListOf<String>()

    private fun show() = rule.setContent {
        MaterialTheme {
            SpeechDataList(
                speakers = listOf(alex, sam),
                onEditClick = { edited += it.name },
                onShareClick = { shared += it.name },
                onDeleteClick = { deleted += it.name }
            )
        }
    }

    @Test
    fun everyRowNamesItsSpeakerAndSpeechType() {
        show()
        rule.onNodeWithContentDescription("Edit Alex Morgan, TABLE TOPICS").assertExists()
        rule.onNodeWithContentDescription("Delete Alex Morgan, TABLE TOPICS").assertExists()
        rule.onNodeWithContentDescription("Share Alex Morgan, TABLE TOPICS").assertExists()
        rule.onNodeWithContentDescription("Edit Sam Lee, SPEECH").assertExists()
        rule.onNodeWithContentDescription("Delete Sam Lee, SPEECH").assertExists()
        rule.onNodeWithContentDescription("Share Sam Lee, SPEECH").assertExists()
    }

    @Test
    fun noRowUsesTheBareDeleteOrShareLabel() {
        show()
        rule.onAllNodesWithContentDescription("Edit").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Delete").assertCountEquals(0)
        rule.onAllNodesWithContentDescription("Share").assertCountEquals(0)
    }

    @Test
    fun labelledButtonsStillActOnTheirOwnRow() {
        show()
        rule.onNodeWithContentDescription("Edit Alex Morgan, TABLE TOPICS").performClick()
        rule.onNodeWithContentDescription("Delete Sam Lee, SPEECH").performClick()
        rule.onNodeWithContentDescription("Share Alex Morgan, TABLE TOPICS").performClick()
        assertEquals(listOf("Alex Morgan"), edited)
        assertEquals(listOf("Sam Lee"), deleted)
        assertEquals(listOf("Alex Morgan"), shared)
    }
}
