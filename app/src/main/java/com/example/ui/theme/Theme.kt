package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PrimeDarkColorScheme = darkColorScheme(
    primary = PrimeAccent,
    onPrimary = PrimeBackground,
    primaryContainer = PrimeAccentDark,
    onPrimaryContainer = PrimeAccentSecondary,
    secondary = PrimeAccentSecondary,
    onSecondary = PrimeBackground,
    secondaryContainer = PrimeSurfaceElevated,
    onSecondaryContainer = PrimeTextPrimary,
    tertiary = PrimeInfo,
    onTertiary = PrimeBackground,
    background = PrimeBackground,
    onBackground = PrimeTextPrimary,
    surface = PrimeSurface,
    onSurface = PrimeTextPrimary,
    surfaceVariant = PrimeSurfaceElevated,
    onSurfaceVariant = PrimeTextSecondary,
    outline = PrimeSubtleBorder,
    outlineVariant = PrimeGlassBorder,
    error = PrimeError,
    onError = PrimeBackground
)

@Composable
fun OpalTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = PrimeBackground.toArgb()
                window.navigationBarColor = PrimeBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = PrimeDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Retain alias for compatibility
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    OpalTheme(content = content)
}
