package com.winecellar.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Burgundy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3E0016),
    secondary = Gold,
    onSecondary = Color.White,
    tertiary = BurgundyLight,
    background = Cream,
    onBackground = Charcoal,
    surface = Color.White,
    onSurface = Charcoal,
    surfaceVariant = Color(0xFFF2E7E9),
    onSurfaceVariant = Color(0xFF524345),
)

private val DarkColors = darkColorScheme(
    primary = BurgundyDark,
    onPrimary = Color(0xFF5A0022),
    primaryContainer = Color(0xFF7D2740),
    onPrimaryContainer = Color(0xFFFFD9DF),
    secondary = GoldDark,
    onSecondary = Color(0xFF3A2E00),
    tertiary = ClaretDark,
    background = Color(0xFF181113),
    onBackground = Color(0xFFEDE0E2),
    surface = SurfaceDark,
    onSurface = Color(0xFFEDE0E2),
    surfaceVariant = Color(0xFF524345),
    onSurfaceVariant = Color(0xFFD7C1C4),
)

@Composable
fun WineCellarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = WineTypography,
        content = content,
    )
}
