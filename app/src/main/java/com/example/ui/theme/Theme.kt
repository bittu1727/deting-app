package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SparkPrimary,
    onPrimary = Color.White,
    primaryContainer = SparkPrimaryContainerDark,
    onPrimaryContainer = SparkOnPrimaryContainerDark,
    secondary = SparkSecondary,
    onSecondary = Color.White,
    tertiary = SparkTertiary,
    background = SparkBgDark,
    surface = SparkSurfaceDark,
    surfaceVariant = SparkSurfaceVariantDark,
    onBackground = Color(0xFFF1EFF4),
    onSurface = Color(0xFFF1EFF4)
)

private val LightColorScheme = lightColorScheme(
    primary = SparkPrimary,
    onPrimary = Color.White,
    primaryContainer = SparkPrimaryContainerLight,
    onPrimaryContainer = SparkOnPrimaryContainerLight,
    secondary = SparkSecondary,
    onSecondary = Color.White,
    tertiary = SparkTertiary,
    background = SparkBgLight,
    surface = SparkSurfaceLight,
    surfaceVariant = SparkSurfaceVariantLight,
    onBackground = Color(0xFF1E1A1B),
    onSurface = Color(0xFF1E1A1B)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
