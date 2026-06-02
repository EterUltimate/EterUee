package com.eterultimate.eteruee.web.relay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.eterultimate.eteruee.web.BadRequestException
import com.eterultimate.eteruee.web.dto.HttpRelayRequest
import com.eterultimate.eteruee.web.dto.HttpRelayResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val MAX_RELAY_REQUEST_BODY_BYTES = 5L * 1024 * 1024
private const val MAX_RELAY_RESPONSE_BODY_BYTES = 10L * 1024 * 1024
private const val DEFAULT_RELAY_CONTENT_TYPE = "text/plain; charset=utf-8"

private val ALLOWED_RELAY_METHODS = setOf("GET", "POST", "PUT", "PATCH", "DELETE")
private val METHODS_WITH_REQUIRED_BODY = setOf("POST", "PUT", "PATCH")
private val BLOCKED_REQUEST_HEADERS = setOf(
    "connection",
    "content-length",
    "content-type",
    "host",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
)

class HttpRelayService(
    private val okHttpClient: OkHttpClient,
) {
    suspend fun execute(request: HttpRelayRequest): HttpRelayResponse = withContext(Dispatchers.IO) {
        val targetUrl = request.url.trim().toHttpUrlOrNull()
            ?: throw BadRequestException("url must be a valid http or https URL")
        if (targetUrl.scheme !in setOf("http", "https")) {
            throw BadRequestException("url must be http or https")
        }
        if (targetUrl.username.isNotEmpty() || targetUrl.password.isNotEmpty()) {
            throw BadRequestException("url must not contain credentials")
        }

        val method = request.method.trim().uppercase(Locale.ROOT)
        if (method !in ALLOWED_RELAY_METHODS) {
            throw BadRequestException("Unsupported relay method")
        }

        val bodyBytes = decodeRequestBody(request)
        if (method == "GET" && bodyBytes != null) {
            throw BadRequestException("GET relay requests must not include a body")
        }

        val mediaType = (request.contentType ?: DEFAULT_RELAY_CONTENT_TYPE).toMediaTypeOrNull()
        val requestBody = when {
            bodyBytes != null -> bodyBytes.toRequestBody(mediaType)
            method in METHODS_WITH_REQUIRED_BODY -> ByteArray(0).toRequestBody(mediaType)
            else -> null
        }

        val okRequest = Request.Builder()
            .url(targetUrl)
            .apply {
                request.headers.forEach { (name, value) ->
                    val normalizedName = validateHeader(name, value)
                    if (normalizedName.lowercase(Locale.ROOT) !in BLOCKED_REQUEST_HEADERS) {
                        header(normalizedName, value)
                    }
                }
            }
            .method(method, requestBody)
            .build()

        val call = okHttpClient.newCall(okRequest)
        request.timeoutMillis?.let { timeoutMillis ->
            if (timeoutMillis !in 1..120_000) {
                throw BadRequestException("timeoutMillis must be in 1..120000")
            }
            call.timeout().timeout(timeoutMillis, TimeUnit.MILLISECONDS)
        }

        call.execute().use { response ->
            val bodyResult = readResponseBody(response.body.byteStream())
            val contentType = response.body.contentType()?.toString()
                ?: response.header("Content-Type")
            val isText = shouldReturnText(contentType)

            HttpRelayResponse(
                url = response.request.url.toString(),
                statusCode = response.code,
                statusMessage = response.message,
                headers = response.headers.toMultimap(),
                contentType = contentType,
                body = if (isText) bodyResult.bytes.toString(Charsets.UTF_8) else null,
                bodyBase64 = if (isText) null else Base64.getEncoder().encodeToString(bodyResult.bytes),
                bodyEncoding = if (isText) "text" else "base64",
                bodyTruncated = bodyResult.truncated,
            )
        }
    }

    private fun decodeRequestBody(request: HttpRelayRequest): ByteArray? {
        if (request.body != null && request.bodyBase64 != null) {
            throw BadRequestException("Only one of body or bodyBase64 can be provided")
        }

        val bytes = when {
            request.body != null -> request.body.toByteArray(Charsets.UTF_8)
            request.bodyBase64 != null -> try {
                Base64.getDecoder().decode(request.bodyBase64)
            } catch (_: IllegalArgumentException) {
                throw BadRequestException("bodyBase64 is not valid base64")
            }
            else -> null
        }

        if (bytes != null && bytes.size > MAX_RELAY_REQUEST_BODY_BYTES) {
            throw BadRequestException(
                "Relay request body too large: max ${MAX_RELAY_REQUEST_BODY_BYTES / (1024 * 1024)} MB"
            )
        }

        return bytes
    }

    private fun validateHeader(name: String, value: String): String {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            throw BadRequestException("Header name must not be blank")
        }
        if (normalizedName.any { it.code <= 31 || it.code == 127 }) {
            throw BadRequestException("Header name contains invalid characters")
        }
        if (value.any { it == '\n' || it == '\r' }) {
            throw BadRequestException("Header value contains invalid characters")
        }
        return normalizedName
    }

    private fun readResponseBody(input: java.io.InputStream?): RelayBodyResult {
        if (input == null) {
            return RelayBodyResult(bytes = ByteArray(0), truncated = false)
        }

        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var totalBytes = 0L
            var truncated = false

            while (true) {
                val read = stream.read(buffer)
                if (read < 0) break

                val remaining = MAX_RELAY_RESPONSE_BODY_BYTES - totalBytes
                if (remaining <= 0) {
                    truncated = true
                    break
                }

                val bytesToWrite = minOf(read.toLong(), remaining).toInt()
                output.write(buffer, 0, bytesToWrite)
                totalBytes += bytesToWrite

                if (bytesToWrite < read) {
                    truncated = true
                    break
                }
            }

            return RelayBodyResult(
                bytes = output.toByteArray(),
                truncated = truncated,
            )
        }
    }

    private fun shouldReturnText(contentType: String?): Boolean {
        val normalized = contentType?.lowercase(Locale.ROOT) ?: return false
        return normalized.startsWith("text/") ||
            normalized.contains("json") ||
            normalized.contains("xml") ||
            normalized.contains("javascript") ||
            normalized.contains("x-www-form-urlencoded")
    }
}

private data class RelayBodyResult(
    val bytes: ByteArray,
    val truncated: Boolean,
)
