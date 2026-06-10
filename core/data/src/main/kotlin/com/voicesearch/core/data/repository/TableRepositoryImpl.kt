package com.voicesearch.core.data.repository

import androidx.room.withTransaction
import com.voicesearch.core.data.db.VoiceSearchDatabase
import com.voicesearch.core.data.db.dao.TableDao
import com.voicesearch.core.data.db.dao.TableRowDao
import com.voicesearch.core.data.mapper.buildRowEntity
import com.voicesearch.core.data.mapper.toDomain
import com.voicesearch.core.data.mapper.toEntity
import com.voicesearch.core.data.db.dao.TableSettingsDao
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.model.TableInfo
import com.voicesearch.core.domain.model.TableSyncStatus
import com.voicesearch.core.domain.repository.TableRepository
import com.voicesearch.core.domain.repository.TableRow
import com.voicesearch.core.domain.sync.AutoSyncTrigger
import com.voicesearch.core.domain.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
internal class TableRepositoryImpl @Inject constructor(
    private val database: VoiceSearchDatabase,
    private val tableDao: TableDao,
    private val rowDao: TableRowDao,
    private val settingsDao: TableSettingsDao,
    private val json: Json,
    private val clock: Clock,
    private val autoSyncTrigger: AutoSyncTrigger,
) : TableRepository {

    override fun observeAll(): Flow<List<Table>> =
        tableDao.observeAll().map { list -> list.map { it.toDomain(json) } }

    override fun observe(id: String): Flow<Table?> =
        tableDao.observe(id).map { it?.toDomain(json) }

    override suspend fun get(id: String): Table? =
        tableDao.get(id)?.toDomain(json)

    override suspend fun save(table: Table, rows: List<List<String>>) {
        val tableEntity = table.toEntity(
            json = json,
            importedAt = clock.nowMillis(),
            originalFileName = (table.source as? com.voicesearch.core.domain.model.TableSource.LocalFile)?.originalFileName,
        )
        val rowEntities = rows.mapIndexed { index, cells ->
            buildRowEntity(tableId = table.id, rowIndex = index, cells = cells, json = json)
        }
        // One transaction so either the table + every row land or nothing does.
        database.withTransaction {
            tableDao.upsert(tableEntity)
            rowDao.replaceAll(table.id, rowEntities)
        }
    }

    override suspend fun rename(id: String, newName: String) {
        tableDao.rename(id, newName)
    }

    override suspend fun delete(id: String) {
        tableDao.deleteById(id)
    }

    override fun observeRows(tableId: String): Flow<List<TableRow>> =
        rowDao.observe(tableId).map { list -> list.map { it.toDomain(json) } }

    override suspend fun getRows(tableId: String): List<TableRow> =
        rowDao.getAll(tableId).map { it.toDomain(json) }

    override suspend fun setRowMarked(rowId: Long, marked: Boolean, markedAt: Long?) {
        rowDao.setMarked(rowId, marked, markedAt)
        val tableId = rowDao.getById(rowId)?.tableId ?: return
        notifyMaybeAutoSync(tableId)
    }

    override suspend fun setRowsMarked(rowIds: Collection<Long>, marked: Boolean, markedAt: Long?) {
        if (rowIds.isEmpty()) return
        rowDao.setMarkedAll(rowIds, marked, markedAt)
        // All ids in a single batch belong to the same table (the multi-match flow
        // only ever marks from one table). Take the first to fetch its parent id.
        val tableId = rowDao.getById(rowIds.first())?.tableId ?: return
        notifyMaybeAutoSync(tableId)
    }

    /**
     * Only nudge the sync trigger when the table's settings have `autoSync = true`.
     * Settings missing → user hasn't configured yet → no sync.
     */
    private suspend fun notifyMaybeAutoSync(tableId: String) {
        val autoSync = settingsDao.get(tableId)?.autoSync == true
        if (autoSync) autoSyncTrigger.requestSync(tableId)
    }

    override fun observeInfo(tableId: String): Flow<TableInfo?> =
        tableDao.observe(tableId).flatMapLatest { entity ->
            if (entity == null) {
                flowOf(null)
            } else {
                val syncedUntil = entity.lastSyncSuccessAt ?: 0L
                combine(
                    rowDao.countTotal(tableId),
                    rowDao.countMarked(tableId),
                    rowDao.countMarkedSynced(tableId, syncedUntil),
                ) { total, marked, syncedMarked ->
                    TableInfo(
                        tableId = tableId,
                        totalRows = total,
                        markedRows = marked,
                        syncedMarkedRows = syncedMarked,
                        sync = TableSyncStatus(
                            lastSuccessAt = entity.lastSyncSuccessAt,
                            lastAttemptAt = entity.lastSyncAttemptAt,
                            lastError = entity.lastSyncError,
                        ),
                    )
                }
            }
        }

    override suspend fun recordSyncSuccess(tableId: String, attemptAt: Long, successAt: Long) {
        tableDao.recordSyncSuccess(tableId, attemptAt, successAt)
    }

    override suspend fun recordSyncFailure(tableId: String, attemptAt: Long, error: String) {
        tableDao.recordSyncFailure(tableId, attemptAt, error)
    }

    override suspend fun observeSyncStatus(tableId: String): TableSyncStatus? =
        tableDao.get(tableId)?.let {
            TableSyncStatus(
                lastSuccessAt = it.lastSyncSuccessAt,
                lastAttemptAt = it.lastSyncAttemptAt,
                lastError = it.lastSyncError,
            )
        }
}
