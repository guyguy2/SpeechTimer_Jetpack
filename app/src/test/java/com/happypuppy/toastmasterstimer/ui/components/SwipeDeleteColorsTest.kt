package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.happypuppy.toastmasterstimer.ui.theme.DarkColorScheme
import com.happypuppy.toastmasterstimer.ui.theme.LightColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies two-stage destructive swipe visuals and WCAG contrast ratios for both light and dark
 * brand color schemes (Wave 7 UX-ST-1).
 */
@OptIn(ExperimentalMaterial3Api::class)
class SwipeDeleteColorsTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val effective = foreground.compositeOver(background)
        val a = effective.luminance().toDouble() + 0.05
        val b = background.luminance().toDouble() + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    @Test
    fun settledDirection_hidesIconAndUsesTransparentBackgroundInBothSchemes() {
        for ((scheme, isDark) in listOf(LightColorScheme to false, DarkColorScheme to true)) {
            val visuals = resolveSwipeDeleteVisuals(
                colorScheme = scheme,
                isDarkTheme = isDark,
                dismissDirection = SwipeToDismissBoxValue.Settled,
                targetValue = SwipeToDismissBoxValue.Settled
            )
            assertEquals(Color.Transparent, visuals.backgroundColor)
            assertFalse(visuals.showIcon)
            assertFalse(visuals.isThresholdCrossed)
        }
    }

    @Test
    fun iconAlignment_followsDismissDirection() {
        val startToEnd = resolveSwipeDeleteVisuals(
            colorScheme = LightColorScheme,
            isDarkTheme = false,
            dismissDirection = SwipeToDismissBoxValue.StartToEnd,
            targetValue = SwipeToDismissBoxValue.Settled
        )
        val endToStart = resolveSwipeDeleteVisuals(
            colorScheme = LightColorScheme,
            isDarkTheme = false,
            dismissDirection = SwipeToDismissBoxValue.EndToStart,
            targetValue = SwipeToDismissBoxValue.Settled
        )

        assertTrue(startToEnd.showIcon)
        assertEquals(Alignment.CenterStart, startToEnd.iconAlignment)
        assertTrue(endToStart.showIcon)
        assertEquals(Alignment.CenterEnd, endToStart.iconAlignment)
    }

    @Test
    fun stage1PreThreshold_showsOpaqueDestructiveTintWithHighIconContrast() {
        for ((name, scheme, isDark) in listOf(
            Triple("light", LightColorScheme, false),
            Triple("dark", DarkColorScheme, true)
        )) {
            val stage1 = resolveSwipeDeleteVisuals(
                colorScheme = scheme,
                isDarkTheme = isDark,
                dismissDirection = SwipeToDismissBoxValue.EndToStart,
                targetValue = SwipeToDismissBoxValue.Settled
            )
            assertTrue("$name should show icon", stage1.showIcon)
            assertFalse("$name should not be threshold-crossed", stage1.isThresholdCrossed)
            assertNotEquals("$name background must not be transparent", Color.Transparent, stage1.backgroundColor)
            assertEquals("$name background should be opaque", 1f, stage1.backgroundColor.alpha, 0.001f)

            val ratio = contrast(stage1.iconTint, stage1.backgroundColor)
            assertTrue("$name stage 1 contrast %.2f:1 must reach 3.0:1".format(ratio), ratio >= 3.0)
        }
    }

    @Test
    fun stage2PastThreshold_escalatesToBoldDestructiveColorsWith4_5To1Contrast() {
        val lightStage1 = resolveSwipeDeleteVisuals(
            colorScheme = LightColorScheme,
            isDarkTheme = false,
            dismissDirection = SwipeToDismissBoxValue.EndToStart,
            targetValue = SwipeToDismissBoxValue.Settled
        )
        val lightStage2 = resolveSwipeDeleteVisuals(
            colorScheme = LightColorScheme,
            isDarkTheme = false,
            dismissDirection = SwipeToDismissBoxValue.EndToStart,
            targetValue = SwipeToDismissBoxValue.EndToStart
        )
        assertTrue(lightStage2.showIcon)
        assertTrue(lightStage2.isThresholdCrossed)
        assertEquals(LightColorScheme.error, lightStage2.backgroundColor)
        assertEquals(LightColorScheme.onError, lightStage2.iconTint)
        assertNotEquals(lightStage1.backgroundColor, lightStage2.backgroundColor)
        val lightContrast = contrast(lightStage2.iconTint, lightStage2.backgroundColor)
        assertTrue("light stage 2 contrast %.2f:1 must reach 4.5:1".format(lightContrast), lightContrast >= 4.5)

        val darkStage1 = resolveSwipeDeleteVisuals(
            colorScheme = DarkColorScheme,
            isDarkTheme = true,
            dismissDirection = SwipeToDismissBoxValue.EndToStart,
            targetValue = SwipeToDismissBoxValue.Settled
        )
        val darkStage2 = resolveSwipeDeleteVisuals(
            colorScheme = DarkColorScheme,
            isDarkTheme = true,
            dismissDirection = SwipeToDismissBoxValue.EndToStart,
            targetValue = SwipeToDismissBoxValue.EndToStart
        )
        assertTrue(darkStage2.showIcon)
        assertTrue(darkStage2.isThresholdCrossed)
        assertEquals(DarkColorScheme.errorContainer, darkStage2.backgroundColor)
        assertEquals(DarkColorScheme.onErrorContainer, darkStage2.iconTint)
        assertNotEquals(darkStage1.backgroundColor, darkStage2.backgroundColor)
        val darkContrast = contrast(darkStage2.iconTint, darkStage2.backgroundColor)
        assertTrue("dark stage 2 contrast %.2f:1 must reach 4.5:1".format(darkContrast), darkContrast >= 4.5)
    }
}
