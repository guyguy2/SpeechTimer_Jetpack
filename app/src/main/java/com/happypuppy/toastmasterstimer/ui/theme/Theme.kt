package com.happypuppy.toastmasterstimer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import org.koin.compose.koinInject

internal val DarkColorScheme = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = BrandOnBlueDark,
    primaryContainer = BrandBlueContainerDark,
    onPrimaryContainer = BrandOnBlueContainerDark,
    secondary = BrandMaroonDark,
    onSecondary = BrandOnMaroonDark,
    secondaryContainer = BrandMaroonContainerDark,
    onSecondaryContainer = BrandOnMaroonContainerDark,
    tertiary = BrandGoldDark,
    onTertiary = BrandOnGoldDark,
    tertiaryContainer = BrandGoldContainerDark,
    onTertiaryContainer = BrandOnGoldContainerDark,
    error = BrandErrorDark,
    onError = BrandOnErrorDark,
    errorContainer = BrandErrorContainerDark,
    onErrorContainer = BrandOnErrorContainerDark,
    background = BrandBackgroundDark,
    onBackground = BrandOnBackgroundDark,
    surface = BrandBackgroundDark,
    onSurface = BrandOnBackgroundDark,
    surfaceVariant = BrandSurfaceVariantDark,
    onSurfaceVariant = BrandOnSurfaceVariantDark,
    outline = BrandOutlineDark,
    outlineVariant = BrandOutlineVariantDark,
    surfaceContainerLowest = BrandSurfaceContainerLowestDark,
    surfaceContainerLow = BrandSurfaceContainerLowDark,
    surfaceContainer = BrandSurfaceContainerDark,
    surfaceContainerHigh = BrandSurfaceContainerHighDark,
    surfaceContainerHighest = BrandSurfaceContainerHighestDark,
    inverseSurface = BrandInverseSurfaceDark,
    inverseOnSurface = BrandInverseOnSurfaceDark,
    inversePrimary = BrandInversePrimaryDark
)

internal val LightColorScheme = lightColorScheme(
    primary = BrandBlueLight,
    onPrimary = BrandOnBlueLight,
    primaryContainer = BrandBlueContainerLight,
    onPrimaryContainer = BrandOnBlueContainerLight,
    secondary = BrandMaroonLight,
    onSecondary = BrandOnMaroonLight,
    secondaryContainer = BrandMaroonContainerLight,
    onSecondaryContainer = BrandOnMaroonContainerLight,
    tertiary = BrandGoldLight,
    onTertiary = BrandOnGoldLight,
    tertiaryContainer = BrandGoldContainerLight,
    onTertiaryContainer = BrandOnGoldContainerLight,
    error = BrandErrorLight,
    onError = BrandOnErrorLight,
    errorContainer = BrandErrorContainerLight,
    onErrorContainer = BrandOnErrorContainerLight,
    background = BrandBackgroundLight,
    onBackground = BrandOnBackgroundLight,
    surface = BrandBackgroundLight,
    onSurface = BrandOnBackgroundLight,
    surfaceVariant = BrandSurfaceVariantLight,
    onSurfaceVariant = BrandOnSurfaceVariantLight,
    outline = BrandOutlineLight,
    outlineVariant = BrandOutlineVariantLight,
    surfaceContainerLowest = BrandSurfaceContainerLowestLight,
    surfaceContainerLow = BrandSurfaceContainerLowLight,
    surfaceContainer = BrandSurfaceContainerLight,
    surfaceContainerHigh = BrandSurfaceContainerHighLight,
    surfaceContainerHighest = BrandSurfaceContainerHighestLight,
    inverseSurface = BrandInverseSurfaceLight,
    inverseOnSurface = BrandInverseOnSurfaceLight,
    inversePrimary = BrandInversePrimaryLight
)

@Composable
fun SimpleSpeechTimerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themePreference: AppTheme? = null,
    content: @Composable () -> Unit
) {
    val resolvedPreference = if (themePreference != null) {
        themePreference
    } else {
        val dataStoreManager = koinInject<DataStoreManager>()
        val collected by dataStoreManager.themeFlow.collectAsState(initial = AppTheme.SYSTEM_DEFAULT)
        collected
    }

    val useDarkTheme = when (resolvedPreference) {
        AppTheme.SYSTEM_DEFAULT -> darkTheme
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val phaseColors = remember(colorScheme, useDarkTheme) {
        phaseColorSchemeFor(colorScheme = colorScheme, darkTheme = useDarkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    CompositionLocalProvider(
        LocalPhaseColors provides phaseColors,
        LocalIsDarkTheme provides useDarkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
