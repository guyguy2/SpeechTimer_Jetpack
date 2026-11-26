package com.happypuppy.toastmasterstimer.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.model.SpeechType
import com.happypuppy.toastmasterstimer.ui.screens.recentSpeakers
import com.happypuppy.toastmasterstimer.ui.theme.DarkColorScheme
import com.happypuppy.toastmasterstimer.ui.theme.LightColorScheme
import com.happypuppy.toastmasterstimer.ui.theme.phaseColorSchemeFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests for Wave 7 preset G/Y/R cue formatting, recent speaker slicing, and cue-dot contrast.
 */
class PresetCueTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val effective = foreground.compositeOver(background)
        val a = effective.luminance().toDouble() + 0.05
        val b = background.luminance().toDouble() + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    @Test
    fun presetCueTimes_matchThresholdsForAllFourPresets() {
        val expected = mapOf(
            SpeechType.TABLE_TOPICS to PresetCueTimes("1:00", "1:30", "2:00"),
            SpeechType.EVALUATION to PresetCueTimes("2:00", "2:30", "3:00"),
            SpeechType.ICE_BREAKER to PresetCueTimes("4:00", "5:00", "6:00"),
            SpeechType.SPEECH to PresetCueTimes("5:00", "6:00", "7:00")
        )

        expected.forEach { (type, cues) ->
            val actual = presetCueTimes(type.presetMinMinutes!!, type.presetMaxMinutes!!)
            assertEquals("cues for ${type.id}", cues, actual)
        }
    }

    @Test
    fun formatCueTime_formatsZeroAndMultiDigitMinutes() {
        assertEquals("0:00", formatCueTime(0))
        assertEquals("0:00", formatCueTime(-5))
        assertEquals("1:05", formatCueTime(65))
        assertEquals("59:30", formatCueTime(59 * 60 + 30))
    }

    @Test
    fun recentSpeakers_returnsAtMostThreeSortedNewestFirst() {
        assertTrue(recentSpeakers(emptyList()).isEmpty())

        val older = Speaker(id = 1, name = "Older", speechType = "SPEECH", duration = "05:00", createdAt = Date(1_000L))
        val mid = Speaker(id = 2, name = "Mid", speechType = "SPEECH", duration = "05:30", createdAt = Date(3_000L))
        val newest = Speaker(id = 3, name = "Newest", speechType = "SPEECH", duration = "06:00", createdAt = Date(4_000L))
        val oldest = Speaker(id = 4, name = "Oldest", speechType = "SPEECH", duration = "05:10", createdAt = Date(500L))

        val sliced = recentSpeakers(listOf(older, oldest, newest, mid))

        assertEquals(listOf("Newest", "Mid", "Older"), sliced.map { it.name })
        assertTrue(recentSpeakers(listOf(older, mid), limit = 0).isEmpty())
    }

    @Test
    fun presetCueTimes_formatsCustomRangesAccurately() {
        assertEquals(PresetCueTimes("3:00", "4:00", "5:00"), presetCueTimes(3, 5))
        assertEquals(PresetCueTimes("8:00", "9:00", "10:00"), presetCueTimes(8, 10))
        assertEquals(PresetCueTimes("1:00", "30:30", "60:00"), presetCueTimes(1, 60))
        assertEquals(PresetCueTimes("59:00", "59:30", "60:00"), presetCueTimes(59, 60))
    }

    @Test
    fun beforeGreenMarkerTokens_meetNonTextContrastOnCardSurfaces() {
        val modes = listOf(
            Triple("light", LightColorScheme, false),
            Triple("dark", DarkColorScheme, true)
        )
        modes.forEach { (mode, scheme, isDark) ->
            val beforeGreen = phaseColorSchemeFor(scheme, darkTheme = isDark).beforeGreen
            val cardSurfaces = listOf(
                "surface" to scheme.surface,
                "surfaceContainerLow" to scheme.surfaceContainerLow,
                "surfaceContainer" to scheme.surfaceContainer
            )
            cardSurfaces.forEach { (surfaceName, bg) ->
                listOf(
                    "markerGreen" to beforeGreen.markerGreen,
                    "markerYellow" to beforeGreen.markerYellow,
                    "markerRed" to beforeGreen.markerRed
                ).forEach { (tokenName, fg) ->
                    val ratio = contrast(fg, bg)
                    assertTrue(
                        "$mode $tokenName on $surfaceName contrast %.2f:1 must be >= 3.0:1".format(ratio),
                        ratio >= 3.0
                    )
                }
            }
        }
    }
}
