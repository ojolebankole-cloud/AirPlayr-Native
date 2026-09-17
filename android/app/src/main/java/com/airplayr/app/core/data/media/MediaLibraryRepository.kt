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

    /**
     * Deliberately rebuilds the MediaStore-owned portion of the library.
     *
     * This is intentionally rare: first-time initialization and explicit/manual recovery.
     * Normal launches, workers, screens and ViewModels must use [deltaRefresh] instead.
     */
    suspend fun fullRescan(onProgress: ((found: Int, total: Int, label: String) -> Unit)? = null): RefreshResult = refreshMutex.withLock {
        val audio = mediaStore.query(MediaType.AUDIO)
        val video = mediaStore.query(MediaType.VIDEO)
        var accessibleCount = 0
        val total = (if (audio.accessible) audio.items.size else 0) + (if (video.accessible) video.items.size else 0)
        var found = 0
        val label = when {
            audio.accessible && audio.items.isNotEmpty() && video.accessible && video.items.isNotEmpty() -> "songs and videos"
            audio.accessible && audio.items.isNotEmpty() -> "songs"
            video.accessible && video.items.isNotEmpty() -> "videos"
            else -> "media files"
        }

        if (audio.accessible) {
            fullSyncMediaStoreType(MediaType.AUDIO, audio.items) { committed ->
                found += committed
                onProgress?.invoke(found, total, label)
            }
            accessibleCount++
        }
        if (video.accessible) {
            fullSyncMediaStoreType(MediaType.VIDEO, video.items) { committed ->
                found += committed
                onProgress?.invoke(found, total, label)
            }
            accessibleCount++
        }

        if (accessibleCount == 0) {
            throw IllegalStateException("MediaStore is not currently accessible")
        }

        RefreshResult(
            audioCount = if (audio.accessible) audio.items.size else database.mediaItemDao().count(MediaType.AUDIO.value),
            videoCount = if (video.accessible) video.items.size else database.mediaItemDao().count(MediaType.VIDEO.value),
        )
    }

    /**
     * Reconciles MediaStore-owned Room rows with the current MediaStore snapshot.
     * SAF/imported rows (sourceDirectoryUri != null) are never touched.
     */
    suspend fun deltaRefresh(): RefreshResult = refreshMutex.withLock {
        val audio = mediaStore.query(MediaType.AUDIO)
        val video = mediaStore.query(MediaType.VIDEO)
        var accessibleCount = 0
        var totalAdded = 0
        var totalUpdated = 0
        var totalDeleted = 0

        database.withTransaction {
            if (audio.accessible) {
                val result = reconcileMediaStoreType(MediaType.AUDIO, audio.items)
                accessibleCount++
                totalAdded += result.added
                totalUpdated += result.updated
                totalDeleted += result.deleted
            }
            if (video.accessible) {
                val result = reconcileMediaStoreType(MediaType.VIDEO, video.items)
                accessibleCount++
                totalAdded += result.added
                totalUpdated += result.updated
                totalDeleted += result.deleted
            }
        }

        if (accessibleCount == 0) {
            throw IllegalStateException("MediaStore is not currently accessible")
        }

        RefreshResult(
            audioCount = database.mediaItemDao().count(MediaType.AUDIO.value),
            videoCount = database.mediaItemDao().count(MediaType.VIDEO.value),
            added = totalAdded,
            updated = totalUpdated,
            deleted = totalDeleted,
        )
    }

    /** Reconciles one MediaStore collection; useful after a targeted metadata/file change. */
    suspend fun deltaRefresh(type: MediaType): Int = refreshMutex.withLock {
        val result = mediaStore.query(type)
        if (!result.accessible) throw IllegalStateException("MediaStore ${type.value} collection is not accessible")
        reconcileMediaStoreType(type, result.items)
        database.mediaItemDao().count(type.value)
    }


    private suspend fun fullSyncMediaStoreType(type: MediaType, items: List<MediaItem>, onBatchCommitted: ((Int) -> Unit)? = null) {
        val dao = database.mediaItemDao()
        val existing = dao.getMediaStoreByType(type.value).associateBy { it.id }
        val incoming = items.map(MediaItem::asEntity).associateBy { it.id }

        incoming.values.chunked(FULL_SCAN_BATCH_SIZE).forEach { batch ->
            if (batch.isNotEmpty()) {
                dao.insertAll(batch)
                onBatchCommitted?.invoke(batch.size)
            }
        }

        val removedIds = existing.keys - incoming.keys
        if (removedIds.isNotEmpty()) dao.deleteByIds(removedIds.toList())
    }

    private suspend fun reconcileMediaStoreType(type: MediaType, items: List<MediaItem>): DeltaResult {
        val dao = database.mediaItemDao()
        val existing = dao.getMediaStoreByType(type.value).associateBy { it.id }
        val incoming = items.map(MediaItem::asEntity).associateBy { it.id }

        val changed = incoming.values.filter { existing[it.id] != it }
        if (changed.isNotEmpty()) dao.insertAll(changed)

        val removedIds = existing.keys - incoming.keys
        if (removedIds.isNotEmpty()) dao.deleteByIds(removedIds.toList())

        val added = incoming.keys.count { it !in existing }
        val updated = changed.count { it.id in existing }
        return DeltaResult(added = added, updated = updated, deleted = removedIds.size)
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
        const val FULL_SCAN_BATCH_SIZE = 250
    }
}

data class RefreshResult(
    val audioCount: Int,
    val videoCount: Int,
    val added: Int = 0,
    val updated: Int = 0,
    val deleted: Int = 0,
)

private data class DeltaResult(
    val added: Int,
    val updated: Int,
    val deleted: Int,
)
