package com.happypuppy.toastmasterstimer

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The app opens straight to the preset list (Wave 3b): no in-app Compose splash and no artificial
 * delay between the system splash and the first screen.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class StartupInstrumentedTest {

    @get:Rule
    val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    private fun mainScreenShown() =
        rule.onAllNodes(hasText("TABLE TOPICS (1 TO 2 MIN)"), useUnmergedTree = true)
            .fetchSemanticsNodes().isNotEmpty()

    @Test
    fun startsOnPresetsWithoutWaitingForSplashTimer() {
        rule.mainClock.autoAdvance = false
        var frames = 0
        val deadline = System.currentTimeMillis() + 10_000
        while (!mainScreenShown() && System.currentTimeMillis() < deadline) {
            rule.mainClock.advanceTimeByFrame()
            Thread.sleep(20)
            frames++
        }
        val virtualMs = frames * 16L

        assertTrue("main screen never shown", mainScreenShown())
        assertTrue("main screen needed $virtualMs ms of test clock, expected no splash delay", virtualMs < 1_000)
    }
}
