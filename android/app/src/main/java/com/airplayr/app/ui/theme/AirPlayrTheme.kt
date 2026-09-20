package com.airplayr.app.ui.theme

import android.app.WallpaperManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier

private val AirPlayrDarkFallback = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    inverseSurface = Color(0xFFE6E0E9),
    inverseOnSurface = Color(0xFF322F35),
    inversePrimary = Color(0xFF6750A4),
    scrim = Color(0xB3000000),
    surfaceDim = Color(0xFF141218),
    surfaceBright = Color(0xFF3B383F),
    surfaceContainerLowest = Color(0xFF0F0D13),
    surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26),
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),
)

private val AirPlayrLightFallback = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF2FBF8),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    inverseSurface = Color(0xFF322F35),
    inverseOnSurface = Color(0xFFF5EFF7),
    inversePrimary = Color(0xFFD0BCFF),
    scrim = Color(0x59000000),
    surfaceDim = Color(0xFFD6E9E3),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFF7FCFA),
    surfaceContainerLow = Color(0xFFEDF7F3),
    surfaceContainer = Color(0xFFDEEFE9),
    surfaceContainerHigh = Color(0xFFD5E8E1),
    surfaceContainerHighest = Color(0xFFCCDCD6),
)


private fun arrangeLightScheme(scheme: ColorScheme): ColorScheme {
    fun blend(from: Color, to: Color, amount: Float): Color =
        Color(
            red = from.red + (to.red - from.red) * amount,
            green = from.green + (to.green - from.green) * amount,
            blue = from.blue + (to.blue - from.blue) * amount,
            alpha = from.alpha + (to.alpha - from.alpha) * amount,
        )

    // The reference uses the Material You relationship of:
    //   pale dynamic canvas -> near-white cards -> visibly tinted containers.
    // The old implementation pushed the background toward surfaceContainerLow only weakly and
    // also pulled the base surface toward that same low container, which could collapse both into
    // an almost-white canvas. Keep the wallpaper-derived roles, but deliberately restore tonal
    // separation so the light theme behaves like the reference image.
    val background = blend(scheme.background, scheme.surfaceContainer, 0.66f)
    val surface = blend(scheme.surface, Color.White, 0.88f)
    val surfaceBright = blend(scheme.surfaceBright, Color.White, 0.60f)
    return scheme.copy(
        background = background,
        surface = surface,
        surfaceBright = surfaceBright,
    )
}


@Composable
fun AirPlayrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Configuration changes (including Android's dynamic color resource updates) naturally
    // invalidate this composition. WallpaperManager below covers the case where the wallpaper
    // colors change while the Activity stays alive without recreating the configuration.
    LocalConfiguration.current

    val useDynamicColor = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // Do not invalidate the entire app on every ON_RESUME. That used to make warm re-entry do a
    // global theme rebuild. Listen specifically for wallpaper color changes instead, so dynamic
    // Material You updates are ready before the next visible frame after a wallpaper change.
    val dynamicColorRefresh = remember { mutableIntStateOf(0) }
    DisposableEffect(context, useDynamicColor) {
        if (!useDynamicColor) {
            return@DisposableEffect onDispose { }
        }
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
                // Coalesce duplicate Android wallpaper callbacks without adding a visible delay.
                // The increment happens on the next main-looper turn, allowing the new Material
                // You palette to be read before the next rendered frame.
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
    val baseScheme: ColorScheme = remember(darkTheme, useDynamicColor, dynamicColorRefresh.intValue) {
        when {
            useDynamicColor && darkTheme -> dynamicDarkColorScheme(context)
            useDynamicColor && !darkTheme -> dynamicLightColorScheme(context)
            darkTheme -> AirPlayrDarkFallback
            else -> AirPlayrLightFallback
        }
    }

    // Keep Material You's wallpaper-derived hue while giving the light app a visibly tinted
    // background/surface hierarchy instead of an almost-white canvas. Containers remain the
    // system-derived Material roles so cards, sheets and controls retain tonal separation.
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

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAirPlayrColors provides airColors,
    ) {
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
