package com.eterultimate.eteruee.roleplay.data.tavern

import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.data.serialization.RoleplayJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.time.Instant

object TavernCharacterCodec {
    fun decode(jsonString: String): Character {
        val root = RoleplayJson.parseToJsonElement(jsonString).jsonObject
        if (root.containsKey("id") && root.containsKey("firstMessage")) {
            return RoleplayJson.decodeFromString<Character>(jsonString)
        }

        val data = root["data"]?.jsonObjectOrNull()
        val extensions = (data?.get("extensions") ?: root["extensions"])
            ?.jsonObjectOrNull()
            ?.filterValues { it !is JsonNull }
            ?: emptyMap()

        val spec = root.string("spec").ifBlank { "chara_card_v1" }
        val specVersion = root.string("spec_version").ifBlank {
            when (spec) {
                "chara_card_v3" -> "3.0"
                "chara_card_v2" -> "2.0"
                else -> "1.0"
            }
        }

        return Character(
            name = data.string("name", root.string("name")),
            description = data.string("description", root.string("description")),
            personality = data.string("personality", root.string("personality")),
            scenario = data.string("scenario", root.string("scenario")),
            firstMessage = data.string("first_mes", root.string("first_mes")),
            messageExamples = data.string("mes_example", root.string("mes_example")),
            systemPrompt = data.string("system_prompt", root.string("system_prompt")),
            postHistoryInstructions = data.string(
                "post_history_instructions",
                root.string("post_history_instructions")
            ),
            avatarUrl = root.string("avatar").ifBlank { null },
            creator = data.string("creator", root.string("creator")),
            creatorNotes = data.string("creator_notes", root.string("creator_notes")),
            tags = data.stringList("tags", root.stringList("tags")),
            talkativeness = extensions.mapFloat("talkativeness", root.float("talkativeness", 0.5f)),
            alternateGreetings = data.stringList(
                "alternate_greetings",
                root.stringList("alternate_greetings")
            ),
            characterVersion = data.string("character_version", root.string("character_version")),
            createdAt = root.parseCreateDate(),
            updatedAt = Instant.now(),
            favorite = extensions.mapBoolean("fav", root.boolean("fav")),
            spec = spec,
            specVersion = specVersion,
            characterBook = data?.get("character_book") ?: root["character_book"],
            extensions = extensions
        )
    }

    fun encode(character: Character, format: TavernCharacterCardFormat): String {
        val root = when (format) {
            TavernCharacterCardFormat.V1 -> buildV1(character)
            TavernCharacterCardFormat.V2 -> buildV2(character, "chara_card_v2", "2.0")
            TavernCharacterCardFormat.V3 -> buildV2(character, "chara_card_v3", "3.0")
        }
        return RoleplayJson.encodeToString(JsonElement.serializer(), root)
    }

    fun toJsonElement(character: Character, format: TavernCharacterCardFormat): JsonObject {
        return when (format) {
            TavernCharacterCardFormat.V1 -> buildV1(character)
            TavernCharacterCardFormat.V2 -> buildV2(character, "chara_card_v2", "2.0")
            TavernCharacterCardFormat.V3 -> buildV2(character, "chara_card_v3", "3.0")
        }
    }

    private fun buildV1(character: Character): JsonObject {
        return JsonObject(
            mapOf(
                "name" to JsonPrimitive(character.name),
                "description" to JsonPrimitive(character.description),
                "personality" to JsonPrimitive(character.personality),
                "scenario" to JsonPrimitive(character.scenario),
                "first_mes" to JsonPrimitive(character.firstMessage),
                "mes_example" to JsonPrimitive(character.messageExamples),
                "creator" to JsonPrimitive(character.creator),
                "creator_notes" to JsonPrimitive(character.creatorNotes),
                "character_version" to JsonPrimitive(character.characterVersion),
                "tags" to JsonArray(character.tags.map(::JsonPrimitive)),
                "talkativeness" to JsonPrimitive(character.talkativeness),
                "fav" to JsonPrimitive(character.favorite)
            )
        )
    }

    private fun buildV2(character: Character, spec: String, specVersion: String): JsonObject {
        val extensions = character.extensions.toMutableMap().apply {
            put("talkativeness", JsonPrimitive(character.talkativeness))
            put("fav", JsonPrimitive(character.favorite))
        }

        val data = linkedMapOf(
            "name" to JsonPrimitive(character.name),
            "description" to JsonPrimitive(character.description),
            "personality" to JsonPrimitive(character.personality),
            "scenario" to JsonPrimitive(character.scenario),
            "first_mes" to JsonPrimitive(character.firstMessage),
            "mes_example" to JsonPrimitive(character.messageExamples),
            "creator_notes" to JsonPrimitive(character.creatorNotes),
            "system_prompt" to JsonPrimitive(character.systemPrompt),
            "post_history_instructions" to JsonPrimitive(character.postHistoryInstructions),
            "alternate_greetings" to JsonArray(character.alternateGreetings.map(::JsonPrimitive)),
            "tags" to JsonArray(character.tags.map(::JsonPrimitive)),
            "creator" to JsonPrimitive(character.creator),
            "character_version" to JsonPrimitive(character.characterVersion),
            "extensions" to JsonObject(extensions)
        )

        character.characterBook?.let { data["character_book"] = ensureCharacterBookExtensions(it) }

        return JsonObject(
            linkedMapOf(
                "spec" to JsonPrimitive(spec),
                "spec_version" to JsonPrimitive(specVersion),
                "data" to JsonObject(data),
                "name" to JsonPrimitive(character.name),
                "description" to JsonPrimitive(character.description),
                "personality" to JsonPrimitive(character.personality),
                "scenario" to JsonPrimitive(character.scenario),
                "first_mes" to JsonPrimitive(character.firstMessage),
                "mes_example" to JsonPrimitive(character.messageExamples),
                "avatar" to JsonPrimitive(character.avatarUrl?.substringAfterLast('\\')?.substringAfterLast('/') ?: "none"),
                "chat" to JsonPrimitive(""),
                "creator" to JsonPrimitive(character.creator),
                "creator_notes" to JsonPrimitive(character.creatorNotes),
                "character_version" to JsonPrimitive(character.characterVersion),
                "tags" to JsonArray(character.tags.map(::JsonPrimitive)),
                "create_date" to JsonPrimitive(character.createdAt.toString()),
                "talkativeness" to JsonPrimitive(character.talkativeness),
                "fav" to JsonPrimitive(false)
            )
        )
    }

    private fun ensureCharacterBookExtensions(value: JsonElement): JsonElement {
        val book = value.jsonObjectOrNull() ?: return value
        if (book["extensions"] is JsonObject) return value
        return JsonObject(book + ("extensions" to JsonObject(emptyMap())))
    }

    private fun JsonObject?.string(key: String, fallback: String = ""): String {
        return this?.get(key)?.jsonPrimitiveOrNull()?.contentOrNull ?: fallback
    }

    private fun JsonObject?.boolean(key: String, fallback: Boolean = false): Boolean {
        return this?.get(key)?.jsonPrimitiveOrNull()?.booleanOrNull ?: fallback
    }

    private fun JsonObject?.float(key: String, fallback: Float = 0f): Float {
        val primitive = this?.get(key)?.jsonPrimitiveOrNull() ?: return fallback
        return primitive.floatOrNull ?: primitive.contentOrNull?.toFloatOrNull() ?: fallback
    }

    private fun Map<String, JsonElement>.mapBoolean(key: String, fallback: Boolean = false): Boolean {
        return get(key)?.jsonPrimitiveOrNull()?.booleanOrNull ?: fallback
    }

    private fun Map<String, JsonElement>.mapFloat(key: String, fallback: Float = 0f): Float {
        val primitive = get(key)?.jsonPrimitiveOrNull() ?: return fallback
        return primitive.floatOrNull ?: primitive.contentOrNull?.toFloatOrNull() ?: fallback
    }

    private fun JsonObject?.stringList(key: String, fallback: List<String> = emptyList()): List<String> {
        val value = this?.get(key) ?: return fallback
        return when (value) {
            is JsonArray -> value.mapNotNull { it.jsonPrimitiveOrNull()?.contentOrNull?.trim() }
                .filter { it.isNotEmpty() }
            is JsonPrimitive -> value.contentOrNull
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?: fallback
            else -> fallback
        }
    }

    private fun JsonObject.parseCreateDate(): Instant {
        return string("create_date")
            .takeIf { it.isNotBlank() }
            ?.let { runCatching { Instant.parse(it) }.getOrNull() }
            ?: Instant.now()
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement.jsonPrimitiveOrNull() = this as? JsonPrimitive
}
