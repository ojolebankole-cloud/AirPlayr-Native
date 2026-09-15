package com.airplayr.app.core.playback.dsp

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer

/** Device-scoped Android audio effects. Every effect is optional and failures degrade gracefully. */
class AudioEffectsEngine {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var virtualizer: Virtualizer? = null
    private var sessionId = 0

    fun attach(audioSessionId: Int): EffectInfo {
        release()
        if (audioSessionId <= 0) return EffectInfo(false, false, false, false, false, "Audio session unavailable")
        sessionId = audioSessionId
        fun <T> create(block: () -> T): T? = runCatching { block() }.getOrNull()
        equalizer = create { Equalizer(0, sessionId) }
        bassBoost = create { BassBoost(0, sessionId) }
        loudness = create { LoudnessEnhancer(sessionId) }
        virtualizer = create { Virtualizer(0, sessionId) }
        return info()
    }

    fun info() = EffectInfo(
        supported = equalizer != null || bassBoost != null || loudness != null || virtualizer != null,
        equalizer = equalizer != null,
        bassBoost = bassBoost != null,
        loudnessEnhancer = loudness != null,
        virtualizer = virtualizer != null,
        reason = if (equalizer != null || bassBoost != null || loudness != null || virtualizer != null) null else "Android device exposes no compatible audio effects",
    )

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        loudness?.enabled = enabled
        virtualizer?.enabled = enabled
    }

    fun setBass(millibels: Int) {
        bassBoost?.let { runCatching { it.setStrength((millibels.coerceIn(-1500, 1500) + 1500).coerceIn(0, 1000).toShort()) } }
    }

    fun setLoudness(millibels: Int) {
        loudness?.let { runCatching { it.setTargetGain(millibels.coerceIn(0, 1500)) } }
    }

    fun setVirtualizer(strength: Int) {
        virtualizer?.let { runCatching { it.setStrength(strength.coerceIn(0, 1000).toShort()) } }
    }

    fun setBand(index: Int, levelMb: Int): Boolean {
        val eq = equalizer ?: return false
        return runCatching {
            if (index !in 0 until eq.numberOfBands) return false
            val range = eq.bandLevelRange
            eq.setBandLevel(index.toShort(), levelMb.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            true
        }.getOrDefault(false)
    }

    fun setBands(bands: Map<Int, Int>): Boolean {
        val eq = equalizer ?: return false
        return runCatching {
            bands.forEach { (index, level) -> setBand(index, level) }
            true
        }.getOrDefault(false)
    }

    fun setFlat(): Boolean {
        val eq = equalizer ?: return false
        return runCatching {
            for (i in 0 until eq.numberOfBands) eq.setBandLevel(i.toShort(), 0)
            true
        }.getOrDefault(false)
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudness?.release() }
        runCatching { virtualizer?.release() }
        equalizer = null
        bassBoost = null
        loudness = null
        virtualizer = null
        sessionId = 0
    }
}

data class EffectInfo(
    val supported: Boolean,
    val equalizer: Boolean,
    val bassBoost: Boolean,
    val loudnessEnhancer: Boolean,
    val virtualizer: Boolean,
    val reason: String?,
)
