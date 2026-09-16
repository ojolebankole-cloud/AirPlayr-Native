package com.airplayr.app.core.playback.service

import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaType

object MediaItemMapper {
    fun toExo(item: MediaItem): ExoMediaItem = ExoMediaItem.Builder()
        .setMediaId(item.id)
        .setUri(item.uri)
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
