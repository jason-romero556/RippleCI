package com.example.rippleci.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("app_theme")
        val DARK_MODE_KEY = stringPreferencesKey("dark_mode")
    }

    val appTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        val themeName = preferences[THEME_KEY] ?: AppTheme.NEW_SCHOOL.name
        try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.NEW_SCHOOL
        }
    }

    val isDarkTheme: Flow<Boolean?> = context.dataStore.data.map { preferences ->
        when (preferences[DARK_MODE_KEY]) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    suspend fun saveTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.name
        }
    }

    suspend fun saveDarkMode(isDark: Boolean?) {
        context.dataStore.edit { preferences ->
            when (isDark) {
                true -> preferences[DARK_MODE_KEY] = "true"
                false -> preferences[DARK_MODE_KEY] = "false"
                null -> preferences.remove(DARK_MODE_KEY)
            }
        }
    }
}
