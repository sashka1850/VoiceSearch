package com.voicesearch.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Per-table search configuration written from the "Настройки таблицы" screen.
 *
 * 1-to-1 with [TableEntity] — the tableId is the primary key. Deleting the
 * parent table removes its settings via cascade.
 */
@Entity(
    tableName = "table_settings",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TableSettingsEntity(
    @PrimaryKey val tableId: String,
    val searchColumnIndex: Int,
    val markColumnIndex: Int,
    val nameColumnIndex: Int,
    val startRowIndex: Int,
    val successMarker: String,
    /** Serialised [com.voicesearch.core.domain.model.PrefixHint] or null. */
    val prefixHintJson: String?,
    val autoSync: Boolean,
)
