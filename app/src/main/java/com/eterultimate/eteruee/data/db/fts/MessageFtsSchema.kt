package com.eterultimate.eteruee.data.db.fts

import android.database.Cursor
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import com.eterultimate.eteruee.data.db.migrations.execSQL
import com.eterultimate.eteruee.data.db.migrations.query

object MessageFtsSchema {
    private val createSql = """
        CREATE VIRTUAL TABLE IF NOT EXISTS message_fts USING fts5(
            text,
            node_id UNINDEXED,
            message_id UNINDEXED,
            conversation_id UNINDEXED,
            title UNINDEXED,
            update_at UNINDEXED,
            tokenize = 'unicode61'
        )
    """.trimIndent()

    fun ensure(db: SupportSQLiteDatabase) {
        if (db.isLegacySimpleTokenizerTable()) {
            db.execSQL("DROP TABLE IF EXISTS message_fts")
        }
        db.execSQL(createSql)
    }

    fun ensure(connection: SQLiteConnection) {
        if (connection.isLegacySimpleTokenizerTable()) {
            connection.execSQL("DROP TABLE IF EXISTS message_fts")
        }
        connection.execSQL(createSql)
    }

    private fun SupportSQLiteDatabase.isLegacySimpleTokenizerTable(): Boolean {
        return query(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'message_fts'"
        ).useString { sql ->
            sql?.contains("tokenize = 'simple'", ignoreCase = true) == true ||
                sql?.contains("tokenize='simple'", ignoreCase = true) == true
        }
    }

    private fun SQLiteConnection.isLegacySimpleTokenizerTable(): Boolean {
        return query(
            "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'message_fts'"
        ) { statement ->
            if (statement.step()) {
                val sql = if (statement.isNull(0)) null else statement.getText(0)
                sql?.contains("tokenize = 'simple'", ignoreCase = true) == true ||
                    sql?.contains("tokenize='simple'", ignoreCase = true) == true
            } else {
                false
            }
        }
    }
}

private inline fun <T> Cursor.useString(block: (String?) -> T): T {
    return use {
        block(if (it.moveToFirst()) it.getString(0) else null)
    }
}
