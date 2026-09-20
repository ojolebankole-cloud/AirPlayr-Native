package com.airplayr.app.core.data.media

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Lightweight application-lifetime listener for MediaStore changes.
 *
 * Android can emit several notifications while one file is being downloaded or indexed. The
 * observer therefore does no database/query work in onChange(); it only coalesces notifications
 * and schedules a background delta refresh. A short second pass handles the common case where
 * the first notification arrives before MediaStore has finished indexing the downloaded file.
 */
class MediaStoreChangeObserver(
    private val resolver: ContentResolver,
    private val scope: CoroutineScope,
    private val refresh: suspend () -> Unit,
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var refreshJob: Job? = null

    fun register() {
        resolver.registerContentObserver(
            MediaStoreUris.audio,
            true,
            this,
        )
        resolver.registerContentObserver(
            MediaStoreUris.video,
            true,
            this,
        )
    }

    fun unregister() {
        refreshJob?.cancel()
        refreshJob = null
        resolver.unregisterContentObserver(this)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        scheduleRefresh()
    }

    override fun onChange(selfChange: Boolean) {
        scheduleRefresh()
    }

    private fun scheduleRefresh() {
        refreshJob?.cancel()
        refreshJob = scope.launch {
            // Coalesce the burst of MediaStore notifications produced by a download/copy.
            delay(NOTIFICATION_QUIET_WINDOW_MS)
            runCatching { refresh() }

            // MediaStore can notify before metadata/indexing is fully committed. This is a
            // bounded retry, not polling: it happens only after an actual change notification.
            delay(INDEXING_SETTLE_RETRY_MS)
            runCatching { refresh() }
        }
    }

    private object MediaStoreUris {
        val audio: Uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val video: Uri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    private companion object {
        const val NOTIFICATION_QUIET_WINDOW_MS = 900L
        const val INDEXING_SETTLE_RETRY_MS = 1_200L
    }
}
