package com.airplayr.app.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.airplayr.app.AirPlayrApplication
import com.airplayr.app.core.data.media.MediaPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class LibraryRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as AirPlayrApplication).container
        if (!MediaPermission.granted(applicationContext)) return Result.success()

        // Before initialization exists, finish the one-time full scan gate. After that,
        // periodic work is strictly delta synchronization. The worker never blindly
        // converts a normal refresh into a destructive full rebuild.
        if (!container.preferences.hasCompletedInitialLibraryScan()) {
            return try {
                container.ensureInitialLibraryScan()
                if (container.preferences.hasCompletedInitialLibraryScan()) Result.success() else Result.retry()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                Result.retry()
            }
        }

        if (!container.preferences.settings.first().autoRefreshLibrary) return Result.success()
        return try {
            container.mediaLibrary.deltaRefresh()
            Result.success()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
