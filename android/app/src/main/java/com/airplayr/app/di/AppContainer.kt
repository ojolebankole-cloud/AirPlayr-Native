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
import com.airplayr.app.core.data.media.SafMediaScanner
import com.airplayr.app.core.data.preferences.PreferencesRepository
import com.airplayr.app.core.work.LibraryRefreshWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.airplayr.app.core.video.NativeVideoController
import com.airplayr.app.core.metadata.MetadataRepository

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    val database: AirPlayrDatabase = Room.databaseBuilder(
        appContext,
        AirPlayrDatabase::class.java,
        "airplayr.db",
    ).addMigrations(migration1To2).build()

    val preferences = PreferencesRepository(appContext)
    val mediaStore = MediaStoreDataSource(appContext)
    val mediaLibrary = MediaLibraryRepository(database, mediaStore)
    val safMediaScanner = SafMediaScanner(appContext)
    val structured = com.airplayr.app.core.data.StructuredRepository(database)
    val search = com.airplayr.app.core.data.search.SearchRepository(database)
    val collections = com.airplayr.app.core.intelligence.CollectionsRepository(mediaLibrary, structured)
    val dsp = com.airplayr.app.core.playback.dsp.DspRepository(appContext)
    val videoController = NativeVideoController(appContext)
    val metadata = MetadataRepository(appContext)

    init {
        scheduleLibraryRefresh()
        // Cache-first startup: Room remains the immediate source for UI, while this refresh
        // reconciles MediaStore asynchronously and never blocks Activity creation.
        applicationScope.launch {
            if (MediaPermission.granted(appContext) && preferences.settings.first().autoRefreshLibrary) {
                runCatching { mediaLibrary.refreshAll() }
            }
            preferences.safRoots.first().forEach { root ->
                runCatching { safMediaScanner.scanTree(android.net.Uri.parse(root), mediaLibrary) }
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
