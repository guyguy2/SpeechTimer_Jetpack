package com.happypuppy.toastmasterstimer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.happypuppy.toastmasterstimer.util.TimerPhase

/** Opacity of the time digits while paused: dimmed yet still readable (>= 3:1 large text). */
internal const val PausedDigitsAlpha = 0.7f

/**
 * Semantic color tokens for a single [TimerPhase].
 * Every foreground/background pair is pinned by WCAG 2.1 JVM contrast tests.
 */
@Immutable
data class PhasePalette(
    val background: Color,
    val onBackground: Color,
    val secondaryOnBackground: Color,
    val chipContainer: Color,
    val onChipContainer: Color,
    val progressTrack: Color,
    val progressTrackBorder: Color,
    val progressIndicator: Color,
    val markerGreen: Color,
    val markerYellow: Color,
    val markerRed: Color,
    val markerBorder: Color,
    val primaryControlContainer: Color,
    val onPrimaryControlContainer: Color,
    val secondaryControlContainer: Color,
    val onSecondaryControlContainer: Color
)

/** Semantic phase color extension provided through [LocalPhaseColors]. */
@Immutable
data class PhaseColorScheme(
    val beforeGreen: PhasePalette,
    val green: PhasePalette,
    val yellow: PhasePalette,
    val red: PhasePalette
) {
    fun paletteFor(phase: TimerPhase): PhasePalette = when (phase) {
        TimerPhase.BEFORE_GREEN -> beforeGreen
        TimerPhase.GREEN -> green
        TimerPhase.YELLOW -> yellow
        TimerPhase.RED -> red
    }
}

private val ActiveGreenPalette = PhasePalette(
    background = Color(0xFF0E632A),
    onBackground = Color(0xFFFFFFFF),
    secondaryOnBackground = Color(0xFFE6F7EB),
    chipContainer = Color(0xFF07421B),
    onChipContainer = Color(0xFFFFFFFF),
    progressTrack = Color(0xFF063014),
    progressTrackBorder = Color(0xFFC8F2D4),
    progressIndicator = Color(0xFFFFFFFF),
    markerGreen = Color(0xFF8FF5A8),
    markerYellow = Color(0xFFFFE359),
    markerRed = Color(0xFFFFA3A3),
    markerBorder = Color(0xFF04240E),
    primaryControlContainer = Color(0xFFFFFFFF),
    onPrimaryControlContainer = Color(0xFF07421B),
    secondaryControlContainer = Color(0xFFD6F5DF),
    onSecondaryControlContainer = Color(0xFF063817)
)

private val ActiveYellowPalette = PhasePalette(
    background = Color(0xFFFFD926),
    onBackground = Color(0xFF141100),
    secondaryOnBackground = Color(0xFF2B2300),
    chipContainer = Color(0xFF332900),
    onChipContainer = Color(0xFFFFF5CC),
    progressTrack = Color(0xFFFFF4C2),
    progressTrackBorder = Color(0xFF2B2300),
    progressIndicator = Color(0xFF1C1600),
    markerGreen = Color(0xFF116329),
    markerYellow = Color(0xFF6B4D00),
    markerRed = Color(0xFFA61717),
    markerBorder = Color(0xFF141100),
    primaryControlContainer = Color(0xFF1C1600),
    onPrimaryControlContainer = Color(0xFFFFFFFF),
    secondaryControlContainer = Color(0xFF3D3100),
    onSecondaryControlContainer = Color(0xFFFFFFFF)
)

private val ActiveRedPalette = PhasePalette(
    background = Color(0xFFB71C1C),
    onBackground = Color(0xFFFFFFFF),
    secondaryOnBackground = Color(0xFFFFE8E8),
    chipContainer = Color(0xFF7D0F0F),
    onChipContainer = Color(0xFFFFFFFF),
    progressTrack = Color(0xFF570808),
    progressTrackBorder = Color(0xFFFFD6D6),
    progressIndicator = Color(0xFFFFFFFF),
    markerGreen = Color(0xFF96F7B0),
    markerYellow = Color(0xFFFFE666),
    markerRed = Color(0xFFFFB3B3),
    markerBorder = Color(0xFF3D0404),
    primaryControlContainer = Color(0xFFFFFFFF),
    onPrimaryControlContainer = Color(0xFF7D0F0F),
    secondaryControlContainer = Color(0xFFFFDAD6),
    onSecondaryControlContainer = Color(0xFF690005)
)

internal fun phaseColorSchemeFor(
    colorScheme: ColorScheme,
    darkTheme: Boolean
): PhaseColorScheme {
    val beforeGreen = if (darkTheme) {
        PhasePalette(
            background = colorScheme.background,
            onBackground = colorScheme.onBackground,
            secondaryOnBackground = colorScheme.onSurfaceVariant,
            chipContainer = colorScheme.surfaceContainerHigh,
            onChipContainer = colorScheme.onSurface,
            progressTrack = Color(0xFF2A2F38),
            progressTrackBorder = Color(0xFF9EA6B2),
            progressIndicator = Color(0xFF9ACBFB),
            markerGreen = Color(0xFF66DF7B),
            markerYellow = Color(0xFFFFD54F),
            markerRed = Color(0xFFFF8585),
            markerBorder = Color(0xFF0C0E12),
            primaryControlContainer = colorScheme.primary,
            onPrimaryControlContainer = colorScheme.onPrimary,
            secondaryControlContainer = colorScheme.primaryContainer,
            onSecondaryControlContainer = colorScheme.onPrimaryContainer
        )
    } else {
        PhasePalette(
            background = colorScheme.background,
            onBackground = colorScheme.onBackground,
            secondaryOnBackground = colorScheme.onSurfaceVariant,
            chipContainer = colorScheme.surfaceContainerHigh,
            onChipContainer = colorScheme.onSurface,
            progressTrack = Color(0xFFE1E6EE),
            progressTrackBorder = Color(0xFF47505C),
            progressIndicator = Color(0xFF004165),
            markerGreen = Color(0xFF146B2E),
            markerYellow = Color(0xFF8C6200),
            markerRed = Color(0xFFC62828),
            markerBorder = Color(0xFF191C20),
            primaryControlContainer = colorScheme.primary,
            onPrimaryControlContainer = colorScheme.onPrimary,
            secondaryControlContainer = colorScheme.primaryContainer,
            onSecondaryControlContainer = colorScheme.onPrimaryContainer
        )
    }

    return PhaseColorScheme(
        beforeGreen = beforeGreen,
        green = ActiveGreenPalette,
        yellow = ActiveYellowPalette,
        red = ActiveRedPalette
    )
}

val LocalPhaseColors = staticCompositionLocalOf {
    phaseColorSchemeFor(LightColorScheme, darkTheme = false)
}

val LocalIsDarkTheme = staticCompositionLocalOf { false }
