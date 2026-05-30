package com.voicesearch.core.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.voicesearch.core.data.db.entity.TableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {

    @Query("SELECT * FROM tables ORDER BY importedAt DESC")
    fun observeAll(): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE id = :id")
    fun observe(id: String): Flow<TableEntity?>

    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun get(id: String): TableEntity?

    @Upsert
    suspend fun upsert(table: TableEntity)

    @Query("UPDATE tables SET name = :newName WHERE id = :id")
    suspend fun rename(id: String, newName: String)

    @Delete
    suspend fun delete(table: TableEntity)

    @Query("DELETE FROM tables WHERE id = :id")
    suspend fun deleteById(id: String)
}
