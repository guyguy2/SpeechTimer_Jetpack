package com.happypuppy.toastmasterstimer

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimePicker
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The custom range steppers must be named after the field they change, not read as triangle glyphs. */
@RunWith(AndroidJUnit4::class)
class CustomSpeechSemanticsTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun show() = rule.setContent {
        MaterialTheme { SpeechTimePicker(navController = rememberNavController()) }
    }

    @Test
    fun steppersAreNamedAfterTheirField() {
        show()
        rule.onNodeWithContentDescription("Increase minimum minutes").assertExists()
        rule.onNodeWithContentDescription("Decrease minimum minutes").assertExists()
        rule.onNodeWithContentDescription("Increase maximum minutes").assertExists()
        rule.onNodeWithContentDescription("Decrease maximum minutes").assertExists()
    }

    @Test
    fun steppersDoNotExposeTheTriangleGlyphs() {
        show()
        rule.onAllNodesWithText("▲").assertCountEquals(0)
        rule.onAllNodesWithText("▼").assertCountEquals(0)
    }

    @Test
    fun steppersStillChangeTheirOwnField() {
        show()
        rule.onNodeWithText("Speech will last between 8 and 10 minutes").assertExists()

        rule.onNodeWithContentDescription("Increase minimum minutes").performClick()
        rule.onNodeWithText("Speech will last between 9 and 10 minutes").assertExists()

        rule.onNodeWithContentDescription("Increase maximum minutes").performClick()
        rule.onNodeWithText("Speech will last between 9 and 11 minutes").assertExists()

        rule.onNodeWithContentDescription("Decrease maximum minutes").performClick()
        rule.onNodeWithContentDescription("Decrease minimum minutes").performClick()
        rule.onNodeWithText("Speech will last between 8 and 10 minutes").assertExists()
    }
}
