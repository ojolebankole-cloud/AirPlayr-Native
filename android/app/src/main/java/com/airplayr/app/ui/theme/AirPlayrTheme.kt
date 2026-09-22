package com.airplayr.app.ui.theme
import androidx.compose.ui.graphics.luminance

import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration

private val AirPlayrDarkFallback = darkColorScheme(
    primary = Color(0xFF9CCBFF),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF004A7D),
    onPrimaryContainer = Color(0xFFCCE5FF),
    secondary = Color(0xFFB6CBE0),
    onSecondary = Color(0xFF20323F),
    secondaryContainer = Color(0xFF354A59),
    onSecondaryContainer = Color(0xFFD3E6F5),
    tertiary = Color(0xFFFFB6C7),
    onTertiary = Color(0xFF4D1927),
    tertiaryContainer = Color(0xFF672F3F),
    onTertiaryContainer = Color(0xFFFFD9E1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1015),
    onBackground = Color(0xFFE4EDF5),
    surface = Color(0xFF0B1015),
    onSurface = Color(0xFFE4EDF5),
    surfaceVariant = Color(0xFF414B54),
    onSurfaceVariant = Color(0xFFC1CCD5),
    outline = Color(0xFF8A969F),
    outlineVariant = Color(0xFF414B54),
    inverseSurface = Color(0xFFE4EDF5),
    inverseOnSurface = Color(0xFF293139),
    inversePrimary = Color(0xFF00639D),
    scrim = Color(0xB3000000),
    surfaceDim = Color(0xFF0B1015),
    surfaceBright = Color(0xFF30383F),
    surfaceContainerLowest = Color(0xFF06090D),
    surfaceContainerLow = Color(0xFF151B20),
    surfaceContainer = Color(0xFF192126),
    surfaceContainerHigh = Color(0xFF242C32),
    surfaceContainerHighest = Color(0xFF30383F),
)

/**
 * AirPlayr's pre-Material-You light baseline. The canvas stays an almost-white sky blue so the
 * app still has a recognizable visual identity on Android 7-11 and when the user disables dynamic
 * colour on newer Android releases.
 */
private val AirPlayrLightFallback = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9ECFF),
    onPrimaryContainer = Color(0xFF003C6D),
    secondary = Color(0xFF46627B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E8F8),
    onSecondaryContainer = Color(0xFF102638),
    tertiary = Color(0xFF6B5778),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0DDFC),
    onTertiaryContainer = Color(0xFF261433),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF5FBFF),
    onBackground = Color(0xFF101418),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF101418),
    surfaceVariant = Color(0xFFE2EDF5),
    onSurfaceVariant = Color(0xFF46535E),
    outline = Color(0xFF6F7E89),
    outlineVariant = Color(0xFFC4D0D9),
    inverseSurface = Color(0xFF293139),
    inverseOnSurface = Color(0xFFEAF2F8),
    inversePrimary = Color(0xFF9DCCFF),
    scrim = Color(0x59000000),
    surfaceDim = Color(0xFFD7E7F1),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color(0xFFFAFDFF),
    surfaceContainerLow = Color(0xFFF0F7FC),
    surfaceContainer = Color(0xFFE8F1F7),
    surfaceContainerHigh = Color(0xFFE0EAF1),
    surfaceContainerHighest = Color(0xFFD7E3EA),
)

private fun blend(from: Color, to: Color, amount: Float): Color = Color(
    red = from.red + (to.red - from.red) * amount,
    green = from.green + (to.green - from.green) * amount,
    blue = from.blue + (to.blue - from.blue) * amount,
    alpha = from.alpha + (to.alpha - from.alpha) * amount,
)

/**
 * Builds a coherent Material 3 light/dark scheme from one of AirPlayr's fixed accent colours.
 * Keeping the generation here means Android 7-11 gets the same semantic role relationships as the
 * Material-You devices instead of falling back to an unrelated default purple palette.
 */
private fun fixedAccentScheme(seed: Color, darkTheme: Boolean): ColorScheme {
    return if (!darkTheme) {
        val primaryContainer = blend(seed, Color.White, 0.82f)
        val secondary = blend(seed, Color.Black, 0.22f)
        val tertiary = blend(seed, Color(0xFF6A5C72), 0.34f)
        val onAccent = if (seed.luminance() > 0.179f) Color.Black else Color.White
        val onTertiary = if (tertiary.luminance() > 0.179f) Color.Black else Color.White
        lightColorScheme(
            primary = seed,
            onPrimary = onAccent,
            primaryContainer = primaryContainer,
            onPrimaryContainer = blend(seed, Color.Black, 0.64f),
            secondary = secondary,
            onSecondary = if (secondary.luminance() > 0.179f) Color.Black else Color.White,
            secondaryContainer = blend(seed, Color.White, 0.86f),
            onSecondaryContainer = blend(seed, Color.Black, 0.70f),
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = blend(seed, Color.White, 0.90f),
            onTertiaryContainer = blend(seed, Color.Black, 0.72f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            background = AirPlayrLightFallback.background,
            onBackground = AirPlayrLightFallback.onBackground,
            surface = AirPlayrLightFallback.surface,
            onSurface = AirPlayrLightFallback.onSurface,
            surfaceVariant = blend(seed, Color.White, 0.90f),
            onSurfaceVariant = blend(seed, Color.Black, 0.48f),
            outline = blend(seed, Color.Black, 0.38f),
            outlineVariant = blend(seed, Color.White, 0.70f),
            inverseSurface = Color(0xFF273139),
            inverseOnSurface = Color(0xFFEAF2F8),
            inversePrimary = blend(seed, Color.White, 0.35f),
            scrim = Color(0x59000000),
            surfaceDim = AirPlayrLightFallback.surfaceDim,
            surfaceBright = Color.White,
            surfaceContainerLowest = AirPlayrLightFallback.surfaceContainerLowest,
            surfaceContainerLow = blend(seed, Color.White, 0.96f),
            surfaceContainer = blend(seed, Color.White, 0.91f),
            surfaceContainerHigh = blend(seed, Color.White, 0.86f),
            surfaceContainerHighest = blend(seed, Color.White, 0.80f),
        )
    } else {
        val primary = blend(seed, Color.White, 0.34f)
        darkColorScheme(
            primary = primary,
            onPrimary = blend(seed, Color.Black, 0.67f),
            primaryContainer = blend(seed, Color.Black, 0.34f),
            onPrimaryContainer = blend(primary, Color.White, 0.80f),
            secondary = blend(seed, Color.White, 0.16f),
            onSecondary = blend(seed, Color.Black, 0.72f),
            secondaryContainer = blend(seed, Color.Black, 0.26f),
            onSecondaryContainer = blend(primary, Color.White, 0.84f),
            tertiary = blend(seed, Color.White, 0.22f),
            onTertiary = blend(seed, Color.Black, 0.72f),
            tertiaryContainer = blend(seed, Color.Black, 0.18f),
            onTertiaryContainer = blend(primary, Color.White, 0.88f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            background = AirPlayrDarkFallback.background,
            onBackground = AirPlayrDarkFallback.onBackground,
            surface = AirPlayrDarkFallback.surface,
            onSurface = AirPlayrDarkFallback.onSurface,
            surfaceVariant = blend(seed, Color.Black, 0.58f),
            onSurfaceVariant = Color(0xFFC1CCD5),
            outline = blend(primary, Color.Black, 0.38f),
            outlineVariant = blend(seed, Color.Black, 0.52f),
            inverseSurface = Color(0xFFEAF2F8),
            inverseOnSurface = Color(0xFF273139),
            inversePrimary = seed,
            scrim = Color(0xB3000000),
            surfaceDim = AirPlayrDarkFallback.surfaceDim,
            surfaceBright = AirPlayrDarkFallback.surfaceBright,
            surfaceContainerLowest = AirPlayrDarkFallback.surfaceContainerLowest,
            surfaceContainerLow = AirPlayrDarkFallback.surfaceContainerLow,
            surfaceContainer = AirPlayrDarkFallback.surfaceContainer,
            surfaceContainerHigh = AirPlayrDarkFallback.surfaceContainerHigh,
            surfaceContainerHighest = AirPlayrDarkFallback.surfaceContainerHighest,
        )
    }
}

private fun arrangeLightScheme(scheme: ColorScheme): ColorScheme {
    // Preserve the AirPlayr near-white sky-blue canvas while allowing dynamic/fixed accents to
    // keep their Material-You tonal hierarchy.
    val background = blend(scheme.background, Color(0xFFF5FBFF), 0.62f)
    val surface = blend(scheme.surface, Color.White, 0.90f)
    val surfaceBright = blend(scheme.surfaceBright, Color.White, 0.64f)
    return scheme.copy(
        background = background,
        surface = surface,
        surfaceBright = surfaceBright,
    )
}

@Composable
fun AirPlayrTheme(
    darkTheme: Boolean = false,
    accentColor: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "dynamic" else "blue",
    dynamicColor: Boolean = true,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LocalConfiguration.current

    val useDynamicColor = dynamicColor && accentColor == "dynamic" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dynamicColorRefresh = remember { mutableIntStateOf(0) }

    DisposableEffect(context, useDynamicColor) {
        if (!useDynamicColor) return@DisposableEffect onDispose { }
        val wallpaperManager = WallpaperManager.getInstance(context)
        val handler = Handler(Looper.getMainLooper())
        var pendingRefresh: Runnable? = null
        val listener = object : WallpaperManager.OnColorsChangedListener {
            override fun onColorsChanged(colors: android.app.WallpaperColors?, which: Int) {
                pendingRefresh?.let(handler::removeCallbacks)
                val refresh = Runnable {
                    pendingRefresh = null
                    dynamicColorRefresh.intValue++
                }
                pendingRefresh = refresh
                handler.post(refresh)
            }
        }
        wallpaperManager.addOnColorsChangedListener(listener, handler)
        onDispose {
            wallpaperManager.removeOnColorsChangedListener(listener)
            pendingRefresh?.let(handler::removeCallbacks)
            handler.removeCallbacksAndMessages(null)
        }
    }

    val fixedSeed = accentColorSeed(accentColor)
    val baseScheme: ColorScheme = remember(
        darkTheme,
        accentColor,
        useDynamicColor,
        dynamicColorRefresh.intValue,
    ) {
        when {
            useDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
            useDynamicColor -> dynamicLightColorScheme(context)
            accentColor == "dynamic" && !useDynamicColor -> fixedAccentScheme(fixedSeed, darkTheme)
            else -> fixedAccentScheme(fixedSeed, darkTheme)
        }
    }

    val scheme = remember(baseScheme, darkTheme) {
        if (darkTheme) baseScheme else arrangeLightScheme(baseScheme)
    }

    val accessibleScheme = if (highContrast) {
        scheme.copy(
            onBackground = if (darkTheme) Color.White else Color.Black,
            onSurface = if (darkTheme) Color.White else Color.Black,
            onSurfaceVariant = if (darkTheme) Color.White else Color.Black,
            outline = if (darkTheme) Color.White else Color.Black,
        )
    } else scheme

    val airColors = airPlayrColorsFor(
        scheme = accessibleScheme,
        darkTheme = darkTheme,
        dynamic = useDynamicColor,
    )

    androidx.compose.runtime.CompositionLocalProvider(LocalAirPlayrColors provides airColors) {
        MaterialTheme(
            colorScheme = accessibleScheme,
            typography = AirPlayrTypography,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ) {
                content()
            }
        }
    }
}
