package com.voicesearch.core.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One physical row of an imported table.
 *
 * Cells are serialised as a JSON array of strings so we never lose column
 * structure regardless of how Stage 4 settings interpret them. The DAO never
 * decodes [cellsJson] — that's the repository's job — but we keep it queryable
 * via LIKE for the cheap initial search path.
 *
 * Deleting a parent [TableEntity] cascades through and removes every row,
 * removing the need for manual cleanup.
 */
@Entity(
    tableName = "table_rows",
    foreignKeys = [
        ForeignKey(
            entity = TableEntity::class,
            parentColumns = ["id"],
            childColumns = ["tableId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("tableId"),
        Index(value = ["tableId", "rowIndex"], unique = true),
    ],
)
data class TableRowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tableId: String,
    val rowIndex: Int,
    val cellsJson: String,
    val isMarked: Boolean = false,
    val markedAt: Long? = null,
)
