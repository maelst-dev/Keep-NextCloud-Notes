package com.keepnc.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import com.keepnc.data.auth.TokenStorage
import com.keepnc.data.repository.NotesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that syncs notes between Room and the Nextcloud server.
 *
 * @HiltWorker + @AssistedInject allow Hilt to inject dependencies into this worker.
 * This requires [KeepNcApp] to implement [Configuration.Provider] and return a
 * [HiltWorkerFactory] — see KeepNcApp.kt.
 *
 * WorkManager handles:
 * - Running this on a background thread (CoroutineWorker is coroutine-based)
 * - Retrying on failure (up to 3 times)
 * - Deferring work until the device has network connectivity
 * - Persisting work across app restarts and device reboots
 *
 * BEGINNER NOTE: Never start network calls from a Service or AlarmManager yourself —
 * WorkManager is the correct modern approach for deferrable background work on Android.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: NotesRepository,
    private val tokenStorage: TokenStorage
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Don't attempt sync if the user hasn't logged in yet
        if (!tokenStorage.isLoggedIn()) return Result.success()

        return repository.syncWithServer().fold(
            onSuccess = { Result.success() },
            onFailure = {
                // Retry up to 3 times with exponential backoff (WorkManager handles this automatically)
                if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
            }
        )
    }

    companion object {
        /** WorkManager unique work name — used to enqueue and observe this worker. */
        const val WORK_NAME_PERIODIC = "keepnc_sync_periodic"
        const val WORK_NAME_ONE_TIME = "keepnc_sync_once"
        private const val MAX_RETRIES = 3

        private val networkConstraint = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        /** Periodic sync every 15 minutes (minimum allowed by WorkManager). */
        fun buildPeriodicRequest() = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(networkConstraint)
            .build()

        /** One-time sync triggered manually (e.g. pull-to-refresh). */
        fun buildOneTimeRequest() = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraint)
            .build()
    }
}
