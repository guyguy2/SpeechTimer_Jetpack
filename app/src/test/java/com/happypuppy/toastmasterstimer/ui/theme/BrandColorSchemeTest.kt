package com.happypuppy.toastmasterstimer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifies WCAG 2.1 AA contrast across the Brand Default light and dark Material 3 schemes, and
 * pins `values/colors.xml` and `values-night/colors.xml` splash/window colors to those schemes so
 * cold starts hand over without a color flash.
 */
class BrandColorSchemeTest {

    private fun contrast(foreground: Color, background: Color): Double {
        val a = foreground.luminance().toDouble() + 0.05
        val b = background.luminance().toDouble() + 0.05
        return maxOf(a, b) / minOf(a, b)
    }

    private fun assertReadable(name: String, foreground: Color, background: Color) {
        val ratio = contrast(foreground, background)
        assertTrue("$name contrast is %.2f:1, needs at least 4.5:1".format(ratio), ratio >= 4.5)
    }

    private fun assertCorePairsReadable(mode: String, scheme: ColorScheme) {
        assertReadable("$mode onBackground/background", scheme.onBackground, scheme.background)
        assertReadable("$mode onSurface/surface", scheme.onSurface, scheme.surface)
        assertReadable("$mode onSurfaceVariant/surface", scheme.onSurfaceVariant, scheme.surface)
        assertReadable("$mode onSurfaceVariant/surfaceContainer", scheme.onSurfaceVariant, scheme.surfaceContainer)
        assertReadable("$mode onPrimary/primary", scheme.onPrimary, scheme.primary)
        assertReadable("$mode onPrimaryContainer/primaryContainer", scheme.onPrimaryContainer, scheme.primaryContainer)
        assertReadable("$mode onSecondary/secondary", scheme.onSecondary, scheme.secondary)
        assertReadable("$mode onSecondaryContainer/secondaryContainer", scheme.onSecondaryContainer, scheme.secondaryContainer)
        assertReadable("$mode onTertiary/tertiary", scheme.onTertiary, scheme.tertiary)
        assertReadable("$mode onTertiaryContainer/tertiaryContainer", scheme.onTertiaryContainer, scheme.tertiaryContainer)
        assertReadable("$mode onError/error", scheme.onError, scheme.error)
        assertReadable("$mode onErrorContainer/errorContainer", scheme.onErrorContainer, scheme.errorContainer)
        assertReadable("$mode inverseOnSurface/inverseSurface", scheme.inverseOnSurface, scheme.inverseSurface)
    }

    private fun parseHexColor(file: File, colorName: String): String {
        val content = file.readText()
        val regex = Regex("""<color\s+name="$colorName">\s*(#[0-9A-Fa-f]{6,8})\s*</color>""")
        val match = regex.find(content)
            ?: throw AssertionError("Color $colorName not found in ${file.path}")
        return match.groupValues[1].uppercase()
    }

    private fun Color.toRgbHex(): String =
        "#%06X".format(toArgb() and 0xFFFFFF)

    @Test
    fun lightBrandColorScheme_meetsContrastRequirements() {
        assertCorePairsReadable("light", LightColorScheme)
    }

    @Test
    fun darkBrandColorScheme_meetsContrastRequirements() {
        assertCorePairsReadable("dark", DarkColorScheme)
    }

    @Test
    fun splashColorsXml_alignWithBrandSchemes() {
        val lightColorsXml = File("src/main/res/values/colors.xml")
        val darkColorsXml = File("src/main/res/values-night/colors.xml")
        assertTrue("missing ${lightColorsXml.absolutePath}", lightColorsXml.isFile)
        assertTrue("missing ${darkColorsXml.absolutePath}", darkColorsXml.isFile)

        assertEquals(LightColorScheme.background.toRgbHex(), parseHexColor(lightColorsXml, "window_background"))
        assertEquals(LightColorScheme.primary.toRgbHex(), parseHexColor(lightColorsXml, "splash_icon_tint"))
        assertEquals(DarkColorScheme.background.toRgbHex(), parseHexColor(darkColorsXml, "window_background"))
        assertEquals(DarkColorScheme.primary.toRgbHex(), parseHexColor(darkColorsXml, "splash_icon_tint"))
    }
}
