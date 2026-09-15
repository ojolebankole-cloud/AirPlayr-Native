package com.airplayr.app.core.playback.dsp

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.airPlayrDspStore by preferencesDataStore(name = "airplayr_dsp")
private val SPEED = doublePreferencesKey("speed")
private val PITCH = intPreferencesKey("pitch_semitones")
private val PRESERVE = booleanPreferencesKey("preserve_pitch")
private val LOCK = booleanPreferencesKey("lock_pitch")
private val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
private val EQ_PRESET = androidx.datastore.preferences.core.stringPreferencesKey("eq_preset")
private val EQ_BASS = intPreferencesKey("eq_bass_mb")
private val EQ_LOUDNESS = intPreferencesKey("eq_loudness_mb")
private val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer_1000")
private val EQ_BANDS = androidx.datastore.preferences.core.stringPreferencesKey("eq_bands")

class DspRepository(private val context: Context) {
    val state: Flow<DspState> = context.airPlayrDspStore.data.map { p ->
        DspState(
            speed = (p[SPEED] ?: 1.0).coerceIn(0.25, 4.0),
            pitchSemitones = (p[PITCH] ?: 0).coerceIn(-12, 12),
            preservePitch = p[PRESERVE] ?: true,
            lockPitch = p[LOCK] ?: false,
            eqEnabled = p[EQ_ENABLED] ?: true,
            eqPreset = p[EQ_PRESET] ?: "flat",
            bassMb = (p[EQ_BASS] ?: 0).coerceIn(-1500, 1500),
            loudnessMb = (p[EQ_LOUDNESS] ?: 0).coerceIn(0, 1500),
            virtualizerStrength = (p[EQ_VIRTUALIZER] ?: 0).coerceIn(0, 1000),
            bands = decodeBands(p[EQ_BANDS]),
        )
    }

    suspend fun setSpeed(value: Double) = context.airPlayrDspStore.edit { it[SPEED] = value.coerceIn(0.25, 4.0) }
    suspend fun setPitch(value: Int) = context.airPlayrDspStore.edit { it[PITCH] = value.coerceIn(-12, 12) }
    suspend fun setPreservePitch(value: Boolean) = context.airPlayrDspStore.edit { it[PRESERVE] = value }
    suspend fun setLockPitch(value: Boolean) = context.airPlayrDspStore.edit { it[LOCK] = value }
    suspend fun setBand(index: Int, levelMb: Int) { context.airPlayrDspStore.edit { p ->
        val current = decodeBands(p[EQ_BANDS]).toMutableMap(); current[index.coerceAtLeast(0)] = levelMb
        p[EQ_BANDS] = current.entries.sortedBy { it.key }.joinToString(";") { "${it.key}:${it.value}" }
    } }

    suspend fun clearBands() = context.airPlayrDspStore.edit { it.remove(EQ_BANDS) }

    suspend fun reset() = context.airPlayrDspStore.edit { it.clear() }

    suspend fun setEqualizer(enabled: Boolean, preset: String, bassMb: Int, loudnessMb: Int, virtualizerStrength: Int) =
        context.airPlayrDspStore.edit {
            it[EQ_ENABLED] = enabled
            it[EQ_PRESET] = preset
            it[EQ_BASS] = bassMb.coerceIn(-1500, 1500)
            it[EQ_LOUDNESS] = loudnessMb.coerceIn(0, 1500)
            it[EQ_VIRTUALIZER] = virtualizerStrength.coerceIn(0, 1000)
        }
}

private fun decodeBands(encoded: String?): Map<Int, Int> = encoded.orEmpty().split(';').mapNotNull { token ->
    val parts = token.split(':'); if (parts.size != 2) null else parts[0].toIntOrNull()?.let { i -> parts[1].toIntOrNull()?.let { v -> i to v } }
}.toMap()

data class DspState(
    val speed: Double = 1.0,
    val pitchSemitones: Int = 0,
    val preservePitch: Boolean = true,
    val lockPitch: Boolean = false,
    val eqEnabled: Boolean = true,
    val eqPreset: String = "flat",
    val bassMb: Int = 0,
    val loudnessMb: Int = 0,
    val virtualizerStrength: Int = 0,
    val bands: Map<Int, Int> = emptyMap(),
)
