package com.voicesearch.core.data.repository

import com.voicesearch.core.data.db.dao.TableSettingsDao
import com.voicesearch.core.data.mapper.toDomain
import com.voicesearch.core.data.mapper.toEntity
import com.voicesearch.core.domain.model.TableSettings
import com.voicesearch.core.domain.repository.TableSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TableSettingsRepositoryImpl @Inject constructor(
    private val dao: TableSettingsDao,
    private val json: Json,
) : TableSettingsRepository {

    override fun observe(tableId: String): Flow<TableSettings?> =
        dao.observe(tableId).map { it?.toDomain(json) }

    override suspend fun get(tableId: String): TableSettings? =
        dao.get(tableId)?.toDomain(json)

    override suspend fun save(settings: TableSettings) {
        dao.upsert(settings.toEntity(json))
    }

    override suspend fun delete(tableId: String) {
        dao.delete(tableId)
    }
}
