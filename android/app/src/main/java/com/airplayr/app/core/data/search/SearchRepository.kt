package com.airplayr.app.core.data.search

import com.airplayr.app.core.data.db.AirPlayrDatabase
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaItemEntity
import com.airplayr.app.core.data.media.asExternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SearchRepository(private val db: AirPlayrDatabase) {
    fun search(query: String, limit: Int = 80): Flow<List<MediaItem>> = db.mediaItemDao().search(query.trim(), limit.coerceIn(1, 200)).map { it.map(MediaItemEntity::asExternal) }
}
