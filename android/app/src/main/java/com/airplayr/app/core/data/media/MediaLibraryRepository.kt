package com.airplayr.app.core.data.media

import androidx.room.withTransaction
import com.airplayr.app.core.data.db.AirPlayrDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaLibraryRepository(
    private val database: AirPlayrDatabase,
    private val mediaStore: MediaStoreDataSource,
    private val metadata: com.airplayr.app.core.metadata.MetadataRepository,
) {
    private val refreshMutex = Mutex()

    fun observe(type: MediaType): Flow<List<MediaItem>> =
        database.mediaItemDao().observeByType(type.value)
            .map { list -> list.map(MediaItemEntity::asExternal) }

    suspend fun page(type: MediaType, limit: Int = 250, offset: Int = 0): List<MediaItem> =
        database.mediaItemDao().getPage(type.value, limit.coerceIn(1, 250), offset.coerceAtLeast(0))
            .map(MediaItemEntity::asExternal)

    suspend fun count(type: MediaType): Int = database.mediaItemDao().count(type.value)

    suspend fun get(id: String): MediaItem? = database.mediaItemDao().getById(id)?.asExternal()?.let { metadata.overlay(it) }

    suspend fun refreshAll(): RefreshResult = refreshMutex.withLock {
        val audio = mediaStore.query(MediaType.AUDIO)
        val video = mediaStore.query(MediaType.VIDEO)
        database.withTransaction {
            if (audio.accessible) {
                database.mediaItemDao().replaceMediaStoreType(MediaType.AUDIO.value, audio.items.map(MediaItem::asEntity))
            }
            if (video.accessible) {
                database.mediaItemDao().replaceMediaStoreType(MediaType.VIDEO.value, video.items.map(MediaItem::asEntity))
            }
        }
        RefreshResult(
            audioCount = if (audio.accessible) audio.items.size else database.mediaItemDao().count(MediaType.AUDIO.value),
            videoCount = if (video.accessible) video.items.size else database.mediaItemDao().count(MediaType.VIDEO.value),
        )
    }

    suspend fun refresh(type: MediaType): Int = refreshMutex.withLock {
        val result = mediaStore.query(type)
        if (result.accessible) {
            database.mediaItemDao().replaceMediaStoreType(type.value, result.items.map(MediaItem::asEntity))
            result.items.size
        } else {
            database.mediaItemDao().count(type.value)
        }
    }

    suspend fun clearSupplementalRoot(type: MediaType, rootUri: String) {
        database.mediaItemDao().deleteSupplementalRoot(type.value, rootUri)
    }

    suspend fun insertSupplemental(type: MediaType, items: List<MediaItem>) {
        val filtered = items.filter { it.mediaType == type }.take(MAX_LIBRARY_ITEMS)
        if (filtered.isNotEmpty()) database.mediaItemDao().insertAll(filtered.map(MediaItem::asEntity))
    }

    suspend fun clearAll() = database.mediaItemDao().clearAll()

    companion object {
        const val PAGE_SIZE = 250
        const val MAX_LIBRARY_ITEMS = Int.MAX_VALUE
    }
}

data class RefreshResult(val audioCount: Int, val videoCount: Int)
