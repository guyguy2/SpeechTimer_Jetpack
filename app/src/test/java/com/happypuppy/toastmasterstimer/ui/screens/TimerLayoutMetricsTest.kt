package com.happypuppy.toastmasterstimer.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerLayoutMetricsTest {

    @Test
    fun isSideBySide_matchesLandscapeAndExcludesPortraitOrNarrowWindows() {
        assertFalse(TimerLayoutMetrics.isSideBySide(360f, 800f))
        assertFalse(TimerLayoutMetrics.isSideBySide(840f, 1000f))
        assertFalse(TimerLayoutMetrics.isSideBySide(480f, 320f))
        assertTrue(TimerLayoutMetrics.isSideBySide(800f, 360f))
        assertTrue(TimerLayoutMetrics.isSideBySide(640f, 360f))
        assertTrue(TimerLayoutMetrics.isSideBySide(1000f, 840f))
    }

    @Test
    fun digitFontSizeSp_scalesUpForTabletRelativeToPhone() {
        val phoneSp = TimerLayoutMetrics.digitFontSizeSp(
            maxWidthDp = 360f,
            maxHeightDp = 700f,
            fontScale = 1.0f,
            isSideBySide = false
        )
        val tabletSp = TimerLayoutMetrics.digitFontSizeSp(
            maxWidthDp = 840f,
            maxHeightDp = 920f,
            fontScale = 1.0f,
            isSideBySide = false
        )

        assertTrue("Expected tablet digits ($tabletSp) > phone digits ($phoneSp)", tabletSp > phoneSp + 30f)
        assertTrue("Phone digits should be comfortably large ($phoneSp)", phoneSp >= 80f)
        assertTrue("Tablet digits should be capped cleanly ($tabletSp)", tabletSp <= TimerLayoutMetrics.MAX_RENDERED_DIGIT_DP)
    }

    @Test
    fun digitFontSizeSp_keepsFiveCharTimeWithinPaneWidthAtLargeFontScales() {
        val viewports = listOf(
            Triple(360f, 700f, false),
            Triple(800f, 280f, true),
            Triple(840f, 920f, false)
        )
        val fontScales = listOf(1.0f, 1.3f, 2.0f)

        viewports.forEach { (width, height, sideBySide) ->
            val availablePaneWidth = if (sideBySide) width * 0.56f - 32f else width - 32f
            fontScales.forEach { scale ->
                val sp = TimerLayoutMetrics.digitFontSizeSp(width, height, scale, sideBySide)
                val renderedDp = sp * scale
                val estimatedFiveCharWidth = renderedDp * 3.1f
                assertTrue(
                    "Digits overflow on ${width}x${height} @ ${scale}x: sp=$sp rendered=$renderedDp estWidth=$estimatedFiveCharWidth > $availablePaneWidth",
                    estimatedFiveCharWidth <= availablePaneWidth
                )
                assertTrue(
                    "Digits too small on ${width}x${height} @ ${scale}x: rendered=$renderedDp",
                    renderedDp >= TimerLayoutMetrics.MIN_RENDERED_DIGIT_DP
                )
            }
        }
    }
}
