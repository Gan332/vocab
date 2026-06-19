package com.vocabapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ink,
    onPrimary = Color.White,
    primaryContainer = ink100,
    secondary = inkLight,
    onSecondary = Color.White,
    background = paper,
    onBackground = ink900,
    surface = surface,
    onSurface = ink900,
    surfaceVariant = ink100,
    onSurfaceVariant = ink500,
    outline = ink300,
    outlineVariant = ink200,
    error = vermillion,
    onError = Color.White,
    errorContainer = vermillionLight,
    tertiary = jade,
    onTertiary = Color.White,
    tertiaryContainer = jadeLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = darkInk900,
    onPrimary = darkPaper,
    primaryContainer = darkInk100,
    secondary = darkInk700,
    onSecondary = darkPaper,
    background = darkPaper,
    onBackground = darkInk900,
    surface = darkSurface,
    onSurface = darkInk900,
    surfaceVariant = darkInk100,
    onSurfaceVariant = darkInk500,
    outline = darkInk300,
    outlineVariant = darkInk200,
    error = darkVermillion,
    onError = darkPaper,
    errorContainer = vermillionLight,
    tertiary = darkJade,
    onTertiary = darkPaper,
    tertiaryContainer = jadeLight,
)

@Composable
fun VocabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
