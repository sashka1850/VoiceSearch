package com.voicesearch.core.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.voicesearch.core.data.db.entity.TableSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableSettingsDao {

    @Query("SELECT * FROM table_settings WHERE tableId = :tableId")
    fun observe(tableId: String): Flow<TableSettingsEntity?>

    @Query("SELECT * FROM table_settings WHERE tableId = :tableId")
    suspend fun get(tableId: String): TableSettingsEntity?

    @Upsert
    suspend fun upsert(settings: TableSettingsEntity)

    @Query("DELETE FROM table_settings WHERE tableId = :tableId")
    suspend fun delete(tableId: String)
}
