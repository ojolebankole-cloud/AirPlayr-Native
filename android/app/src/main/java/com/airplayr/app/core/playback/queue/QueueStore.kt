package com.airplayr.app.core.playback.queue

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.airplayr.app.core.data.media.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.airPlayrQueueDataStore by preferencesDataStore(name = "airplayr_queue")

private val QUEUE_ITEMS = stringPreferencesKey("queue_items")
private val CURRENT_INDEX = intPreferencesKey("current_index")
private val CURRENT_ID = stringPreferencesKey("current_id")
private val POSITION_MS = longPreferencesKey("position_ms")
private val SHUFFLE = booleanPreferencesKey("shuffle")
private val REPEAT_MODE = intPreferencesKey("repeat_mode")
private val PLAY_WHEN_READY = booleanPreferencesKey("play_when_ready")

/** Native persistence owner for playback selection state. Queue stores stable media IDs + URIs,
 * never Compose state or transient player objects. */
class QueueStore(private val context: Context) {
    private val writeMutex = Mutex()
    val state: Flow<PersistedQueueState> = context.airPlayrQueueDataStore.data.map { prefs ->
        PersistedQueueState(
            items = decodeItems(prefs[QUEUE_ITEMS].orEmpty()),
            currentIndex = prefs[CURRENT_INDEX] ?: 0,
            currentId = prefs[CURRENT_ID],
            positionMs = prefs[POSITION_MS] ?: 0L,
            shuffle = prefs[SHUFFLE] ?: false,
            repeatMode = prefs[REPEAT_MODE] ?: 0,
            playWhenReady = prefs[PLAY_WHEN_READY] ?: false,
        )
    }

    suspend fun read(): PersistedQueueState = state.first()

    suspend fun save(
        items: List<MediaItem>,
        currentIndex: Int,
        currentId: String?,
        positionMs: Long,
        shuffle: Boolean,
        repeatMode: Int,
        playWhenReady: Boolean,
    ) {
        writeMutex.withLock {
            val limited = items.take(MAX_QUEUE_ITEMS)
            context.airPlayrQueueDataStore.edit { prefs ->
            prefs[QUEUE_ITEMS] = encodeItems(limited)
            prefs[CURRENT_INDEX] = currentIndex.coerceIn(0, (limited.size - 1).coerceAtLeast(0))
            if (currentId.isNullOrBlank()) prefs.remove(CURRENT_ID) else prefs[CURRENT_ID] = currentId
            prefs[POSITION_MS] = positionMs.coerceAtLeast(0L)
            prefs[SHUFFLE] = shuffle
            prefs[REPEAT_MODE] = repeatMode
                prefs[PLAY_WHEN_READY] = playWhenReady
            }
        }
    }

    suspend fun clear() {
        writeMutex.withLock {
            context.airPlayrQueueDataStore.edit { prefs ->
                prefs.remove(QUEUE_ITEMS)
                prefs.remove(CURRENT_INDEX)
                prefs.remove(CURRENT_ID)
                prefs.remove(POSITION_MS)
                prefs.remove(SHUFFLE)
                prefs.remove(REPEAT_MODE)
                prefs.remove(PLAY_WHEN_READY)
            }
        }
    }

    private fun encodeItems(items: List<MediaItem>): String = items.joinToString("|") { item ->
        listOf(
            item.id,
            item.uri,
            item.title,
            item.artist.orEmpty(),
            item.album.orEmpty(),
            item.mediaType.value,
            item.durationMs.toString(),
        ).joinToString("~") { encode(it) }
    }

    private fun decodeItems(raw: String): List<MediaItem> = raw
        .takeIf { it.isNotBlank() }
        ?.split('|')
        ?.mapNotNull { row ->
            val values = row.split('~').map(::decode)
            if (values.size != 7 || values[0].isBlank() || values[1].isBlank()) return@mapNotNull null
            val type = if (values[5] == "video") com.airplayr.app.core.data.media.MediaType.VIDEO else com.airplayr.app.core.data.media.MediaType.AUDIO
            MediaItem(
                id = values[0],
                mediaStoreId = null,
                uri = values[1],
                title = values[2].ifBlank { "Media" },
                filename = values[2].ifBlank { "Media" },
                artist = values[3].ifBlank { null },
                album = values[4].ifBlank { null },
                albumArtist = null,
                durationMs = values[6].toLongOrNull() ?: 0L,
                width = 0,
                height = 0,
                sizeBytes = 0L,
                dateModified = 0L,
                dateAdded = 0L,
                mediaType = type,
                mimeType = null,
                albumId = null,
                artworkUri = null,
                thumbnailUri = null,
                genre = null,
                sourceDirectoryUri = null,
                folderId = null,
                folder = null,
                path = null,
                relativePath = null,
            )
        }
        ?.take(MAX_QUEUE_ITEMS)
        ?: emptyList()

    private fun encode(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP or Base64.URL_SAFE)
    private fun decode(value: String): String = runCatching { String(Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE), Charsets.UTF_8) }.getOrDefault("")

    companion object { const val MAX_QUEUE_ITEMS = 10_000 }
}

data class PersistedQueueState(
    val items: List<MediaItem>,
    val currentIndex: Int,
    val currentId: String?,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
    val playWhenReady: Boolean,
)
