package com.airplayr.app.core.data.preferences

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray

private val Context.airPlayrDataStore by preferencesDataStore(name = "airplayr_preferences")

private val THEME = stringPreferencesKey("theme")
private val ACCENT_COLOR = stringPreferencesKey("accent_color")
private val AMOLED_MODE = booleanPreferencesKey("amoled_mode")
private val NOW_PLAYING_THEME = stringPreferencesKey("now_playing_theme")
private val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
private val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
private val HAPTIC = booleanPreferencesKey("haptic_feedback")
private val TOUCH = booleanPreferencesKey("touch_feedback")
private val RESUME_MODE = stringPreferencesKey("resume_mode")
private val DEFAULT_SPEED = doublePreferencesKey("default_speed")
private val CROSSFADE_SECONDS = doublePreferencesKey("crossfade_seconds")
private val GAPLESS = booleanPreferencesKey("gapless_playback")
private val REPLAY_GAIN_MODE = stringPreferencesKey("replay_gain_mode")
private val REPLAY_GAIN_PREAMP = doublePreferencesKey("replay_gain_preamp_db")
private val REPLAY_GAIN_CLIPPING = booleanPreferencesKey("replay_gain_prevent_clipping")
private val DEFAULT_ASPECT = stringPreferencesKey("default_aspect")
private val AUTO_HIDE_MS = intPreferencesKey("auto_hide_ms")
private val KEEP_AWAKE = booleanPreferencesKey("keep_awake")
private val SUBTITLE_DEFAULT = booleanPreferencesKey("subtitles_enabled_default")
private val SUBTITLE_SIZE = doublePreferencesKey("subtitle_size")
private val SUBTITLE_POSITION = stringPreferencesKey("subtitle_position")
private val SUBTITLE_BG = booleanPreferencesKey("subtitle_bg")
private val SUBTITLE_COLOR = stringPreferencesKey("subtitle_color")
private val SUBTITLE_DELAY_MS = intPreferencesKey("subtitle_delay_ms")
private val REMEMBER_SUBTITLE_TRACK = booleanPreferencesKey("remember_subtitle_track")
private val PREFER_EXTERNAL_SUBTITLES = booleanPreferencesKey("prefer_external_subtitles")
private val REMEMBER_AUDIO_TRACK = booleanPreferencesKey("remember_audio_track")
private val DEFAULT_VOLUME = doublePreferencesKey("default_volume")
private val MUTE_ON_START = booleanPreferencesKey("mute_on_start")
private val GESTURE_DOUBLE_TAP = booleanPreferencesKey("gesture_double_tap_seek")
private val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
private val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
private val GESTURE_LONG_PRESS = booleanPreferencesKey("gesture_long_press_2x")
private val GESTURE_PINCH = booleanPreferencesKey("gesture_pinch_zoom")
private val GESTURE_HORIZONTAL = booleanPreferencesKey("gesture_horizontal_seek")
private val SEEK_STEP_SECONDS = intPreferencesKey("seek_step_seconds")
private val LIBRARY_SORT = stringPreferencesKey("library_sort")
private val LIBRARY_SORT_ORDER = stringPreferencesKey("library_sort_order")
private val LIBRARY_LAYOUT = stringPreferencesKey("library_layout")
private val VIDEO_LIBRARY_MODE = stringPreferencesKey("video_library_mode")
private val VIDEO_LIBRARY_SORT = stringPreferencesKey("video_library_sort")
private val VIDEO_LIBRARY_SORT_ORDER = stringPreferencesKey("video_library_sort_order")
private val GRID_COLUMNS = intPreferencesKey("grid_columns")
private val AUTO_REFRESH = booleanPreferencesKey("auto_refresh_library")
private val SHOW_NOTIFICATION = booleanPreferencesKey("show_now_playing_notification")
private val ALLOW_PIP = booleanPreferencesKey("allows_picture_in_picture")
private val AUTO_PIP = booleanPreferencesKey("auto_enter_pip_on_background")
private val BACKGROUND_PLAYBACK = booleanPreferencesKey("background_playback")
private val SAVE_HISTORY = booleanPreferencesKey("save_playback_history")
private val SHOW_CONTINUE = booleanPreferencesKey("show_continue_watching")
private val SHOW_RECENT = booleanPreferencesKey("show_recently_played")
private val SLEEP_DEADLINE_MS = androidx.datastore.preferences.core.longPreferencesKey("sleep_deadline_ms")
private val SLEEP_FINISH_CURRENT = booleanPreferencesKey("sleep_finish_current")
private val SLEEP_FADE_OUT = booleanPreferencesKey("sleep_fade_out")
private val SLEEP_START_VOLUME = doublePreferencesKey("sleep_start_volume")
private val SLEEP_PENDING_TRACK_ID = stringPreferencesKey("sleep_pending_track_id")
private val SAF_ROOTS = stringSetPreferencesKey("saf_roots")
private val WHATS_NEW_SEEN_VERSION = stringPreferencesKey("whats_new_seen_version")
private val INITIAL_LIBRARY_SCAN_DONE = booleanPreferencesKey("initial_library_scan_done")
private val MEDIASTORE_VERSION = stringPreferencesKey("mediastore_version")
private val MEDIASTORE_LAST_REFRESH_MS = androidx.datastore.preferences.core.longPreferencesKey("mediastore_last_refresh_ms")
private val GENRE_METADATA_VERSION = stringPreferencesKey("genre_metadata_version")
private val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
private const val STARTUP_PREFS = "airplayr_startup"
private const val STARTUP_THEME = "theme"

private fun encodeRecentSearches(values: List<String>): String =
    JSONArray().apply { values.forEach { put(it) } }.toString()

private fun decodeRecentSearches(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList(array.length()) { for (index in 0 until array.length()) add(array.getString(index)) }
    }.getOrDefault(emptyList())
}

class PreferencesRepository(private val context: Context) {
    val recentSearches: Flow<List<String>> = context.airPlayrDataStore.data.map { p ->
        decodeRecentSearches(p[RECENT_SEARCHES])
    }

    suspend fun recordRecentSearch(query: String) {
        val value = query.trim()
        if (value.isBlank()) return
        context.airPlayrDataStore.edit { prefs ->
            val current = decodeRecentSearches(prefs[RECENT_SEARCHES])
            val updated = listOf(value) + current.filterNot { it.equals(value, ignoreCase = true) }
            prefs[RECENT_SEARCHES] = encodeRecentSearches(updated.take(8))
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.airPlayrDataStore.edit { prefs ->
            val updated = decodeRecentSearches(prefs[RECENT_SEARCHES]).filterNot { it == query }
            prefs[RECENT_SEARCHES] = encodeRecentSearches(updated)
        }
    }

    val settings: Flow<AirPlayrSettings> = context.airPlayrDataStore.data.map { p ->
        AirPlayrSettings(
            theme = p[THEME] ?: "light",
            accentColor = p[ACCENT_COLOR] ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) "dynamic" else "blue",
            amoledMode = p[AMOLED_MODE] ?: false,
            nowPlayingTheme = p[NOW_PLAYING_THEME] ?: "default",
            highContrast = p[HIGH_CONTRAST] ?: false,
            reduceMotion = p[REDUCE_MOTION] ?: false,
            hapticFeedback = p[HAPTIC] ?: true,
            touchFeedback = p[TOUCH] ?: true,
            resumeMode = p[RESUME_MODE] ?: "auto",
            defaultSpeed = p[DEFAULT_SPEED] ?: 1.0,
            crossfadeSeconds = p[CROSSFADE_SECONDS] ?: 0.0,
            gaplessPlayback = p[GAPLESS] ?: true,
            replayGainMode = p[REPLAY_GAIN_MODE] ?: "off",
            replayGainPreampDb = p[REPLAY_GAIN_PREAMP] ?: 0.0,
            replayGainPreventClipping = p[REPLAY_GAIN_CLIPPING] ?: true,
            defaultAspect = p[DEFAULT_ASPECT] ?: "contain",
            autoHideMs = p[AUTO_HIDE_MS] ?: 4000,
            keepAwake = p[KEEP_AWAKE] ?: true,
            subtitlesEnabledDefault = p[SUBTITLE_DEFAULT] ?: true,
            subtitleSize = p[SUBTITLE_SIZE] ?: 1.0,
            subtitlePosition = p[SUBTITLE_POSITION] ?: "bottom",
            subtitleBg = p[SUBTITLE_BG] ?: true,
            subtitleColor = p[SUBTITLE_COLOR] ?: "#FFFFFF",
            subtitleDelayMs = p[SUBTITLE_DELAY_MS] ?: 0,
            rememberSubtitleTrack = p[REMEMBER_SUBTITLE_TRACK] ?: true,
            preferExternalSubtitles = p[PREFER_EXTERNAL_SUBTITLES] ?: true,
            rememberAudioTrack = p[REMEMBER_AUDIO_TRACK] ?: true,
            defaultVolume = p[DEFAULT_VOLUME] ?: 1.0,
            muteOnStart = p[MUTE_ON_START] ?: false,
            gestureDoubleTapSeek = p[GESTURE_DOUBLE_TAP] ?: true,
            gestureVolume = p[GESTURE_VOLUME] ?: true,
            gestureBrightness = p[GESTURE_BRIGHTNESS] ?: true,
            gestureLongPress2x = p[GESTURE_LONG_PRESS] ?: true,
            gesturePinchZoom = p[GESTURE_PINCH] ?: true,
            gestureHorizontalSeek = p[GESTURE_HORIZONTAL] ?: true,
            seekStepSeconds = p[SEEK_STEP_SECONDS] ?: 10,
            librarySort = p[LIBRARY_SORT] ?: "recent",
            librarySortOrder = p[LIBRARY_SORT_ORDER] ?: "desc",
            libraryLayout = p[LIBRARY_LAYOUT] ?: "grid",
            gridColumns = p[GRID_COLUMNS] ?: 0,
            autoRefreshLibrary = p[AUTO_REFRESH] ?: true,
            showNowPlayingNotification = p[SHOW_NOTIFICATION] ?: true,
            allowsPictureInPicture = p[ALLOW_PIP] ?: true,
            autoEnterPiPOnBackground = p[AUTO_PIP] ?: false,
            backgroundPlayback = p[BACKGROUND_PLAYBACK] ?: true,
            savePlaybackHistory = p[SAVE_HISTORY] ?: true,
            showContinueWatching = p[SHOW_CONTINUE] ?: true,
            showRecentlyPlayed = p[SHOW_RECENT] ?: true,
            sleepDeadlineMs = p[SLEEP_DEADLINE_MS] ?: 0L,
            sleepFinishCurrent = p[SLEEP_FINISH_CURRENT] ?: false,
            sleepFadeOut = p[SLEEP_FADE_OUT] ?: true,
            sleepStartVolume = (p[SLEEP_START_VOLUME] ?: 1.0).coerceIn(0.0, 1.0),
            sleepPendingTrackId = p[SLEEP_PENDING_TRACK_ID],
        )
    }

    suspend fun setTheme(v: String) {
        val theme = v.coerceTheme()
        // Commit the tiny startup mirror synchronously so a process death immediately after a
        // theme change can never relaunch into the previous light/dark mode for one frame.
        context.getSharedPreferences(STARTUP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(STARTUP_THEME, theme)
            .commit()
        context.airPlayrDataStore.edit { it[THEME] = theme }
    }
    suspend fun setAmoledMode(v: Boolean) = editBool(AMOLED_MODE, v)
    suspend fun setAccentColor(v: String) {
        context.airPlayrDataStore.edit { it[ACCENT_COLOR] = v.coerceAccent() }
    }
    suspend fun setNowPlayingTheme(v: String) {
        context.airPlayrDataStore.edit { it[NOW_PLAYING_THEME] = v.takeIf { value -> value in NowPlayingTheme.values } ?: NowPlayingTheme.DEFAULT }
    }
    suspend fun setHighContrast(v: Boolean) = editBool(HIGH_CONTRAST, v)
    suspend fun setReduceMotion(v: Boolean) = editBool(REDUCE_MOTION, v)
    suspend fun setHapticFeedback(v: Boolean) = editBool(HAPTIC, v)
    suspend fun setTouchFeedback(v: Boolean) = editBool(TOUCH, v)
    suspend fun setResumeMode(v: String) = context.airPlayrDataStore.edit { it[RESUME_MODE] = v.coerceIn(setOf("auto", "ask", "start")) }
    suspend fun setDefaultSpeed(v: Double) = context.airPlayrDataStore.edit { it[DEFAULT_SPEED] = v.coerceIn(0.25, 4.0) }
    suspend fun setCrossfadeSeconds(v: Double) = context.airPlayrDataStore.edit { it[CROSSFADE_SECONDS] = v.coerceIn(0.0, 12.0) }
    suspend fun setGaplessPlayback(v: Boolean) = editBool(GAPLESS, v)
    suspend fun setReplayGainMode(v: String) = context.airPlayrDataStore.edit { it[REPLAY_GAIN_MODE] = v.coerceIn(setOf("off", "track", "album", "track_preferred_album")) }
    suspend fun setReplayGainPreampDb(v: Double) = context.airPlayrDataStore.edit { it[REPLAY_GAIN_PREAMP] = v.coerceIn(-12.0, 12.0) }
    suspend fun setReplayGainPreventClipping(v: Boolean) = editBool(REPLAY_GAIN_CLIPPING, v)
    suspend fun setDefaultAspect(v: String) = context.airPlayrDataStore.edit { it[DEFAULT_ASPECT] = v.coerceIn(setOf("contain", "cover", "fill")) }
    suspend fun setAutoHideMs(v: Int) = context.airPlayrDataStore.edit { it[AUTO_HIDE_MS] = v.coerceIn(setOf(0, 2000, 4000, 6000, 10000)) }
    suspend fun setKeepAwake(v: Boolean) = editBool(KEEP_AWAKE, v)
    suspend fun setSubtitlesEnabledDefault(v: Boolean) = editBool(SUBTITLE_DEFAULT, v)
    suspend fun setSubtitleSize(v: Double) = context.airPlayrDataStore.edit { it[SUBTITLE_SIZE] = v.coerceIn(0.8, 1.5) }
    suspend fun setSubtitlePosition(v: String) = context.airPlayrDataStore.edit { it[SUBTITLE_POSITION] = v.coerceIn(setOf("bottom", "top")) }
    suspend fun setSubtitleBg(v: Boolean) = editBool(SUBTITLE_BG, v)
    suspend fun setSubtitleColor(v: String) = context.airPlayrDataStore.edit { it[SUBTITLE_COLOR] = v }
    suspend fun setSubtitleDelayMs(v: Int) = context.airPlayrDataStore.edit { it[SUBTITLE_DELAY_MS] = v.coerceIn(-10000, 10000) }
    suspend fun setRememberSubtitleTrack(v: Boolean) = editBool(REMEMBER_SUBTITLE_TRACK, v)
    suspend fun setPreferExternalSubtitles(v: Boolean) = editBool(PREFER_EXTERNAL_SUBTITLES, v)
    suspend fun setRememberAudioTrack(v: Boolean) = editBool(REMEMBER_AUDIO_TRACK, v)
    suspend fun setDefaultVolume(v: Double) = context.airPlayrDataStore.edit { it[DEFAULT_VOLUME] = v.coerceIn(0.0, 1.0) }
    suspend fun setMuteOnStart(v: Boolean) = editBool(MUTE_ON_START, v)
    suspend fun setGestureDoubleTapSeek(v: Boolean) = editBool(GESTURE_DOUBLE_TAP, v)
    suspend fun setGestureVolume(v: Boolean) = editBool(GESTURE_VOLUME, v)
    suspend fun setGestureBrightness(v: Boolean) = editBool(GESTURE_BRIGHTNESS, v)
    suspend fun setGestureLongPress2x(v: Boolean) = editBool(GESTURE_LONG_PRESS, v)
    suspend fun setGesturePinchZoom(v: Boolean) = editBool(GESTURE_PINCH, v)
    suspend fun setGestureHorizontalSeek(v: Boolean) = editBool(GESTURE_HORIZONTAL, v)
    suspend fun setSeekStepSeconds(v: Int) = context.airPlayrDataStore.edit { it[SEEK_STEP_SECONDS] = v.coerceIn(setOf(5, 10, 15, 30)) }
    suspend fun setLibrarySort(v: String) = context.airPlayrDataStore.edit { it[LIBRARY_SORT] = v.coerceIn(setOf("recent", "name", "artist", "album", "genre", "duration", "modified")) }
    suspend fun setLibrarySortOrder(v: String) = context.airPlayrDataStore.edit { it[LIBRARY_SORT_ORDER] = v.coerceIn(setOf("asc", "desc")) }
    suspend fun setLibraryLayout(v: String) = context.airPlayrDataStore.edit { it[LIBRARY_LAYOUT] = v.coerceIn(setOf("grid", "list")) }

    /** Video library has its own browsing mode and sort state so changing it never alters Music. */
    val videoLibraryMode: Flow<String> = context.airPlayrDataStore.data.map { it[VIDEO_LIBRARY_MODE] ?: "folders" }
    val videoLibrarySort: Flow<String> = context.airPlayrDataStore.data.map { it[VIDEO_LIBRARY_SORT] ?: "recent" }
    val videoLibrarySortOrder: Flow<String> = context.airPlayrDataStore.data.map { it[VIDEO_LIBRARY_SORT_ORDER] ?: "desc" }
    suspend fun setVideoLibraryMode(v: String) = context.airPlayrDataStore.edit { it[VIDEO_LIBRARY_MODE] = v.coerceIn(setOf("folders", "all")) }
    suspend fun setVideoLibrarySort(v: String) = context.airPlayrDataStore.edit { it[VIDEO_LIBRARY_SORT] = v.coerceIn(setOf("recent", "name", "artist", "album", "genre", "duration", "modified")) }
    suspend fun setVideoLibrarySortOrder(v: String) = context.airPlayrDataStore.edit { it[VIDEO_LIBRARY_SORT_ORDER] = v.coerceIn(setOf("asc", "desc")) }
    suspend fun setGridColumns(v: Int) = context.airPlayrDataStore.edit { it[GRID_COLUMNS] = v.coerceIn(setOf(0, 2, 3, 4)) }
    suspend fun setAutoRefreshLibrary(v: Boolean) = editBool(AUTO_REFRESH, v)
    suspend fun setShowNowPlayingNotification(v: Boolean) = editBool(SHOW_NOTIFICATION, v)
    suspend fun setAllowsPictureInPicture(v: Boolean) = editBool(ALLOW_PIP, v)
    suspend fun setAutoEnterPiPOnBackground(v: Boolean) = editBool(AUTO_PIP, v)
    suspend fun setBackgroundPlayback(v: Boolean) = editBool(BACKGROUND_PLAYBACK, v)
    suspend fun setSavePlaybackHistory(v: Boolean) = editBool(SAVE_HISTORY, v)
    suspend fun setShowContinueWatching(v: Boolean) = editBool(SHOW_CONTINUE, v)
    suspend fun setShowRecentlyPlayed(v: Boolean) = editBool(SHOW_RECENT, v)

    val safRoots: Flow<Set<String>> = context.airPlayrDataStore.data.map { it[SAF_ROOTS] ?: emptySet() }
    suspend fun hasSeenWhatsNew(version: String): Boolean = context.airPlayrDataStore.data.map { it[WHATS_NEW_SEEN_VERSION] == version }.first()
    suspend fun markWhatsNewSeen(version: String) { context.airPlayrDataStore.edit { it[WHATS_NEW_SEEN_VERSION] = version } }

    // Tracks whether the one-time approved initial MediaStore scan has ever completed.
    // Used to stop normal launches from repeating the one-time full initialization scan.
    // Once complete, normal freshness is maintained by delta synchronization.
    suspend fun hasCompletedInitialLibraryScan(): Boolean =
        context.airPlayrDataStore.data.map { it[INITIAL_LIBRARY_SCAN_DONE] == true }.first()
    suspend fun markInitialLibraryScanDone() { context.airPlayrDataStore.edit { it[INITIAL_LIBRARY_SCAN_DONE] = true } }

    suspend fun shouldRefreshMediaStore(currentFingerprint: String?, minimumIntervalMs: Long = 15 * 60_000L): Boolean {
        val p = context.airPlayrDataStore.data.first()
        val storedFingerprint = p[MEDIASTORE_VERSION]
        val lastRefresh = p[MEDIASTORE_LAST_REFRESH_MS] ?: 0L
        return if (!currentFingerprint.isNullOrBlank() && !storedFingerprint.isNullOrBlank()) {
            currentFingerprint != storedFingerprint
        } else {
            System.currentTimeMillis() - lastRefresh >= minimumIntervalMs
        }
    }

    suspend fun markMediaStoreRefresh(fingerprint: String?) {
        context.airPlayrDataStore.edit { prefs ->
            if (fingerprint.isNullOrBlank()) prefs.remove(MEDIASTORE_VERSION) else prefs[MEDIASTORE_VERSION] = fingerprint
            prefs[MEDIASTORE_LAST_REFRESH_MS] = System.currentTimeMillis()
        }
    }

    suspend fun needsGenreMetadataBackfill(version: String = "1"): Boolean =
        context.airPlayrDataStore.data.map { it[GENRE_METADATA_VERSION] != version }.first()

    suspend fun markGenreMetadataBackfillDone(version: String = "1") {
        context.airPlayrDataStore.edit { it[GENRE_METADATA_VERSION] = version }
    }
    suspend fun addSafRoot(uri: String) { context.airPlayrDataStore.edit { it[SAF_ROOTS] = (it[SAF_ROOTS] ?: emptySet()) + uri } }
    suspend fun removeSafRoot(uri: String) { context.airPlayrDataStore.edit { it[SAF_ROOTS] = (it[SAF_ROOTS] ?: emptySet()) - uri } }

    suspend fun startSleepTimer(minutes: Int, finishCurrent: Boolean, fadeOut: Boolean, startVolume: Double, nowMs: Long = System.currentTimeMillis()) {
        context.airPlayrDataStore.edit { prefs ->
            prefs[SLEEP_DEADLINE_MS] = nowMs + minutes.coerceIn(1, 120) * 60_000L
            prefs[SLEEP_FINISH_CURRENT] = finishCurrent
            prefs[SLEEP_FADE_OUT] = fadeOut
            prefs[SLEEP_START_VOLUME] = startVolume.coerceIn(0.0, 1.0)
            prefs.remove(SLEEP_PENDING_TRACK_ID)
        }
    }

    suspend fun cancelSleepTimer() {
        context.airPlayrDataStore.edit { prefs ->
            prefs[SLEEP_DEADLINE_MS] = 0L
            prefs[SLEEP_FINISH_CURRENT] = false
            prefs[SLEEP_FADE_OUT] = true
            prefs[SLEEP_START_VOLUME] = 1.0
            prefs.remove(SLEEP_PENDING_TRACK_ID)
        }
    }

    suspend fun setSleepPendingTrack(id: String?) {
        context.airPlayrDataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(SLEEP_PENDING_TRACK_ID) else prefs[SLEEP_PENDING_TRACK_ID] = id
        }
    }

    suspend fun resetToDefaults() {
        context.airPlayrDataStore.edit { it.clear() }
        context.getSharedPreferences(STARTUP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(STARTUP_THEME, "light")
            .commit()
    }

    private suspend fun editBool(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) { context.airPlayrDataStore.edit { it[key] = value } }
}

data class AirPlayrSettings(
    val theme: String,
    val accentColor: String,
    val amoledMode: Boolean = false,
    val nowPlayingTheme: String,
    val highContrast: Boolean,
    val reduceMotion: Boolean,
    val hapticFeedback: Boolean,
    val touchFeedback: Boolean,
    val resumeMode: String,
    val defaultSpeed: Double,
    val crossfadeSeconds: Double,
    val gaplessPlayback: Boolean,
    val replayGainMode: String,
    val replayGainPreampDb: Double,
    val replayGainPreventClipping: Boolean,
    val defaultAspect: String,
    val autoHideMs: Int,
    val keepAwake: Boolean,
    val subtitlesEnabledDefault: Boolean,
    val subtitleSize: Double,
    val subtitlePosition: String,
    val subtitleBg: Boolean,
    val subtitleColor: String,
    val subtitleDelayMs: Int,
    val rememberSubtitleTrack: Boolean,
    val preferExternalSubtitles: Boolean,
    val rememberAudioTrack: Boolean,
    val defaultVolume: Double,
    val muteOnStart: Boolean,
    val gestureDoubleTapSeek: Boolean,
    val gestureVolume: Boolean,
    val gestureBrightness: Boolean,
    val gestureLongPress2x: Boolean,
    val gesturePinchZoom: Boolean,
    val gestureHorizontalSeek: Boolean,
    val seekStepSeconds: Int,
    val librarySort: String,
    val librarySortOrder: String,
    val libraryLayout: String,
    val gridColumns: Int,
    val autoRefreshLibrary: Boolean,
    val showNowPlayingNotification: Boolean,
    val allowsPictureInPicture: Boolean,
    val autoEnterPiPOnBackground: Boolean,
    val backgroundPlayback: Boolean,
    val savePlaybackHistory: Boolean,
    val showContinueWatching: Boolean,
    val showRecentlyPlayed: Boolean,
    val sleepDeadlineMs: Long,
    val sleepFinishCurrent: Boolean,
    val sleepFadeOut: Boolean,
    val sleepStartVolume: Double,
    val sleepPendingTrackId: String?,
)

private fun String.coerceTheme() = takeIf { it in setOf("system", "light", "dark") } ?: "light"
private fun String.coerceAccent() = takeIf { it in setOf("dynamic", "blue", "purple", "teal", "orange", "pink") } ?: "blue"
private fun String.coerceIn(values: Set<String>) = takeIf { it in values } ?: values.first()
private fun Int.coerceIn(values: Set<Int>) = takeIf { it in values } ?: values.first()
