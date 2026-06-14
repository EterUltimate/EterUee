package com.eterultimate.eteruee.data.datastore

import com.eterultimate.eteruee.ai.provider.ProviderSetting
import com.eterultimate.eteruee.data.ai.mcp.McpServerConfig
import com.eterultimate.eteruee.data.ai.tools.LocalToolOption
import com.eterultimate.eteruee.data.datastore.migration.mergeDefaultAssistantRuntimeAccess
import com.eterultimate.eteruee.utils.JsonInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultIntegrationsTest {
    @Test
    fun defaultLocalSapiProviderUsesAndroidHostLoopback() {
        val provider = DEFAULT_PROVIDERS
            .filterIsInstance<ProviderSetting.OpenAI>()
            .first { it.id == DEFAULT_LOCAL_SAPI_PROVIDER_ID }

        assertEquals("SAPI Local", provider.name)
        assertEquals("http://10.0.2.2:3000/v1", provider.baseUrl)
        assertEquals(false, provider.enabled)
        assertTrue(provider.models.any { it.modelId == "auto" })
    }

    @Test
    fun defaultJshookMcpUsesHostStreamableHttpEndpoint() {
        val config = DEFAULT_MCP_SERVERS.single { it.id == DEFAULT_JSHOOK_MCP_SERVER_ID }

        assertEquals("jshookmcp", config.commonOptions.name)
        assertEquals("http://10.0.2.2:3001/mcp", config.url)
        assertEquals(true, config.commonOptions.enable)
    }

    @Test
    fun defaultAssistantEnablesJshookMcp() {
        val assistant = DEFAULT_ASSISTANTS.single { it.id == DEFAULT_ASSISTANT_ID }

        assertTrue(assistant.mcpServers.contains(DEFAULT_JSHOOK_MCP_SERVER_ID))
    }

    @Test
    fun defaultAssistantExposesRuntimeToolsToLlm() {
        val assistant = DEFAULT_ASSISTANTS.single { it.id == DEFAULT_ASSISTANT_ID }

        assertTrue(assistant.localTools.contains(LocalToolOption.Shell))
        assertTrue(assistant.localTools.contains(LocalToolOption.LinuxEnvironment))
        assertTrue(assistant.localTools.contains(LocalToolOption.DeviceAgent))
    }

    @Test
    fun linuxEnvironmentToolOptionHasStableSerializedName() {
        val options: List<LocalToolOption> = listOf(LocalToolOption.LinuxEnvironment)
        val encoded = JsonInstant.encodeToString(options)

        assertEquals("""[{"type":"linux_environment"}]""", encoded)
    }

    @Test
    fun migrationAddsDefaultRuntimeAccessToExistingDefaultAssistant() {
        val migrated = mergeDefaultAssistantRuntimeAccess(
            """
                [
                  {
                    "id": "$DEFAULT_ASSISTANT_ID",
                    "name": "",
                    "systemPrompt": "",
                    "mcpServers": [],
                    "localTools": [{"type":"time_info"}]
                  }
                ]
            """.trimIndent()
        )

        val assistants: List<com.eterultimate.eteruee.data.model.Assistant> =
            JsonInstant.decodeFromString(migrated)
        val assistant = assistants.single()

        assertTrue(assistant.mcpServers.contains(DEFAULT_JSHOOK_MCP_SERVER_ID))
        assertTrue(assistant.localTools.contains(LocalToolOption.Shell))
        assertTrue(assistant.localTools.contains(LocalToolOption.LinuxEnvironment))
        assertTrue(assistant.localTools.contains(LocalToolOption.DeviceAgent))
    }
}
