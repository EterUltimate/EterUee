package com.eterultimate.eteruee.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.utils.JsonInstant
import kotlin.uuid.Uuid

class PreferenceStoreV3Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 3
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()

        val (migratedAssistants, extractedQuickMessages) =
            migrateAssistantsQuickMessages(prefs[SettingsStore.ASSISTANTS] ?: "[]")

        prefs[SettingsStore.ASSISTANTS] = migratedAssistants

        // 鍚堝苟宸叉湁鐨勫叏灞€蹇嵎娑堟伅锛堥槻姝㈤噸澶嶏級
        val existingQuickMessages = prefs[SettingsStore.QUICK_MESSAGES]?.let { json ->
            runCatching<JsonArray> {
                JsonInstant.parseToJsonElement(json).jsonArray
            }.getOrElse { JsonArray(emptyList()) }
        } ?: JsonArray(emptyList())

        val existingIds = existingQuickMessages.mapNotNull {
            (it as? JsonObject)?.get("id")?.toString()?.trim('"')
        }.toSet()

        val merged = JsonArray(
            existingQuickMessages + extractedQuickMessages.filter { element ->
                val id = (element as? JsonObject)?.get("id")?.toString()?.trim('"')
                id != null && id !in existingIds
            }
        )

        prefs[SettingsStore.QUICK_MESSAGES] = JsonInstant.encodeToString(merged)
        prefs[SettingsStore.VERSION] = 3

        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}

/**
 * 浠庢棫鏍煎紡 assistants JSON 涓彁鍙?quickMessages 瀛楁锛堝畬鏁村璞★紝鏃?id 瀛楁锛夛紝
 * 涓烘瘡鏉℃秷鎭敓鎴愭柊 UUID锛屽皢鍏舵浛鎹负 quickMessageIds锛堜粎 ID 鍒楄〃锛夛紝
 * 骞惰繑鍥炶ˉ鍏呬簡 id 鐨勫叏灞€娑堟伅鍒楄〃銆?
 */
internal fun migrateAssistantsQuickMessages(
    assistantsJson: String
): Pair<String, JsonArray> {
    return runCatching {
        val root = JsonInstant.parseToJsonElement(assistantsJson) as? JsonArray
            ?: return@runCatching assistantsJson to JsonArray(emptyList())

        val allQuickMessages = mutableListOf<JsonElement>()

        val migratedAssistants = JsonArray(
            root.map { assistant ->
                val assistantObj = assistant as? JsonObject
                    ?: return@map assistant

                // 濡傛灉涓嶅瓨鍦ㄦ棫鐨?quickMessages 瀛楁鍒欐棤闇€杩佺Щ
                val oldQuickMessages = assistantObj["quickMessages"] as? JsonArray
                    ?: return@map assistant

                // 涓烘瘡鏉℃棫娑堟伅娉ㄥ叆鏂扮敓鎴愮殑 id
                val messagesWithIds = oldQuickMessages.map { element ->
                    val obj = element as? JsonObject ?: return@map element
                    val newId = Uuid.random().toString()
                    JsonObject(obj.toMutableMap().apply {
                        put("id", JsonPrimitive(newId))
                    })
                }

                // 鏀堕泦鍒板叏灞€鍒楄〃
                allQuickMessages.addAll(messagesWithIds)

                // 鎻愬彇 ID 鍒楄〃鏋勫缓 quickMessageIds
                val ids = JsonArray(
                    messagesWithIds.mapNotNull { element ->
                        (element as? JsonObject)?.get("id")
                    }
                )

                JsonObject(
                    assistantObj.toMutableMap().apply {
                        remove("quickMessages")
                        put("quickMessageIds", ids)
                    }
                )
            }
        )

        JsonInstant.encodeToString(migratedAssistants) to JsonArray(allQuickMessages)
    }.getOrElse { assistantsJson to JsonArray(emptyList()) }
}

