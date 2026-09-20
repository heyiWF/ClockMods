package com.clockmods.ui.compose

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ClockDarkColors = darkColorScheme(
    primary = Color(0xFFFF766D),
    onPrimary = Color(0xFF3B0000),
    secondary = Color(0xFFB9C9D9),
    onSecondary = Color(0xFF24323D),
    background = Color(0xFF101418),
    onBackground = Color(0xFFE3E8ED),
    surface = Color(0xFF101418),
    onSurface = Color(0xFFE3E8ED),
    surfaceVariant = Color(0xFF3F484F),
    onSurfaceVariant = Color(0xFFC0C8CF),
)

private val ClockLightColors = lightColorScheme(
    primary = Color(0xFF9E1C1B),
    onPrimary = Color.White,
    secondary = Color(0xFF4E616F),
    onSecondary = Color.White,
    background = Color(0xFFF9F9FC),
    onBackground = Color(0xFF191C1F),
    surface = Color(0xFFF9F9FC),
    onSurface = Color(0xFF191C1F),
    surfaceVariant = Color(0xFFDDE3E9),
    onSurfaceVariant = Color(0xFF41484E),
)

/** Shared Material 3 theme for every Compose screen and transition surface. */
@Composable
fun ClockModsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    typography: Typography = Typography(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(context)
        darkTheme -> ClockDarkColors
        else -> ClockLightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        content = content,
    )
}
