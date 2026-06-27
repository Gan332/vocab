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
    primary = iosBlue,
    onPrimary = Color.White,
    primaryContainer = iosBlueLight,
    secondary = iosGreen,
    onSecondary = Color.White,
    secondaryContainer = iosGreenLight,
    tertiary = iosOrange,
    onTertiary = Color.White,
    tertiaryContainer = iosOrangeLight,
    background = iosGroupedBackground,
    onBackground = iosLabel,
    surface = iosSecondaryGroupedBg,
    onSurface = iosLabel,
    surfaceVariant = iosSystemGray5,
    onSurfaceVariant = iosSecondaryLabel,
    outline = iosSeparator,
    outlineVariant = iosOpaqueSeparator,
    error = iosRed,
    onError = Color.White,
    errorContainer = iosRedLight,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color(0xFFF2F2F7),
    inversePrimary = iosBlueLight,
    scrim = Color.Black.copy(alpha = 0.32f)
)

private val DarkColorScheme = darkColorScheme(
    primary = iosBlue,
    onPrimary = Color.White,
    primaryContainer = iosBlueLight,
    secondary = iosGreen,
    onSecondary = Color.White,
    secondaryContainer = iosGreenLight,
    tertiary = iosOrange,
    onTertiary = Color.White,
    tertiaryContainer = iosOrangeLight,
    background = darkIosGroupedBackground,
    onBackground = darkIosLabel,
    surface = darkIosSecondaryGroupedBg,
    onSurface = darkIosLabel,
    surfaceVariant = darkIosSystemGray5,
    onSurfaceVariant = darkIosSecondaryLabel,
    outline = darkIosSeparator,
    outlineVariant = darkIosOpaqueSeparator,
    error = iosRed,
    onError = Color.White,
    errorContainer = iosRedLight,
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1C1C1E),
    inversePrimary = iosBlueLight,
    scrim = Color.Black.copy(alpha = 0.48f)
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
