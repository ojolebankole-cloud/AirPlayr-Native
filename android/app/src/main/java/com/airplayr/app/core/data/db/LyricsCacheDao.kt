package com.airplayr.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LyricsCacheDao {
    @Query("SELECT * FROM lyrics_cache WHERE id = :id LIMIT 1")
    fun get(id: String): LyricsCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: LyricsCacheEntity)

    @Query("UPDATE lyrics_cache SET lastAccessAt = :at WHERE id = :id")
    fun touch(id: String, at: Long)

    @Query("DELETE FROM lyrics_cache WHERE id = :id")
    fun delete(id: String)

    @Query("SELECT id FROM lyrics_cache")
    fun ids(): List<String>

    @Query("SELECT COUNT(*) FROM lyrics_cache")
    fun count(): Int

    @Query("DELETE FROM lyrics_cache WHERE id IN (SELECT id FROM lyrics_cache WHERE provider NOT IN ('Manual', 'File import', 'Clipboard', 'AirPlayr') ORDER BY lastAccessAt ASC LIMIT :count)")
    fun deleteLeastRecentlyUsed(count: Int)

    @Query("DELETE FROM lyrics_cache")
    fun clearAll()
}
