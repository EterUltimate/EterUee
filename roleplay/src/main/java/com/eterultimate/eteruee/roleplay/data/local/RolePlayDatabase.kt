package com.eterultimate.eteruee.roleplay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eterultimate.eteruee.roleplay.data.local.dao.*
import com.eterultimate.eteruee.roleplay.data.local.entity.*

/**
 * RolePlay 模块的 Room 数据库
 */
@Database(
    entities = [
        CharacterEntity::class,
        ChatEntity::class,
        WorldInfoEntity::class,
        GroupEntity::class,
        BookmarkEntity::class,
        PresetEntity::class
    ],
    version = 4,  // 升级到版本4以支持预设功能
    exportSchema = true
)
abstract class RolePlayDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDAO
    abstract fun chatDao(): ChatDAO
    abstract fun worldInfoDao(): WorldInfoDAO
    abstract fun groupDao(): GroupDAO
    abstract fun bookmarkDao(): BookmarkDAO
    abstract fun presetDao(): PresetDAO
    
    companion object {
        const val DATABASE_NAME = "roleplay_database"
        
        /**
         * 从版本1迁移到版本2：添加分支支持字段
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 添加 activeBranchId 列
                database.execSQL("ALTER TABLE rp_chats ADD COLUMN activeBranchId TEXT")
                // 添加 rootNodesJson 列，默认值为空JSON数组
                database.execSQL("ALTER TABLE rp_chats ADD COLUMN rootNodesJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
        
        /**
         * 从版本2迁移到版本3：添加书签表
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建书签表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS rp_bookmarks (
                        id TEXT NOT NULL PRIMARY KEY,
                        chatId TEXT NOT NULL,
                        characterId TEXT NOT NULL,
                        messageId TEXT,
                        nodeId TEXT,
                        title TEXT NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        color TEXT NOT NULL,
                        tagsJson TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        /**
         * 从版本3迁移到版本4：添加预设表
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 创建预设表
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS rp_presets (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        type TEXT NOT NULL,
                        parametersJson TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
