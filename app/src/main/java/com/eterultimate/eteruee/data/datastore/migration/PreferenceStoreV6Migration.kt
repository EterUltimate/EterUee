package com.eterultimate.eteruee.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.eterultimate.eteruee.data.datastore.DEFAULT_ASSISTANT_ID
import com.eterultimate.eteruee.data.datastore.DEFAULT_JSHOOK_MCP_SERVER_ID
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.utils.JsonInstant
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val DEFAULT_ASSISTANT_ID_TEXT = "0950e2dc-9bd5-4801-afa3-aa887aa36b4e"
private const val DEFAULT_JSHOOK_MCP_SERVER_ID_TEXT = "27429a87-8db8-4f95-9f94-61ca3e82002e"

private val DEFAULT_LOCAL_TOOL_TYPES = listOf(
    "time_info",
    "shell",
    "linux_environment",
    "device_agent",
)

class PreferenceStoreV6Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 6
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()

        prefs[SettingsStore.ASSISTANTS] = mergeDefaultAssistantRuntimeAccess(
            prefs[SettingsStore.ASSISTANTS] ?: "[]",
        )
        prefs[SettingsStore.VERSION] = 6
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}

internal fun mergeDefaultAssistantRuntimeAccess(assistantsJson: String): String {
    return runCatching {
        val root = JsonInstant.parseToJsonElement(assistantsJson) as? JsonArray
            ?: return@runCatching assistantsJson

        val migrated = JsonArray(
            root.map { assistant ->
                val obj = assistant as? JsonObject ?: return@map assistant
                val id = (obj["id"] as? JsonPrimitive)?.content
                if (id != DEFAULT_ASSISTANT_ID.toString() && id != DEFAULT_ASSISTANT_ID_TEXT) {
                    return@map assistant
                }

                val updated = obj.toMutableMap()
                updated["mcpServers"] = mergeStringArray(
                    existing = obj["mcpServers"],
                    values = listOf(DEFAULT_JSHOOK_MCP_SERVER_ID.toString(), DEFAULT_JSHOOK_MCP_SERVER_ID_TEXT),
                    distinctKey = { it },
                )
                updated["localTools"] = mergeLocalTools(obj["localTools"])
                JsonObject(updated)
            }
        )

        if (migrated == root) assistantsJson else JsonInstant.encodeToString(migrated)
    }.getOrDefault(assistantsJson)
}

private fun mergeLocalTools(existing: JsonElement?): JsonArray {
    val existingArray = existing as? JsonArray ?: JsonArray(emptyList())
    val existingTypes = existingArray.mapNotNull(::localToolType).toSet()
    val additions = DEFAULT_LOCAL_TOOL_TYPES
        .filter { it !in existingTypes }
        .map { JsonObject(mapOf("type" to JsonPrimitive(it))) }
    return JsonArray(existingArray + additions)
}

private fun mergeStringArray(
    existing: JsonElement?,
    values: List<String>,
    distinctKey: (String) -> String,
): JsonArray {
    val existingArray = existing as? JsonArray ?: JsonArray(emptyList())
    val seen = existingArray
        .mapNotNull { (it as? JsonPrimitive)?.content }
        .map(distinctKey)
        .toMutableSet()
    val additions = values
        .filter { seen.add(distinctKey(it)) }
        .map { JsonPrimitive(it) }
    return JsonArray(existingArray + additions)
}

private fun localToolType(element: JsonElement): String? {
    val obj = element as? JsonObject ?: return null
    return (obj["type"] as? JsonPrimitive)?.content
}
