package com.example.rippleci.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Generates the application typography with a dynamic shadow.
 * @param isDark Whether to use a light-colored shadow for Dark Mode visibility.
 */
fun getTypography(isDark: Boolean): Typography {
    val shadowColor = if (isDark) {
        Color.White.copy(alpha = 0.2f) // Subtle glow for dark mode
    } else {
        Color.Black.copy(alpha = 0.15f) // Subtle depth for light mode
    }

    val textShadow = Shadow(
        color = shadowColor,
        offset = Offset(1f, 1f),
        blurRadius = if (isDark) 3f else 2f // Slightly more blur for the "glow" effect
    )

    return Typography(
        titleLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
            shadow = textShadow
        ),
        titleMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
            shadow = textShadow
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
            shadow = textShadow
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
            shadow = textShadow
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
            shadow = textShadow
        ),
        labelLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.1.sp,
            shadow = textShadow
        )
    )
}

// Default instance for preview or initial state
val Typography = getTypography(false)
