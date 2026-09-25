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
    @Query("DELETE FROM playback_history WHERE mediaId = :mediaId") suspend fun removeHistory(mediaId: String)
    @Query("DELETE FROM music_playlist_items WHERE mediaId = :mediaId") suspend fun removePlaylistReferences(mediaId: String)
    @Query("DELETE FROM video_progress WHERE mediaId = :mediaId") suspend fun removeVideoProgress(mediaId: String)
    @Query("DELETE FROM video_bookmarks WHERE mediaId = :mediaId") suspend fun removeVideoBookmarks(mediaId: String)
    @Query("DELETE FROM playback_events WHERE mediaId = :mediaId") suspend fun removePlaybackEvents(mediaId: String)
    @Query("DELETE FROM playback_sessions WHERE mediaId = :mediaId") suspend fun removePlaybackSessions(mediaId: String)
    @Query("SELECT mediaId FROM favorites") suspend fun favoriteIds(): List<String>

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit") fun observeHistory(limit: Int): Flow<List<PlaybackHistoryEntity>>
    @Query("SELECT * FROM playback_history WHERE playCount >= 2 ORDER BY playCount DESC, playedAt DESC LIMIT :limit") fun observeRepeated(limit: Int): Flow<List<PlaybackHistoryEntity>>
    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit") suspend fun history(limit: Int): List<PlaybackHistoryEntity>
    @Query("SELECT * FROM playback_history WHERE mediaId = :mediaId LIMIT 1") suspend fun historyById(mediaId: String): PlaybackHistoryEntity?
    @Query("SELECT * FROM media_items WHERE id IN (:ids)") suspend fun mediaItemsByIds(ids: List<String>): List<com.airplayr.app.core.data.media.MediaItemEntity>
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
    @Transaction suspend fun replacePlaylistItems(id: String, items: List<MusicPlaylistItemEntity>) { clearPlaylistItems(id); if (items.isNotEmpty()) insertPlaylistItems(items) }

    @Insert suspend fun insertPlaybackEvent(event: PlaybackEventEntity)
    @Query("SELECT * FROM playback_events WHERE playedAt >= :since AND playedAt <= :until ORDER BY playedAt DESC") suspend fun playbackEvents(since: Long, until: Long): List<PlaybackEventEntity>
    @Query("SELECT * FROM playback_events WHERE mediaType = :mediaType ORDER BY playedAt ASC") suspend fun playbackEventsByType(mediaType: String): List<PlaybackEventEntity>
    @Query("SELECT COUNT(*) FROM playback_events WHERE mediaType = :mediaType AND mediaId = :mediaId") suspend fun playbackEventCount(mediaType: String, mediaId: String): Int
    @Query("SELECT * FROM playback_events ORDER BY playedAt DESC LIMIT :limit") suspend fun playbackEvents(limit: Int): List<PlaybackEventEntity>
    data class PlaybackMediaAggregate(val mediaId: String, val plays: Int, val title: String)

    @Query("SELECT COUNT(*) FROM playback_events WHERE mediaType = :mediaType") suspend fun playbackEventCountByType(mediaType: String): Int

    @Query("""SELECT pe.mediaId AS mediaId, COUNT(*) AS plays, COALESCE(mi.title, pe.mediaId) AS title
        FROM playback_events pe LEFT JOIN media_items mi ON mi.id = pe.mediaId
        WHERE pe.mediaType = :mediaType
        GROUP BY pe.mediaId
        ORDER BY plays DESC
        LIMIT :limit""")
    suspend fun topPlaybackMedia(mediaType: String, limit: Int): List<PlaybackMediaAggregate>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPlaybackSession(session: PlaybackSessionEntity)
    @Query("SELECT * FROM playback_sessions WHERE sessionId = :id") suspend fun playbackSession(id: String): PlaybackSessionEntity?
    @Query("SELECT * FROM playback_sessions WHERE startedAt >= :since AND startedAt <= :until ORDER BY startedAt DESC") suspend fun playbackSessions(since: Long, until: Long): List<PlaybackSessionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertPlaybackActivity(activity: PlaybackActivityEntity)
    @Query("SELECT * FROM playback_activity WHERE dayKey BETWEEN :fromDay AND :toDay ORDER BY dayKey ASC") suspend fun playbackActivity(fromDay: String, toDay: String): List<PlaybackActivityEntity>
    @Query("SELECT COALESCE(SUM(seconds), 0) FROM playback_activity WHERE mediaType = :mediaType") suspend fun totalPlaybackSeconds(mediaType: String): Long
    @Query("SELECT COUNT(*) FROM playback_history WHERE mediaType = :mediaType AND completed = 1") suspend fun completedHistoryCount(mediaType: String): Int

    @Query("SELECT * FROM video_progress WHERE mediaId = :id") fun observeVideoProgress(id: String): Flow<VideoProgressEntity?>
    @Query("SELECT * FROM video_progress ORDER BY updatedAt DESC") fun observeVideoProgress(): Flow<List<VideoProgressEntity>>
    @Query("SELECT * FROM video_progress ORDER BY updatedAt DESC LIMIT :limit") fun observeRecentVideoProgress(limit: Int): Flow<List<VideoProgressEntity>>
    @Query("SELECT * FROM video_progress WHERE mediaId = :id") suspend fun videoProgress(id: String): VideoProgressEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertVideoProgress(item: VideoProgressEntity)
    @Query("DELETE FROM video_progress WHERE mediaId = :id") suspend fun clearVideoProgress(id: String)

    @Query("SELECT * FROM video_bookmarks WHERE mediaId = :id ORDER BY positionMs ASC") fun observeBookmarks(id: String): Flow<List<VideoBookmarkEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addBookmark(item: VideoBookmarkEntity)
    @Query("DELETE FROM video_bookmarks WHERE mediaId = :id AND positionMs = :positionMs") suspend fun removeBookmark(id: String, positionMs: Long)
}
