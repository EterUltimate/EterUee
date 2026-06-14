package com.eterultimate.eteruee.plugin

import com.eterultimate.eteruee.utils.JsonInstant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class PluginProtocolTest {
    @Test
    fun parsesCallRequestWithParams() {
        val request = JsonInstant.decodeFromString(
            PluginRequest.serializer(),
            """{"id":"1","type":"call","method":"conversation.list","params":{"limit":20}}"""
        )

        assertEquals("1", request.id)
        assertEquals(PluginMessageType.Call, request.type)
        assertEquals("conversation.list", request.method)
        assertEquals("20", request.params["limit"]?.jsonPrimitive?.content)
    }

    @Test
    fun resultEnvelopeUsesJsonRpcStyleShape() {
        val result = pluginResult(
            id = "1",
            result = buildJsonObject { put("status", "ok") },
        )

        assertEquals("1", result["id"]?.jsonPrimitive?.content)
        assertEquals("result", result["type"]?.jsonPrimitive?.content)
        assertEquals("ok", result["result"]?.jsonObject?.get("status")?.jsonPrimitive?.content)
    }

    @Test
    fun errorEnvelopeIncludesCodeAndMessage() {
        val error = pluginError(
            id = "1",
            code = "METHOD_NOT_FOUND",
            message = "Unknown capability",
        )

        assertEquals("error", error["type"]?.jsonPrimitive?.content)
        assertEquals("METHOD_NOT_FOUND", error["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content)
        assertEquals("Unknown capability", error["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content)
    }

    @Test
    fun permissionScopeRoundTripUsesStableStrings() {
        assertEquals(PluginPermission.ConversationRead, PluginPermission.fromScope("conversation:read"))
        assertEquals(PluginPermission.DeviceRead, PluginPermission.fromScope("device:read"))
        assertEquals(PluginPermission.DeviceControl, PluginPermission.fromScope("device:control"))
        assertEquals(null, PluginPermission.fromScope("unknown:scope"))
    }
}
