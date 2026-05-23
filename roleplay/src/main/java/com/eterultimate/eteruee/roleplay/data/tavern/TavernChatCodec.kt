package com.eterultimate.eteruee.roleplay.data.tavern

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.data.model.ChatMetadata
import com.eterultimate.eteruee.roleplay.data.serialization.CompactRoleplayJson
import com.eterultimate.eteruee.roleplay.data.serialization.RoleplayJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale

object TavernChatCodec {
    private val sillyMessageDateFormatter: DateTimeFormatter =
        DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM d, yyyy h:mma")
            .toFormatter(Locale.US)

    fun decodeJsonl(lines: Sequence<String>, characterId: kotlin.uuid.Uuid, groupId: kotlin.uuid.Uuid? = null): TavernChatPayload {
        val values = lines.filter { it.isNotBlank() }
            .map { RoleplayJson.parseToJsonElement(it).jsonObject }
            .toList()

        val header = values.firstOrNull()?.takeIf { it.containsKey("user_name") || it.containsKey("chat_metadata") }
        val messages = values.drop(if (header != null) 1 else 0).map(::decodeMessage)
        val metadataJson = header?.get("chat_metadata")
        val metadata = ChatMetadata(
            characterId = characterId,
            groupId = groupId,
            title = header.string("file_name", header.string("create_date", "Imported Chat")),
            userName = header.string("user_name"),
            characterName = header.string("character_name"),
            createDate = header.string("create_date"),
            tavernChatId = metadataJson?.jsonObjectOrNull()?.string("chat_id_hash").orEmpty(),
            messageCount = messages.size,
            variables = metadataJson?.jsonObjectOrNull()?.get("variables")?.jsonObjectOrNull()
                ?.mapValues { it.value.jsonPrimitiveOrNull()?.contentOrNull.orEmpty() }
                ?: emptyMap(),
            tavernMetadata = metadataJson,
            extensions = metadataJson?.jsonObjectOrNull()?.get("extensions")?.jsonObjectOrNull()
                ?.filterValues { it !is JsonNull }
                ?: emptyMap()
        )

        return TavernChatPayload(metadata, messages)
    }

    fun encodeJsonl(metadata: ChatMetadata, messages: List<ChatMessage>): String {
        val header = JsonObject(
            linkedMapOf(
                "user_name" to JsonPrimitive(metadata.userName.ifBlank { "User" }),
                "character_name" to JsonPrimitive(metadata.characterName),
                "create_date" to JsonPrimitive(metadata.createDate.ifBlank { metadata.createdAt.toString() }),
                "chat_metadata" to buildChatMetadata(metadata)
            )
        )

        return buildString {
            appendLine(CompactRoleplayJson.encodeToString(JsonElement.serializer(), header))
            messages.forEach { message ->
                appendLine(CompactRoleplayJson.encodeToString(JsonElement.serializer(), encodeMessage(message, metadata)))
            }
        }
    }

    fun decodeMessage(value: JsonObject): ChatMessage {
        val extra = value["extra"]?.jsonObjectOrNull() ?: JsonObject(emptyMap())
        val isUser = value.boolean("is_user")
        val isSystem = value.boolean("is_system")
        val role = when {
            isSystem -> MessageRole.SYSTEM
            isUser -> MessageRole.USER
            else -> MessageRole.ASSISTANT
        }

        return ChatMessage(
            role = role,
            content = value.string("mes"),
            timestamp = parseSendDate(value["send_date"]),
            tavernName = value.string("name"),
            tavernSendDate = value.string("send_date"),
            model = extra.string("model").ifBlank { null },
            tokenCount = extra.int("token_count", null),
            swipeAlternatives = extra.stringList("swipes"),
            extra = extra.filterValues { it !is JsonNull }
        )
    }

    fun encodeMessage(message: ChatMessage, metadata: ChatMetadata): JsonObject {
        val extra = message.extra.toMutableMap().apply {
            message.model?.let { put("model", JsonPrimitive(it)) }
            message.tokenCount?.let { put("token_count", JsonPrimitive(it)) }
            if (message.swipeAlternatives.isNotEmpty()) {
                put("swipes", JsonArray(message.swipeAlternatives.map(::JsonPrimitive)))
            }
        }
        return JsonObject(
            linkedMapOf(
                "name" to JsonPrimitive(message.tavernName.ifBlank { message.defaultTavernName(metadata) }),
                "is_user" to JsonPrimitive(message.role == MessageRole.USER),
                "is_system" to JsonPrimitive(message.role == MessageRole.SYSTEM),
                "send_date" to JsonPrimitive(message.tavernSendDate.ifBlank { formatSendDate(message.timestamp) }),
                "mes" to JsonPrimitive(message.content),
                "extra" to JsonObject(extra)
            )
        )
    }

    private fun buildChatMetadata(metadata: ChatMetadata): JsonElement {
        val original = metadata.tavernMetadata?.jsonObjectOrNull()?.toMutableMap() ?: mutableMapOf()
        if (metadata.variables.isNotEmpty()) {
            original["variables"] = JsonObject(metadata.variables.mapValues { JsonPrimitive(it.value) })
        }
        if (metadata.extensions.isNotEmpty()) {
            original["extensions"] = JsonObject(metadata.extensions)
        }
        if (metadata.tavernChatId.isNotBlank()) {
            original["chat_id_hash"] = JsonPrimitive(metadata.tavernChatId)
        }
        return JsonObject(original)
    }

    private fun ChatMessage.defaultTavernName(metadata: ChatMetadata): String {
        return when (role) {
            MessageRole.USER -> metadata.userName.ifBlank { "User" }
            MessageRole.SYSTEM -> "System"
            else -> speakerName ?: metadata.characterName.ifBlank { "Assistant" }
        }
    }

    private fun parseSendDate(value: JsonElement?): Instant {
        val primitive = value?.jsonPrimitiveOrNull() ?: return Instant.now()
        primitive.contentOrNull?.trim()?.let { text ->
            if (text.isNotBlank()) {
                text.toLongOrNull()?.let { return normalizeEpochMillis(it) }
                runCatching { return Instant.parse(text) }
                runCatching {
                    return LocalDateTime.parse(text.uppercase(Locale.US), sillyMessageDateFormatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                }
                runCatching {
                    return LocalDateTime.parse(
                        text,
                        DateTimeFormatterBuilder()
                            .appendPattern("yyyy-MM-dd@HH'h'mm'm'ss's'")
                            .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
                            .toFormatter(Locale.US)
                    ).atZone(ZoneId.systemDefault()).toInstant()
                }
            }
        }
        return Instant.now()
    }

    private fun normalizeEpochMillis(value: Long): Instant {
        return if (kotlin.math.abs(value) < 1_000_000_000_000L) {
            Instant.ofEpochMilli(value * 1000)
        } else {
            Instant.ofEpochMilli(value)
        }
    }

    private fun formatSendDate(instant: Instant): String {
        return DateTimeFormatter.ofPattern("MMMM d, yyyy h:mma", Locale.US)
            .withZone(ZoneId.systemDefault())
            .format(instant)
            .lowercase(Locale.US)
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

    private fun JsonObject?.string(key: String, fallback: String = ""): String {
        return this?.get(key)?.jsonPrimitiveOrNull()?.contentOrNull ?: fallback
    }

    private fun JsonObject?.boolean(key: String, fallback: Boolean = false): Boolean {
        return this?.get(key)?.jsonPrimitiveOrNull()?.booleanOrNull ?: fallback
    }

    private fun JsonObject?.int(key: String, fallback: Int?): Int? {
        return this?.get(key)?.jsonPrimitiveOrNull()?.intOrNull
            ?: this?.get(key)?.jsonPrimitiveOrNull()?.contentOrNull?.toIntOrNull()
            ?: fallback
    }

    private fun JsonObject?.stringList(key: String): List<String> {
        val value = this?.get(key) ?: return emptyList()
        return when (value) {
            is JsonArray -> value.mapNotNull { it.jsonPrimitiveOrNull()?.contentOrNull?.trim() }
                .filter { it.isNotEmpty() }
            is JsonPrimitive -> value.contentOrNull
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: emptyList()
            else -> emptyList()
        }
    }
}

data class TavernChatPayload(
    val metadata: ChatMetadata,
    val messages: List<ChatMessage>
)
