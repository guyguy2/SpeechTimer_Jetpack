package com.happypuppy.toastmasterstimer.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.happypuppy.toastmasterstimer.ui.theme.DarkColorScheme
import com.happypuppy.toastmasterstimer.ui.theme.LightColorScheme
import com.happypuppy.toastmasterstimer.ui.theme.PausedDigitsAlpha
import com.happypuppy.toastmasterstimer.ui.theme.PhasePalette
import com.happypuppy.toastmasterstimer.ui.theme.phaseColorSchemeFor
import com.happypuppy.toastmasterstimer.util.TimerPhase
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG 2.1 contrast tests across all 4 [TimerPhase] palettes in both static light and dark brand
 * schemes:
 * - Small text, labels, hints, chip text, top bar title/icons, and button icons/text: >= 4.5:1
 * - Large digits & dimmed paused digits: >= 3.0:1
 * - Non-text UI components (progress indicator vs track, track/border vs background, markers vs
 *   background, active-phase control containers vs background): >= 3.0:1
 */
class PhaseTextContrastTest {

    private val schemes = listOf(
        "light" to phaseColorSchemeFor(LightColorScheme, darkTheme = false),
        "dark" to phaseColorSchemeFor(DarkColorScheme, darkTheme = true)
    )

    private fun allPalettes(): List<Pair<String, PhasePalette>> =
        schemes.flatMap { (mode, scheme) ->
            TimerPhase.entries.map { phase -> "$mode/${phase.name}" to scheme.paletteFor(phase) }
        }

    private fun contrast(foreground: Color, background: Color): Double {
        val effective = foreground.compositeOver(background)
        val a = effective.luminance().toDouble() + 0.05
        val b = background.luminance().toDouble() + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    private fun assertSmallTextContrast(name: String, foreground: Color, background: Color) {
        val ratio = contrast(foreground, background)
        assertTrue("$name contrast is %.2f:1, needs at least 4.5:1".format(ratio), ratio >= 4.5)
    }

    private fun assertLargeOrNonTextContrast(name: String, foreground: Color, background: Color) {
        val ratio = contrast(foreground, background)
        assertTrue("$name contrast is %.2f:1, needs at least 3.0:1".format(ratio), ratio >= 3.0)
    }

    @Test
    fun primaryOnBackground_digitsTopBarAndRestoredLabel_meetSmallAndLargeContrast() {
        allPalettes().forEach { (label, palette) ->
            assertLargeOrNonTextContrast("digits on $label", palette.onBackground, palette.background)
            assertSmallTextContrast("top bar and restored label on $label", palette.onBackground, palette.background)
        }
    }

    @Test
    fun pausedDigits_areDimmedAndMeetLargeTextContrast() {
        assertTrue("paused digits must be dimmer than running digits", PausedDigitsAlpha < 1f)
        allPalettes().forEach { (label, palette) ->
            assertLargeOrNonTextContrast(
                "paused digits on $label",
                palette.onBackground.copy(alpha = PausedDigitsAlpha),
                palette.background
            )
        }
    }

    @Test
    fun secondaryLabels_pausedAndForegroundHint_meetSmallTextContrast() {
        allPalettes().forEach { (label, palette) ->
            assertSmallTextContrast(
                "secondary labels and hint on $label",
                palette.secondaryOnBackground,
                palette.background
            )
        }
    }

    @Test
    fun phaseChipText_meetsSmallTextContrastOnChipContainer() {
        allPalettes().forEach { (label, palette) ->
            assertSmallTextContrast(
                "phase chip text on $label",
                palette.onChipContainer,
                palette.chipContainer
            )
        }
    }

    @Test
    fun controlButtons_meetSmallTextContrastForIconsAndNonTextContrastOnCanvas() {
        allPalettes().forEach { (label, palette) ->
            assertSmallTextContrast(
                "primary button content on $label",
                palette.onPrimaryControlContainer,
                palette.primaryControlContainer
            )
            assertSmallTextContrast(
                "secondary button content on $label",
                palette.onSecondaryControlContainer,
                palette.secondaryControlContainer
            )
            assertLargeOrNonTextContrast(
                "primary button container vs background on $label",
                palette.primaryControlContainer,
                palette.background
            )
        }

        val activePhases = setOf(TimerPhase.GREEN, TimerPhase.YELLOW, TimerPhase.RED)
        schemes.forEach { (mode, scheme) ->
            activePhases.forEach { phase ->
                val palette = scheme.paletteFor(phase)
                assertLargeOrNonTextContrast(
                    "secondary button container vs background on $mode/${phase.name}",
                    palette.secondaryControlContainer,
                    palette.background
                )
            }
        }
    }

    @Test
    fun progressBar_indicatorTrackBorderAndMarkers_meetNonTextContrast() {
        allPalettes().forEach { (label, palette) ->
            assertLargeOrNonTextContrast(
                "progress indicator vs track on $label",
                palette.progressIndicator,
                palette.progressTrack
            )
            val trackOrBorderRatio = maxOf(
                contrast(palette.progressTrack, palette.background),
                contrast(palette.progressTrackBorder, palette.background)
            )
            assertTrue(
                "progress track/border vs background on $label is %.2f:1, needs >= 3.0:1".format(trackOrBorderRatio),
                trackOrBorderRatio >= 3.0
            )
            assertLargeOrNonTextContrast(
                "progress track border vs background on $label",
                palette.progressTrackBorder,
                palette.background
            )
            assertLargeOrNonTextContrast("green marker vs background on $label", palette.markerGreen, palette.background)
            assertLargeOrNonTextContrast("yellow marker vs background on $label", palette.markerYellow, palette.background)
            assertLargeOrNonTextContrast("red marker vs background on $label", palette.markerRed, palette.background)
        }
    }
}
