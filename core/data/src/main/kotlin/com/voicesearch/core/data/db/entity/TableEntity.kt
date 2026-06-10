package com.voicesearch.core.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Row in the `tables` table — one entry per imported spreadsheet.
 *
 * - [sourceJson] is the serialized [com.voicesearch.core.domain.model.TableSource].
 *   Kept as JSON so adding new source variants (e.g. a future Telegram bot import)
 *   doesn't force a Room migration.
 * - [headersJson] is a JSON list of header labels in column order.
 */
@Entity(tableName = "tables")
data class TableEntity(
    @PrimaryKey val id: String,
    val name: String,
    val sourceJson: String,
    val headersJson: String,
    val rowCount: Int,
    val importedAt: Long,
    val originalFileName: String?,
    /** Wall-clock millis of the most recent attempted sync (success OR failure). */
    val lastSyncAttemptAt: Long? = null,
    /** Wall-clock millis of the most recent **successful** sync. */
    val lastSyncSuccessAt: Long? = null,
    /** Last error message — null after a success. Drives the red glow + caption. */
    val lastSyncError: String? = null,
)
