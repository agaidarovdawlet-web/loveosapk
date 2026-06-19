package com.example.loveosapk.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = AccentSecondary,
    tertiary = AccentTertiary,
    background = DarkBg,
    surface = DarkBgSecondary,
    surfaceVariant = GlassWhite,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Danger
)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    secondary = AccentSecondary,
    tertiary = AccentTertiary,
    background = LightBg,
    surface = LightBgSecondary,
    surfaceVariant = LightGlass,
    onPrimary = TextPrimary,
    onSecondary = TextPrimary,
    onTertiary = TextPrimary,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    error = Danger
)

@Composable
fun LoveOsApkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appThemeSetting: String = "dark",
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for LoveOS specific aesthetic
    content: @Composable () -> Unit
) {
    val useDarkTheme = when(appThemeSetting) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }

    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
