package com.example.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = FreshIndigo,
    onPrimary = Color.White,
    primaryContainer = FreshLightIndigoContainer,
    onPrimaryContainer = FreshDeepIndigo,
    secondary = FreshSecondary,
    onSecondary = Color.White,
    secondaryContainer = FreshSecondaryContainer,
    onSecondaryContainer = Color(0xFF0369A1),
    tertiary = Color(0xFFEC4899),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE7F3),
    onTertiaryContainer = Color(0xFF9D174D),
    background = FreshBackground,
    onBackground = FreshTextPrimary,
    surface = FreshSurface,
    onSurface = FreshTextPrimary,
    surfaceVariant = FreshSurfaceVariant,
    onSurfaceVariant = FreshTextVariant,
    outline = FreshOutline
)

private val DarkColorScheme = LightColorScheme

@Composable
fun EmotionDiaryTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    EmotionDiaryTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

