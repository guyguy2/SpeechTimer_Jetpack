package com.happypuppy.toastmasterstimer

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.navigation.TimerRoute
import com.happypuppy.toastmasterstimer.ui.components.displayTitle
import com.happypuppy.toastmasterstimer.ui.screens.SortOption
import com.happypuppy.toastmasterstimer.ui.screens.SpeakerError
import com.happypuppy.toastmasterstimer.ui.screens.SpeakerShareLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies on the Android runtime that all Wave 5 string/plural resources, error messages,
 * sort options, speech types, and composable display titles resolve properly.
 */
@RunWith(AndroidJUnit4::class)
class LocalizationResourcesTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun speechTypes_resolveAllLabelResourcesAndShareLabels() {
        val resolved = SpeechType.entries.map { context.getString(it.labelRes) }
        assertTrue(resolved.all { it.isNotBlank() })
        assertEquals(resolved.size, resolved.toSet().size)

        val shareLabels = SpeakerShareLabels.fromResources(context.resources)
        SpeechType.entries.forEach { type ->
            assertEquals(context.getString(type.labelRes), shareLabels.speechTypeLabels[type.id])
        }
    }

    @Test
    fun sortOptions_resolveDistinctNonBlankLabels() {
        val labels = SortOption.entries.map { context.getString(it.labelRes) }
        assertTrue(labels.all { it.isNotBlank() })
        assertEquals(labels.size, labels.toSet().size)
    }

    @Test
    fun speakerErrors_resolveDistinctNonBlankMessages() {
        val errors = listOf(
            SpeakerError.LoadFailed,
            SpeakerError.SaveFailed,
            SpeakerError.UpdateFailed,
            SpeakerError.DeleteFailed,
            SpeakerError.DeleteAllFailed,
            SpeakerError.RestoreFailed
        )
        val messages = errors.map { it.message.asString(context) }
        assertTrue(messages.all { it.isNotBlank() })
        assertEquals(messages.size, messages.toSet().size)
    }

    @Test
    fun customSpeechMinutesUnit_pluralizesOneAndOther() {
        val resources = context.resources
        assertEquals("minute", resources.getQuantityString(R.plurals.custom_speech_minutes_unit, 1))
        assertEquals("minutes", resources.getQuantityString(R.plurals.custom_speech_minutes_unit, 2))
    }

    @Test
    fun timerRouteDisplayTitle_formatsPresetsAndCustomRoutes() {
        val presetRoute = TimerRoute(SpeechType.TABLE_TOPICS.id, 1, 2)
        val defaultCustomRoute = TimerRoute(SpeechType.CUSTOM.id, 3, 8)
        val labeledCustomRoute = TimerRoute(SpeechType.CUSTOM.id, 4, 6, customLabel = "Club Contest")

        rule.setContent {
            Text(presetRoute.displayTitle())
            Text(defaultCustomRoute.displayTitle())
            Text(labeledCustomRoute.displayTitle())
        }

        rule.onNodeWithText("TABLE TOPICS (1 TO 2 MIN)").assertExists()
        rule.onNodeWithText("Custom Speech (3 - 8 min)").assertExists()
        rule.onNodeWithText("Club Contest (4 - 6 min)").assertExists()
    }
}
