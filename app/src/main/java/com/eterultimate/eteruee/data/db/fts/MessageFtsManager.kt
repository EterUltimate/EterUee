package com.eterultimate.eteruee.data.db.fts

import android.util.Log
import androidx.room.PooledConnection
import androidx.room.useWriterConnection
import androidx.sqlite.SQLiteStatement
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.db.AppDatabase
import com.eterultimate.eteruee.data.db.migrations.execSQL
import com.eterultimate.eteruee.data.db.migrations.query
import com.eterultimate.eteruee.data.model.Conversation
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MessageSearchResult(
    val nodeId: String,
    val messageId: String,
    val conversationId: String,
    val title: String,
    val updateAt: Instant,
    val snippet: String,
)

private const val TAG = "MessageFtsManager"

class MessageFtsManager(private val database: AppDatabase) {

    suspend fun indexConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        val conversationId = conversation.id.toString()
        database.useWriterConnection { connection ->
            MessageFtsSchema.ensure(connection)
            connection.execSQL("DELETE FROM message_fts WHERE conversation_id = ?", arrayOf(conversationId))
            conversation.messageNodes.forEach { node ->
                node.messages.forEach { message ->
                    val text = message.extractFtsText()
                    if (text.isNotBlank()) {
                        connection.execSQL(
                            "INSERT INTO message_fts(text, node_id, message_id, conversation_id, title, update_at) VALUES (?, ?, ?, ?, ?, ?)",
                            arrayOf<Any?>(
                                text,
                                node.id.toString(),
                                message.id.toString(),
                                conversationId,
                                conversation.title,
                                conversation.updateAt.toEpochMilli(),
                            )
                        )
                    }
                }
            }
        }
    }

    suspend fun deleteConversation(conversationId: String) = withContext(Dispatchers.IO) {
        database.useWriterConnection { connection ->
            MessageFtsSchema.ensure(connection)
            connection.execSQL("DELETE FROM message_fts WHERE conversation_id = ?", arrayOf(conversationId))
        }
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        database.useWriterConnection { connection ->
            MessageFtsSchema.ensure(connection)
            connection.execSQL("DELETE FROM message_fts")
        }
    }

    suspend fun search(keyword: String): List<MessageSearchResult> = withContext(Dispatchers.IO) {
        val ftsQuery = keyword.toFtsQuery()
        if (ftsQuery.isBlank()) {
            return@withContext emptyList()
        }

        val results = mutableListOf<MessageSearchResult>()
        database.useWriterConnection { connection ->
            MessageFtsSchema.ensure(connection)
            runCatching {
                connection.query(
                    """
                    SELECT node_id, message_id, conversation_id, title, update_at,
                           snippet(message_fts, 0, '[', ']', '...', 30) AS snippet
                    FROM message_fts
                    WHERE text MATCH ?
                    ORDER BY rank, update_at DESC
                    LIMIT 50
                    """.trimIndent(),
                    arrayOf(ftsQuery)
                ) { statement ->
                    while (statement.step()) {
                        results.add(statement.toSearchResult())
                    }
                }
            }.onFailure { error ->
                Log.w(TAG, "search: FTS query failed, falling back to LIKE", error)
                results.clear()
            }

            if (results.isEmpty()) {
                results.addAll(searchByLike(connection, keyword))
            }
        }

        Log.i(TAG, "search: $keyword")
        results
    }

    private suspend fun searchByLike(connection: PooledConnection, keyword: String): List<MessageSearchResult> {
        val results = mutableListOf<MessageSearchResult>()
        connection.query(
            """
            SELECT node_id, message_id, conversation_id, title, update_at,
                   substr(text, 1, 200) AS snippet
            FROM message_fts
            WHERE text LIKE ? ESCAPE '\'
            ORDER BY update_at DESC
            LIMIT 50
            """.trimIndent(),
            arrayOf("%${keyword.escapeLike()}%")
        ) { statement ->
            while (statement.step()) {
                results.add(statement.toSearchResult())
            }
        }
        return results
    }
}

private fun SQLiteStatement.toSearchResult(): MessageSearchResult =
    MessageSearchResult(
        nodeId = getText(0),
        messageId = getText(1),
        conversationId = getText(2),
        title = getText(3),
        updateAt = Instant.ofEpochMilli(getLong(4)),
        snippet = getText(5),
    )

private fun UIMessage.extractFtsText(): String =
    parts.filterIsInstance<UIMessagePart.Text>()
        .joinToString("\n") { it.text }
        .take(10_000)

private fun String.toFtsQuery(): String =
    trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" AND ") { "\"${it.replace("\"", "\"\"")}\"" }

private fun String.escapeLike(): String =
    replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
