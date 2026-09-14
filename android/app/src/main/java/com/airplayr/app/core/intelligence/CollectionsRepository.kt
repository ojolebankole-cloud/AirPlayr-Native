package com.airplayr.app.core.intelligence

import com.airplayr.app.core.data.StructuredRepository
import com.airplayr.app.core.data.db.PlaybackHistoryEntity
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaLibraryRepository
import com.airplayr.app.core.data.media.MediaType
import kotlinx.coroutines.flow.first

class CollectionsRepository(private val library: MediaLibraryRepository, private val structured: StructuredRepository) {
    suspend fun recentlyAdded(limit:Int=50)=library.page(MediaType.AUDIO,250,0).sortedByDescending { it.dateAdded }.take(limit)
    suspend fun recentlyPlayed(limit:Int=50):List<PlaybackHistoryEntity> = structured.recent(limit*2).filter { it.mediaType==MediaType.AUDIO.value }.take(limit)
    suspend fun mostPlayed(limit:Int=50):List<MediaItem> {
        val counts=structured.recent(500).filter { it.mediaType==MediaType.AUDIO.value }.associateBy({it.mediaId},{it.playCount})
        return library.page(MediaType.AUDIO,250,0).sortedByDescending { counts[it.id] ?: 0 }.take(limit)
    }
    suspend fun favorites(limit:Int=50):List<MediaItem> {
        val ids=structured.favoriteIds(); return library.page(MediaType.AUDIO,250,0).filter { it.id in ids }.take(limit)
    }
    suspend fun continueWatching(limit:Int=50):List<MediaItem> {
        val progress=structured.allVideoProgress().first().filter { !it.completed && it.positionMs>0 }.sortedByDescending { it.updatedAt }.take(limit)
        val lookup=library.page(MediaType.VIDEO,250,0).associateBy { it.id }
        return progress.mapNotNull { lookup[it.mediaId] }
    }
    suspend fun unwatched(limit:Int=50):List<MediaItem> {
        val progress=structured.allVideoProgress().first().filter { it.completed }.map { it.mediaId }.toSet()
        return library.page(MediaType.VIDEO,250,0).filterNot { it.id in progress }.take(limit)
    }
    suspend fun forYou(limit:Int=40):List<MediaItem> = (favorites(limit) + recentlyAdded(limit)).distinctBy { it.id }.take(limit)
    suspend fun rediscover(limit:Int=40):List<MediaItem> = mostPlayed(limit).ifEmpty { recentlyAdded(limit) }
    suspend fun tunedIn(limit:Int=40):List<MediaItem> = recentlyPlayed(limit).mapNotNull { h -> library.page(MediaType.AUDIO,250,0).firstOrNull { it.id==h.mediaId } }.take(limit)
    suspend fun rockYourWorld(limit:Int=40):List<MediaItem> = unwatched(limit).take(limit)
}
