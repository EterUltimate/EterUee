package com.eterultimate.eteruee.data.db.fts

import android.util.Log
import androidx.room.support.getSupportWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.db.AppDatabase
import com.eterultimate.eteruee.data.model.Conversation
import com.eterultimate.eteruee.data.model.MessageNode
import java.time.Instant

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

    private val db get() = database.getSupportWrapper().also { MessageFtsSchema.ensure(it) }

    suspend fun indexConversation(conversation: Conversation) = withContext(Dispatchers.IO) {
        val conversationId = conversation.id.toString()
        db.execSQL("DELETE FROM message_fts WHERE conversation_id = ?", arrayOf(conversationId))
        conversation.messageNodes.forEach { node ->
            node.messages.forEach { message ->
                val text = message.extractFtsText()
                if (text.isNotBlank()) {
                    db.execSQL(
                        "INSERT INTO message_fts(text, node_id, message_id, conversation_id, title, update_at) VALUES (?, ?, ?, ?, ?, ?)",
                        arrayOf(
                            text,
                            node.id.toString(),
                            message.id.toString(),
                            conversationId,
                            conversation.title,
                            conversation.updateAt.toEpochMilli().toString(),
                        )
                    )
                }
            }
        }
    }

    suspend fun deleteConversation(conversationId: String) = withContext(Dispatchers.IO) {
        db.execSQL("DELETE FROM message_fts WHERE conversation_id = ?", arrayOf(conversationId))
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        db.execSQL("DELETE FROM message_fts")
    }

    suspend fun search(keyword: String): List<MessageSearchResult> = withContext(Dispatchers.IO) {
        val ftsQuery = keyword.toFtsQuery()
        if (ftsQuery.isBlank()) {
            return@withContext emptyList()
        }
        val results = mutableListOf<MessageSearchResult>()
        runCatching {
            db.query(
                """
                SELECT node_id, message_id, conversation_id, title, update_at,
                       snippet(message_fts, 0, '[', ']', '...', 30) AS snippet
                FROM message_fts
                WHERE text MATCH ?
                ORDER BY rank, update_at DESC
                LIMIT 50
                """.trimIndent(),
                arrayOf(ftsQuery)
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(cursor.toSearchResult())
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "search: FTS query failed, falling back to LIKE", error)
            results.clear()
        }
        if (results.isEmpty()) {
            results.addAll(searchByLike(keyword))
        }
        Log.i(TAG, "search: $keyword")
        results
    }

    private fun searchByLike(keyword: String): List<MessageSearchResult> {
        val results = mutableListOf<MessageSearchResult>()
        db.query(
            """
            SELECT node_id, message_id, conversation_id, title, update_at,
                   substr(text, 1, 200) AS snippet
            FROM message_fts
            WHERE text LIKE ? ESCAPE '\'
            ORDER BY update_at DESC
            LIMIT 50
            """.trimIndent(),
            arrayOf("%${keyword.escapeLike()}%")
        ).use { cursor ->
            while (cursor.moveToNext()) {
                results.add(cursor.toSearchResult())
            }
        }
        return results
    }
}

private fun android.database.Cursor.toSearchResult(): MessageSearchResult =
    MessageSearchResult(
        nodeId = getString(0),
        messageId = getString(1),
        conversationId = getString(2),
        title = getString(3),
        updateAt = Instant.ofEpochMilli(getLong(4)),
        snippet = getString(5),
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

