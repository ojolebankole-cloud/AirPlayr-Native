package com.airplayr.app.core.playback.dsp

import kotlin.math.pow

data class ReplayGainTags(
    val trackDb: Double? = null,
    val albumDb: Double? = null,
    val peak: Double? = null,
)

object ReplayGainCalculator {
    fun multiplier(
        tags: ReplayGainTags,
        mode: String,
        preampDb: Double,
        preventClipping: Boolean,
    ): Float {
        if (mode == "off") return 1f
        val gainDb = when (mode) {
            "album" -> tags.albumDb ?: tags.trackDb ?: 0.0
            "track_preferred_album" -> tags.trackDb ?: tags.albumDb ?: 0.0
            else -> tags.trackDb ?: 0.0
        } + preampDb.coerceIn(-12.0, 12.0)
        var multiplier = 10.0.pow(gainDb / 20.0)
        if (preventClipping) {
            val peak = tags.peak
            if (peak != null && peak > 0.0 && peak * multiplier > 1.0) multiplier = 1.0 / peak
        }
        return multiplier.coerceIn(0.125, 4.0).toFloat()
    }
}
