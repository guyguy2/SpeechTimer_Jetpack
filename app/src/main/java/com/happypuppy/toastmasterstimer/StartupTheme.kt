package com.happypuppy.toastmasterstimer

import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** Combined theme preferences preloaded before the first Compose frame renders. */
data class StartupThemeState(
    val theme: AppTheme = AppTheme.SYSTEM_DEFAULT,
    val dynamicColorEnabled: Boolean = false
)

/**
 * Holds the first resolved [StartupThemeState] for the root of the app. [state] is null only until
 * both preference sources emit once; MainActivity keeps the system splash screen up while
 * [isResolved] is false and renders nothing until it is true, so the first Compose frame already
 * uses the stored theme and dynamic-color setting without flash.
 *
 * A first read that stalls for [FIRST_READ_TIMEOUT_MS] releases the splash with the default
 * [StartupThemeState] (`AppTheme.SYSTEM_DEFAULT`, `dynamicColorEnabled = false`).
 * [com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager.themeFlow] and
 * [com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager.dynamicColorEnabledFlow]
 * map IOExceptions to empty preferences so failed reads resolve immediately as well.
 */
class StartupTheme {
    private val resolved = MutableStateFlow<StartupThemeState?>(null)

    val state: StateFlow<StartupThemeState?> = resolved

    val isResolved: Boolean get() = resolved.value != null

    /**
     * Collects [themeSource] and [dynamicColorSource] into [state]. If the first combined value
     * has not arrived after [FIRST_READ_TIMEOUT_MS], the default [StartupThemeState] is emitted so
     * the splash cannot hang; real values arriving later still replace the fallback.
     */
    suspend fun collectFrom(
        themeSource: Flow<AppTheme>,
        dynamicColorSource: Flow<Boolean> = flowOf(false)
    ) = coroutineScope {
        val timeout = launch {
            delay(FIRST_READ_TIMEOUT_MS)
            resolved.compareAndSet(null, StartupThemeState())
        }
        combine(themeSource, dynamicColorSource) { theme, dynamicColor ->
            StartupThemeState(theme = theme, dynamicColorEnabled = dynamicColor)
        }.collect {
            timeout.cancel()
            resolved.value = it
        }
    }

    companion object {
        /** How long the splash may wait for the first theme read before using the default. */
        const val FIRST_READ_TIMEOUT_MS = 2_000L
    }
}
