package com.voicesearch.core.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.voicesearch.core.data.db.entity.TableRowEntity
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions") // Mix of mutations and aggregated counts; splitting hurts cohesion more than it helps.
@Dao
interface TableRowDao {

    @Query("SELECT * FROM table_rows WHERE tableId = :tableId ORDER BY rowIndex")
    fun observe(tableId: String): Flow<List<TableRowEntity>>

    @Query("SELECT * FROM table_rows WHERE tableId = :tableId ORDER BY rowIndex")
    suspend fun getAll(tableId: String): List<TableRowEntity>

    @Query("SELECT * FROM table_rows WHERE id = :id")
    suspend fun getById(id: Long): TableRowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<TableRowEntity>)

    @Query("DELETE FROM table_rows WHERE tableId = :tableId")
    suspend fun clear(tableId: String)

    /**
     * Atomically clear and re-insert. Use this when an import is re-run
     * or when the user replaces a table's source file.
     */
    @Transaction
    suspend fun replaceAll(tableId: String, rows: List<TableRowEntity>) {
        clear(tableId)
        insertAll(rows)
    }

    @Query("UPDATE table_rows SET isMarked = :marked, markedAt = :markedAt WHERE id = :id")
    suspend fun setMarked(id: Long, marked: Boolean, markedAt: Long?)

    @Query("UPDATE table_rows SET isMarked = :marked, markedAt = :markedAt WHERE id IN (:ids)")
    suspend fun setMarkedAll(ids: Collection<Long>, marked: Boolean, markedAt: Long?)

    @Query("SELECT COUNT(*) FROM table_rows WHERE tableId = :tableId")
    fun countTotal(tableId: String): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT COUNT(*) FROM table_rows WHERE tableId = :tableId AND isMarked = 1")
    fun countMarked(tableId: String): kotlinx.coroutines.flow.Flow<Int>

    /**
     * Count of marked rows whose mark was set on or before [syncedUntil]
     * (i.e. the latest known successful sync timestamp). After a sync the
     * server image reflects everything stamped at or before that wall-clock.
     */
    @Query(
        """
        SELECT COUNT(*) FROM table_rows
        WHERE tableId = :tableId AND isMarked = 1
          AND markedAt IS NOT NULL AND markedAt <= :syncedUntil
        """,
    )
    fun countMarkedSynced(tableId: String, syncedUntil: Long): kotlinx.coroutines.flow.Flow<Int>
}
