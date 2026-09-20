package com.airplayr.app.core.playback.service

import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType

object MediaItemMapper {
    fun toExo(item: MediaItem): ExoMediaItem = ExoMediaItem.Builder()
        .setMediaId(item.id)
        .setUri(item.uri)
        .apply {
            val mime = item.mimeType?.takeIf { it.isNotBlank() }
                ?: if (item.mediaType == MediaType.VIDEO) inferVideoMimeType(item.filename, item.uri) else null
            if (mime != null) setMimeType(mime)
        }
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(item.title)
                .setArtist(item.artist)
                .setAlbumTitle(item.album)
                .setArtworkUri(item.artworkUri?.let(android.net.Uri::parse))
                .setMediaType(if (item.mediaType == MediaType.AUDIO) MediaMetadata.MEDIA_TYPE_MUSIC else MediaMetadata.MEDIA_TYPE_VIDEO)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setDurationMs(item.durationMs.takeIf { it > 0L })
                .build()
        ).build()
}


private fun inferVideoMimeType(filename: String, uri: String): String? {
    val name = filename.ifBlank { uri.substringBefore('?').substringAfterLast('/') }
    return when (name.substringAfterLast('.', "").lowercase()) {
        "mp4", "m4v" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "avi" -> "video/avi"
        "wmv" -> "video/x-ms-wmv"
        "3gp" -> "video/3gpp"
        "ts", "m2ts", "mts" -> "video/mp2t"
        "flv" -> "video/x-flv"
        else -> null
    }
}
