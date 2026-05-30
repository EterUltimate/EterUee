package com.eterultimate.eteruee.roleplay.data.tavern

import com.eterultimate.eteruee.roleplay.data.model.Preset
import com.eterultimate.eteruee.roleplay.data.model.PresetType
import com.eterultimate.eteruee.roleplay.data.serialization.RoleplayJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant

object TavernPresetCodec {
    fun decode(jsonString: String, fallbackName: String = "Imported Preset"): Preset {
        val root = RoleplayJson.parseToJsonElement(jsonString).jsonObject
        val parameterRoot = root["parameters"]?.let { it.jsonObjectOrNull() } ?: root
        val name = root.string("name")
            .ifBlank { root.string("preset_name") }
            .ifBlank { root.string("presetName") }
            .ifBlank { fallbackName }
        val description = root.string("description")
            .ifBlank { root.string("notes") }
        val type = root.string("type")
            .takeIf { it.isNotBlank() }
            ?.toPresetTypeOrNull()
            ?: detectPresetType(root)

        return Preset(
            name = name,
            description = description,
            type = type,
            parameters = parameterRoot.filterValues { it !is JsonNull },
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    fun encode(preset: Preset): String {
        val root = preset.parameters
            .mapValues { (_, value) -> value.toJsonElement() }
            .toMutableMap()
        root.putIfAbsent("name", JsonPrimitive(preset.name))
        if (preset.description.isNotBlank()) {
            root.putIfAbsent("description", JsonPrimitive(preset.description))
        }
        root.putIfAbsent("type", JsonPrimitive(preset.type.name))
        return RoleplayJson.encodeToString(JsonElement.serializer(), JsonObject(root))
    }

    private fun detectPresetType(root: JsonObject): PresetType {
        return when {
            root.containsKey("claude_use_sysprompt") || root.containsKey("assistant_prefill") -> PresetType.CLAUDE
            root.containsKey("use_makersuite_sysprompt") -> PresetType.GEMINI
            root.containsKey("kobold_max_context") -> PresetType.KOBOLDAI
            root.containsKey("textgen_max_context") -> PresetType.TEXTGEN
            else -> PresetType.OPENAI
        }
    }

    private fun String.toPresetTypeOrNull(): PresetType? {
        return PresetType.values().firstOrNull { it.name.equals(this, ignoreCase = true) }
    }

    private fun JsonObject.string(key: String): String {
        return get(key)?.jsonPrimitiveOrNull()?.contentOrNull.orEmpty()
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

    private fun Any.toJsonElement(): JsonElement {
        return when (this) {
            is JsonElement -> this
            is Boolean -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is String -> JsonPrimitive(this)
            else -> JsonPrimitive(toString())
        }
    }
}
