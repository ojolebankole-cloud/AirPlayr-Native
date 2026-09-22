package com.airplayr.app.core.data.preferences

object NowPlayingTheme {

    const val DEFAULT = "default"
    const val COLOURED = "coloured"
    const val GRADIENT = "gradient"
    const val BLUR = "blur"
    const val GRADIENT_BLUR = "gradient_blur"
    const val GLOW = "glow"
    const val SHIMMER = "shimmer"

    val values = listOf(DEFAULT, COLOURED, GRADIENT, BLUR, GRADIENT_BLUR, GLOW, SHIMMER)

    fun label(value: String): String = when (value) {
        COLOURED -> "Coloured"
        GRADIENT -> "Gradient"
        BLUR -> "Blur"
        GRADIENT_BLUR -> "Gradient blur"
        GLOW -> "Glow"
        SHIMMER -> "Shimmer"
        else -> "Default"
    }

    fun description(value: String): String = when (value) {
        DEFAULT -> "Material You on Android 12+; Sky Blue on older Android"
        COLOURED -> "Artwork-derived colour background"
        GRADIENT -> "Artwork-derived multi-colour gradient"
        BLUR -> "Artwork as a softly blurred background"
        GRADIENT_BLUR -> "Artwork, gradient and blur combined"
        GLOW -> "Ambient artwork colour glow"
        SHIMMER -> "Subtle animated artwork light"
        else -> description(DEFAULT)
    }

    fun defaultForDevice(): String = DEFAULT
}
