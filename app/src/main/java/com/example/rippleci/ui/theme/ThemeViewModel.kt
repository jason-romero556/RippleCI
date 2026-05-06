package com.example.rippleci.ui.theme

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreferences = ThemePreferences(application)

    var appTheme by mutableStateOf(AppTheme.NEW_SCHOOL)
        private set

    var isDarkTheme by mutableStateOf<Boolean?>(null) // null means follow system
        private set

    init {
        viewModelScope.launch {
            themePreferences.appTheme.collect { theme ->
                appTheme = theme
            }
        }
        viewModelScope.launch {
            themePreferences.isDarkTheme.collect { isDark ->
                isDarkTheme = isDark
            }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferences.saveTheme(theme)
        }
    }

    fun setDarkMode(isDark: Boolean?) {
        viewModelScope.launch {
            themePreferences.saveDarkMode(isDark)
        }
    }
}
