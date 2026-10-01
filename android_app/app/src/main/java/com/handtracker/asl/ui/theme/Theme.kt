package com.handtracker.asl.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEmerald,
    secondary = PrimaryCyan,
    tertiary = AccentPurple,
    background = DarkObsidian,
    surface = DarkSlateCard,
    onPrimary = DarkObsidian,
    onSecondary = DarkObsidian,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ASLHandTrackerTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkObsidian.toArgb()
            window.navigationBarColor = DarkObsidian.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
