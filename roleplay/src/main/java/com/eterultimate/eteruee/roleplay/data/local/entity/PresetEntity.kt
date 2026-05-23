package com.eterultimate.eteruee.roleplay.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 生成预设数据库实体
 */
@Entity(tableName = "rp_presets")
data class PresetEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val type: String,  // PresetType.name()
    val parametersJson: String,  // JSON字符串存储Map参数
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        fun fromModel(model: com.eterultimate.eteruee.roleplay.data.model.Preset): PresetEntity {
            return PresetEntity(
                id = model.id.toString(),
                name = model.name,
                description = model.description,
                type = model.type.name,
                parametersJson = json.encodeToString(JsonObject.serializer(), model.parameters.toJsonObject()),
                createdAt = model.createdAt.toEpochMilli(),
                updatedAt = model.updatedAt.toEpochMilli()
            )
        }
        
        fun toModel(entity: PresetEntity): com.eterultimate.eteruee.roleplay.data.model.Preset {
            val parameters = runCatching {
                json.decodeFromString(JsonObject.serializer(), entity.parametersJson).toPrimitiveMap()
            }.getOrDefault(emptyMap())
            return com.eterultimate.eteruee.roleplay.data.model.Preset(
                id = kotlin.uuid.Uuid.parse(entity.id),
                name = entity.name,
                description = entity.description,
                type = com.eterultimate.eteruee.roleplay.data.model.PresetType.valueOf(entity.type),
                parameters = parameters,
                createdAt = java.time.Instant.ofEpochMilli(entity.createdAt),
                updatedAt = java.time.Instant.ofEpochMilli(entity.updatedAt)
            )
        }

        private fun Map<String, Any>.toJsonObject(): JsonObject {
            return JsonObject(mapValues { (_, value) -> value.toJsonElement() })
        }

        private fun Any.toJsonElement(): JsonElement {
            return when (this) {
                is Boolean -> JsonPrimitive(this)
                is Number -> JsonPrimitive(this)
                is String -> JsonPrimitive(this)
                is JsonElement -> this
                else -> JsonPrimitive(toString())
            }
        }

        private fun JsonObject.toPrimitiveMap(): Map<String, Any> {
            return mapValues { (_, value) ->
                if (value == JsonNull) {
                    ""
                } else {
                    val primitive = value.jsonPrimitive
                    primitive.booleanOrNull
                        ?: primitive.longOrNull
                        ?: primitive.doubleOrNull
                        ?: primitive.toString().trim('"')
                }
            }
        }
    }
}
