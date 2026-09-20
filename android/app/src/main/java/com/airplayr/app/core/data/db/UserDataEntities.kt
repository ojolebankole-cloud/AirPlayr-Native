package com.airplayr.app.core.data.db

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "favorites", primaryKeys = ["mediaId"])
data class FavoriteEntity(val mediaId: String, val mediaType: String, val addedAt: Long)

@Entity(tableName = "playback_history", primaryKeys = ["mediaId"], indices = [Index(value = ["playedAt"]), Index(value = ["mediaType"]), Index(value = ["mediaType", "completed"]), Index(value = ["playCount", "playedAt"])])
data class PlaybackHistoryEntity(
    val mediaId: String,
    val mediaType: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val artworkUri: String?,
    val playedAt: Long,
    val playCount: Int,
    val completed: Boolean,
    val completedAt: Long?,
)

@Entity(tableName = "music_playlists")
data class MusicPlaylistEntity(
    @androidx.room.PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val artworkUri: String?,
)

@Entity(tableName = "music_playlist_items", primaryKeys = ["playlistId", "mediaId"], indices = [Index(value=["playlistId", "position"])])
data class MusicPlaylistItemEntity(val playlistId: String, val mediaId: String, val position: Int, val addedAt: Long)

@Entity(tableName = "video_progress", primaryKeys = ["mediaId"], indices = [Index(value=["updatedAt"])])
data class VideoProgressEntity(val mediaId: String, val positionMs: Long, val durationMs: Long, val completed: Boolean, val updatedAt: Long)

@Entity(tableName = "video_bookmarks", primaryKeys = ["mediaId", "positionMs"])
data class VideoBookmarkEntity(val mediaId: String, val positionMs: Long, val label: String?, val createdAt: Long)

@Entity(tableName = "playback_events", indices = [Index(value = ["playedAt"]), Index(value = ["mediaType"]), Index(value = ["mediaType", "playedAt"])])
data class PlaybackEventEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val eventId: Long = 0,
    val mediaId: String,
    val mediaType: String,
    val playedAt: Long,
)

@Entity(tableName = "playback_sessions", indices = [Index(value = ["startedAt"]), Index(value = ["mediaType"])])
data class PlaybackSessionEntity(
    @androidx.room.PrimaryKey val sessionId: String,
    val mediaId: String,
    val mediaType: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val artworkUri: String?,
    val startedAt: Long,
    val endedAt: Long?,
    val seconds: Long,
    val lastMediaTime: Long,
)

@Entity(tableName = "playback_activity", primaryKeys = ["dayKey", "bucket", "mediaType"], indices = [Index(value = ["dayKey"]), Index(value = ["mediaType", "dayKey"])])
data class PlaybackActivityEntity(
    val dayKey: String,
    val bucket: String,
    val mediaType: String,
    val seconds: Long,
)
