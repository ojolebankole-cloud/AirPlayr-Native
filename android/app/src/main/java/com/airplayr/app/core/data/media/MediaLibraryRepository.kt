package com.airplayr.app.core.data.media

import androidx.room.withTransaction
import com.airplayr.app.core.data.db.AirPlayrDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MediaLibraryRepository(
    private val databaseProvider: () -> AirPlayrDatabase,
    private val mediaStore: MediaStoreDataSource,
    private val metadataProvider: () -> com.airplayr.app.core.metadata.MetadataRepository,
) {
    private val refreshMutex = Mutex()
    private val database: AirPlayrDatabase get() = databaseProvider()
    private val metadata: com.airplayr.app.core.metadata.MetadataRepository get() = metadataProvider()

    fun observe(type: MediaType): Flow<List<MediaItem>> = flow {
        emitAll(database.mediaItemDao().observeByType(type.value))
    }.map { list -> list.map(MediaItemEntity::asExternal) }
        .flowOn(kotlinx.coroutines.Dispatchers.IO)

    fun observeRecent(type: MediaType, limit: Int = 30): Flow<List<MediaItem>> = flow {
        emitAll(database.mediaItemDao().observeRecentByType(type.value, limit.coerceIn(1, 100)))
    }.map { list -> list.map(MediaItemEntity::asExternal) }
        .flowOn(kotlinx.coroutines.Dispatchers.IO)

    suspend fun page(type: MediaType, limit: Int = 250, offset: Int = 0): List<MediaItem> =
        database.mediaItemDao().getPage(type.value, limit.coerceIn(1, 250), offset.coerceAtLeast(0))
            .map(MediaItemEntity::asExternal)

    suspend fun all(type: MediaType): List<MediaItem> =
        database.mediaItemDao().getPage(type.value, Int.MAX_VALUE, 0).map(MediaItemEntity::asExternal)

    fun observeFolderCount(): Flow<Int> = flow { emitAll(database.mediaItemDao().observeFolderCount()) }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    suspend fun count(type: MediaType): Int = database.mediaItemDao().count(type.value)

    fun observeCount(type: MediaType): Flow<Int> = flow { emitAll(database.mediaItemDao().observeCount(type.value)) }.flowOn(kotlinx.coroutines.Dispatchers.IO)

    suspend fun get(id: String): MediaItem? = database.mediaItemDao().getById(id)?.asExternal()?.let { metadata.overlay(it) }

    suspend fun getByIds(ids: Set<String>): List<MediaItem> =
        if (ids.isEmpty()) emptyList() else database.mediaItemDao().getByIds(ids.toList()).map(MediaItemEntity::asExternal)

    /**
     * Deliberately rebuilds the MediaStore-owned portion of the library.
     *
     * This is intentionally rare: first-time initialization and explicit/manual recovery.
     * Normal launches, workers, screens and ViewModels must use [deltaRefresh] instead.
     */
    suspend fun fullRescan(onProgress: ((found: Int, total: Int, label: String) -> Unit)? = null): RefreshResult = refreshMutex.withLock {
        val (audio, video) = coroutineScope {
            val audioDeferred = async { mediaStore.query(MediaType.AUDIO) }
            val videoDeferred = async { mediaStore.query(MediaType.VIDEO) }
            audioDeferred.await() to videoDeferred.await()
        }
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
        val (audio, video) = coroutineScope {
            val audioDeferred = async { mediaStore.query(MediaType.AUDIO) }
            val videoDeferred = async { mediaStore.query(MediaType.VIDEO) }
            audioDeferred.await() to videoDeferred.await()
        }
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
        database.withTransaction {
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

    /**
     * Backfills MediaStore-provided genre metadata for existing Room rows without replacing a
     * genre that is already stored. This is intended as a one-time post-update task because older
     * builds stored null genres even though modern MediaStore exposes indexed genre metadata.
     */
    suspend fun backfillMissingGenres(): Int = refreshMutex.withLock {
        val audio = mediaStore.query(MediaType.AUDIO)
        if (!audio.accessible) throw IllegalStateException("MediaStore audio collection is not accessible")
        val video = mediaStore.query(MediaType.VIDEO)
        val incoming = (audio.items + video.items)
            .filter { !it.genre.isNullOrBlank() }
            .associateBy { it.id }
        if (incoming.isEmpty()) return@withLock 0

        database.withTransaction {
            val dao = database.mediaItemDao()
            // Avoid a giant SQLite IN (...) query here. Large libraries can contain far more
            // than the platform's bind-argument limit, so read the two typed MediaStore sets
            // that the repository already owns and match them in memory.
            val existing = (
                dao.getMediaStoreByType(MediaType.AUDIO.value) +
                    dao.getMediaStoreByType(MediaType.VIDEO.value)
                ).associateBy { it.id }
            val changed = incoming.values.mapNotNull { item ->
                val old = existing[item.id] ?: return@mapNotNull null
                if (!old.genre.isNullOrBlank() || item.genre.isNullOrBlank()) null
                else old.copy(genre = item.genre)
            }
            if (changed.isNotEmpty()) dao.insertAll(changed)
            changed.size
        }
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
