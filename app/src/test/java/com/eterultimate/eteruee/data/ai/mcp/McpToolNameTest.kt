package com.eterultimate.eteruee.data.ai.mcp

import kotlin.uuid.Uuid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class McpToolNameTest {
    @Test
    fun `provider tool name includes server key to avoid duplicate MCP tool names`() {
        val firstServerId = Uuid.parse("11111111-1111-1111-1111-111111111111")
        val secondServerId = Uuid.parse("22222222-2222-2222-2222-222222222222")

        val firstName = mcpProviderToolName(firstServerId, "search")
        val secondName = mcpProviderToolName(secondServerId, "search")

        assertEquals("mcp__111111111111__search", firstName)
        assertEquals("mcp__222222222222__search", secondName)
        assertNotEquals(firstName, secondName)
    }

    @Test
    fun `display tool name strips MCP server key when present`() {
        assertEquals("search", mcpDisplayToolName("mcp__111111111111__search"))
    }

    @Test
    fun `display tool name keeps legacy MCP tool names readable`() {
        assertEquals("search", mcpDisplayToolName("mcp__search"))
    }
}
