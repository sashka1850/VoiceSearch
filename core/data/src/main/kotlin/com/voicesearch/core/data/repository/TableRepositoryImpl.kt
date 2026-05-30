package com.voicesearch.core.data.repository

import androidx.room.withTransaction
import com.voicesearch.core.data.db.VoiceSearchDatabase
import com.voicesearch.core.data.db.dao.TableDao
import com.voicesearch.core.data.db.dao.TableRowDao
import com.voicesearch.core.data.mapper.buildRowEntity
import com.voicesearch.core.data.mapper.toDomain
import com.voicesearch.core.data.mapper.toEntity
import com.voicesearch.core.domain.model.Table
import com.voicesearch.core.domain.repository.TableRepository
import com.voicesearch.core.domain.repository.TableRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TableRepositoryImpl @Inject constructor(
    private val database: VoiceSearchDatabase,
    private val tableDao: TableDao,
    private val rowDao: TableRowDao,
    private val json: Json,
    private val clock: () -> Long = System::currentTimeMillis,
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
            importedAt = clock(),
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
    }

    override suspend fun setRowsMarked(rowIds: Collection<Long>, marked: Boolean, markedAt: Long?) {
        if (rowIds.isEmpty()) return
        rowDao.setMarkedAll(rowIds, marked, markedAt)
    }
}
