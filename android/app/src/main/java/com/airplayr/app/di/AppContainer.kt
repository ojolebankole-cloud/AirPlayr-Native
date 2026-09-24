package com.airplayr.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.airplayr.app.core.data.db.AirPlayrDatabase
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.media.MediaPermission
import com.airplayr.app.core.data.media.MediaStoreDataSource
import com.airplayr.app.core.data.media.MediaStoreChangeObserver
import com.airplayr.app.core.data.media.SafMediaScanner
import com.airplayr.app.core.data.preferences.PreferencesRepository
import com.airplayr.app.core.work.LibraryRefreshWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.airplayr.app.core.video.NativeVideoController
import com.airplayr.app.core.metadata.MetadataRepository
import com.airplayr.app.core.cast.CastController
import com.airplayr.app.ui.navigation.HomeRepository
import com.airplayr.app.ui.navigation.HomeStatsCache
import com.airplayr.app.ui.navigation.HomeSnapshotStore

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialScanMutex = Mutex()
    private val _syncState = MutableStateFlow(LibrarySyncState.IDLE)
    val syncState: StateFlow<LibrarySyncState> = _syncState
    private val _syncProgress = MutableStateFlow(LibrarySyncProgress())
    val syncProgress: StateFlow<LibrarySyncProgress> = _syncProgress
    private val _safScanState = MutableStateFlow(SafScanUiState())
    val safScanState: StateFlow<SafScanUiState> = _safScanState

    // The launch theme is mirrored in SharedPreferences so it can be read synchronously before
    // Android releases the splash screen. DataStore remains the authoritative settings stream
    // after the first frame, but it must never block startup/re-entry.
    private val _startupTheme = MutableStateFlow(
        appContext.getSharedPreferences("airplayr_startup", Context.MODE_PRIVATE)
            .getString("theme", "light")
            ?.takeIf { it == "light" || it == "dark" || it == "system" }
            ?: "light"
    )
    val startupTheme: StateFlow<String> = _startupTheme

    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS favorites (mediaId TEXT NOT NULL, mediaType TEXT NOT NULL, addedAt INTEGER NOT NULL, PRIMARY KEY(mediaId))")
            db.execSQL("CREATE TABLE IF NOT EXISTS playback_history (mediaId TEXT NOT NULL, mediaType TEXT NOT NULL, title TEXT NOT NULL, artist TEXT, album TEXT, artworkUri TEXT, playedAt INTEGER NOT NULL, playCount INTEGER NOT NULL, completed INTEGER NOT NULL, completedAt INTEGER, PRIMARY KEY(mediaId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_playedAt ON playback_history(playedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_mediaType ON playback_history(mediaType)")
            db.execSQL("CREATE TABLE IF NOT EXISTS music_playlists (id TEXT NOT NULL, name TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, artworkUri TEXT, PRIMARY KEY(id))")
            db.execSQL("CREATE TABLE IF NOT EXISTS music_playlist_items (playlistId TEXT NOT NULL, mediaId TEXT NOT NULL, position INTEGER NOT NULL, addedAt INTEGER NOT NULL, PRIMARY KEY(playlistId, mediaId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_music_playlist_items_playlistId_position ON music_playlist_items(playlistId, position)")
            db.execSQL("CREATE TABLE IF NOT EXISTS video_progress (mediaId TEXT NOT NULL, positionMs INTEGER NOT NULL, durationMs INTEGER NOT NULL, completed INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(mediaId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_video_progress_updatedAt ON video_progress(updatedAt)")
            db.execSQL("CREATE TABLE IF NOT EXISTS video_bookmarks (mediaId TEXT NOT NULL, positionMs INTEGER NOT NULL, label TEXT, createdAt INTEGER NOT NULL, PRIMARY KEY(mediaId, positionMs))")
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE IF NOT EXISTS playback_events (eventId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, mediaId TEXT NOT NULL, mediaType TEXT NOT NULL, playedAt INTEGER NOT NULL)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_events_playedAt ON playback_events(playedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_events_mediaType ON playback_events(mediaType)")
            db.execSQL("CREATE TABLE IF NOT EXISTS playback_sessions (sessionId TEXT NOT NULL, mediaId TEXT NOT NULL, mediaType TEXT NOT NULL, title TEXT NOT NULL, artist TEXT, album TEXT, artworkUri TEXT, startedAt INTEGER NOT NULL, endedAt INTEGER, seconds INTEGER NOT NULL, lastMediaTime INTEGER NOT NULL, PRIMARY KEY(sessionId))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_sessions_startedAt ON playback_sessions(startedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_sessions_mediaType ON playback_sessions(mediaType)")
            db.execSQL("CREATE TABLE IF NOT EXISTS playback_activity (dayKey TEXT NOT NULL, bucket TEXT NOT NULL, mediaType TEXT NOT NULL, seconds INTEGER NOT NULL, PRIMARY KEY(dayKey, bucket, mediaType))")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_activity_dayKey ON playback_activity(dayKey)")
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_media_items_mediaType_dateAdded ON media_items(mediaType, dateAdded)")
        }
    }

    private val migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_media_items_mediaType_sourceDirectoryUri ON media_items(mediaType, sourceDirectoryUri)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_mediaType_completed ON playback_history(mediaType, completed)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_history_playCount_playedAt ON playback_history(playCount, playedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_events_mediaType_playedAt ON playback_events(mediaType, playedAt)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playback_activity_mediaType_dayKey ON playback_activity(mediaType, dayKey)")
        }
    }

    private val migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS lyrics_cache (
                id TEXT NOT NULL PRIMARY KEY,
                provider TEXT NOT NULL,
                trackName TEXT NOT NULL,
                artistName TEXT NOT NULL,
                albumName TEXT NOT NULL,
                duration REAL NOT NULL,
                plainLyrics TEXT,
                syncedLyrics TEXT,
                confidence INTEGER NOT NULL,
                syncLevel INTEGER NOT NULL,
                fetchedAt INTEGER NOT NULL,
                lastAccessAt INTEGER NOT NULL
            )""")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_lyrics_cache_lastAccessAt ON lyrics_cache(lastAccessAt)")
        }
    }


    private val migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE music_playlists ADD COLUMN mediaType TEXT NOT NULL DEFAULT 'audio'")
        }
    }

    // Keep application construction cheap. Room, Cast and player-adjacent repositories are
    // created only when their first consumer actually needs them, so an existing user can get
    // a Compose frame without constructing the entire media stack on the Activity thread.
    val database: AirPlayrDatabase by lazy {
        Room.databaseBuilder(
            appContext,
            AirPlayrDatabase::class.java,
            "airplayr.db",
        ).addMigrations(migration1To2, migration2To3, migration3To4, migration4To5, migration5To6, migration6To7).build()
    }

    val lyricsRepository: com.airplayr.app.core.lyrics.LyricsRepository by lazy {
        com.airplayr.app.core.lyrics.LyricsRepository(appContext, database.lyricsCacheDao())
    }

    val preferences = PreferencesRepository(appContext)
    val mediaStore = MediaStoreDataSource(appContext)
    private val mediaStoreChangeObserver = MediaStoreChangeObserver(
        resolver = appContext.contentResolver,
        scope = applicationScope,
        refresh = { refreshLibraryAfterMediaStoreChange() },
    )
    val metadata: MetadataRepository by lazy { MetadataRepository(appContext) }
    val mediaLibrary: MediaLibraryRepository by lazy { MediaLibraryRepository({ database }, mediaStore, { metadata }) }
    val safMediaScanner = SafMediaScanner(appContext)
    val structured: com.airplayr.app.core.data.StructuredRepository by lazy { com.airplayr.app.core.data.StructuredRepository { database } }
    val stats: com.airplayr.app.core.data.PlaybackStatsRepository by lazy { com.airplayr.app.core.data.PlaybackStatsRepository { database } }
    // HomeRepository construction is cheap; starting it here lets its Room observer warm
    // on the application IO scope before the first Home composition.
    private val homeSnapshotStore = HomeSnapshotStore(appContext)
    val homeRepository: HomeRepository = HomeRepository({ mediaLibrary }, { structured }, applicationScope, homeSnapshotStore)
    val homeStats: HomeStatsCache by lazy { HomeStatsCache(appContext) { structured } }
    val search: com.airplayr.app.core.data.search.SearchRepository by lazy { com.airplayr.app.core.data.search.SearchRepository { database } }
    val collections: com.airplayr.app.core.intelligence.CollectionsRepository by lazy { com.airplayr.app.core.intelligence.CollectionsRepository(mediaLibrary, structured) }
    val dsp: com.airplayr.app.core.playback.dsp.DspRepository by lazy { com.airplayr.app.core.playback.dsp.DspRepository(appContext) }
    val videoController: NativeVideoController by lazy { NativeVideoController(appContext) }
    val cast: CastController by lazy { CastController(appContext) }

    init {
        // Home has first priority, but its Room-backed observer must not open Room while the
        // Application object is still being created on the main thread. Start the warm-up from
        // the existing application IO scope instead.
        applicationScope.launch { homeRepository.start() }

        // Keep the library fresh while the app process is alive. Registration is cheap and the
        // observer performs no MediaStore query on its callback thread; actual reconciliation is
        // debounced and runs on the existing application IO scope.
        mediaStoreChangeObserver.register()

        // WorkManager setup and SAF roots are never part of the Activity's critical startup path.
        applicationScope.launch {
            scheduleLibraryRefresh()
        }
        // Startup synchronization is deliberately gated by the UI permission flow.
        // Room remains the immediate source for existing users; the UI coordinator starts
        // the first full scan only after all startup permission steps have been presented.
        applicationScope.launch {
            // SAF roots are supplemental and are intentionally warmed well after the first Home
            // render. They must never compete with the user's first navigation frame.
            if (preferences.hasCompletedInitialLibraryScan()) kotlinx.coroutines.delay(12_000L)
            preferences.safRoots.first().forEach { root ->
                runCatching { safMediaScanner.scanTree(android.net.Uri.parse(root), mediaLibrary) }
            }
        }
        applicationScope.launch {
            // The Home observer is already running from init; wait for its first Room snapshot
            // without putting any work on the Activity thread.
            if (preferences.hasCompletedInitialLibraryScan()) {
                runCatching { homeRepository.prewarm() }
            }
        }
        applicationScope.launch {
            // Genre backfill is upgrade maintenance, not first-frame work. Run it only for an
            // existing initialized library and only once per metadata schema version.
            if (preferences.hasCompletedInitialLibraryScan() && preferences.needsGenreMetadataBackfill()) {
                kotlinx.coroutines.delay(1_800L)
                runCatching { mediaLibrary.backfillMissingGenres() }
                    .onSuccess { preferences.markGenreMetadataBackfillDone() }
            }
        }
    }

    /**
     * Requests startup synchronization from an application-owned scope. Existing users receive
     * a normal delta refresh; only an uninitialized library receives the one-time full scan.
     */
    private suspend fun refreshLibraryAfterMediaStoreChange() {
        if (!MediaPermission.granted(appContext)) return
        if (!preferences.hasCompletedInitialLibraryScan()) return

        _syncState.value = LibrarySyncState.DELTA
        runCatching { mediaLibrary.deltaRefresh() }
            .onFailure { _syncState.value = LibrarySyncState.FAILED }
            .onSuccess {
                preferences.markMediaStoreRefresh(mediaStore.externalLibraryVersion())
                _syncState.value = LibrarySyncState.IDLE
            }
    }

    fun requestLibrarySynchronization(startupDelayMs: Long = 3_000L) {
        applicationScope.launch {
            if (startupDelayMs > 0L) kotlinx.coroutines.delay(startupDelayMs)
            if (!MediaPermission.granted(appContext)) return@launch
            if (!preferences.hasCompletedInitialLibraryScan()) {
                ensureInitialLibraryScan()
                runCatching { homeRepository.prewarm() }
            } else if (preferences.shouldRefreshMediaStore(mediaStore.externalLibraryVersion())) {
                // Freshness work is application-scoped and only runs when MediaStore can
                // actually have changed. If its version is unchanged, there is nothing to
                // reconcile and Room emits nothing to Home.
                _syncState.value = LibrarySyncState.DELTA
                runCatching { mediaLibrary.deltaRefresh() }
                    .onFailure { _syncState.value = LibrarySyncState.FAILED }
                    .onSuccess {
                        preferences.markMediaStoreRefresh(mediaStore.externalLibraryVersion())
                        _syncState.value = LibrarySyncState.IDLE
                    }
            }
        }
    }

    /**
     * Single-flight gate for the one-time initial MediaStore scan. The check is intentionally
     * repeated after acquiring the mutex so concurrent callers cannot both perform a full scan.
     */
    suspend fun ensureInitialLibraryScan() {
        initialScanMutex.withLock {
            if (!MediaPermission.granted(appContext)) return
            if (preferences.hasCompletedInitialLibraryScan()) return

            _syncState.value = LibrarySyncState.INITIAL_SCAN
            _syncProgress.value = LibrarySyncProgress(visible = true, count = 0, total = 0, label = "media files", complete = false)
            try {
                mediaLibrary.fullRescan { found, total, label ->
                    _syncProgress.value = LibrarySyncProgress(
                        visible = true,
                        count = found,
                        total = total,
                        label = label,
                        complete = false,
                    )
                }.also { result ->
                    val label = when {
                        result.audioCount > 0 && result.videoCount > 0 -> "songs and videos"
                        result.audioCount > 0 -> "songs"
                        result.videoCount > 0 -> "videos"
                        else -> "media files"
                    }
                    _syncProgress.value = LibrarySyncProgress(
                        visible = true,
                        count = result.audioCount + result.videoCount,
                        total = result.audioCount + result.videoCount,
                        label = label,
                        complete = true,
                    )
                }
                preferences.markInitialLibraryScanDone()
                preferences.markMediaStoreRefresh(mediaStore.externalLibraryVersion())
                _syncState.value = LibrarySyncState.IDLE
                kotlinx.coroutines.delay(650)
                _syncProgress.value = LibrarySyncProgress()
            } catch (cancellation: CancellationException) {
                _syncState.value = LibrarySyncState.FAILED
                _syncProgress.value = LibrarySyncProgress()
                throw cancellation
            } catch (error: Throwable) {
                _syncState.value = LibrarySyncState.FAILED
                _syncProgress.value = LibrarySyncProgress()
                throw error
            }
        }
    }

    /** Starts a user-requested SAF folder scan without blocking the Home UI. */
    fun startSafMediaScan(treeUri: android.net.Uri) {
        if (_safScanState.value.visible) return
        _safScanState.value = SafScanUiState(visible = true, phase = SafScanPhase.SCANNING)
        applicationScope.launch {
            try {
                val result = safMediaScanner.scanTreeDetailed(treeUri, mediaLibrary) { found ->
                    _safScanState.value = _safScanState.value.copy(found = found)
                }
                preferences.addSafRoot(treeUri.toString())
                _safScanState.value = SafScanUiState(
                    visible = true,
                    phase = SafScanPhase.COMPLETE,
                    found = result.total,
                    addedAudio = result.addedAudio,
                    updatedAudio = result.updatedAudio,
                    addedVideo = result.addedVideo,
                    updatedVideo = result.updatedVideo,
                )
                runCatching { homeRepository.prewarm() }
                kotlinx.coroutines.delay(3_200L)
                _safScanState.value = SafScanUiState()
            } catch (error: Throwable) {
                _safScanState.value = SafScanUiState(
                    visible = true,
                    phase = SafScanPhase.ERROR,
                    error = error.message ?: "The selected folder could not be scanned",
                )
                kotlinx.coroutines.delay(3_200L)
                _safScanState.value = SafScanUiState()
            }
        }
    }

    private fun scheduleLibraryRefresh() {
        val request = PeriodicWorkRequestBuilder<LibraryRefreshWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(6, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            "airplayr-library-refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}


data class LibrarySyncProgress(
    val visible: Boolean = false,
    val count: Int = 0,
    val total: Int = 0,
    val label: String = "media files",
    val complete: Boolean = false,
)

enum class LibrarySyncState { IDLE, INITIAL_SCAN, DELTA, FAILED }

enum class SafScanPhase { SCANNING, COMPLETE, ERROR }

data class SafScanUiState(
    val visible: Boolean = false,
    val phase: SafScanPhase = SafScanPhase.SCANNING,
    val found: Int = 0,
    val addedAudio: Int = 0,
    val updatedAudio: Int = 0,
    val addedVideo: Int = 0,
    val updatedVideo: Int = 0,
    val error: String? = null,
)
