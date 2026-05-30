package com.voicesearch.core.domain.repository

import com.voicesearch.core.domain.model.Table
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for imported tables.
 *
 * Implementations live in `:core:data` and back this onto Room + parsed file
 * snapshots. ViewModels and use cases depend on the interface only, so
 * swapping the storage (e.g. for testing) is one-line.
 */
interface TableRepository {

    fun observeAll(): Flow<List<Table>>

    fun observe(id: String): Flow<Table?>

    suspend fun get(id: String): Table?

    /**
     * Save a freshly-imported table. Replaces any previous rows for the same id.
     *
     * @param rows each row is a list of cell values, in column order matching [Table.headers].
     */
    suspend fun save(table: Table, rows: List<List<String>>)

    suspend fun rename(id: String, newName: String)

    suspend fun delete(id: String)

    /** Returns rows for [tableId] in original order. */
    fun observeRows(tableId: String): Flow<List<TableRow>>

    /** One-shot fetch, e.g. for an export pipeline. */
    suspend fun getRows(tableId: String): List<TableRow>

    /** Marks a single row. [markedAt] is epoch millis; null clears the mark. */
    suspend fun setRowMarked(rowId: Long, marked: Boolean, markedAt: Long?)

    /** Bulk variant used when the user picks "Выбрать все" in multi-match. */
    suspend fun setRowsMarked(rowIds: Collection<Long>, marked: Boolean, markedAt: Long?)
}

/**
 * A row read back from storage. Carries identity so we can mark it later
 * without having to re-locate by content.
 */
data class TableRow(
    val id: Long,
    val tableId: String,
    val rowIndex: Int,
    val cells: List<String>,
    val isMarked: Boolean,
    val markedAt: Long?,
)
