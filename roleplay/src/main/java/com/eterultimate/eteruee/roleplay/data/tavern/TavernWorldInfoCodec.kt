package com.eterultimate.eteruee.roleplay.data.tavern

import com.eterultimate.eteruee.roleplay.data.model.InsertionPosition
import com.eterultimate.eteruee.roleplay.data.model.SelectiveLogic
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.data.serialization.RoleplayJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import java.time.Instant

object TavernWorldInfoCodec {
    fun decode(jsonString: String, fallbackName: String = "Imported Lorebook"): WorldInfo {
        val root = RoleplayJson.parseToJsonElement(jsonString).jsonObject
        if (root.containsKey("id") && root["entries"] is JsonArray) {
            return RoleplayJson.decodeFromString<WorldInfo>(jsonString)
        }

        val entries = decodeEntries(root["entries"])
        val extensions = root["extensions"]?.jsonObjectOrNull()?.filterValues { it !is JsonNull } ?: emptyMap()

        return WorldInfo(
            name = root.string("name", fallbackName),
            description = root.string("description"),
            entries = entries,
            scanDepth = extensions.int("scan_depth", root.int("scanDepth", 4)) ?: 4,
            selectiveLogic = if (entries.any { it.secondaryKeys.isNotEmpty() }) {
                SelectiveLogic.AND
            } else {
                SelectiveLogic.OR
            },
            extensions = extensions,
            originalData = root,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }

    fun encodeWorldInfo(worldInfo: WorldInfo): String {
        return RoleplayJson.encodeToString(JsonElement.serializer(), toWorldInfoJson(worldInfo))
    }

    fun encodeCharacterBook(worldInfo: WorldInfo): JsonObject {
        val original = worldInfo.originalData?.jsonObjectOrNull()?.toMutableMap() ?: mutableMapOf()
        original["name"] = JsonPrimitive(worldInfo.name)
        if (worldInfo.description.isNotBlank()) {
            original["description"] = JsonPrimitive(worldInfo.description)
        }
        original["extensions"] = original["extensions"]?.jsonObjectOrNull() ?: JsonObject(worldInfo.extensions)
        original["entries"] = JsonArray(worldInfo.entries.mapIndexed { index, entry ->
            toCharacterBookEntry(entry, index)
        })
        return JsonObject(original)
    }

    fun toWorldInfoJson(worldInfo: WorldInfo): JsonObject {
        val entries = worldInfo.entries.associate { entry ->
            val uid = entry.extensions.int("uid", null) ?: entry.displayIndex.takeIf { it > 0 } ?: entry.order
            uid.toString() to toWorldInfoEntry(entry, uid)
        }
        val root = linkedMapOf<String, JsonElement>(
            "entries" to JsonObject(entries),
            "extensions" to JsonObject(worldInfo.extensions)
        )
        worldInfo.originalData?.let { root["originalData"] = it }
        return JsonObject(root)
    }

    private fun decodeEntries(value: JsonElement?): List<WorldInfoEntry> {
        return when (value) {
            is JsonArray -> value.mapIndexed { index, entry ->
                fromCharacterBookEntry(entry.jsonObject, index)
            }
            is JsonObject -> value.values.mapIndexed { index, entry ->
                fromWorldInfoEntry(entry.jsonObject, index)
            }
            else -> emptyList()
        }
    }

    private fun fromCharacterBookEntry(entry: JsonObject, index: Int): WorldInfoEntry {
        val extensions = entry["extensions"]?.jsonObjectOrNull()?.filterValues { it !is JsonNull } ?: emptyMap()
        val tavernPosition = extensions.int("position", null)
            ?: if (entry.string("position") == "before_char") 0 else 1
        val probability = extensions.float("probability", 100f)

        return WorldInfoEntry(
            key = entry.stringList("keys").firstOrNull().orEmpty(),
            keys = entry.stringList("keys"),
            secondaryKeys = entry.stringList("secondary_keys"),
            comment = entry.string("comment"),
            content = entry.string("content"),
            constant = entry.boolean("constant"),
            selective = entry.boolean("selective"),
            order = entry.int("insertion_order", 100),
            position = tavernPosition.toInsertionPosition(),
            tavernPosition = tavernPosition,
            enabled = entry.boolean("enabled"),
            probability = normalizeProbability(probability),
            useProbability = extensions.boolean("useProbability", true),
            depth = extensions.int("depth", 4) ?: 4,
            role = extensions.int("role", 0) ?: 0,
            displayIndex = extensions.int("display_index", index) ?: index,
            excludeRecursion = extensions.boolean("exclude_recursion"),
            preventRecursion = extensions.boolean("prevent_recursion"),
            delayUntilRecursion = extensions.boolean("delay_until_recursion"),
            outletName = extensions.string("outlet_name"),
            group = extensions.string("group"),
            groupOverride = extensions.boolean("group_override"),
            groupWeight = extensions["group_weight"],
            scanDepthOverride = extensions["scan_depth"],
            caseSensitive = extensions["case_sensitive"],
            matchWholeWords = extensions["match_whole_words"],
            useGroupScoring = extensions["use_group_scoring"],
            automationId = extensions.string("automation_id"),
            vectorized = extensions.boolean("vectorized"),
            sticky = extensions["sticky"],
            cooldown = extensions["cooldown"],
            delay = extensions["delay"],
            triggers = extensions.stringList("triggers"),
            ignoreBudget = extensions.boolean("ignore_budget"),
            matchPersonaDescription = extensions.boolean("match_persona_description"),
            matchCharacterDescription = extensions.boolean("match_character_description"),
            matchCharacterPersonality = extensions.boolean("match_character_personality"),
            matchCharacterDepthPrompt = extensions.boolean("match_character_depth_prompt"),
            matchScenario = extensions.boolean("match_scenario"),
            matchCreatorNotes = extensions.boolean("match_creator_notes"),
            extensions = extensions + ("uid" to JsonPrimitive(entry.int("id", index)))
        )
    }

    private fun fromWorldInfoEntry(entry: JsonObject, index: Int): WorldInfoEntry {
        val extensions = entry["extensions"]?.jsonObjectOrNull()?.filterValues { it !is JsonNull } ?: emptyMap()
        val tavernPosition = entry.int("position", 1)
        val keys = entry.stringList("key")

        return WorldInfoEntry(
            key = keys.firstOrNull().orEmpty(),
            keys = keys,
            secondaryKeys = entry.stringList("keysecondary"),
            comment = entry.string("comment"),
            content = entry.string("content"),
            constant = entry.boolean("constant"),
            selective = entry.boolean("selective"),
            order = entry.int("order", 100),
            position = tavernPosition.toInsertionPosition(),
            tavernPosition = tavernPosition,
            enabled = !entry.boolean("disable"),
            probability = normalizeProbability(entry.float("probability", 100f)),
            useProbability = entry.boolean("useProbability"),
            depth = entry.int("depth", 4),
            role = entry.int("role", 0),
            displayIndex = entry.int("displayIndex", index),
            excludeRecursion = entry.boolean("excludeRecursion"),
            preventRecursion = entry.boolean("preventRecursion"),
            delayUntilRecursion = entry.boolean("delayUntilRecursion"),
            outletName = entry.string("outletName"),
            group = entry.string("group"),
            groupOverride = entry.boolean("groupOverride"),
            groupWeight = entry["groupWeight"],
            scanDepthOverride = entry["scanDepth"],
            caseSensitive = entry["caseSensitive"],
            matchWholeWords = entry["matchWholeWords"],
            useGroupScoring = entry["useGroupScoring"],
            automationId = entry.string("automationId"),
            vectorized = entry.boolean("vectorized"),
            sticky = entry["sticky"],
            cooldown = entry["cooldown"],
            delay = entry["delay"],
            triggers = entry.stringList("triggers"),
            ignoreBudget = entry.boolean("ignoreBudget"),
            matchPersonaDescription = entry.boolean("matchPersonaDescription"),
            matchCharacterDescription = entry.boolean("matchCharacterDescription"),
            matchCharacterPersonality = entry.boolean("matchCharacterPersonality"),
            matchCharacterDepthPrompt = entry.boolean("matchCharacterDepthPrompt"),
            matchScenario = entry.boolean("matchScenario"),
            matchCreatorNotes = entry.boolean("matchCreatorNotes"),
            extensions = extensions + ("uid" to JsonPrimitive(entry.int("uid", index)))
        )
    }

    private fun toWorldInfoEntry(entry: WorldInfoEntry, uid: Int): JsonObject {
        return JsonObject(
            linkedMapOf(
                "uid" to JsonPrimitive(uid),
                "key" to JsonArray(entry.getAllKeys().map(::JsonPrimitive)),
                "keysecondary" to JsonArray(entry.secondaryKeys.map(::JsonPrimitive)),
                "comment" to JsonPrimitive(entry.comment),
                "content" to JsonPrimitive(entry.content),
                "constant" to JsonPrimitive(entry.constant),
                "selective" to JsonPrimitive(entry.selective),
                "order" to JsonPrimitive(entry.order),
                "position" to JsonPrimitive(entry.tavernPosition),
                "disable" to JsonPrimitive(!entry.enabled),
                "addMemo" to JsonPrimitive(entry.comment.isNotBlank()),
                "excludeRecursion" to JsonPrimitive(entry.excludeRecursion),
                "preventRecursion" to JsonPrimitive(entry.preventRecursion),
                "delayUntilRecursion" to JsonPrimitive(entry.delayUntilRecursion),
                "displayIndex" to JsonPrimitive(entry.displayIndex),
                "probability" to JsonPrimitive((entry.probability * 100).toInt()),
                "useProbability" to JsonPrimitive(entry.useProbability),
                "depth" to JsonPrimitive(entry.depth),
                "selectiveLogic" to JsonPrimitive(0),
                "outletName" to JsonPrimitive(entry.outletName),
                "group" to JsonPrimitive(entry.group),
                "groupOverride" to JsonPrimitive(entry.groupOverride),
                "groupWeight" to (entry.groupWeight ?: JsonNull),
                "scanDepth" to (entry.scanDepthOverride ?: JsonNull),
                "caseSensitive" to (entry.caseSensitive ?: JsonNull),
                "matchWholeWords" to (entry.matchWholeWords ?: JsonNull),
                "useGroupScoring" to (entry.useGroupScoring ?: JsonNull),
                "automationId" to JsonPrimitive(entry.automationId),
                "role" to JsonPrimitive(entry.role),
                "vectorized" to JsonPrimitive(entry.vectorized),
                "sticky" to (entry.sticky ?: JsonNull),
                "cooldown" to (entry.cooldown ?: JsonNull),
                "delay" to (entry.delay ?: JsonNull),
                "triggers" to JsonArray(entry.triggers.map(::JsonPrimitive)),
                "ignoreBudget" to JsonPrimitive(entry.ignoreBudget),
                "matchPersonaDescription" to JsonPrimitive(entry.matchPersonaDescription),
                "matchCharacterDescription" to JsonPrimitive(entry.matchCharacterDescription),
                "matchCharacterPersonality" to JsonPrimitive(entry.matchCharacterPersonality),
                "matchCharacterDepthPrompt" to JsonPrimitive(entry.matchCharacterDepthPrompt),
                "matchScenario" to JsonPrimitive(entry.matchScenario),
                "matchCreatorNotes" to JsonPrimitive(entry.matchCreatorNotes),
                "extensions" to JsonObject(entry.extensions.filterKeys { it != "uid" })
            )
        )
    }

    private fun toCharacterBookEntry(entry: WorldInfoEntry, index: Int): JsonObject {
        val extensions = entry.extensions.filterKeys { it != "uid" }.toMutableMap().apply {
            put("position", JsonPrimitive(entry.tavernPosition))
            put("probability", JsonPrimitive((entry.probability * 100).toInt()))
            put("useProbability", JsonPrimitive(entry.useProbability))
            put("depth", JsonPrimitive(entry.depth))
            put("role", JsonPrimitive(entry.role))
            put("display_index", JsonPrimitive(entry.displayIndex))
            put("triggers", JsonArray(entry.triggers.map(::JsonPrimitive)))
        }
        return JsonObject(
            linkedMapOf(
                "id" to JsonPrimitive(index),
                "keys" to JsonArray(entry.getAllKeys().map(::JsonPrimitive)),
                "secondary_keys" to JsonArray(entry.secondaryKeys.map(::JsonPrimitive)),
                "comment" to JsonPrimitive(entry.comment),
                "content" to JsonPrimitive(entry.content),
                "constant" to JsonPrimitive(entry.constant),
                "selective" to JsonPrimitive(entry.selective),
                "insertion_order" to JsonPrimitive(entry.order),
                "enabled" to JsonPrimitive(entry.enabled),
                "position" to JsonPrimitive(if (entry.tavernPosition == 0) "before_char" else "after_char"),
                "use_regex" to JsonPrimitive(true),
                "extensions" to JsonObject(extensions)
            )
        )
    }

    private fun normalizeProbability(value: Float): Float {
        return if (value > 1f) (value / 100f).coerceIn(0f, 1f) else value.coerceIn(0f, 1f)
    }

    private fun Int.toInsertionPosition(): InsertionPosition {
        return when (this) {
            0 -> InsertionPosition.BEFORE_LAST_USER_MESSAGE
            1 -> InsertionPosition.AFTER_SYSTEM_PROMPT
            else -> InsertionPosition.AT_END
        }
    }

    private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

    private fun JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? = this as? JsonPrimitive

    private fun JsonObject.string(key: String, fallback: String = ""): String {
        return get(key)?.jsonPrimitiveOrNull()?.contentOrNull ?: fallback
    }

    private fun JsonObject.boolean(key: String, fallback: Boolean = false): Boolean {
        return get(key)?.jsonPrimitiveOrNull()?.booleanOrNull ?: fallback
    }

    private fun JsonObject.int(key: String, fallback: Int): Int {
        return get(key)?.jsonPrimitiveOrNull()?.intOrNull
            ?: get(key)?.jsonPrimitiveOrNull()?.contentOrNull?.toIntOrNull()
            ?: fallback
    }

    private fun Map<String, JsonElement>.int(key: String, fallback: Int?): Int? {
        return get(key)?.jsonPrimitiveOrNull()?.intOrNull
            ?: get(key)?.jsonPrimitiveOrNull()?.contentOrNull?.toIntOrNull()
            ?: fallback
    }

    private fun JsonObject.float(key: String, fallback: Float): Float {
        return get(key)?.jsonPrimitiveOrNull()?.floatOrNull
            ?: get(key)?.jsonPrimitiveOrNull()?.contentOrNull?.toFloatOrNull()
            ?: fallback
    }

    private fun Map<String, JsonElement>.float(key: String, fallback: Float): Float {
        return get(key)?.jsonPrimitiveOrNull()?.floatOrNull
            ?: get(key)?.jsonPrimitiveOrNull()?.contentOrNull?.toFloatOrNull()
            ?: fallback
    }

    private fun Map<String, JsonElement>.boolean(key: String, fallback: Boolean = false): Boolean {
        return get(key)?.jsonPrimitiveOrNull()?.booleanOrNull ?: fallback
    }

    private fun Map<String, JsonElement>.string(key: String, fallback: String = ""): String {
        return get(key)?.jsonPrimitiveOrNull()?.contentOrNull ?: fallback
    }

    private fun JsonObject.stringList(key: String): List<String> {
        val value = get(key) ?: return emptyList()
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

    private fun Map<String, JsonElement>.stringList(key: String): List<String> {
        val value = get(key) ?: return emptyList()
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
