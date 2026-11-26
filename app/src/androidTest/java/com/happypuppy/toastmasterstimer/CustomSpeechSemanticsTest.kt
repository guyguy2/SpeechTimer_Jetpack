package com.happypuppy.toastmasterstimer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.screens.CUSTOM_RANGE_SLIDER_TAG
import com.happypuppy.toastmasterstimer.ui.screens.SpeechTimePicker
import com.happypuppy.toastmasterstimer.ui.theme.SimpleSpeechTimerTheme
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** The custom range steppers must be named after the field they change, not read as triangle glyphs. */
@RunWith(AndroidJUnit4::class)
class CustomSpeechSemanticsTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun clearPersistedCustomRange() = runBlocking {
        GlobalContext.get().get<DataStoreManager>().clearLastCustomRange()
    }

    private fun show() = rule.setContent {
        SimpleSpeechTimerTheme { SpeechTimePicker(navController = rememberNavController()) }
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

    @Test
    fun quickPresetChip_updatesRangeAndLiveThresholdsPreview() {
        show()
        rule.onNodeWithText("Green 8:00").assertExists()
        rule.onNodeWithText("Yellow 9:00").assertExists()
        rule.onNodeWithText("Red 10:00").assertExists()

        rule.onNodeWithText("20\u201330 min").performClick()
        rule.onNodeWithText("Speech will last between 20 and 30 minutes").assertExists()
        rule.onNodeWithText("Green 20:00").assertExists()
        rule.onNodeWithText("Yellow 25:00").assertExists()
        rule.onNodeWithText("Red 30:00").assertExists()
    }

    @Test
    fun holdToRepeat_stepsContinuouslyWhileHeld() {
        show()
        rule.waitForIdle()
        rule.onNodeWithText("Speech will last between 8 and 10 minutes").assertExists()

        val increaseMin = rule.onNodeWithContentDescription("Increase minimum minutes")
        increaseMin.performTouchInput { down(center) }
        try {
            // Advance virtual time in 48ms increments (< 90ms repeat interval) so TapIndicationDelay (100ms) +
            // initial delay (400ms) + repeats do not spin 38+ full waitForIdle() frames against a 3s clock.
            rule.waitUntil(10_000) {
                rule.mainClock.advanceTimeBy(48)
                rule.onAllNodesWithText("11").fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            increaseMin.performTouchInput { up() }
        }
        rule.waitForIdle()
        rule.onAllNodesWithText("8").assertCountEquals(0)
    }

    @Test
    fun steppers_disableAtBounds() {
        show()
        rule.onNodeWithText("3\u20135 min").performClick()
        rule.onNodeWithContentDescription("Decrease minimum minutes").assertIsEnabled()

        rule.onNodeWithContentDescription("Decrease minimum minutes").performClick()
        rule.onNodeWithContentDescription("Decrease minimum minutes").performClick()
        rule.onNodeWithText("Speech will last between 1 and 5 minutes").assertExists()
        rule.onNodeWithContentDescription("Decrease minimum minutes").assertIsNotEnabled()
    }

    @Test
    fun quickPresetChip_upwardAndDownwardJumpsKeepSliderThumbsSynced() {
        show()
        rule.onNodeWithText("Speech will last between 8 and 10 minutes").assertExists()

        // Upward jump past stale endValue (10 -> 20..30) must advance endValue before startValue.
        rule.onNodeWithText("20\u201330 min").performClick()
        rule.onNodeWithText("Speech will last between 20 and 30 minutes").assertExists()
        rule.onNode(
            hasAnyAncestor(hasTestTag(CUSTOM_RANGE_SLIDER_TAG)) and hasStateDescription("20.0")
        ).assertExists()
        rule.onNode(
            hasAnyAncestor(hasTestTag(CUSTOM_RANGE_SLIDER_TAG)) and hasStateDescription("30.0")
        ).assertExists()

        // Subsequent stepper adjustment from the upper range stays synced at 21..30.
        rule.onNodeWithContentDescription("Increase minimum minutes").performClick()
        rule.onNodeWithText("Speech will last between 21 and 30 minutes").assertExists()
        rule.onNode(
            hasAnyAncestor(hasTestTag(CUSTOM_RANGE_SLIDER_TAG)) and hasStateDescription("21.0")
        ).assertExists()

        // Downward jump below stale startValue (21 -> 3..5) must lower startValue before endValue.
        rule.onNodeWithText("3\u20135 min").performClick()
        rule.onNodeWithText("Speech will last between 3 and 5 minutes").assertExists()
        rule.onNode(
            hasAnyAncestor(hasTestTag(CUSTOM_RANGE_SLIDER_TAG)) and hasStateDescription("3.0")
        ).assertExists()
        rule.onNode(
            hasAnyAncestor(hasTestTag(CUSTOM_RANGE_SLIDER_TAG)) and hasStateDescription("5.0")
        ).assertExists()
    }

    @Test
    fun holdToBound_doesNotSwallowNextClickAfterBackingOff() {
        show()
        rule.onNodeWithText("3\u20135 min").performClick()
        rule.onNodeWithText("Speech will last between 3 and 5 minutes").assertExists()

        val decreaseMin = rule.onNodeWithContentDescription("Decrease minimum minutes")
        decreaseMin.performTouchInput { down(center) }
        try {
            rule.waitUntil(10_000) {
                rule.mainClock.advanceTimeBy(48)
                rule.onAllNodes(
                    hasContentDescription("Decrease minimum minutes") and isNotEnabled()
                ).fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            decreaseMin.performTouchInput { up() }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Speech will last between 1 and 5 minutes").assertExists()

        // Step up once, then immediately click Decrease (must not be swallowed by stale didHoldDecrease).
        rule.onNodeWithContentDescription("Increase minimum minutes").performClick()
        rule.onNodeWithText("Speech will last between 2 and 5 minutes").assertExists()
        rule.onNodeWithContentDescription("Decrease minimum minutes").performClick()
        rule.onNodeWithText("Speech will last between 1 and 5 minutes").assertExists()

        // Verify the same for maximum minutes bound (5 -> 2, where min=1).
        val decreaseMax = rule.onNodeWithContentDescription("Decrease maximum minutes")
        decreaseMax.performTouchInput { down(center) }
        try {
            rule.waitUntil(10_000) {
                rule.mainClock.advanceTimeBy(48)
                rule.onAllNodes(
                    hasContentDescription("Decrease maximum minutes") and isNotEnabled()
                ).fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            decreaseMax.performTouchInput { up() }
        }
        rule.waitForIdle()
        rule.onNodeWithText("Speech will last between 1 and 2 minutes").assertExists()

        rule.onNodeWithContentDescription("Increase maximum minutes").performClick()
        rule.onNodeWithText("Speech will last between 1 and 3 minutes").assertExists()
        rule.onNodeWithContentDescription("Decrease maximum minutes").performClick()
        rule.onNodeWithText("Speech will last between 1 and 2 minutes").assertExists()
    }
}
