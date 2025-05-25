package me.rerere.rikkahub.data.mcp

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.Tool
import io.modelcontextprotocol.kotlin.sdk.client.Client
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import me.rerere.ai.core.Schema
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.mcp.transport.SseClientTransport
import me.rerere.rikkahub.utils.checkDifferent

private const val TAG = "McpManager"

class McpManager(
    private val settingsStore: SettingsStore,
    private val appScope: AppScope
) {
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(McpJson)
        }
//        install(Logging) {
//            level = LogLevel.ALL
//            logger = object : Logger {
//                override fun log(message: String) {
//                    println()
//                    println("[ktor] $message")
//                }
//            }
//        }
        install(SSE)
        install(WebSockets)
    }

    private val clients: MutableMap<McpServerConfig, Client> = mutableMapOf()

    init {
        appScope.launch {
            settingsStore.settingsFlow
                .map { settings -> settings.mcpServers }
                .collect { mcpServerConfigs ->
                    val newConfigs = mcpServerConfigs.filter { it.commonOptions.enable }
                    val currentConfigs = clients.keys.toList()
                    val (toAdd, toRemove) = currentConfigs.checkDifferent(
                        other = newConfigs,
                        eq = { a, b -> a.id == b.id }
                    )
                    toAdd.forEach { cfg ->
                        appScope.launch { addClient(cfg) }
                    }
                    toRemove.forEach { cfg ->
                        appScope.launch { removeClient(cfg) }
                    }
                    Log.i(TAG, "add: $toAdd")
                    Log.i(TAG, "remove: $toRemove")
                }
        }
    }

    fun getClient(config: McpServerConfig): Client? = clients[config]

    suspend fun addClient(config: McpServerConfig) {
        this.removeClient(config) // Remove first
        val client = when (config) {
            is McpServerConfig.SseTransportServer -> {
                val transport = SseClientTransport(
                    urlString = config.url,
                    client = httpClient,
                )
                val client = Client(
                    clientInfo = Implementation(
                        name = "test", version = "1.0.0"
                    ),
                )
                client.connect(transport)
                client
            }

            is McpServerConfig.WebSocketServer -> {
                TODO()
            }
        }
        clients[config] = client
        this.sync(config)
    }

    suspend fun sync(config: McpServerConfig) {
        val client = clients[config] ?: return

        // Update tools
        val serverTools = client.listTools()?.tools ?: emptyList()
        Log.i(TAG, "addClient: $serverTools")
        settingsStore.update { old ->
            old.copy(
                mcpServers = old.mcpServers.map { serverConfig ->
                    val common = serverConfig.commonOptions
                    val tools = common.tools.toMutableList()

                    // 基于server对比
                    serverTools.forEach { serverTool ->
                        val tool = tools.find { it.name == serverTool.name }
                        if (tool == null) {
                            tools.add(McpTool(
                                name = serverTool.name,
                                description = serverTool.description,
                                enable = true,
                                inputSchema = serverTool.inputSchema.toSchema()
                            ))
                        } else {
                            val index = tools.indexOf(tool)
                            tools[index] = tool.copy(
                                description = serverTool.description,
                                inputSchema = serverTool.inputSchema.toSchema()
                            )
                        }
                    }

                    // 删除不在server内的
                    tools.removeIf { tool -> serverTools.none { it.name == tool.name }}

                    serverConfig.clone(
                        commonOptions = common.copy(
                            tools = tools
                        )
                    )
                }
            )
        }
    }

    suspend fun removeClient(config: McpServerConfig) {
        val toRemove = clients.entries.filter { it.key.id == config.id }
        toRemove.forEach { entry ->
            runCatching {
                entry.value.close()
            }.onFailure {
                it.printStackTrace()
            }
            clients.remove(entry.key)
            Log.i(TAG, "removeClient: ${entry.key}")
        }
    }

    suspend fun closeAll() {
        clients.values.forEach {
            runCatching { it.close() }.onFailure { it.printStackTrace() }
        }
        clients.clear()
    }
}

internal val McpJson: Json by lazy {
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
        classDiscriminatorMode = ClassDiscriminatorMode.NONE
        explicitNulls = false
    }
}

private fun Tool.Input.toSchema(): Schema.ObjectSchema {
    return Schema.ObjectSchema(
        properties = McpJson.decodeFromString<Map<String, Schema>>(McpJson.encodeToString(this.properties)),
        required = required ?: emptyList()
    )
}