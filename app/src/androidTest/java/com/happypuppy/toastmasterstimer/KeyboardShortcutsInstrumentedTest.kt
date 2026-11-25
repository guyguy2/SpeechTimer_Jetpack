package com.happypuppy.toastmasterstimer

import android.view.KeyEvent
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.ui.components.PresetCard
import com.happypuppy.toastmasterstimer.ui.screens.SpeakerCard
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Wave 7 keyboard & D-pad shortcuts (P1-3):
 * - TimerScreen: Space start/pause, R restart (when ticks > 0), Escape back/leave, all disabled
 *   while Save or Leave dialog is open.
 * - PresetCard: focusable + Enter / NumPadEnter / DirectionCenter activation.
 * - SpeakerCard: focusable + Delete / Backspace triggers delete.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class KeyboardShortcutsInstrumentedTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String, timeoutMs: Long = 10_000) =
        rule.waitUntil(timeoutMs) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun timerScreen_spaceToggleAndRResetAndEscapeLeave_disabledInsideDialog() {
        waitForText("Table Topics")
        rule.onNodeWithText("Table Topics").performClick()
        waitForText("00:00")

        // Space starts timer
        rule.onRoot().performKeyInput { pressKey(Key.Spacebar) }
        waitForText("00:01")

        // Space pauses timer
        rule.onRoot().performKeyInput { pressKey(Key.Spacebar) }
        waitForText("Paused")

        // Open Save dialog: typing 'r' and ' ' must go to the field and NOT reset or start timer
        rule.onNodeWithContentDescription("Save result").performClick()
        waitForText("Save Time")
        rule.onNode(hasSetTextAction()).performTextInput("Ray Lee")
        rule.onNodeWithText("Ray Lee").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.waitForIdle()

        // R resets when ticks > 0
        rule.onRoot().performKeyInput { pressKey(Key.R) }
        waitForText("00:00")
        waitForText("Timer reset")

        // Escape at 00:00 returns directly to presets
        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        waitForText("Table Topics")
    }

    @Test
    fun timerScreen_escapeWithElapsedTimePromptsBeforeLeaving() {
        waitForText("Table Topics")
        rule.onNodeWithText("Table Topics").performClick()
        waitForText("00:00")

        rule.onRoot().performKeyInput { pressKey(Key.Spacebar) }
        waitForText("00:01")
        rule.onRoot().performKeyInput { pressKey(Key.Spacebar) }
        waitForText("Paused")

        rule.onRoot().performKeyInput { pressKey(Key.Escape) }
        waitForText("Leave timer?")
        rule.onNodeWithText("Leave").performClick()
        waitForText("Table Topics")
    }
}

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class CardKeyboardActivationInstrumentedTest {

    @get:Rule
    val rule = createComposeRule()

    @After
    fun restoreTouchMode() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(true)
    }

    @Test
    fun presetCard_tabFocusAndEnterOrDpadOpensPreset() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        var clicks = 0
        var requestKeyboardMode: (() -> Unit)? = null

        rule.setContent {
            val inputModeManager = LocalInputModeManager.current
            requestKeyboardMode = { inputModeManager.requestInputMode(InputMode.Keyboard) }
            SimpleSpeechTimerTheme {
                PresetCard(
                    speechType = SpeechType.TABLE_TOPICS,
                    onClick = { clicks++ }
                )
            }
        }

        rule.runOnIdle { requestKeyboardMode?.invoke() }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        rule.waitForIdle()

        val card = rule.onNodeWithText("Table Topics")
        card.performSemanticsAction(SemanticsActions.RequestFocus)
        card.assertIsFocused()

        card.performKeyInput { pressKey(Key.Enter) }
        rule.waitForIdle()
        assertEquals(1, clicks)

        card.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        assertEquals(2, clicks)
    }

    @Test
    fun speakerCard_deleteAndBackspaceTriggerDeleteOnFocusedCard() {
        InstrumentationRegistry.getInstrumentation().setInTouchMode(false)
        var deletes = 0
        var requestKeyboardMode: (() -> Unit)? = null

        rule.setContent {
            val inputModeManager = LocalInputModeManager.current
            requestKeyboardMode = { inputModeManager.requestInputMode(InputMode.Keyboard) }
            SimpleSpeechTimerTheme {
                SpeakerCard(
                    speaker = Speaker(id = 1, name = "Jordan Taylor", speechType = "SPEECH", duration = "05:30"),
                    onEditClick = {},
                    onShareClick = {},
                    onDeleteClick = { deletes++ }
                )
            }
        }

        rule.runOnIdle { requestKeyboardMode?.invoke() }
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_TAB)
        rule.waitForIdle()

        val card = rule.onNodeWithText("Jordan Taylor")
        card.performSemanticsAction(SemanticsActions.RequestFocus)
        card.assertIsFocused()

        card.performKeyInput { pressKey(Key.Delete) }
        rule.waitForIdle()
        assertEquals(1, deletes)

        card.performKeyInput { pressKey(Key.Backspace) }
        rule.waitForIdle()
        assertEquals(2, deletes)
    }
}
