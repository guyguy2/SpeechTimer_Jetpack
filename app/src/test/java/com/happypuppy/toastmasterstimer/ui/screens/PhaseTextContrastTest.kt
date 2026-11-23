package com.happypuppy.toastmasterstimer.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.x contrast for the small text drawn on the phase backgrounds, using the production
 * colors and hint opacity (not copies). Small text needs 4.5:1.
 */
class PhaseTextContrastTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val effective = foreground.compositeOver(background)
        val a = effective.luminance().toDouble() + 0.05
        val b = background.luminance().toDouble() + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    private fun assertReadable(name: String, foreground: Color, background: Color) {
        val ratio = contrast(foreground, background)
        assertTrue("$name contrast is %.2f:1, needs at least 4.5:1".format(ratio), ratio >= 4.5)
    }

    private val phases = listOf(
        Triple("GREEN", PhaseOnGreenColor, PhaseGreenColor),
        Triple("YELLOW", PhaseOnYellowColor, PhaseYellowColor),
        Triple("RED", PhaseOnRedColor, PhaseRedColor)
    )

    @Test
    fun alertsHint_isReadableOnEveryPhaseBackground() {
        phases.forEach { (name, onColor, background) ->
            assertReadable("hint on $name", onColor.copy(alpha = HintTextAlpha), background)
        }
    }

    @Test
    fun timerRestoredLabel_isReadableOnEveryPhaseBackground() {
        phases.forEach { (name, onColor, background) ->
            assertReadable("restored label on $name", onColor, background)
        }
    }
}
