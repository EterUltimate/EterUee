package com.eterultimate.eteruee.data.ai.mcp

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit

private const val TAG = "LanScanner"

/**
 * Result of scanning a single host
 */
data class ScanResult(
    val host: String,
    val port: Int,
    val url: String,
    val isMcpServer: Boolean,
    val serverName: String? = null,
    val responseTimeMs: Long = 0L,
)

/**
 * LAN MCP server scanner — discovers MCP servers on the local network
 * by probing HTTP endpoints with a JSON-RPC initialize request.
 */
class LanScanner(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val scanClient = HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(2, TimeUnit.SECONDS)
                readTimeout(3, TimeUnit.SECONDS)
                writeTimeout(2, TimeUnit.SECONDS)
                followRedirects(true)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Get the device's current WiFi IPv4 address
     */
    fun getWifiIpAddress(): String? {
        return try {
            // Method 1: NetworkInterface (works on Android 10+ without location permission)
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
            // Method 2: WifiManager fallback (deprecated but still works)
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            val ipInt = wifiInfo?.ipAddress ?: return null
            if (ipInt == 0) return null
            String.format(
                "%d.%d.%d.%d",
                ipInt and 0xff,
                ipInt shr 8 and 0xff,
                ipInt shr 16 and 0xff,
                ipInt shr 24 and 0xff
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get WiFi IP", e)
            null
        }
    }

    /**
     * Scan the local subnet for MCP servers on the given port.
     * Returns a list of discovered MCP servers.
     *
     * @param port The port to scan (default 9000 for ComfyUI MCP)
     * @param path The path to probe (default /mcp)
     * @param concurrency How many hosts to probe simultaneously
     */
    suspend fun scanSubnet(
        port: Int = 9000,
        path: String = "/mcp",
        concurrency: Int = 32,
    ): List<ScanResult> = withContext(Dispatchers.IO) {
        val myIp = getWifiIpAddress()
        if (myIp == null) {
            Log.w(TAG, "No WiFi IP address, cannot scan subnet")
            return@withContext emptyList()
        }

        val parts = myIp.split(".")
        if (parts.size != 4) return@withContext emptyList()
        val subnet = "${parts[0]}.${parts[1]}.${parts[2]}"

        Log.i(TAG, "Scanning subnet $subnet.*:$port$path from IP $myIp")

        val results = mutableListOf<ScanResult>()

        coroutineScope {
            (1..254).map { hostNum ->
                async {
                    if (hostNum == parts[3].toInt()) return@async // skip self
                    val host = "$subnet.$hostNum"
                    val url = "http://$host:$port$path"
                    val result = probeMcpServer(url)
                    if (result != null) {
                        synchronized(results) { results.add(result) }
                    }
                }
            }.awaitAll()
        }

        Log.i(TAG, "Scan complete, found ${results.size} MCP servers")
        results.sortedBy { it.host }
    }

    /**
     * Test connectivity to a specific MCP server URL.
     * Returns a ScanResult if the server responds, null otherwise.
     */
    suspend fun testConnection(url: String): ScanResult? = withContext(Dispatchers.IO) {
        probeMcpServer(url)
    }

    /**
     * Probe a URL to check if it's a valid MCP server.
     * Sends a JSON-RPC initialize request and checks the response.
     */
    private suspend fun probeMcpServer(url: String): ScanResult? {
        return try {
            val startTime = System.currentTimeMillis()
            val initRequest = """
                {
                    "jsonrpc": "2.0",
                    "id": 1,
                    "method": "initialize",
                    "params": {
                        "protocolVersion": "2024-11-05",
                        "capabilities": {},
                        "clientInfo": {
                            "name": "EterUee-Scanner",
                            "version": "1.0"
                        }
                    }
                }
            """.trimIndent()

            val response: HttpResponse = scanClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(initRequest)
            }

            val elapsed = System.currentTimeMillis() - startTime

            if (response.status.isSuccess()) {
                val body = response.bodyAsText()
                // Check if response looks like a valid JSON-RPC response
                val serverName = parseServerName(body)
                val parsedUrl = parseUrlComponents(url)
                Log.i(TAG, "Found MCP server at $url (name=$serverName, ${elapsed}ms)")
                ScanResult(
                    host = parsedUrl.first,
                    port = parsedUrl.second,
                    url = url,
                    isMcpServer = true,
                    serverName = serverName,
                    responseTimeMs = elapsed,
                )
            } else {
                null
            }
        } catch (e: Exception) {
            // Connection refused, timeout, etc — normal for non-MCP hosts
            null
        }
    }

    private fun parseUrlComponents(url: String): Pair<String, Int> {
        val regex = Regex("https?://([^:/]+)(?::(\\d+))?")
        val match = regex.find(url)
        return if (match != null) {
            Pair(match.groupValues[1], match.groupValues[2].toIntOrNull() ?: 80)
        } else {
            Pair(url, 80)
        }
    }

    private fun parseServerName(responseBody: String): String? {
        return try {
            val json = Json.parseToJsonElement(responseBody).jsonObject
            val result = json["result"]?.jsonObject
            result?.get("serverInfo")?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull
        } catch (e: Exception) {
            null
        }
    }
}
