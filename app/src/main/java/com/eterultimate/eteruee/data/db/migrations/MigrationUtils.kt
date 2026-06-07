package com.eterultimate.eteruee.data.db.migrations

import androidx.room.PooledConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import com.eterultimate.eteruee.utils.JsonInstant
import com.eterultimate.eteruee.utils.jsonPrimitiveOrNull

internal fun SQLiteConnection.execSQL(sql: String, bindArgs: Array<out Any?> = emptyArray()) {
    prepare(sql).use { statement ->
        statement.bindArgs(bindArgs)
        statement.step()
    }
}

internal inline fun <T> SQLiteConnection.query(
    sql: String,
    bindArgs: Array<out Any?> = emptyArray(),
    block: (SQLiteStatement) -> T,
): T {
    return prepare(sql).use { statement ->
        statement.bindArgs(bindArgs)
        block(statement)
    }
}

internal suspend fun PooledConnection.execSQL(sql: String, bindArgs: Array<out Any?> = emptyArray()) {
    usePrepared(sql) { statement ->
        statement.bindArgs(bindArgs)
        statement.step()
    }
}

internal suspend inline fun <T> PooledConnection.query(
    sql: String,
    bindArgs: Array<out Any?> = emptyArray(),
    crossinline block: (SQLiteStatement) -> T,
): T {
    return usePrepared(sql) { statement ->
        statement.bindArgs(bindArgs)
        block(statement)
    }
}

internal fun SQLiteStatement.bindArgs(bindArgs: Array<out Any?>) {
    bindArgs.forEachIndexed { index, arg ->
        val bindIndex = index + 1
        when (arg) {
            null -> bindNull(bindIndex)
            is Boolean -> bindBoolean(bindIndex, arg)
            is Int -> bindInt(bindIndex, arg)
            is Long -> bindLong(bindIndex, arg)
            is Float -> bindFloat(bindIndex, arg)
            is Double -> bindDouble(bindIndex, arg)
            is String -> bindText(bindIndex, arg)
            is ByteArray -> bindBlob(bindIndex, arg)
            else -> error("Unsupported SQLite bind argument type: ${arg::class.qualifiedName}")
        }
    }
}

internal fun SQLiteStatement.getTextOrNull(index: Int): String? =
    if (isNull(index)) null else getText(index)

internal val partTypeMapping = mapOf(
    "Text" to "text",
    "UIMessagePart.Text" to "text",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.Text" to "text",
    "Image" to "image",
    "UIMessagePart.Image" to "image",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.Image" to "image",
    "Video" to "video",
    "UIMessagePart.Video" to "video",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.Video" to "video",
    "Audio" to "audio",
    "UIMessagePart.Audio" to "audio",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.Audio" to "audio",
    "Document" to "document",
    "UIMessagePart.Document" to "document",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.Document" to "document",
    "Reasoning" to "reasoning",
    "UIMessagePart.Reasoning" to "reasoning",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.Reasoning" to "reasoning",
    "Search" to "search",
    "UIMessagePart.Search" to "search",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.Search" to "search",
    "ToolCall" to "tool_call",
    "UIMessagePart.ToolCall" to "tool_call",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.ToolCall" to "tool_call",
    "ToolResult" to "tool_result",
    "UIMessagePart.ToolResult" to "tool_result",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.ToolResult" to "tool_result",
    "Tool" to "tool",
    "UIMessagePart.Tool" to "tool",
    "com.eterultimate.eteruee.ai.ui.UIMessagePart.Tool" to "tool",
)

internal fun migrateMessagesJson(messagesJson: String): String {
    return runCatching {
        val element = JsonInstant.parseToJsonElement(messagesJson)
        val migrated = migrateMessagesElement(element)
        if (migrated == element) messagesJson else JsonInstant.encodeToString(migrated)
    }.getOrElse { messagesJson }
}

internal fun migrateMessagesElement(element: JsonElement): JsonElement {
    val rootArray = element as? JsonArray ?: return element
    val migratedArray = JsonArray(
        rootArray.map { message ->
            val messageObject = message as? JsonObject ?: return@map message
            val partsElement = messageObject["parts"] as? JsonArray ?: return@map message
            val migratedParts = migratePartsArray(partsElement)
            if (migratedParts == partsElement) {
                message
            } else {
                JsonObject(messageObject.toMutableMap().apply {
                    put("parts", migratedParts)
                })
            }
        }
    )
    return if (migratedArray == rootArray) element else migratedArray
}

internal fun migratePartsArray(partsElement: JsonArray): JsonArray {
    return JsonArray(
        partsElement.map { part ->
            val partObject = part as? JsonObject ?: return@map part
            val typeValue = partObject["type"]?.jsonPrimitiveOrNull?.contentOrNull
            val mappedType = typeValue?.let { partTypeMapping[it] } ?: typeValue

            var updatedPart: JsonElement = part
            if (mappedType != null && mappedType != typeValue) {
                updatedPart = JsonObject(partObject.toMutableMap().apply {
                    put("type", JsonPrimitive(mappedType))
                })
            }

            val updatedObject = updatedPart as JsonObject
            val outputElement = updatedObject["output"] as? JsonArray ?: return@map updatedPart
            val migratedOutput = migratePartsArray(outputElement)
            if (migratedOutput == outputElement) {
                updatedPart
            } else {
                JsonObject(updatedObject.toMutableMap().apply {
                    put("output", migratedOutput)
                })
            }
        }
    )
}

