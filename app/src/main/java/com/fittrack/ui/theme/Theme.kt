package com.fittrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FitGreenLight,
    onPrimary = FitDarkBackground,
    primaryContainer = FitGreenDark,
    onPrimaryContainer = FitGreenContainer,
    secondary = FitBlueLight,
    onSecondary = FitDarkBackground,
    secondaryContainer = Color(0xFF1A3A5A),
    onSecondaryContainer = FitBlueContainer,
    tertiary = FitOrange,
    onTertiary = Color.White,
    tertiaryContainer = FitOrangeContainer,
    onTertiaryContainer = FitOrange,
    background = FitDarkBackground,
    onBackground = FitDarkOnSurface,
    surface = FitDarkSurface,
    onSurface = FitDarkOnSurface,
    onSurfaceVariant = Color(0xFF8A9E96),
    error = FitRed,
    onError = Color.White,
    errorContainer = Color(0xFF3A1A1A),
    outline = FitDarkOutline,
    outlineVariant = Color(0xFF3A4A44)
)

private val LightColorScheme = lightColorScheme(
    primary = FitGreen,
    onPrimary = Color.White,
    primaryContainer = FitGreenContainer,
    onPrimaryContainer = FitGreenDark,
    secondary = FitBlue,
    onSecondary = Color.White,
    secondaryContainer = FitBlueContainer,
    onSecondaryContainer = Color(0xFF1A3A5A),
    tertiary = FitOrange,
    onTertiary = Color.White,
    tertiaryContainer = FitOrangeContainer,
    onTertiaryContainer = FitOrange,
    background = FitBackground,
    onBackground = FitOnSurface,
    surface = FitSurface,
    onSurface = FitOnSurface,
    onSurfaceVariant = FitOnSurfaceVariant,
    error = FitRed,
    onError = Color.White,
    errorContainer = FitRedContainer,
    outline = FitOutline,
    outlineVariant = FitOutlineVariant
)

@Composable
fun FitTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // 关闭动态颜色，使用品牌色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = FitShapes,
        content = content
    )
}
