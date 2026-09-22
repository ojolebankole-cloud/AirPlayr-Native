package com.airplayr.app.core.playback.dsp

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import kotlin.math.roundToInt

/**
 * Owns the Android audio effects for exactly one live audio session.
 * Attachment is session-scoped; changing EQ values never recreates the effect objects.
 */
class AudioEffectsEngine {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudness: LoudnessEnhancer? = null
    private var virtualizer: Virtualizer? = null
    private var sessionId = 0

    fun attach(audioSessionId: Int): EffectInfo {
        if (audioSessionId <= 0) {
            release()
            return EffectInfo(false, false, false, false, false, "Audio session unavailable")
        }
        if (sessionId == audioSessionId && hasAnyEffect()) return info()

        release()
        sessionId = audioSessionId
        fun <T> create(block: () -> T): T? = runCatching { block() }.getOrNull()
        equalizer = create { Equalizer(0, sessionId) }
        bassBoost = create { BassBoost(0, sessionId) }
        loudness = create { LoudnessEnhancer(sessionId) }
        virtualizer = create { Virtualizer(0, sessionId) }
        return info()
    }

    fun isAttachedTo(audioSessionId: Int): Boolean = sessionId == audioSessionId && hasAnyEffect()

    fun applyState(state: DspState): EffectInfo {
        if (!hasAnyEffect()) return info()
        setEnabled(state.eqEnabled)
        setBass(state.bassMb)
        setLoudness(state.loudnessMb)
        setVirtualizer(state.virtualizerStrength)
        if (state.eqPreset == "flat") setFlat() else setBands(state.bands)
        return info()
    }

    fun info() = EffectInfo(
        supported = hasAnyEffect(),
        equalizer = equalizer != null,
        bassBoost = bassBoost != null,
        loudnessEnhancer = loudness != null,
        virtualizer = virtualizer != null,
        reason = if (hasAnyEffect()) null else "Android device exposes no compatible audio effects",
    )

    fun setEnabled(enabled: Boolean) {
        equalizer?.let { runCatching { it.enabled = enabled } }
        bassBoost?.let { runCatching { it.enabled = enabled } }
        loudness?.let { runCatching { it.enabled = enabled } }
        virtualizer?.let { runCatching { it.enabled = enabled } }
    }

    fun setBass(millibels: Int) {
        bassBoost?.let { runCatching { it.setStrength(millibels.coerceIn(0, 1000).toShort()) } }
    }

    fun setLoudness(millibels: Int) {
        loudness?.let { runCatching { it.setTargetGain(millibels.coerceIn(0, 1000)) } }
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
            if (bands.isEmpty()) return@runCatching true
            val logical = (0..9).map { index -> bands[index] ?: 0 }
            val count = eq.numberOfBands.toInt()
            if (count <= 0) return@runCatching false
            val range = eq.bandLevelRange
            for (bandIndex in 0 until count) {
                val position = if (count == 1) 0f else bandIndex.toFloat() * 9f / (count - 1)
                val low = position.toInt().coerceIn(0, 9)
                val high = (low + 1).coerceAtMost(9)
                val fraction = position - low
                val level = logical[low] + (logical[high] - logical[low]) * fraction
                eq.setBandLevel(
                    bandIndex.toShort(),
                    level.roundToInt().coerceIn(range[0].toInt(), range[1].toInt()).toShort(),
                )
            }
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

    private fun hasAnyEffect(): Boolean =
        equalizer != null || bassBoost != null || loudness != null || virtualizer != null
}

data class EffectInfo(
    val supported: Boolean,
    val equalizer: Boolean,
    val bassBoost: Boolean,
    val loudnessEnhancer: Boolean,
    val virtualizer: Boolean,
    val reason: String?,
)
