package com.voicesearch.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations.
 *
 * Schema JSON for each version lives under `core/data/schemas/` and is the
 * authoritative source of what each migration must end up matching. Adding
 * a column? Bump the version, add the migration here, regenerate the schema,
 * commit both.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Nullable INTEGER / TEXT columns; no default needed, existing rows
        // get NULL which is the "never synced" state.
        db.execSQL("ALTER TABLE tables ADD COLUMN lastSyncAttemptAt INTEGER")
        db.execSQL("ALTER TABLE tables ADD COLUMN lastSyncSuccessAt INTEGER")
        db.execSQL("ALTER TABLE tables ADD COLUMN lastSyncError TEXT")
    }
}
