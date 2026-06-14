package com.eterultimate.eteruee.plugin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class PluginRequest(
    val id: String? = null,
    val type: PluginMessageType,
    val method: String? = null,
    val params: JsonObject = JsonObject(emptyMap()),
    val topic: String? = null,
)

@Serializable
enum class PluginMessageType {
    @SerialName("call")
    Call,

    @SerialName("list_capabilities")
    ListCapabilities,

    @SerialName("subscribe")
    Subscribe,

    @SerialName("unsubscribe")
    Unsubscribe,
}

fun pluginResult(id: String?, result: JsonElement): JsonObject = buildJsonObject {
    id?.let { put("id", it) }
    put("type", "result")
    put("result", result)
}

fun pluginError(
    id: String?,
    code: String,
    message: String,
    details: JsonElement? = null,
): JsonObject = buildJsonObject {
    id?.let { put("id", it) }
    put("type", "error")
    put(
        "error",
        buildJsonObject {
            put("code", code)
            put("message", message)
            details?.let { put("details", it) }
        }
    )
}

fun pluginUnsupportedSubscriptionResult(topic: String?) = buildJsonObject {
    put("subscribed", false)
    put("topic", topic?.let { JsonPrimitive(it) } ?: JsonNull)
    put("reason", "No subscription topics are available in the plugin WebSocket MVP.")
}
