package com.example.rippleci.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class AppTheme(
    val label: String,
) {
    NEW_SCHOOL("New School"),
    OLD_SCHOOL("Old School"),
    ECO_FRIENDLY("Eco Friendly"),
    SKY("Sky"),
    SUNSET("Sunset"),
    OCEAN("Ocean"),
    DYNAMIC("Dynamic (System)"),
}

private val NewSchoolDarkColorScheme = darkColorScheme(
    primary = CSUCI_Clay,
    secondary = CSUCI_Sand,
    tertiary = CSUCI_IslandBlue,
    onPrimary = Color.White,
    background = CSUCI_Shale,
    surface = CSUCI_IslandBlue,
    onSurface = Color.White,
    onBackground = Color.White,
)

private val NewSchoolLightColorScheme = lightColorScheme(
    primary = CSUCI_Clay,
    secondary = CSUCI_IslandBlue,
    tertiary = CSUCI_Sand,
    onPrimary = Color.White,
    background = CSUCI_Sand,
    surface = Color.White,
)

private val OldSchoolLightColorScheme = lightColorScheme(
    primary = CSUCI_Red,
    secondary = CSUCI_IslandBlue,
    onPrimary = Color.White,
)

private val OldSchoolDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFC092F),
    secondary = CSUCI_Sand,
    background = CSUCI_Shale,
    surface = CSUCI_Shale,
    onPrimary = Color.White,
    onSurface = Color.White,
)

private val EcoLightColorScheme = lightColorScheme(
    primary = CSUCI_Sage,
    secondary = CSUCI_Desert,
    onPrimary = Color.White,
)

private val EcoDarkColorScheme = darkColorScheme(
    primary = CSUCI_Sage,
    secondary = CSUCI_Desert,
    background = Color(0xFF1B2414),
    surface = Color(0xFF1B2414),
    onPrimary = Color.White,
    onSurface = Color.White,
)

private val LightSkyScheme = lightColorScheme(
    primary = CSUCI_Sky,
    secondary = CSUCI_Sand,
)

private val DarkSkyScheme = darkColorScheme(
    primary = CSUCI_Sky,
    secondary = CSUCI_Sand,
)

private val LightSunsetScheme = lightColorScheme(
    primary = CSUCI_Horizon,
    secondary = CSUCI_Desert,
)

private val DarkSunsetScheme = darkColorScheme(
    primary = CSUCI_Horizon,
    secondary = CSUCI_Desert,
)

private val LightOceanScheme = lightColorScheme(
    primary = CSUCI_Teal,
    secondary = CSUCI_IslandBlue,
)

private val DarkOceanScheme = darkColorScheme(
    primary = CSUCI_Teal,
    secondary = CSUCI_IslandBlue,
    background = Color(0xFF1B2414),
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
)

@Composable
fun RippleCITheme(
    appTheme: AppTheme = AppTheme.NEW_SCHOOL,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        appTheme == AppTheme.NEW_SCHOOL -> if (darkTheme) NewSchoolDarkColorScheme else NewSchoolLightColorScheme
        appTheme == AppTheme.OLD_SCHOOL -> if (darkTheme) OldSchoolDarkColorScheme else OldSchoolLightColorScheme
        appTheme == AppTheme.ECO_FRIENDLY -> if (darkTheme) EcoDarkColorScheme else EcoLightColorScheme
        appTheme == AppTheme.SKY -> if (darkTheme) DarkSkyScheme else LightSkyScheme
        appTheme == AppTheme.SUNSET -> if (darkTheme) DarkSunsetScheme else LightSunsetScheme
        appTheme == AppTheme.OCEAN -> if (darkTheme) DarkOceanScheme else LightOceanScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
