package com.airplayr.app.core.data.media

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["mediaType", "dateModified"]),
        Index(value = ["mediaType", "dateAdded"]),
        Index(value = ["mediaType", "sourceDirectoryUri"]),
        Index(value = ["mediaType", "title"]),
        Index(value = ["mediaType", "artist"]),
        Index(value = ["mediaStoreId"], unique = false),
    ],
)
data class MediaItemEntity(
    @PrimaryKey val id: String,
    val mediaStoreId: Long?,
    val uri: String,
    val title: String,
    val filename: String,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateModified: Long,
    val dateAdded: Long,
    val mediaType: String,
    val mimeType: String?,
    val albumId: Long?,
    val artworkUri: String?,
    val thumbnailUri: String?,
    val genre: String?,
    val sourceDirectoryUri: String?,
    val folderId: String?,
    val folder: String?,
    val path: String?,
    val relativePath: String?,
)

enum class MediaType(val value: String) {
    AUDIO("audio"), VIDEO("video");

    companion object {
        fun from(value: String?): MediaType = if (value == VIDEO.value) VIDEO else AUDIO
    }
}

data class MediaItem(
    val id: String,
    val mediaStoreId: Long?,
    val uri: String,
    val title: String,
    val filename: String,
    val artist: String?,
    val album: String?,
    val albumArtist: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val dateModified: Long,
    val dateAdded: Long,
    val mediaType: MediaType,
    val mimeType: String?,
    val albumId: Long?,
    val artworkUri: String?,
    val thumbnailUri: String?,
    val genre: String?,
    val sourceDirectoryUri: String?,
    val folderId: String?,
    val folder: String?,
    val path: String?,
    val relativePath: String?,
)

fun MediaItemEntity.asExternal() = MediaItem(
    id = id,
    mediaStoreId = mediaStoreId,
    uri = uri,
    title = title,
    filename = filename,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    durationMs = durationMs,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    dateModified = dateModified,
    dateAdded = dateAdded,
    mediaType = MediaType.from(mediaType),
    mimeType = mimeType,
    albumId = albumId,
    artworkUri = artworkUri,
    thumbnailUri = thumbnailUri,
    genre = genre,
    sourceDirectoryUri = sourceDirectoryUri,
    folderId = folderId,
    folder = folder,
    path = path,
    relativePath = relativePath,
)

fun MediaItem.asEntity() = MediaItemEntity(
    id = id,
    mediaStoreId = mediaStoreId,
    uri = uri,
    title = title,
    filename = filename,
    artist = artist,
    album = album,
    albumArtist = albumArtist,
    durationMs = durationMs,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    dateModified = dateModified,
    dateAdded = dateAdded,
    mediaType = mediaType.value,
    mimeType = mimeType,
    albumId = albumId,
    artworkUri = artworkUri,
    thumbnailUri = thumbnailUri,
    genre = genre,
    sourceDirectoryUri = sourceDirectoryUri,
    folderId = folderId,
    folder = folder,
    path = path,
    relativePath = relativePath,
)
