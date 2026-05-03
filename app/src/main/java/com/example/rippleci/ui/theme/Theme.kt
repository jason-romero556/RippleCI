package com.example.rippleci.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme {
    SCHOOL,
    DYNAMIC,
}

private val SchoolDarkColorScheme =
    darkColorScheme(
        primary = CSUCI_Clay,
        secondary = CSUCI_Sand,
        tertiary = CSUCI_IslandBlue,
        onPrimary = Color.White,
    )

private val SchoolLightColorScheme =
    lightColorScheme(
        primary = CSUCI_Clay,
        secondary = CSUCI_IslandBlue,
        tertiary = CSUCI_Sand,
        onPrimary = Color.White,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
    )

@Composable
fun RippleCITheme(
    appTheme: AppTheme = AppTheme.SCHOOL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when (appTheme) {
            AppTheme.SCHOOL -> {
                if (darkTheme) SchoolDarkColorScheme else SchoolLightColorScheme
            }

            AppTheme.DYNAMIC -> {
                when {
                    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        val context = LocalContext.current
                        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    }

                    darkTheme -> {
                        DarkColorScheme
                    }

                    else -> {
                        LightColorScheme
                    }
                }
            }
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
