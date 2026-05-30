package com.voicesearch.core.domain.repository

import com.voicesearch.core.domain.model.TableSettings
import kotlinx.coroutines.flow.Flow

/**
 * Per-table search configuration: which column to search, the success marker,
 * detected prefix hint, etc. Settings are filled in by the user on the
 * "Настройки таблицы" screen after import.
 */
interface TableSettingsRepository {

    fun observe(tableId: String): Flow<TableSettings?>

    suspend fun get(tableId: String): TableSettings?

    suspend fun save(settings: TableSettings)

    suspend fun delete(tableId: String)
}
