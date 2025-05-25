package me.rerere.mcp

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class McpCommonOptions(
    val enable: Boolean = true,
    val name: String = "",
    val headers: List<Pair<String, String>> = emptyList(),
    val disabledTools: Set<String> = emptySet(),
)

@Serializable
sealed class McpServerConfig {
    abstract val id: Uuid
    abstract val commonOptions: McpCommonOptions

    @Serializable
    data class SseTransportServer(
        override val id: Uuid = Uuid.random(),
        override val commonOptions: McpCommonOptions = McpCommonOptions(),
        val url: String = "",
    ) : McpServerConfig()

    @Serializable
    data class WebSocketServer(
        override val id: Uuid = Uuid.random(),
        override val commonOptions: McpCommonOptions,
        val url: String = "",
    ) : McpServerConfig()
}