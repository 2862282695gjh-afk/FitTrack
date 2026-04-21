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

// FitTrack 品牌色
val FitTrackPrimary = Color(0xFF4CAF50) // 绿色，代表健康
val FitTrackPrimaryDark = Color(0xFF388E3C)
val FitTrackSecondary = Color(0xFF2196F3) // 蓝色
val FitTrackSecondaryDark = Color(0xFF1976D2)
val FitTrackAccent = Color(0xFFFF9800) // 橙色，代表活力
val FitTrackBackground = Color(0xFFF5F5F5)
val FitTrackSurface = Color(0xFFFFFFFF)
val FitTrackError = Color(0xFFE53935)

private val DarkColorScheme = darkColorScheme(
    primary = FitTrackPrimary,
    onPrimary = Color.White,
    primaryContainer = FitTrackPrimaryDark,
    onPrimaryContainer = Color.White,
    secondary = FitTrackSecondary,
    onSecondary = Color.White,
    secondaryContainer = FitTrackSecondaryDark,
    onSecondaryContainer = Color.White,
    tertiary = FitTrackAccent,
    onTertiary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
    error = FitTrackError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FitTrackPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = FitTrackPrimaryDark,
    secondary = FitTrackSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBBDEFB),
    onSecondaryContainer = FitTrackSecondaryDark,
    tertiary = FitTrackAccent,
    onTertiary = Color.White,
    background = FitTrackBackground,
    surface = FitTrackSurface,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    error = FitTrackError,
    onError = Color.White
)

@Composable
fun FitTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
