package com.airplayr.app.core.data.search

import com.airplayr.app.core.data.db.AirPlayrDatabase
import com.airplayr.app.core.data.media.MediaItem
import com.airplayr.app.core.data.media.MediaItemEntity
import com.airplayr.app.core.data.media.asExternal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class SearchRepository(private val databaseProvider: () -> AirPlayrDatabase) {
    private val db: AirPlayrDatabase get() = databaseProvider()

    fun search(query: String, limit: Int = 80): Flow<List<MediaItem>> = flow {
        emitAll(db.mediaItemDao().search(query.trim(), limit.coerceIn(1, 200)))
    }.map { it.map(MediaItemEntity::asExternal) }.flowOn(Dispatchers.IO)
}
