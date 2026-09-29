package com.airplayr.app.core.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreDataSource(context: Context) {
    private val appContext = context.applicationContext
    private val resolver: ContentResolver = appContext.contentResolver

    /**
     * Returns an opaque fingerprint of each mounted external MediaStore volume. On API 30+ it
     * combines the documented collection version with the per-volume generation counter. The
     * version is the guard for generation resets; generation is the fine-grained signal for media
     * additions/changes that getVersion() intentionally does not guarantee to expose.
     */
    fun externalLibraryFingerprint(): String? = runCatching {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@runCatching null
        val volumes = MediaStore.getExternalVolumeNames(appContext).toList().sorted()
        if (volumes.isEmpty()) return@runCatching null
        val root = JSONObject()
        volumes.forEach { volume ->
            val entry = JSONObject()
            entry.put("version", MediaStore.getVersion(appContext, volume))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                entry.put("generation", MediaStore.getGeneration(appContext, volume))
            }
            root.put(volume, entry)
        }
        root.toString()
    }.getOrNull()


    suspend fun query(type: MediaType, maxItems: Int = MAX_LIBRARY_ITEMS): MediaStoreRead = withContext(Dispatchers.IO) {
        val projection = when (type) {
            MediaType.AUDIO -> audioProjection()
            MediaType.VIDEO -> videoProjection()
        }
        val collection = when (type) {
            MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            MediaType.VIDEO -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val result = ArrayList<MediaItem>(minOf(maxItems, 1024))

        val cursor = try {
            resolver.query(
                collection,
                projection,
                null,
                null,
                null,
            )
        } catch (_: SecurityException) {
            return@withContext MediaStoreRead(accessible = false, items = emptyList())
        }

        if (cursor == null) {
            // A null ContentResolver query is not an empty library; it means the source
            // could not be read reliably. Never interpret that as permission to delete rows.
            return@withContext MediaStoreRead(accessible = false, items = emptyList())
        }

        cursor.use { c ->
            val columns = CursorColumns(c)
            while (c.moveToNext() && result.size < maxItems) {
                val item = when (type) {
                    MediaType.AUDIO -> readAudio(c, columns)
                    MediaType.VIDEO -> readVideo(c, columns)
                }
                if (item.mediaStoreId != null && item.mediaStoreId > 0L) {
                    result += item
                }
            }
        }
        MediaStoreRead(accessible = true, items = result)
    }

    private fun readAudio(c: android.database.Cursor, columns: CursorColumns): MediaItem {
        val storeId = c.getLongOrNull(columns.id) ?: 0L
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, storeId).toString()
        val filename = c.getStringOrNull(columns.displayName) ?: "Audio"
        val title = c.getStringOrNull(columns.title)?.takeUnless { it.isBlank() } ?: filename.substringBeforeLast('.')
        val albumId = c.getLongOrNull(columns.albumId)
        return MediaItem(
            id = "audio:mediastore:$storeId",
            mediaStoreId = storeId,
            uri = uri,
            title = title.ifBlank { "Audio" },
            filename = filename,
            artist = c.getStringOrNull(columns.artist),
            album = c.getStringOrNull(columns.album),
            albumArtist = c.getStringOrNull(columns.albumArtist),
            durationMs = c.getLongOrNull(columns.duration) ?: 0L,
            width = 0,
            height = 0,
            sizeBytes = c.getLongOrNull(columns.size) ?: 0L,
            dateModified = c.getLongOrNull(columns.dateModified) ?: 0L,
            dateAdded = c.getLongOrNull(columns.dateAdded) ?: 0L,
            mediaType = MediaType.AUDIO,
            mimeType = c.getStringOrNull(columns.mimeType),
            albumId = albumId,
            artworkUri = albumId?.let { ContentUris.withAppendedId(ALBUM_ART_URI, it).toString() },
            thumbnailUri = null,
            genre = c.getStringOrNull(columns.genre)?.trim()?.takeIf { it.isNotEmpty() },
            sourceDirectoryUri = null,
            folderId = null,
            folder = null,
            path = c.getStringOrNull(columns.relativePath),
            relativePath = c.getStringOrNull(columns.relativePath),
        )
    }

    private fun readVideo(c: android.database.Cursor, columns: CursorColumns): MediaItem {
        val storeId = c.getLongOrNull(columns.id) ?: 0L
        val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, storeId).toString()
        val filename = c.getStringOrNull(columns.displayName) ?: "Video"
        val title = c.getStringOrNull(columns.title)?.takeUnless { it.isBlank() } ?: filename.substringBeforeLast('.')
        return MediaItem(
            id = "video:mediastore:$storeId",
            mediaStoreId = storeId,
            uri = uri,
            title = title.ifBlank { "Video" },
            filename = filename,
            artist = c.getStringOrNull(columns.artist),
            album = c.getStringOrNull(columns.album),
            albumArtist = null,
            durationMs = c.getLongOrNull(columns.duration) ?: 0L,
            width = c.getIntOrNull(columns.width) ?: 0,
            height = c.getIntOrNull(columns.height) ?: 0,
            sizeBytes = c.getLongOrNull(columns.size) ?: 0L,
            dateModified = c.getLongOrNull(columns.dateModified) ?: 0L,
            dateAdded = c.getLongOrNull(columns.dateAdded) ?: 0L,
            mediaType = MediaType.VIDEO,
            mimeType = c.getStringOrNull(columns.mimeType),
            albumId = null,
            artworkUri = uri,
            thumbnailUri = uri,
            genre = c.getStringOrNull(columns.genre)?.trim()?.takeIf { it.isNotEmpty() },
            sourceDirectoryUri = null,
            folderId = null,
            folder = null,
            path = c.getStringOrNull(columns.relativePath),
            relativePath = c.getStringOrNull(columns.relativePath),
        )
    }

    private data class CursorColumns(
        val id: Int,
        val displayName: Int,
        val title: Int,
        val artist: Int,
        val album: Int,
        val albumArtist: Int,
        val duration: Int,
        val size: Int,
        val dateModified: Int,
        val dateAdded: Int,
        val mimeType: Int,
        val relativePath: Int,
        val albumId: Int,
        val width: Int,
        val height: Int,
        val genre: Int,
    ) {
        constructor(c: android.database.Cursor) : this(
            id = c.getColumnIndex(MediaStore.MediaColumns._ID),
            displayName = c.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME),
            title = c.getColumnIndex(MediaStore.MediaColumns.TITLE),
            artist = c.getColumnIndex(MediaStore.Audio.AudioColumns.ARTIST),
            album = c.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM),
            albumArtist = c.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ARTIST),
            duration = c.getColumnIndex(MediaStore.MediaColumns.DURATION),
            size = c.getColumnIndex(MediaStore.MediaColumns.SIZE),
            dateModified = c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED),
            dateAdded = c.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED),
            mimeType = c.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE),
            relativePath = c.getColumnIndex("relative_path"),
            albumId = c.getColumnIndex(MediaStore.Audio.AudioColumns.ALBUM_ID),
            width = c.getColumnIndex(MediaStore.Video.VideoColumns.WIDTH),
            height = c.getColumnIndex(MediaStore.Video.VideoColumns.HEIGHT),
            genre = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                c.getColumnIndex(MediaStore.MediaColumns.GENRE)
            } else {
                -1
            },
        )
    }

    private companion object {
        const val MAX_LIBRARY_ITEMS = Int.MAX_VALUE
        val ALBUM_ART_URI = android.net.Uri.parse("content://media/external/audio/albumart")

        fun audioProjection(): Array<String> = baseAudioProjection() +
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) arrayOf("relative_path") else emptyArray()) +
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) arrayOf(MediaStore.MediaColumns.GENRE) else emptyArray()) +
            arrayOf(MediaStore.Audio.AudioColumns.ALBUM_ID)

        fun videoProjection(): Array<String> = baseVideoProjection() +
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) arrayOf("relative_path") else emptyArray()) +
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) arrayOf(MediaStore.MediaColumns.GENRE) else emptyArray()) +
            arrayOf(MediaStore.Video.VideoColumns.WIDTH, MediaStore.Video.VideoColumns.HEIGHT)

        fun baseAudioProjection(): Array<String> = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE,
            MediaStore.Audio.AudioColumns.ARTIST, MediaStore.Audio.AudioColumns.ALBUM, MediaStore.Audio.AudioColumns.ALBUM_ARTIST,
            MediaStore.MediaColumns.DURATION, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.MIME_TYPE,
        )

        fun baseVideoProjection(): Array<String> = arrayOf(
            MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.TITLE,
            MediaStore.Video.VideoColumns.ARTIST, MediaStore.Video.VideoColumns.ALBUM, MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.MIME_TYPE,
        )
    }
}

data class MediaStoreRead(val accessible: Boolean, val items: List<MediaItem>)

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (index >= 0 && !isNull(index)) getString(index) else null

private fun android.database.Cursor.getLongOrNull(index: Int): Long? =
    if (index >= 0 && !isNull(index)) getLong(index) else null

private fun android.database.Cursor.getIntOrNull(index: Int): Int? =
    if (index >= 0 && !isNull(index)) getInt(index) else null
