package com.airplayr.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AirPlayrDarkFallback = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
)

private val AirPlayrLightFallback = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
)

@Composable
fun AirPlayrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme: ColorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(androidx.compose.ui.platform.LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(androidx.compose.ui.platform.LocalContext.current)
        darkTheme -> AirPlayrDarkFallback
        else -> AirPlayrLightFallback
    }

    val accessibleScheme = if (highContrast) {
        scheme.copy(
            onBackground = if (darkTheme) Color.White else Color.Black,
            onSurface = if (darkTheme) Color.White else Color.Black,
            onSurfaceVariant = if (darkTheme) Color.White else Color.Black,
            outline = if (darkTheme) Color.White else Color.Black,
        )
    } else scheme

    MaterialTheme(
        colorScheme = accessibleScheme,
        typography = AirPlayrTypography,
        content = content,
    )
}
