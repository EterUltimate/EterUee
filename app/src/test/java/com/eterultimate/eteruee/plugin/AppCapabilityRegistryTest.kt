package com.eterultimate.eteruee.plugin

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCapabilityRegistryTest {
    @Test
    fun executeReturnsCapabilityResultWhenPermissionGranted() = runBlocking {
        val registry = AppCapabilityRegistry(listOf(EchoCapability))
        val context = PluginCallContext(
            connectionId = "test",
            grantedPermissions = setOf(PluginPermission.SettingsRead),
        )

        val result = registry.execute(
            method = "test.echo",
            input = buildJsonObject { put("value", "ok") },
            context = context,
        )

        assertEquals(buildJsonObject { put("value", "ok") }, result)
    }

    @Test
    fun executeRejectsMissingPermission() = runBlocking {
        val registry = AppCapabilityRegistry(listOf(EchoCapability))
        val context = PluginCallContext(
            connectionId = "test",
            grantedPermissions = emptySet(),
        )

        val error = runCatching {
            registry.execute("test.echo", JsonObject(emptyMap()), context)
        }.exceptionOrNull() as PluginPermissionDeniedException

        assertEquals(setOf(PluginPermission.SettingsRead), error.missing)
    }

    @Test
    fun executeRejectsUnknownCapability() = runBlocking {
        val registry = AppCapabilityRegistry(listOf(EchoCapability))
        val context = PluginCallContext(
            connectionId = "test",
            grantedPermissions = setOf(PluginPermission.SettingsRead),
        )

        val error = runCatching {
            registry.execute("missing.method", JsonObject(emptyMap()), context)
        }.exceptionOrNull() as PluginProtocolException

        assertEquals("METHOD_NOT_FOUND", error.code)
    }

    @Test
    fun listReturnsSortedDescriptorsWithScopeNames() {
        val registry = AppCapabilityRegistry(listOf(WriteCapability, EchoCapability))

        val descriptors = registry.list()

        assertEquals(listOf("test.echo", "test.write"), descriptors.map { it.id })
        assertEquals(listOf("settings:read"), descriptors.first().permissions)
    }

    private object EchoCapability : AppCapability {
        override val id = "test.echo"
        override val description = "Echo params."
        override val inputSchema = JsonObject(emptyMap())
        override val permissions = setOf(PluginPermission.SettingsRead)

        override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement = input
    }

    private object WriteCapability : AppCapability {
        override val id = "test.write"
        override val description = "Write test."
        override val inputSchema = JsonObject(emptyMap())
        override val permissions = setOf(PluginPermission.ConversationWrite)

        override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement = input
    }
}
