package com.airplayr.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Full AirPlayr semantic color contract.
 *
 * The base values mirror theme/colors.js in the reference app. When Android
 * dynamic color is enabled, the Material 3 roles are resolved from the system
 * palette and the derived AirPlayr tokens follow those roles.
 */
@Immutable
data class AirPlayrColors(
    val background: Color,
    val backgroundElevated: Color,
    val surface: Color,
    val surfaceSolid: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val surfaceTint: Color,
    val accent: Color,
    val accentSoft: Color,
    val accentMuted: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val overlay: Color,
    val overlayHeavy: Color,
    val scrim: Color,
    val trackMin: Color,
    val trackMax: Color,
    val pill: Color,
    val pillActive: Color,
    val textOnAccent: Color,
)

private fun Color.withOpacity(alpha: Float) = copy(alpha = alpha)

fun airPlayrColorsFor(
    scheme: androidx.compose.material3.ColorScheme,
    darkTheme: Boolean,
    dynamic: Boolean,
): AirPlayrColors {
    val base = if (darkTheme) AirPlayrDarkBase else AirPlayrLightBase
    val primary = scheme.primary
    val outlineVariant = scheme.outlineVariant
    val background = scheme.background
    return base.copy(
        primary = primary,
        onPrimary = scheme.onPrimary,
        primaryContainer = scheme.primaryContainer,
        onPrimaryContainer = scheme.onPrimaryContainer,
        secondary = scheme.secondary,
        onSecondary = scheme.onSecondary,
        secondaryContainer = scheme.secondaryContainer,
        onSecondaryContainer = scheme.onSecondaryContainer,
        tertiary = scheme.tertiary,
        onTertiary = scheme.onTertiary,
        tertiaryContainer = scheme.tertiaryContainer,
        onTertiaryContainer = scheme.onTertiaryContainer,
        error = scheme.error,
        onError = scheme.onError,
        errorContainer = scheme.errorContainer,
        onErrorContainer = scheme.onErrorContainer,
        background = scheme.background,
        onBackground = scheme.onBackground,
        surface = scheme.surface,
        surfaceSolid = scheme.surface,
        backgroundElevated = scheme.surfaceContainerLow,
        onSurface = scheme.onSurface,
        surfaceVariant = scheme.surfaceVariant,
        onSurfaceVariant = scheme.onSurfaceVariant,
        outline = scheme.outline,
        outlineVariant = outlineVariant,
        inverseSurface = scheme.inverseSurface,
        inverseOnSurface = scheme.inverseOnSurface,
        inversePrimary = scheme.inversePrimary,
        surfaceContainerLowest = scheme.surfaceContainerLowest,
        surfaceContainerLow = scheme.surfaceContainerLow,
        surfaceContainer = scheme.surfaceContainer,
        surfaceContainerHigh = scheme.surfaceContainerHigh,
        surfaceContainerHighest = scheme.surfaceContainerHighest,
        surfaceTint = scheme.primary,
        accent = primary,
        accentSoft = primary.withOpacity(0.18f),
        accentMuted = primary.withOpacity(0.45f),
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        textMuted = scheme.outline,
        onSuccess = if (dynamic) scheme.onTertiary else base.onSuccess,
        success = if (dynamic) scheme.tertiary else base.success,
        successContainer = if (dynamic) scheme.tertiaryContainer else base.successContainer,
        onSuccessContainer = if (dynamic) scheme.onTertiaryContainer else base.onSuccessContainer,
        warning = if (dynamic) scheme.secondary else base.warning,
        onWarning = if (dynamic) scheme.onSecondary else base.onWarning,
        warningContainer = if (dynamic) scheme.secondaryContainer else base.warningContainer,
        onWarningContainer = if (dynamic) scheme.onSecondaryContainer else base.onWarningContainer,
        overlay = background.withOpacity(if (darkTheme) 0.55f else 0.45f),
        overlayHeavy = background.withOpacity(if (darkTheme) 0.82f else 0.82f),
        scrim = Color.Black.withOpacity(if (darkTheme) 0.70f else 0.35f),
        trackMin = primary,
        trackMax = (outlineVariant).withOpacity(0.25f),
        pill = scheme.surfaceContainerHigh,
        pillActive = primary,
        textOnAccent = scheme.onPrimary,
    )
}

private val AirPlayrDarkBase = AirPlayrColors(
    background = Color(0xFF141218), backgroundElevated = Color(0xFF1D1B20),
    surface = Color(0xFF141218), surfaceSolid = Color(0xFF141218),
    surfaceContainerLowest = Color(0xFF0F0D13), surfaceContainerLow = Color(0xFF1D1B20),
    surfaceContainer = Color(0xFF211F26), surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B), surfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF938F99), outlineVariant = Color(0xFF49454F),
    onSurface = Color(0xFFE6E0E9), onSurfaceVariant = Color(0xFFCAC4D0),
    onBackground = Color(0xFFE6E0E9), textPrimary = Color(0xFFE6E0E9),
    textSecondary = Color(0xFFCAC4D0), textMuted = Color(0xFF938F99),
    primary = Color(0xFFD0BCFF), onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B), onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC), onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458), onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8), onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48), onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE6E0E9), inverseOnSurface = Color(0xFF322F35),
    inversePrimary = Color(0xFF6750A4), surfaceTint = Color(0xFFD0BCFF),
    accent = Color(0xFFD0BCFF), accentSoft = Color(0xFFD0BCFF2E), accentMuted = Color(0xFFD0BCFF73),
    success = Color(0xFFB4D6B0), onSuccess = Color(0xFF163719),
    successContainer = Color(0xFF294D2C), onSuccessContainer = Color(0xFFD0F0CA),
    warning = Color(0xFFE8C36A), onWarning = Color(0xFF3A2F00),
    warningContainer = Color(0xFF534600), onWarningContainer = Color(0xFFFFE494),
    overlay = Color(0x8C141218), overlayHeavy = Color(0xD1141218),
    scrim = Color(0xB3000000), trackMin = Color(0xFFD0BCFF),
    trackMax = Color(0x40938F99), pill = Color(0xFF2B2930), pillActive = Color(0xFFD0BCFF),
    textOnAccent = Color(0xFF381E72),
)

private val AirPlayrLightBase = AirPlayrColors(
    background = Color(0xFFFFFBFE), backgroundElevated = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE), surfaceSolid = Color(0xFFFFFBFE),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F2FA),
    surfaceContainer = Color(0xFFF3EDF7), surfaceContainerHigh = Color(0xFFECE6F0),
    surfaceContainerHighest = Color(0xFFE6E0E9), surfaceVariant = Color(0xFFE7E0EC),
    outline = Color(0xFF79747E), outlineVariant = Color(0xFFCAC4D0),
    onSurface = Color(0xFF1D1B20), onSurfaceVariant = Color(0xFF49454F),
    onBackground = Color(0xFF1D1B20), textPrimary = Color(0xFF1D1B20),
    textSecondary = Color(0xFF49454F), textMuted = Color(0xFF79747E),
    primary = Color(0xFF6750A4), onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF), onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8), onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4), onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF322F35), inverseOnSurface = Color(0xFFF5EFF7),
    inversePrimary = Color(0xFFD0BCFF), surfaceTint = Color(0xFF6750A4),
    accent = Color(0xFF6750A4), accentSoft = Color(0xFF6750A42E), accentMuted = Color(0xFF6750A473),
    success = Color(0xFF386A3A), onSuccess = Color.White,
    successContainer = Color(0xFFBCECB9), onSuccessContainer = Color(0xFF002108),
    warning = Color(0xFF705C00), onWarning = Color.White,
    warningContainer = Color(0xFFFFE082), onWarningContainer = Color(0xFF231B00),
    overlay = Color(0x73FFFBFE), overlayHeavy = Color(0xD9F7F2FA),
    scrim = Color(0x59000000), trackMin = Color(0xFF6750A4),
    trackMax = Color(0x4079747E), pill = Color(0xFFECE6F0), pillActive = Color(0xFF6750A4),
    textOnAccent = Color.White,
)

val LocalAirPlayrColors = staticCompositionLocalOf { AirPlayrLightBase }

object AirPlayrPalette {
    val light: AirPlayrColors get() = AirPlayrLightBase
    val dark: AirPlayrColors get() = AirPlayrDarkBase
}

/** User-facing accent choices. Dynamic is Material You on Android 12+ and AirPlayr Blue elsewhere. */
data class AirPlayrAccentOption(
    val key: String,
    val label: String,
    val color: Color,
)

val AirPlayrAccentOptions = listOf(
    AirPlayrAccentOption("blue", "AirPlayr Blue", Color(0xFF1976D2)),
    AirPlayrAccentOption("purple", "Purple", Color(0xFF6750A4)),
    AirPlayrAccentOption("teal", "Teal", Color(0xFF00796B)),
    AirPlayrAccentOption("orange", "Orange", Color(0xFFE86A17)),
    AirPlayrAccentOption("pink", "Pink", Color(0xFFC2185B)),
)

fun accentColorSeed(key: String): Color =
    AirPlayrAccentOptions.firstOrNull { it.key == key }?.color ?: Color(0xFF1976D2)

fun accentLabel(key: String, dynamicSupported: Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S): String =
    if (key == "dynamic") {
        if (dynamicSupported) "Dynamic · Material You" else "Dynamic · AirPlayr Blue fallback"
    } else {
        AirPlayrAccentOptions.firstOrNull { it.key == key }?.label ?: "AirPlayr Blue"
    }
