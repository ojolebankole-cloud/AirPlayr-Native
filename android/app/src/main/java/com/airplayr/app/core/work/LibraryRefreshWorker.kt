package com.airplayr.app.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.airplayr.app.AirPlayrApplication
import com.airplayr.app.core.data.media.MediaPermission
import kotlinx.coroutines.flow.first

class LibraryRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as AirPlayrApplication).container
        if (!MediaPermission.granted(applicationContext)) return Result.success()
        if (!container.preferences.settings.first().autoRefreshLibrary) return Result.success()
        return runCatching {
            container.mediaLibrary.refreshAll()
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }
}
