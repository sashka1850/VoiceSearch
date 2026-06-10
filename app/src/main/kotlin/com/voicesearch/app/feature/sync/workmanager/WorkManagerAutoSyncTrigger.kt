package com.voicesearch.app.feature.sync.workmanager

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.voicesearch.core.domain.sync.AutoSyncTrigger
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [AutoSyncTrigger] implementation backed by [WorkManager].
 *
 * Each `requestSync(tableId)` enqueues a [SyncTableWorker] with a 10 s initial
 * delay under a unique-work name `auto-sync-<tableId>` with [ExistingWorkPolicy.REPLACE].
 * That means a second mark within the 10 s window **replaces** the pending
 * work — the clock restarts, and the user's burst of marks coalesces into a
 * single upload at the end. No coroutine debouncer required; WorkManager owns
 * the timing.
 */
@Singleton
class WorkManagerAutoSyncTrigger @Inject constructor(
    private val workManager: WorkManager,
) : AutoSyncTrigger {

    override fun requestSync(tableId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncTableWorker>()
            .setInitialDelay(DEBOUNCE_SECONDS, TimeUnit.SECONDS)
            .setConstraints(constraints)
            .setInputData(Data.Builder().putString(SyncTableWorker.KEY_TABLE_ID, tableId).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_MIN_SECONDS, TimeUnit.SECONDS)
            .addTag(TAG_AUTO_SYNC)
            .build()

        workManager.enqueueUniqueWork(
            uniqueName(tableId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    companion object {
        const val TAG_AUTO_SYNC = "auto-sync"
        private const val DEBOUNCE_SECONDS = 10L
        private const val BACKOFF_MIN_SECONDS = 30L

        fun uniqueName(tableId: String): String = "auto-sync-$tableId"
    }
}
