package com.eterultimate.eteruee.data.sync.postgres

import android.net.Uri
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import io.ktor.util.cio.readChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "PostgresGatewayClient"

class PostgresGatewayClient(
    private val config: PostgresGatewayConfig,
    private val httpClient: HttpClient,
) {
    suspend fun testConnection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig()
            val response: HttpResponse = httpClient.request(config.buildUrl("health")) {
                method = HttpMethod.Get
                gatewayAuth()
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                Log.e(TAG, "testConnection failed: ${response.status} - $errorBody")
                throw PostgresGatewayException("Gateway health check failed: ${response.status}", response.status.value, errorBody)
            }
            Unit
        }
    }

    suspend fun uploadBackup(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            validateConfig()
            val response: HttpResponse = httpClient.request(
                config.buildUrl("backups", config.namespace.trim(), file.name)
            ) {
                method = HttpMethod.Put
                gatewayAuth()
                headers {
                    append(HttpHeaders.ContentType, "application/zip")
                    append(HttpHeaders.ContentLength, file.length().toString())
                }
                setBody(file.readChannel())
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                Log.e(TAG, "uploadBackup failed: ${response.status} - $errorBody")
                throw PostgresGatewayException("Gateway backup upload failed: ${response.status}", response.status.value, errorBody)
            }
            Unit
        }
    }

    private fun validateConfig() {
        require(config.baseUrl.isNotBlank()) { "PostgreSQL Gateway URL is required" }
        require(config.namespace.isNotBlank()) { "PostgreSQL Gateway namespace is required" }
    }

    private fun io.ktor.client.request.HttpRequestBuilder.gatewayAuth() {
        if (config.accessToken.isNotBlank()) {
            headers {
                append(HttpHeaders.Authorization, "Bearer ${config.accessToken}")
            }
        }
    }

    private fun PostgresGatewayConfig.buildUrl(vararg segments: String): String {
        val base = baseUrl.trimEnd('/')
        val encodedSegments = segments
            .map { it.trim('/') }
            .filter { it.isNotBlank() }
            .map { Uri.encode(it) }
        return if (encodedSegments.isEmpty()) {
            base
        } else {
            "$base/${encodedSegments.joinToString("/")}"
        }
    }
}

class PostgresGatewayException(
    message: String,
    val statusCode: Int,
    val responseBody: String,
) : Exception(message)
