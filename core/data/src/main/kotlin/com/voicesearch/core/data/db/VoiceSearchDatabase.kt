package com.voicesearch.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.voicesearch.core.data.db.dao.TableDao
import com.voicesearch.core.data.db.dao.TableRowDao
import com.voicesearch.core.data.db.dao.TableSettingsDao
import com.voicesearch.core.data.db.entity.TableEntity
import com.voicesearch.core.data.db.entity.TableRowEntity
import com.voicesearch.core.data.db.entity.TableSettingsEntity

/**
 * Room database.
 *
 * Schema is exported to `core/data/schemas/` by KSP (see build.gradle.kts).
 * Each version bump in `version` requires:
 *  1. A new file under `schemas/` (KSP regenerates on build).
 *  2. A [androidx.room.migration.Migration] added below.
 *  3. A migration test in `core:data` tests that uses `MigrationTestHelper`.
 */
@Database(
    entities = [
        TableEntity::class,
        TableRowEntity::class,
        TableSettingsEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class VoiceSearchDatabase : RoomDatabase() {

    abstract fun tableDao(): TableDao
    abstract fun tableRowDao(): TableRowDao
    abstract fun tableSettingsDao(): TableSettingsDao

    companion object {
        const val NAME = "voicesearch.db"
    }
}
