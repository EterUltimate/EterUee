package me.rerere.rikkahub.data.mcp

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import kotlinx.serialization.json.ClassDiscriminatorMode
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.mcp.transport.SseClientTransport

class McpManager(private val settingsStore: SettingsStore) {
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

    suspend fun init(configs: List<McpServerConfig>) {
        for (config in configs) {
            val client = when (config) {
                is McpServerConfig.SseTransportServer -> {
                    val transport = SseClientTransport(
                        urlString = config.url,
                        client = httpClient,
                    )
                    val client = Client(
                        clientInfo = Implementation(
                            name = "test",
                            version = "1.0.0"
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
        }
    }

    fun getClient(config: McpServerConfig): Client? = clients[config]

    suspend fun removeClient(config: McpServerConfig) {
        clients.remove(config)?.close()
    }

    suspend fun close() {
        clients.values.forEach {
            it.close()
        }
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