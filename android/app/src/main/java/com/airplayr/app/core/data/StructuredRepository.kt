package com.airplayr.app.core.data

import androidx.room.withTransaction
import com.airplayr.app.core.data.db.*
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class StructuredRepository(private val db: AirPlayrDatabase) {
    fun favorites(): Flow<List<FavoriteEntity>> = db.userDataDao().observeFavorites()
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

    fun history(limit: Int = 100): Flow<List<PlaybackHistoryEntity>> = db.userDataDao().observeHistory(limit.coerceIn(1, 500))
    suspend fun recordPlay(item: MediaItem) {
        val dao = db.userDataDao(); val now=System.currentTimeMillis(); val old=dao.history(500).firstOrNull { it.mediaId==item.id }
        dao.upsertHistory(PlaybackHistoryEntity(item.id,item.mediaType.value,item.title,item.artist,item.album,item.artworkUri,now,(old?.playCount ?: 0)+1,old?.completed ?: false,old?.completedAt))
    }
    suspend fun markCompleted(id: String) = db.userDataDao().markCompleted(id, System.currentTimeMillis())
    suspend fun clearHistory() = db.userDataDao().clearHistory()
    suspend fun recent(limit:Int=100): List<PlaybackHistoryEntity> = db.userDataDao().history(limit)

    fun playlists(): Flow<List<MusicPlaylistEntity>> = db.userDataDao().observePlaylists()
    suspend fun createPlaylist(name:String): MusicPlaylistEntity {
        val now=System.currentTimeMillis(); val p=MusicPlaylistEntity(UUID.randomUUID().toString(), name.trim().ifBlank { "Playlist" }, now, now, null); db.userDataDao().upsertPlaylist(p); return p
    }
    suspend fun addToPlaylist(playlistId:String, mediaId:String) {
        val dao=db.userDataDao(); val ids=dao.playlistItemIds(playlistId).toMutableList(); if(mediaId !in ids) ids.add(mediaId)
        dao.replacePlaylistItems(playlistId, ids.mapIndexed { i,id -> MusicPlaylistItemEntity(playlistId,id,i,System.currentTimeMillis()) })
        dao.playlist(playlistId)?.let { dao.upsertPlaylist(it.copy(updatedAt=System.currentTimeMillis())) }
    }
    suspend fun playlistIds(playlistId:String)=db.userDataDao().playlistItemIds(playlistId)

    fun videoProgress(id:String):Flow<VideoProgressEntity?> =db.userDataDao().observeVideoProgress(id)
    fun allVideoProgress():Flow<List<VideoProgressEntity>> =db.userDataDao().observeVideoProgress()
    suspend fun saveVideoProgress(id:String, positionMs:Long, durationMs:Long) {
        val watched=durationMs>0 && positionMs.toDouble()/durationMs>=0.92
        if(watched) db.userDataDao().clearVideoProgress(id) else if(positionMs>3000) db.userDataDao().upsertVideoProgress(VideoProgressEntity(id,positionMs,durationMs,false,System.currentTimeMillis()))
    }
    suspend fun clearVideoProgress(id:String)=db.userDataDao().clearVideoProgress(id)
    fun bookmarks(id:String):Flow<List<VideoBookmarkEntity>> =db.userDataDao().observeBookmarks(id)
    suspend fun addBookmark(id:String, positionMs:Long, label:String?){ db.userDataDao().addBookmark(VideoBookmarkEntity(id,positionMs,label?.trim()?.ifBlank { null },System.currentTimeMillis())) }
    suspend fun removeBookmark(id:String, positionMs:Long)=db.userDataDao().removeBookmark(id,positionMs)
}
