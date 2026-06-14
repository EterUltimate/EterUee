package com.eterultimate.eteruee.web.routes

import com.eterultimate.eteruee.plugin.AppCapabilityRegistry
import com.eterultimate.eteruee.plugin.PluginCallContext
import com.eterultimate.eteruee.plugin.PluginMessageType
import com.eterultimate.eteruee.plugin.PluginPermissionDeniedException
import com.eterultimate.eteruee.plugin.PluginProtocolException
import com.eterultimate.eteruee.plugin.PluginRequest
import com.eterultimate.eteruee.plugin.defaultPluginPermissions
import com.eterultimate.eteruee.plugin.pluginError
import com.eterultimate.eteruee.plugin.pluginResult
import com.eterultimate.eteruee.plugin.pluginUnsupportedSubscriptionResult
import com.eterultimate.eteruee.utils.JsonInstant
import com.eterultimate.eteruee.web.ApiException
import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import io.ktor.server.auth.principal
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket

fun Route.pluginRoutes(
    registry: AppCapabilityRegistry,
) {
    webSocket("/plugins/ws") {
        val pluginContext = PluginCallContext(
            connectionId = UUID.randomUUID().toString(),
            grantedPermissions = defaultPluginPermissions(),
            principal = call.principalNameOrNull(),
        )

        for (frame in incoming) {
            if (frame !is Frame.Text) {
                sendPluginError(
                    id = null,
                    code = "UNSUPPORTED_FRAME",
                    message = "Only text frames are supported."
                )
                continue
            }

            val request = try {
                JsonInstant.decodeFromString(PluginRequest.serializer(), frame.readText())
            } catch (e: SerializationException) {
                sendPluginError(
                    id = null,
                    code = "INVALID_REQUEST",
                    message = e.message ?: "Invalid plugin request."
                )
                continue
            } catch (e: IllegalArgumentException) {
                sendPluginError(
                    id = null,
                    code = "INVALID_REQUEST",
                    message = e.message ?: "Invalid plugin request."
                )
                continue
            }

            try {
                when (request.type) {
                    PluginMessageType.ListCapabilities -> {
                        sendPluginResult(
                            id = request.id,
                            result = buildJsonObject {
                                put(
                                    "items",
                                    JsonInstant.encodeToJsonElement(
                                        ListSerializer(
                                            com.eterultimate.eteruee.plugin.AppCapabilityDescriptor.serializer()
                                        ),
                                        registry.list()
                                    )
                                )
                            }
                        )
                    }

                    PluginMessageType.Call -> {
                        val method = request.method?.takeIf { it.isNotBlank() }
                            ?: throw PluginProtocolException("INVALID_REQUEST", "method is required for call messages")
                        val result = registry.execute(method, request.params, pluginContext)
                        sendPluginResult(request.id, result)
                    }

                    PluginMessageType.Subscribe,
                    PluginMessageType.Unsubscribe,
                        -> {
                        sendPluginResult(
                            id = request.id,
                            result = pluginUnsupportedSubscriptionResult(request.topic)
                        )
                    }
                }
            } catch (e: PluginPermissionDeniedException) {
                sendPluginError(
                    id = request.id,
                    code = "PERMISSION_DENIED",
                    message = e.message ?: "Permission denied",
                    details = buildJsonObject {
                        put(
                            "missing",
                            JsonArray(e.missing.map { JsonPrimitive(it.scope) })
                        )
                    }
                )
            } catch (e: PluginProtocolException) {
                sendPluginError(request.id, e.code, e.message)
            } catch (e: ApiException) {
                sendPluginError(
                    id = request.id,
                    code = e.status.value.toString(),
                    message = e.message
                )
            } catch (e: IllegalArgumentException) {
                sendPluginError(
                    id = request.id,
                    code = "INVALID_PARAMS",
                    message = e.message ?: "Invalid params"
                )
            } catch (e: Throwable) {
                sendPluginError(
                    id = request.id,
                    code = "INTERNAL_ERROR",
                    message = e.message ?: e.javaClass.simpleName
                )
            }
        }
    }
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendPluginResult(
    id: String?,
    result: kotlinx.serialization.json.JsonElement,
) {
    send(Frame.Text(JsonInstant.encodeToString(pluginResult(id, result))))
}

private suspend fun io.ktor.server.websocket.DefaultWebSocketServerSession.sendPluginError(
    id: String?,
    code: String,
    message: String,
    details: kotlinx.serialization.json.JsonElement? = null,
) {
    send(Frame.Text(JsonInstant.encodeToString(pluginError(id, code, message, details))))
}

private fun io.ktor.server.application.ApplicationCall.principalNameOrNull(): String? =
    principal<io.ktor.server.auth.jwt.JWTPrincipal>()?.payload?.subject
