package com.happypuppy.toastmasterstimer.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.happypuppy.toastmasterstimer.ui.screens.SortOption
import com.happypuppy.toastmasterstimer.util.CustomRange
import com.happypuppy.toastmasterstimer.util.CustomSpeechRange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// DataStore for preferences
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

// Preference keys
val THEME_KEY = stringPreferencesKey("app_theme")
val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color_enabled")
val VIBRATE_KEY = booleanPreferencesKey("vibrate_enabled")
val SOUND_KEY = booleanPreferencesKey("sound_enabled")
val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on_enabled")
val SAVED_SORT_KEY = stringPreferencesKey("saved_sort_option")
val LAST_CUSTOM_MIN_KEY = intPreferencesKey("last_custom_min_minutes")
val LAST_CUSTOM_MAX_KEY = intPreferencesKey("last_custom_max_minutes")

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

    // Opt-in Material You dynamic color (default: false so Brand Default is active out of the box).
    val dynamicColorEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { preferences ->
            preferences[DYNAMIC_COLOR_KEY] ?: false
        }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[DYNAMIC_COLOR_KEY] = enabled
        }
    }

    // Vibrate preference (default: true)
    val vibrateEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
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
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { preferences ->
            preferences[SOUND_KEY] ?: false
        }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[SOUND_KEY] = enabled
        }
    }

    // Keep screen on while timer runs (default: true)
    val keepScreenOnEnabledFlow: Flow<Boolean> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { preferences ->
            preferences[KEEP_SCREEN_ON_KEY] != false
        }

    suspend fun setKeepScreenOnEnabled(enabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[KEEP_SCREEN_ON_KEY] = enabled
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

    // Last-used custom speech duration range (null until a custom timer has been started)
    val lastCustomRangeFlow: Flow<CustomRange?> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { preferences ->
            val min = preferences[LAST_CUSTOM_MIN_KEY]
            val max = preferences[LAST_CUSTOM_MAX_KEY]
            if (min != null && max != null) {
                CustomSpeechRange.coerceRange(min, max)
            } else {
                null
            }
        }

    suspend fun setLastCustomRange(minMinutes: Int, maxMinutes: Int) {
        val coerced = CustomSpeechRange.coerceRange(minMinutes, maxMinutes)
        context.dataStore.edit { settings ->
            settings[LAST_CUSTOM_MIN_KEY] = coerced.minMinutes
            settings[LAST_CUSTOM_MAX_KEY] = coerced.maxMinutes
        }
    }

    suspend fun clearLastCustomRange() {
        context.dataStore.edit { settings ->
            settings.remove(LAST_CUSTOM_MIN_KEY)
            settings.remove(LAST_CUSTOM_MAX_KEY)
        }
    }
}