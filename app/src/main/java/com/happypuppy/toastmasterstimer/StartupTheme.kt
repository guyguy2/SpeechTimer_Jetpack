package com.happypuppy.toastmasterstimer

import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Holds the first resolved [AppTheme] for the root of the app. [theme] is null only until the
 * source emits once; MainActivity keeps the system splash screen up while [isResolved] is false
 * and renders nothing until it is true, so the first Compose frame already uses the stored theme.
 *
 * A first read that stalls for [FIRST_READ_TIMEOUT_MS] releases the splash with
 * [AppTheme.SYSTEM_DEFAULT]. [com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager.themeFlow]
 * maps IOExceptions to empty preferences so failed reads resolve immediately as well.
 */
class StartupTheme {
    private val resolved = MutableStateFlow<AppTheme?>(null)

    val theme: StateFlow<AppTheme?> = resolved

    val isResolved: Boolean get() = resolved.value != null

    /**
     * Collects [source] into [theme]. If the first value has not arrived after
     * [FIRST_READ_TIMEOUT_MS], [AppTheme.SYSTEM_DEFAULT] is emitted so the splash cannot hang;
     * a real value arriving later still replaces the fallback.
     */
    suspend fun collectFrom(source: Flow<AppTheme>) = coroutineScope {
        val timeout = launch {
            delay(FIRST_READ_TIMEOUT_MS)
            resolved.compareAndSet(null, AppTheme.SYSTEM_DEFAULT)
        }
        source.collect {
            timeout.cancel()
            resolved.value = it
        }
    }

    companion object {
        /** How long the splash may wait for the first theme read before using the default. */
        const val FIRST_READ_TIMEOUT_MS = 2_000L
    }
}
