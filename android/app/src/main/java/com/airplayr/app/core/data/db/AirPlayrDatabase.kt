package com.airplayr.app.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.airplayr.app.core.data.media.MediaItemEntity

@Database(
    entities = [MediaItemEntity::class, FavoriteEntity::class, PlaybackHistoryEntity::class, MusicPlaylistEntity::class, MusicPlaylistItemEntity::class, VideoProgressEntity::class, VideoBookmarkEntity::class, PlaybackEventEntity::class, PlaybackSessionEntity::class, PlaybackActivityEntity::class],
    version = 5,
    exportSchema = true,
)
abstract class AirPlayrDatabase : RoomDatabase() {
    abstract fun mediaItemDao(): MediaItemDao
    abstract fun userDataDao(): UserDataDao
}
