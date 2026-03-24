package com.gymtracker.ui

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val GymBlue = Color(0xFF5BB5E0)
val GymBlueDark = Color(0xFF3D9AC7)
val GymGreen = Color(0xFF4ECDC4)
val GymPurple = Color(0xFF7B8CDE)
val GymRed = Color(0xFFE57373)
val GymYellow = Color(0xFFF0C674)

val HeatmapRegular = Color(0xFF5BB5E0)
val HeatmapPT = Color(0xFF7B8CDE)
val HeatmapEmpty = Color(0xFF1A1F2E)

private val DarkColorScheme = darkColorScheme(
    primary = GymBlue,
    onPrimary = Color.White,
    primaryContainer = GymBlueDark,
    secondary = GymPurple,
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
fun GymTrackerTheme(content: @Composable () -> Unit) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
