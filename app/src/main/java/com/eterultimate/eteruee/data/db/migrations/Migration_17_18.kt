package com.eterultimate.eteruee.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 添加 type 列,带默认值 'image_generation'
        database.execSQL(
            "ALTER TABLE GenMediaEntity ADD COLUMN type TEXT NOT NULL DEFAULT 'image_generation'"
        )
        
        // 添加 source_paths 列,可为空
        database.execSQL(
            "ALTER TABLE GenMediaEntity ADD COLUMN source_paths TEXT"
        )
    }
}
