package com.eterultimate.eteruee.data.db.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.rerere.ai.ui.UIMessage
import com.eterultimate.eteruee.data.model.MessageNode
import com.eterultimate.eteruee.data.db.DatabaseMigrationTracker
import com.eterultimate.eteruee.utils.JsonInstant

private const val TAG = "Migration_6_7"

val Migration_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        Log.i(TAG, "migrate: start migrate from 6 to 7")
        DatabaseMigrationTracker.onMigrationStart(6, 7)
        db.beginTransaction()
        try {
            // 鍒涘缓鏂拌〃缁撴瀯锛堜笉鍖呭惈messages鍒楋級
            db.execSQL(
                """
                CREATE TABLE ConversationEntity_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    assistant_id TEXT NOT NULL DEFAULT '0950e2dc-9bd5-4801-afa3-aa887aa36b4e',
                    title TEXT NOT NULL,
                    nodes TEXT NOT NULL,
                    usage TEXT,
                    create_at INTEGER NOT NULL,
                    update_at INTEGER NOT NULL,
                    truncate_index INTEGER NOT NULL DEFAULT -1
                )
            """.trimIndent()
            )

            // 鑾峰彇鎵€鏈夊璇濊褰曞苟杞崲鏁版嵁
            val cursor =
                db.query("SELECT id, assistant_id, title, messages, usage, create_at, update_at, truncate_index FROM ConversationEntity")
            val updates = mutableListOf<Array<Any?>>()

            while (cursor.moveToNext()) {
                val id = cursor.getString(0)
                val assistantId = cursor.getString(1)
                val title = cursor.getString(2)
                val messagesJson = cursor.getString(3)
                val usage = cursor.getString(4)
                val createAt = cursor.getLong(5)
                val updateAt = cursor.getLong(6)
                val truncateIndex = cursor.getInt(7)

                try {
                    // 灏濊瘯瑙ｆ瀽鏃ф牸寮忕殑娑堟伅鍒楄〃 List<UIMessage>
                    val oldMessages = JsonInstant.decodeFromString<List<UIMessage>>(messagesJson)

                    // 杞崲涓烘柊鏍煎紡 List<MessageNode>
                    val newMessages = oldMessages.map { message ->
                        MessageNode.of(message)
                    }

                    // 搴忓垪鍖栨柊鏍煎紡
                    val newMessagesJson = JsonInstant.encodeToString(newMessages)
                    updates.add(
                        arrayOf(
                            id,
                            assistantId,
                            title,
                            newMessagesJson,
                            usage,
                            createAt,
                            updateAt,
                            truncateIndex
                        )
                    )
                } catch (e: Exception) {
                    // 濡傛灉瑙ｆ瀽澶辫触锛屽彲鑳藉凡缁忔槸鏂版牸寮忔垨鑰呮暟鎹崯鍧忥紝璺宠繃
                    error("Failed to migrate messages for conversation $id: ${e.message}")
                }
            }
            cursor.close()

            // 鎵归噺鎻掑叆鏁版嵁鍒版柊琛?
            updates.forEach { values ->
                db.execSQL(
                    "INSERT INTO ConversationEntity_new (id, assistant_id, title, nodes, usage, create_at, update_at, truncate_index) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    values
                )
            }

            // 鍒犻櫎鏃ц〃
            db.execSQL("DROP TABLE ConversationEntity")

            // 閲嶅懡鍚嶆柊琛?
            db.execSQL("ALTER TABLE ConversationEntity_new RENAME TO ConversationEntity")

            db.setTransactionSuccessful()

            Log.i(TAG, "migrate: migrate from 6 to 7 success (${updates.size} conversations updated)")
        } finally {
            db.endTransaction()
            DatabaseMigrationTracker.onMigrationEnd()
        }
    }
}

