package com.voicesearch.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Read model the UI uses to render the table info card.
 *
 * Counts are flowed live from Room; timestamps come from [TableEntity]'s
 * sync columns which the upload pipeline updates after each attempt.
 */
@Serializable
data class TableInfo(
    val tableId: String,
    val totalRows: Int,
    val markedRows: Int,
    val syncedMarkedRows: Int,
    val sync: TableSyncStatus,
)

@Serializable
data class TableSyncStatus(
    val lastSuccessAt: Long?,
    val lastAttemptAt: Long?,
    /** Human-readable error from the latest attempt, null after success. */
    val lastError: String?,
) {
    val hasSyncedAtLeastOnce: Boolean get() = lastSuccessAt != null
    val lastAttemptWasFailure: Boolean get() = lastError != null
}
