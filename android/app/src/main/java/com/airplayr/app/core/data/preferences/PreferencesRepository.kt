package com.airplayr.app.core.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.airPlayrDataStore by preferencesDataStore(name = "airplayr_preferences")

private val THEME = stringPreferencesKey("theme")
private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
private val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
private val HAPTIC = booleanPreferencesKey("haptic_feedback")
private val TOUCH = booleanPreferencesKey("touch_feedback")
private val DEFAULT_SPEED = doublePreferencesKey("default_speed")
private val CROSSFADE_SECONDS = doublePreferencesKey("crossfade_seconds")
private val GAPLESS = booleanPreferencesKey("gapless_playback")
private val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
private val AUTO_REFRESH = booleanPreferencesKey("auto_refresh_library")
private val LIBRARY_SORT = stringPreferencesKey("library_sort")
private val LIBRARY_SORT_ORDER = stringPreferencesKey("library_sort_order")
private val LIBRARY_LAYOUT = stringPreferencesKey("library_layout")
private val SAF_ROOTS = stringSetPreferencesKey("saf_roots")

// Kept in one native repository so JS AsyncStorage state can be retired later without
// spreading persistence logic through the Compose UI.
class PreferencesRepository(private val context: Context) {
    val settings: Flow<AirPlayrSettings> = context.airPlayrDataStore.data.map { prefs ->
        AirPlayrSettings(
            theme = prefs[THEME] ?: "system",
            highContrast = prefs[HIGH_CONTRAST] ?: false,
            reduceMotion = prefs[REDUCE_MOTION] ?: false,
            hapticFeedback = prefs[HAPTIC] ?: true,
            touchFeedback = prefs[TOUCH] ?: true,
            defaultSpeed = prefs[DEFAULT_SPEED] ?: 1.0,
            crossfadeSeconds = prefs[CROSSFADE_SECONDS] ?: 0.0,
            gaplessPlayback = prefs[GAPLESS] ?: true,
            backgroundPlayback = prefs[BACKGROUND_PLAYBACK] ?: true,
            autoRefreshLibrary = prefs[AUTO_REFRESH] ?: true,
            librarySort = prefs[LIBRARY_SORT] ?: "recent",
            librarySortOrder = prefs[LIBRARY_SORT_ORDER] ?: "desc",
            libraryLayout = prefs[LIBRARY_LAYOUT] ?: "grid",
        )
    }

    suspend fun setTheme(value: String) = context.airPlayrDataStore.edit { it[THEME] = value }
    suspend fun setHighContrast(value: Boolean) = context.airPlayrDataStore.edit { it[HIGH_CONTRAST] = value }
    suspend fun setReduceMotion(value: Boolean) = context.airPlayrDataStore.edit { it[REDUCE_MOTION] = value }
    suspend fun setHapticFeedback(value: Boolean) = context.airPlayrDataStore.edit { it[HAPTIC] = value }
    suspend fun setTouchFeedback(value: Boolean) = context.airPlayrDataStore.edit { it[TOUCH] = value }
    suspend fun setDefaultSpeed(value: Double) = context.airPlayrDataStore.edit { it[DEFAULT_SPEED] = value.coerceIn(0.25, 4.0) }
    suspend fun setCrossfadeSeconds(value: Double) = context.airPlayrDataStore.edit { it[CROSSFADE_SECONDS] = value.coerceIn(0.0, 12.0) }
    suspend fun setGaplessPlayback(value: Boolean) = context.airPlayrDataStore.edit { it[GAPLESS] = value }
    suspend fun setBackgroundPlayback(value: Boolean) = context.airPlayrDataStore.edit { it[BACKGROUND_PLAYBACK] = value }
    suspend fun setAutoRefreshLibrary(value: Boolean) = context.airPlayrDataStore.edit { it[AUTO_REFRESH] = value }
    suspend fun setLibrarySort(value: String) = context.airPlayrDataStore.edit { it[LIBRARY_SORT] = value }
    suspend fun setLibrarySortOrder(value: String) = context.airPlayrDataStore.edit { it[LIBRARY_SORT_ORDER] = value }
    suspend fun setLibraryLayout(value: String) = context.airPlayrDataStore.edit { it[LIBRARY_LAYOUT] = value }

    val safRoots: Flow<Set<String>> = context.airPlayrDataStore.data.map { it[SAF_ROOTS] ?: emptySet() }
    suspend fun addSafRoot(uri: String) = context.airPlayrDataStore.edit { prefs ->
        prefs[SAF_ROOTS] = (prefs[SAF_ROOTS] ?: emptySet()) + uri
    }
    suspend fun removeSafRoot(uri: String) = context.airPlayrDataStore.edit { prefs ->
        prefs[SAF_ROOTS] = (prefs[SAF_ROOTS] ?: emptySet()) - uri
    }

    suspend fun resetPlaybackDefaults() {
        context.airPlayrDataStore.edit { prefs ->
            prefs.remove(DEFAULT_SPEED)
            prefs.remove(CROSSFADE_SECONDS)
            prefs.remove(GAPLESS)
            prefs.remove(BACKGROUND_PLAYBACK)
        }
    }
}

data class AirPlayrSettings(
    val theme: String,
    val highContrast: Boolean,
    val reduceMotion: Boolean,
    val hapticFeedback: Boolean,
    val touchFeedback: Boolean,
    val defaultSpeed: Double,
    val crossfadeSeconds: Double,
    val gaplessPlayback: Boolean,
    val backgroundPlayback: Boolean,
    val autoRefreshLibrary: Boolean,
    val librarySort: String,
    val librarySortOrder: String,
    val libraryLayout: String,
)
