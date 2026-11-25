package com.happypuppy.toastmasterstimer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happypuppy.toastmasterstimer.ui.screens.SortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore for preferences
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Preference keys
val THEME_KEY = stringPreferencesKey("app_theme")
val VIBRATE_KEY = booleanPreferencesKey("vibrate_enabled")
val SOUND_KEY = booleanPreferencesKey("sound_enabled")
val SAVED_SORT_KEY = stringPreferencesKey("saved_sort_option")

// Theme options
enum class AppTheme(val value: String) {
    SYSTEM_DEFAULT("system_default"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromString(value: String): AppTheme {
            return entries.find { it.value == value } ?: SYSTEM_DEFAULT
        }
    }
}

class DataStoreManager(private val context: Context) {

    // Theme preference; map IOException to defaults so startup splash never stalls on a read error.
    val themeFlow: Flow<AppTheme> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { preferences ->
            val themeString = preferences[THEME_KEY] ?: AppTheme.SYSTEM_DEFAULT.value
            AppTheme.fromString(themeString)
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { settings ->
            settings[THEME_KEY] = theme.value
        }
    }

    // Vibrate preference (default: true)
    val vibrateEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIBRATE_KEY] != false
        }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[VIBRATE_KEY] = enabled
        }
    }

    // Sound preference (default: false)
    val soundEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SOUND_KEY] ?: false
        }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[SOUND_KEY] = enabled
        }
    }

    // Saved speeches sort order preference (default: DATE_NEWEST)
    val savedSortOptionFlow: Flow<SortOption> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { preferences ->
            SortOption.fromName(preferences[SAVED_SORT_KEY])
        }

    suspend fun setSavedSortOption(option: SortOption) {
        context.dataStore.edit { settings ->
            settings[SAVED_SORT_KEY] = option.name
        }
    }
}