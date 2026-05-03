package com.example.rippleci.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ThemeViewModel : ViewModel() {
    var appTheme by mutableStateOf(AppTheme.SCHOOL)
    var isDarkTheme by mutableStateOf<Boolean?>(null) // null means follow system
}
