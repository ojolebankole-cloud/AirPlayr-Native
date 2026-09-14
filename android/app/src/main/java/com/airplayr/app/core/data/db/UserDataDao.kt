package com.airplayr.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDataDao {
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC") fun observeFavorites(): Flow<List<FavoriteEntity>>
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE mediaId = :mediaId)") suspend fun isFavorite(mediaId: String): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addFavorite(item: FavoriteEntity)
    @Query("DELETE FROM favorites WHERE mediaId = :mediaId") suspend fun removeFavorite(mediaId: String)
    @Query("SELECT mediaId FROM favorites") suspend fun favoriteIds(): List<String>

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit") fun observeHistory(limit: Int): Flow<List<PlaybackHistoryEntity>>
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit") suspend fun history(limit: Int): List<PlaybackHistoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertHistory(item: PlaybackHistoryEntity)
    @Query("UPDATE playback_history SET completed = 1, completedAt = :at WHERE mediaId = :mediaId") suspend fun markCompleted(mediaId: String, at: Long)
    @Query("DELETE FROM playback_history") suspend fun clearHistory()

    @Query("SELECT * FROM music_playlists ORDER BY updatedAt DESC") fun observePlaylists(): Flow<List<MusicPlaylistEntity>>
    @Query("SELECT * FROM music_playlists WHERE id = :id") suspend fun playlist(id: String): MusicPlaylistEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPlaylist(item: MusicPlaylistEntity)
    @Query("DELETE FROM music_playlists WHERE id = :id") suspend fun deletePlaylist(id: String)
    @Query("DELETE FROM music_playlist_items WHERE playlistId = :id") suspend fun clearPlaylistItems(id: String)
    @Query("SELECT mediaId FROM music_playlist_items WHERE playlistId = :id ORDER BY position ASC") suspend fun playlistItemIds(id: String): List<String>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertPlaylistItems(items: List<MusicPlaylistItemEntity>)
    @Transaction suspend fun replacePlaylistItems(id: String, items: List<MusicPlaylistItemEntity>) {
        clearPlaylistItems(id)
        if (items.isNotEmpty()) insertPlaylistItems(items)
    }

    @Query("SELECT * FROM video_progress WHERE mediaId = :id") fun observeVideoProgress(id: String): Flow<VideoProgressEntity?>
    @Query("SELECT * FROM video_progress ORDER BY updatedAt DESC") fun observeVideoProgress(): Flow<List<VideoProgressEntity>>
    @Query("SELECT * FROM video_progress WHERE mediaId = :id") suspend fun videoProgress(id: String): VideoProgressEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertVideoProgress(item: VideoProgressEntity)
    @Query("DELETE FROM video_progress WHERE mediaId = :id") suspend fun clearVideoProgress(id: String)

    @Query("SELECT * FROM video_bookmarks WHERE mediaId = :id ORDER BY positionMs ASC") fun observeBookmarks(id: String): Flow<List<VideoBookmarkEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addBookmark(item: VideoBookmarkEntity)
    @Query("DELETE FROM video_bookmarks WHERE mediaId = :id AND positionMs = :positionMs") suspend fun removeBookmark(id: String, positionMs: Long)
}
