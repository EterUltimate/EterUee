package com.eterultimate.eteruee.roleplay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL
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
    version = 6,  // 升级到版本6以保留 Tavern 扩展元数据
    exportSchema = true
)
abstract class RolePlayDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDAO
    abstract fun chatDao(): ChatDAO
    abstract fun worldInfoDao(): WorldInfoDAO
    abstract fun groupDao(): GroupDAO
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun presetDao(): PresetDAO

    companion object {
        const val DATABASE_NAME = "roleplay_database"

        /**
         * 从版本1迁移到版本2：添加分支支持字段
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 添加 activeBranchId 列
                db.execSQL("ALTER TABLE rp_chats ADD COLUMN activeBranchId TEXT")
                // 添加 rootNodesJson 列，默认值为空JSON数组
                db.execSQL("ALTER TABLE rp_chats ADD COLUMN rootNodesJson TEXT NOT NULL DEFAULT '[]'")
            }

            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE rp_chats ADD COLUMN activeBranchId TEXT")
                connection.execSQL("ALTER TABLE rp_chats ADD COLUMN rootNodesJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        /**
         * 从版本2迁移到版本3：添加书签表
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建书签表
                db.execSQL("""
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

            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("""
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
            override fun migrate(db: SupportSQLiteDatabase) {
                // 创建预设表
                db.execSQL("""
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

            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("""
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

        /**
         * 从版本4迁移到版本5：简化书签表结构
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 删除旧的书签表
                db.execSQL("DROP TABLE IF EXISTS rp_bookmarks")
                // 创建新的书签表（简化版）
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS rp_bookmarks (
                        id TEXT NOT NULL PRIMARY KEY,
                        chatId TEXT NOT NULL,
                        messageIndex INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }

            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP TABLE IF EXISTS rp_bookmarks")
                connection.execSQL("""
                    CREATE TABLE IF NOT EXISTS rp_bookmarks (
                        id TEXT NOT NULL PRIMARY KEY,
                        chatId TEXT NOT NULL,
                        messageIndex INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        note TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * 从版本5迁移到版本6：为世界书和聊天元数据保存完整 JSON，避免丢失 Tavern 扩展字段
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE rp_world_infos ADD COLUMN jsonData TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE rp_chats ADD COLUMN jsonData TEXT NOT NULL DEFAULT ''")
            }

            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE rp_world_infos ADD COLUMN jsonData TEXT NOT NULL DEFAULT ''")
                connection.execSQL("ALTER TABLE rp_chats ADD COLUMN jsonData TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
