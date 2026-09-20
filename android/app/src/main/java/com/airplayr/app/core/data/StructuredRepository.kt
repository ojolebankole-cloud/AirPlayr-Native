package com.airplayr.app.core.data

import androidx.room.withTransaction
import com.airplayr.app.core.data.db.*
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class StructuredRepository(private val databaseProvider: () -> AirPlayrDatabase) {
    private val db: AirPlayrDatabase get() = databaseProvider()

    fun favorites(): Flow<List<FavoriteEntity>> = flow { emitAll(db.userDataDao().observeFavorites()) }.flowOn(Dispatchers.IO)
    suspend fun isFavorite(id: String) = db.userDataDao().isFavorite(id)
    suspend fun toggleFavorite(item: MediaItem): Boolean {
        val dao = db.userDataDao()
        return db.withTransaction {
            if (dao.isFavorite(item.id)) {
                dao.removeFavorite(item.id)
                false
            } else {
                dao.addFavorite(FavoriteEntity(item.id, item.mediaType.value, System.currentTimeMillis()))
                true
            }
        }
    }
    suspend fun favoriteIds(): Set<String> = db.userDataDao().favoriteIds().toSet()

    fun history(limit: Int = 100): Flow<List<PlaybackHistoryEntity>> = flow { emitAll(db.userDataDao().observeHistory(limit.coerceIn(1, 500))) }.flowOn(Dispatchers.IO)
    fun repeated(limit: Int = 20): Flow<List<PlaybackHistoryEntity>> = flow { emitAll(db.userDataDao().observeRepeated(limit.coerceIn(1, 100))) }.flowOn(Dispatchers.IO)
    suspend fun recordPlay(item: MediaItem) {
        val dao = db.userDataDao()
        val now = System.currentTimeMillis()
        val old = dao.historyById(item.id)
        if (old != null && now - old.playedAt < 5_000L) return
        // Match the Expo history contract: a fresh play resets completion, so a
        // replay can become a new completion and contribute to completed-video stats.
        dao.upsertHistory(
            PlaybackHistoryEntity(
                item.id,
                item.mediaType.value,
                item.title,
                item.artist,
                item.album,
                item.artworkUri,
                now,
                (old?.playCount ?: 0) + 1,
                false,
                null,
            )
        )
    }

    suspend fun markCompleted(id: String) {
        val dao = db.userDataDao()
        val now = System.currentTimeMillis()
        val old = dao.historyById(id)
        if (old == null || old.completed) return
        dao.markCompleted(id, now)
        dao.insertPlaybackEvent(PlaybackEventEntity(mediaId = id, mediaType = VIDEO_COMPLETION_EVENT, playedAt = now))
    }
    suspend fun clearHistory() = db.userDataDao().clearHistory()
    suspend fun recent(limit:Int=100): List<PlaybackHistoryEntity> = db.userDataDao().history(limit)

    suspend fun totalPlaybackSeconds(type: MediaType): Long =
        db.userDataDao().totalPlaybackSeconds(type.value)

    suspend fun completedVideoCount(): Int =
        db.userDataDao().completedHistoryCount(MediaType.VIDEO.value)

    fun playlists(): Flow<List<MusicPlaylistEntity>> = flow { emitAll(db.userDataDao().observePlaylists()) }.flowOn(Dispatchers.IO)
    suspend fun createPlaylist(name:String): MusicPlaylistEntity {
        val now=System.currentTimeMillis(); val p=MusicPlaylistEntity(UUID.randomUUID().toString(), name.trim().ifBlank { "Playlist" }, now, now, null); db.userDataDao().upsertPlaylist(p); return p
    }
    suspend fun addToPlaylist(playlistId:String, mediaId:String) {
        val dao=db.userDataDao(); val ids=dao.playlistItemIds(playlistId).toMutableList(); if(mediaId !in ids) ids.add(mediaId)
        dao.replacePlaylistItems(playlistId, ids.mapIndexed { i,id -> MusicPlaylistItemEntity(playlistId,id,i,System.currentTimeMillis()) })
        dao.playlist(playlistId)?.let { dao.upsertPlaylist(it.copy(updatedAt=System.currentTimeMillis())) }
    }
    suspend fun playlistIds(playlistId:String)=db.userDataDao().playlistItemIds(playlistId)
    suspend fun renamePlaylist(playlistId:String, name:String) { db.userDataDao().playlist(playlistId)?.let { db.userDataDao().upsertPlaylist(it.copy(name=name.trim().ifBlank { it.name }, updatedAt=System.currentTimeMillis())) } }
    suspend fun setPlaylistArtwork(playlistId:String, uri:String?) { db.userDataDao().playlist(playlistId)?.let { db.userDataDao().upsertPlaylist(it.copy(artworkUri=uri, updatedAt=System.currentTimeMillis())) } }
    suspend fun deletePlaylist(playlistId:String) { db.userDataDao().clearPlaylistItems(playlistId); db.userDataDao().deletePlaylist(playlistId) }

    fun videoProgress(id:String):Flow<VideoProgressEntity?> = flow { emitAll(db.userDataDao().observeVideoProgress(id)) }.flowOn(Dispatchers.IO)
    fun allVideoProgress():Flow<List<VideoProgressEntity>> = flow { emitAll(db.userDataDao().observeVideoProgress()) }.flowOn(Dispatchers.IO)
    fun recentVideoProgress(limit: Int = 30):Flow<List<VideoProgressEntity>> = flow { emitAll(db.userDataDao().observeRecentVideoProgress(limit.coerceIn(1, 100))) }.flowOn(Dispatchers.IO)
    suspend fun saveVideoProgress(id:String, positionMs:Long, durationMs:Long) {
        val watched=durationMs>0 && positionMs.toDouble()/durationMs>=0.92
        if(watched) db.userDataDao().clearVideoProgress(id) else if(positionMs>3000) db.userDataDao().upsertVideoProgress(VideoProgressEntity(id,positionMs,durationMs,false,System.currentTimeMillis()))
    }
    suspend fun clearVideoProgress(id:String)=db.userDataDao().clearVideoProgress(id)
    fun bookmarks(id:String):Flow<List<VideoBookmarkEntity>> = flow { emitAll(db.userDataDao().observeBookmarks(id)) }.flowOn(Dispatchers.IO)
    suspend fun addBookmark(id:String, positionMs:Long, label:String?){ db.userDataDao().addBookmark(VideoBookmarkEntity(id,positionMs,label?.trim()?.ifBlank { null },System.currentTimeMillis())) }
    suspend fun removeBookmark(id:String, positionMs:Long)=db.userDataDao().removeBookmark(id,positionMs)
}

const val VIDEO_COMPLETION_EVENT = "video_completion"
