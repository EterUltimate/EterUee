package com.eterultimate.eteruee.plugin

class AppCapabilityRegistry(
    capabilities: List<AppCapability>,
) {
    private val byId: Map<String, AppCapability> = capabilities.associateBy { it.id }

    fun list(): List<AppCapabilityDescriptor> =
        byId.values.sortedBy { it.id }.map { it.toDescriptor() }

    suspend fun execute(method: String, input: kotlinx.serialization.json.JsonObject, context: PluginCallContext) =
        get(method).run {
            context.requirePermissions(permissions)
            execute(input, context)
        }

    fun get(id: String): AppCapability =
        byId[id] ?: throw PluginProtocolException("METHOD_NOT_FOUND", "Unknown capability: $id")
}
