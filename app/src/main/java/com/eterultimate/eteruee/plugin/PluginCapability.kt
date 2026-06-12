package com.eterultimate.eteruee.plugin

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

interface AppCapability {
    val id: String
    val description: String
    val inputSchema: JsonObject
    val permissions: Set<PluginPermission>

    suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement
}

data class PluginCallContext(
    val connectionId: String,
    val grantedPermissions: Set<PluginPermission>,
    val principal: String? = null,
) {
    fun requirePermissions(required: Set<PluginPermission>) {
        val missing = required - grantedPermissions
        if (missing.isNotEmpty()) {
            throw PluginPermissionDeniedException(missing)
        }
    }
}

class PluginProtocolException(
    val code: String,
    override val message: String,
) : RuntimeException(message)

class PluginPermissionDeniedException(
    val missing: Set<PluginPermission>,
) : RuntimeException("Missing plugin permissions: ${missing.joinToString { it.scope }}")

@Serializable
data class AppCapabilityDescriptor(
    val id: String,
    val description: String,
    val inputSchema: JsonObject,
    val permissions: List<String>,
)

fun AppCapability.toDescriptor() = AppCapabilityDescriptor(
    id = id,
    description = description,
    inputSchema = inputSchema,
    permissions = permissions.map { it.scope }.sorted(),
)
