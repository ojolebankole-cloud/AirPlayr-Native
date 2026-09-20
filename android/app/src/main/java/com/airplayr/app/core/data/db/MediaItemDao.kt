package com.airplayr.app.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.airplayr.app.core.data.media.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaItemDao {
    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY dateModified DESC, id")
    fun observeByType(type: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY dateAdded DESC, title COLLATE NOCASE ASC LIMIT :limit")
    fun observeRecentByType(type: String, limit: Int): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE mediaType = :type ORDER BY dateModified DESC, id LIMIT :limit OFFSET :offset")
    suspend fun getPage(type: String, limit: Int, offset: Int): List<MediaItemEntity>

    @Query("SELECT COUNT(*) FROM media_items WHERE mediaType = :type")
    suspend fun count(type: String): Int

    @Query("SELECT COUNT(*) FROM media_items WHERE mediaType = :type")
    fun observeCount(type: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT CASE WHEN NULLIF(folder, '') IS NOT NULL THEN folder WHEN NULLIF(relativePath, '') IS NOT NULL THEN relativePath END) FROM media_items")
    fun observeFolderCount(): Flow<Int>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE title LIKE '%' || :query || '%' OR filename LIKE '%' || :query || '%' OR IFNULL(artist, '') LIKE '%' || :query || '%' OR IFNULL(album, '') LIKE '%' || :query || '%' OR IFNULL(genre, '') LIKE '%' || :query || '%' ORDER BY dateModified DESC, title COLLATE NOCASE ASC LIMIT :limit")
    fun search(query: String, limit: Int): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Query("SELECT * FROM media_items WHERE mediaType = :type AND sourceDirectoryUri IS NULL")
    suspend fun getMediaStoreByType(type: String): List<MediaItemEntity>

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM media_items WHERE mediaType = :type AND sourceDirectoryUri IS NULL")
    suspend fun deleteMediaStoreType(type: String)

    @Query("DELETE FROM media_items WHERE mediaType = :type AND sourceDirectoryUri = :rootUri")
    suspend fun deleteSupplementalRoot(type: String, rootUri: String)

    @Transaction
    suspend fun replaceMediaStoreType(type: String, items: List<MediaItemEntity>) {
        deleteMediaStoreType(type)
        if (items.isNotEmpty()) insertAll(items)
    }

    @Query("DELETE FROM media_items")
    suspend fun clearAll()
}
