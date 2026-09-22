package com.airplayr.app.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_cache")
data class LyricsCacheEntity(
    @PrimaryKey val id: String,
    val provider: String,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val duration: Double,
    val plainLyrics: String?,
    val syncedLyrics: String?,
    val confidence: Int,
    val syncLevel: Int,
    val fetchedAt: Long,
    val lastAccessAt: Long,
)
