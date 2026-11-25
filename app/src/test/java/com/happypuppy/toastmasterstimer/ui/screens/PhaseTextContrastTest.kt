package com.happypuppy.toastmasterstimer.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.happypuppy.toastmasterstimer.ui.theme.DarkColorScheme
import com.happypuppy.toastmasterstimer.ui.theme.LightColorScheme
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

    /**
     * Idle (before green) the phase background is transparent, so the hint is the theme's
     * onBackground at [HintTextAlpha] over the theme's background. Only the static brand schemes can
     * be checked here: the app uses dynamic color on API 31+ at runtime (SimpleSpeechTimerTheme
     * default), and those schemes are built from the device wallpaper, which a JVM test cannot
     * produce.
     */
    @Test
    fun alertsHint_isReadableOnTheIdleBackgroundInStaticLightAndDarkSchemes() {
        listOf("light" to LightColorScheme, "dark" to DarkColorScheme).forEach { (name, scheme) ->
            assertReadable(
                "hint on idle $name background",
                scheme.onBackground.copy(alpha = HintTextAlpha),
                scheme.background
            )
        }
    }

    @Test
    fun timerRestoredLabel_isReadableOnEveryPhaseBackground() {
        phases.forEach { (name, onColor, background) ->
            assertReadable("restored label on $name", onColor, background)
        }
    }

    @Test
    fun phaseChipLabel_isReadableOnEveryPhaseBackground() {
        phases.forEach { (name, onColor, background) ->
            assertReadable("phase chip label on $name", onColor, background)
        }
    }

    /** Before green the chip uses the theme's onBackground on its background (static schemes only, see above). */
    @Test
    fun phaseChipLabel_isReadableOnTheIdleBackgroundInStaticLightAndDarkSchemes() {
        listOf("light" to LightColorScheme, "dark" to DarkColorScheme).forEach { (name, scheme) ->
            assertReadable("phase chip label on idle $name background", scheme.onBackground, scheme.background)
        }
    }

    private fun assertReadableLarge(name: String, foreground: Color, background: Color) {
        val ratio = contrast(foreground, background)
        assertTrue("$name contrast is %.2f:1, needs at least 3:1 (large text)".format(ratio), ratio >= 3.0)
    }

    @Test
    fun pausedDigits_areDimmedButStillReadableOnEveryPhaseBackground() {
        assertTrue("paused digits must be dimmer than running digits", PausedDigitsAlpha < 1f)
        phases.forEach { (name, onColor, background) ->
            assertReadableLarge("paused digits on $name", onColor.copy(alpha = PausedDigitsAlpha), background)
        }
    }

    @Test
    fun pausedDigits_areReadableOnTheIdleBackgroundInStaticLightAndDarkSchemes() {
        listOf("light" to LightColorScheme, "dark" to DarkColorScheme).forEach { (name, scheme) ->
            assertReadableLarge(
                "paused digits on idle $name background",
                scheme.onBackground.copy(alpha = PausedDigitsAlpha),
                scheme.background
            )
        }
    }

    @Test
    fun pausedLabel_isReadableOnEveryPhaseBackground() {
        phases.forEach { (name, onColor, background) ->
            assertReadable("Paused label on $name", onColor, background)
        }
        listOf("light" to LightColorScheme, "dark" to DarkColorScheme).forEach { (name, scheme) ->
            assertReadable("Paused label on idle $name background", scheme.onBackground, scheme.background)
        }
    }
}
