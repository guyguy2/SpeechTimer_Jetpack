package com.happypuppy.toastmasterstimer

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.happypuppy.toastmasterstimer.data.preferences.AppTheme
import com.happypuppy.toastmasterstimer.data.preferences.DataStoreManager
import com.happypuppy.toastmasterstimer.ui.screens.SortOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Round-trips for [DataStoreManager] against the real (process-wide) preferences DataStore.
 * Each test restores the values it found, so the order of tests does not matter.
 */
@RunWith(AndroidJUnit4::class)
class DataStoreManagerTest {

    private val manager = DataStoreManager(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun theme_roundTripsEveryOption() = runBlocking {
        val original = manager.themeFlow.first()
        try {
            AppTheme.entries.forEach { theme ->
                manager.setTheme(theme)
                assertEquals(theme, manager.themeFlow.first())
            }
        } finally {
            manager.setTheme(original)
        }
    }

    @Test
    fun vibrate_roundTrips() = runBlocking {
        val original = manager.vibrateEnabledFlow.first()
        try {
            manager.setVibrateEnabled(false)
            assertEquals(false, manager.vibrateEnabledFlow.first())
            manager.setVibrateEnabled(true)
            assertEquals(true, manager.vibrateEnabledFlow.first())
        } finally {
            manager.setVibrateEnabled(original)
        }
    }

    @Test
    fun sound_roundTrips() = runBlocking {
        val original = manager.soundEnabledFlow.first()
        try {
            manager.setSoundEnabled(true)
            assertEquals(true, manager.soundEnabledFlow.first())
            manager.setSoundEnabled(false)
            assertEquals(false, manager.soundEnabledFlow.first())
        } finally {
            manager.setSoundEnabled(original)
        }
    }

    @Test
    fun savedSortOption_roundTripsEveryOption() = runBlocking {
        val original = manager.savedSortOptionFlow.first()
        try {
            SortOption.entries.forEach { option ->
                manager.setSavedSortOption(option)
                assertEquals(option, manager.savedSortOptionFlow.first())
            }
        } finally {
            manager.setSavedSortOption(original)
        }
    }
}
