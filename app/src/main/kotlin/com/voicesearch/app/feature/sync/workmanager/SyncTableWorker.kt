package com.voicesearch.app.feature.sync.workmanager

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.voicesearch.app.feature.sync.domain.UploadTableUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Background uploader that pushes a table back to Yandex.Disk.
 *
 * Triggered (debounced) by [WorkManagerAutoSyncTrigger] whenever a marked
 * row lands on a table whose settings have `autoSync = true`. Reuses the
 * same [UploadTableUseCase] the menu's "Синхронизировать сейчас" entry uses.
 *
 * Network failure → [Result.retry]; lack of auth → [Result.failure]; any
 * other use-case failure is also a retry — the next mark will queue a new run.
 */
@HiltWorker
class SyncTableWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val uploadTableUseCase: UploadTableUseCase,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val tableId = inputData.getString(KEY_TABLE_ID) ?: run {
            Timber.w("SyncTableWorker: missing tableId in input data, dropping run")
            return Result.failure()
        }
        return when (val outcome = uploadTableUseCase.upload(tableId)) {
            is UploadTableUseCase.Result.Success -> {
                Timber.i("Auto-sync uploaded $tableId to ${outcome.targetPath}")
                Result.success()
            }
            UploadTableUseCase.Result.NotAuthenticated -> {
                Timber.w("Auto-sync skipped — not authenticated to Yandex")
                Result.failure()
            }
            is UploadTableUseCase.Result.Failure -> {
                Timber.w("Auto-sync failed: ${outcome.message}; will retry")
                Result.retry()
            }
        }
    }

    companion object {
        const val KEY_TABLE_ID = "table_id"
    }
}
