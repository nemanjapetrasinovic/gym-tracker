package com.gymtracker.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat

val GymBlue = Color(0xFF5BB5E0)
val GymBlueDark = Color(0xFF3D9AC7)
val GymGreen = Color(0xFF4ECDC4)
val GymPurple = Color(0xFF7B8CDE)
val GymRed = Color(0xFFE57373)
val GymYellow = Color(0xFFF0C674)
val GymYellowLight = Color(0xFFB7791F)

val HeatmapRegular = Color(0xFF5BB5E0)
val HeatmapPT = Color(0xFF7B8CDE)
val HeatmapEmpty = Color(0xFF1A1F2E)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF176B8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7EEF7),
    onPrimaryContainer = Color(0xFF082B39),
    secondary = Color(0xFF5F70C8),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5E9FF),
    onSecondaryContainer = Color(0xFF222754),
    background = Color(0xFFF2F6FA),
    onBackground = Color(0xFF0F1720),
    surface = Color(0xFFFCFDFE),
    onSurface = Color(0xFF0F1720),
    surfaceVariant = Color(0xFFDCE5EE),
    onSurfaceVariant = Color(0xFF3F4C5A),
    outline = Color(0xFF95A5B6),
    error = GymRed
)

private val DarkColorScheme = darkColorScheme(
    primary = GymBlue,
    onPrimary = Color.White,
    primaryContainer = GymBlueDark,
    onPrimaryContainer = Color(0xFFD1ECF7),
    secondary = GymPurple,
    onSecondary = Color.White,
    background = Color(0xFF0D1117),
    surface = Color(0xFF161B22),
    surfaceVariant = Color(0xFF1A1F2E),
    onBackground = Color(0xFFC9D1D9),
    onSurface = Color(0xFFC9D1D9),
    onSurfaceVariant = Color(0xFF8B949E),
    outline = Color(0xFF30363D),
    error = GymRed
)

@Composable
fun GymTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val colorScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(activity.window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

@Composable
fun gymYellowForTheme(): Color = if (isSystemInDarkTheme()) GymYellow else GymYellowLight

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
