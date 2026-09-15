package com.airplayr.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Phase 2 visual contract copied from the reference FeatureCard component.
 * Later screen work should consume these constants instead of inventing
 * per-screen gradients, radii, shadows, or artwork placement.
 */
object AirPlayrFeatureCardTokens {
    const val gapDp = 12f
    const val radiusDp = 27f
    const val artworkTopInsetDp = 10f
    const val copyHorizontalInsetDp = 17f
    const val copyBottomInsetDp = 17f
    const val shadowElevationDp = 10f
    const val shadowOpacity = 0.28f
    const val shadowRadiusDp = 16f
    const val shadowOffsetYDp = 10f
    const val artworkHeightFraction = 0.64f
    const val titleFontSizeSp = 21f
    const val titleLineHeightSp = 24f
    const val eyebrowFontSizeSp = 9.5f
    const val eyebrowLineHeightSp = 13f
    const val subtitleFontSizeSp = 11f
    const val subtitleLineHeightSp = 14f

    val forYouLight = listOf(Color(0xFFF6D7E8), Color(0xFFE9D8FA), Color(0xFFD7E8F7))
    val morningLight = listOf(Color(0xFFFFE0B5), Color(0xFFD9F0D8), Color(0xFFD7E9F8))
    val afternoonLight = listOf(Color(0xFFFFD0C1), Color(0xFFD5E8F7), Color(0xFFE8D7F5))
    val nightLight = listOf(Color(0xFFD9D4F7), Color(0xFFE6CEE8), Color(0xFFCFE3F1))
    val rediscoverLight = listOf(Color(0xFFCDEDE4), Color(0xFFD8E1F7), Color(0xFFFFE0D1))
    val rockLight = listOf(Color(0xFFFFD1C5), Color(0xFFD6D8F7), Color(0xFFCBE9F0))
    val playlistsLight = listOf(Color(0xFFF3D1E2), Color(0xFFD5DDF6), Color(0xFFD2EBD9))

    val forYouDark = listOf(Color(0xFF241B3A), Color(0xFF30244A), Color(0xFF182B3D))
    val morningDark = listOf(Color(0xFF3A2920), Color(0xFF203A35), Color(0xFF202C45))
    val afternoonDark = listOf(Color(0xFF17343D), Color(0xFF3A2A20), Color(0xFF2C2343))
    val nightDark = listOf(Color(0xFF23244B), Color(0xFF34213E), Color(0xFF182D42))
    val rediscoverDark = listOf(Color(0xFF163936), Color(0xFF28264A), Color(0xFF3A2824))
    val rockDark = listOf(Color(0xFF3B2422), Color(0xFF29264A), Color(0xFF173641))
    val playlistsDark = listOf(Color(0xFF3A2030), Color(0xFF25294A), Color(0xFF193A35))
}
