package me.rerere.rag.db

import android.content.Context
import io.requery.android.database.sqlite.SQLiteCustomExtension
import io.requery.android.database.sqlite.SQLiteDatabase
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration
import io.requery.android.database.sqlite.SQLiteOpenHelper

private const val TAG = "VectorDatabase"

class SQLiteVectorDatabase(context: Context, name: String) :
    SQLiteOpenHelper(context, name, null, 1), VectorDatabase {

    override fun createConfiguration(path: String?, openFlags: Int): SQLiteDatabaseConfiguration? {
        val configuration = SQLiteDatabaseConfiguration(path, openFlags)
        configuration.customExtensions.add(
            SQLiteCustomExtension(
                "vec0",
                "sqlite3_vec_init"
            )
        )
        return configuration
    }

    fun getAllTableNames(): List<String> {
        val db = readableDatabase
        val tableNames = mutableListOf<String>()
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%'",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                tableNames.add(it.getString(0))
            }
        }
        db.close()
        return tableNames
    }

    fun test() {
        writableDatabase.execSQL("DROP TABLE IF EXISTS rag_chunks")
        writableDatabase.execSQL(
            """
            CREATE VIRTUAL TABLE rag_chunks USING vec0(
              embedding FLOAT[768] distance_metric=cosine,    -- 假设用的是 768 维 BERT 向量
              content TEXT,            -- 原始文本内容
              metadata TEXT            -- 元数据（JSON格式，存储标题、来源等）
            );
        """.trimIndent()
        )
        println("executed")
        println(getAllTableNames())
    }

    override fun onCreate(db: SQLiteDatabase) {
        println("db created")
    }

    override fun onUpgrade(
        db: SQLiteDatabase?,
        oldVersion: Int,
        newVersion: Int
    ) {
    }
}