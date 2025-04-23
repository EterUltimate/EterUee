package me.rerere.ai.provider.providers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import me.rerere.ai.core.MessageRole
import me.rerere.ai.core.Schema
import me.rerere.ai.core.Tool
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelAbility
import me.rerere.ai.provider.ModelType
import me.rerere.ai.provider.Provider
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.MessageChunk
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageChoice
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.util.await
import me.rerere.ai.util.encodeBase64
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit
import kotlin.uuid.Uuid

private const val API_VERSION = "v1beta"
private const val API_URL = "https://generativelanguage.googleapis.com"

object GoogleProvider : Provider<ProviderSetting.Google> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    override suspend fun listModels(providerSetting: ProviderSetting.Google): List<Model> {
        val url = "$API_URL/$API_VERSION/models".toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", providerSetting.apiKey)
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = client.newCall(request).await()

        return if (response.isSuccessful) {
            val body = response.body?.string() ?: error("empty body")
            val bodyObject = json.parseToJsonElement(body).jsonObject
            val models = bodyObject["models"]!!.jsonArray
            models.mapNotNull {
                val modelObject = it.jsonObject

                println(modelObject)

                // 忽略非chat/embedding模型
                val supportedGenerationMethods =
                    modelObject["supportedGenerationMethods"]!!.jsonArray
                        .map { method -> method.jsonPrimitive.content }
                if ("generateContent" !in supportedGenerationMethods && "embedContent" !in supportedGenerationMethods) {
                    return@mapNotNull null
                }

                Model(
                    modelId = modelObject["name"]!!.jsonPrimitive.content.substringAfter("/"),
                    displayName = modelObject["displayName"]!!.jsonPrimitive.content,
                    type = if ("generateContent" in supportedGenerationMethods) ModelType.CHAT else ModelType.EMBEDDING,
                )
            }
        } else {
            emptyList()
        }
    }

    override suspend fun generateText(
        providerSetting: ProviderSetting.Google,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): MessageChunk = withContext(Dispatchers.IO) {
        val requestBody = buildCompletionRequestBody(messages, params)

        val url = "$API_URL/$API_VERSION/models/${params.model.modelId}:generateContent".toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", providerSetting.apiKey)
            .build()

        val request = Request.Builder()
            .url(url)
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            throw Exception("Failed to get response: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body?.string() ?: ""
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject

        val candidates = bodyJson["candidates"]!!.jsonArray

        val messageChunk = MessageChunk(
            id = Uuid.random().toString(),
            model = params.model.modelId,
            choices = candidates.map { candidate ->
                UIMessageChoice(
                    message = parseMessage(candidate.jsonObject),
                    index = 0,
                    finishReason = null,
                    delta = null
                )
            }
        )

        println(messageChunk)

        messageChunk
    }

    override suspend fun streamText(
        providerSetting: ProviderSetting.Google,
        messages: List<UIMessage>,
        params: TextGenerationParams,
    ): Flow<MessageChunk> = callbackFlow {
        val requestBody = buildCompletionRequestBody(messages, params)

        val url =
            "$API_URL/$API_VERSION/models/${params.model.modelId}:streamGenerateContent".toHttpUrl()
                .newBuilder()
                .addQueryParameter("key", providerSetting.apiKey)
                .addQueryParameter("alt", "sse")
                .build()

        val request = Request.Builder()
            .url(url)
            .post(
                json.encodeToString(requestBody).toRequestBody("application/json".toMediaType())
            )
            .build()

        println(json.encodeToString(requestBody))

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                println("[onEvent] $data")
                if (data == "[DONE]") {
                    println("[onEvent] (done) 结束流: $data")
                    eventSource.cancel()
                    close()
                    return
                }

                try {
                    val jsonData = json.parseToJsonElement(data).jsonObject
                    val candidates = jsonData["candidates"]?.jsonArray ?: return
                    if (candidates.isEmpty()) return

                    val messageChunk = MessageChunk(
                        id = Uuid.random().toString(),
                        model = params.model.modelId,
                        choices = candidates.mapIndexed { index, candidate ->
                            val candidateObj = candidate.jsonObject
                            val content = candidateObj["content"]?.jsonObject
                            val finishReason =
                                candidateObj["finishReason"]?.jsonPrimitive?.contentOrNull

                            UIMessageChoice(
                                index = index,
                                delta = content?.let {
                                    parseMessage(
                                        JsonObject(
                                            mapOf(
                                                "role" to JsonPrimitive(
                                                    "model"
                                                ), "content" to it
                                            )
                                        )
                                    )
                                },
                                message = null,
                                finishReason = finishReason
                            )
                        }
                    )

                    trySend(messageChunk)
                } catch (e: Exception) {
                    e.printStackTrace()
                    println("[onEvent] 解析错误: $data")
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?
            ) {
                var exception = t

                t?.printStackTrace()
                println("[onFailure] 发生错误: ${t?.message}")

                try {
                    if (t == null && response != null) {
                        val bodyStr = response.body?.string()
                        if (!bodyStr.isNullOrEmpty()) {
                            val bodyElement = json.parseToJsonElement(bodyStr)
                            println(bodyElement)
                            if (bodyElement is JsonObject) {
                                exception = Exception(
                                    bodyElement["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                                        ?: "unknown"
                                )
                            }
                        } else {
                            exception = Exception("Unknown error: ${response.code}")
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                } finally {
                    close(exception ?: Exception("Stream failed"))
                }
            }

            override fun onClosed(eventSource: EventSource) {
                println("[onClosed] 连接已关闭")
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)

        awaitClose {
            println("[awaitClose] 关闭eventSource")
            eventSource.cancel()
        }
    }

    private fun buildCompletionRequestBody(
        messages: List<UIMessage>,
        params: TextGenerationParams
    ): JsonObject = buildJsonObject {
        // System message if available
        val systemMessage = messages.firstOrNull { it.role == MessageRole.SYSTEM }
        if (systemMessage != null) {
            put("system_instruction", buildJsonObject {
                putJsonArray("parts") {
                    add(buildJsonObject {
                        put(
                            "text",
                            systemMessage.parts.filterIsInstance<UIMessagePart.Text>()
                                .joinToString { it.text })
                    })
                }
            })
        }

        // Generation config
        put("generationConfig", buildJsonObject {
            put("temperature", params.temperature)
            put("topP", params.topP)
        })

        // Contents (user messages)
        put(
            "contents",
            buildContents(messages)
        )

        // Tools
        if (params.tools.isNotEmpty() && params.model.abilities.contains(ModelAbility.TOOL)) {
            put("tools", buildJsonArray {
                add(buildJsonObject {
                    put("functionDeclarations", buildJsonArray {
                        params.tools.forEach { tool ->
                            add(buildJsonObject {
                                when (tool) {
                                    is Tool.Function -> {
                                        put("name", JsonPrimitive(tool.name))
                                        put("description", JsonPrimitive(tool.description))
                                        put(
                                            "parameters",
                                            json.encodeToJsonElement(
                                                Schema.serializer(),
                                                tool.parameters
                                            )
                                        )
                                    }
                                }
                            })
                        }
                    })
                })
            })
        }
    }

    private fun commonRoleToGoogleRole(role: MessageRole): String {
        return when (role) {
            MessageRole.USER -> "user"
            MessageRole.SYSTEM -> "system"
            MessageRole.ASSISTANT -> "model"
            MessageRole.TOOL -> error("Tool role not supported")
        }
    }

    private fun googleRoleToCommonRole(role: String): MessageRole {
        return when (role) {
            "user" -> MessageRole.USER
            "system" -> MessageRole.SYSTEM
            "model" -> MessageRole.ASSISTANT
            else -> error("Unknown role $role")
        }
    }

    private fun parseMessage(message: JsonObject): UIMessage {
        val role = googleRoleToCommonRole(
            message["role"]?.jsonPrimitive?.contentOrNull ?: "model"
        )
        val content = message["content"]?.jsonObject ?: error("No content")
        val parts = content["parts"]?.jsonArray?.map { part ->
            parseMessagePart(part.jsonObject)
        } ?: emptyList()

        return UIMessage(
            role = role,
            parts = parts
        )
    }

    private fun parseMessagePart(jsonObject: JsonObject): UIMessagePart {
        return when {
            jsonObject.containsKey("text") -> {
                UIMessagePart.Text(jsonObject["text"]!!.jsonPrimitive.content)
            }

            jsonObject.containsKey("functionCall") -> {
                error("not support function_call yet: $jsonObject")
            }

            else -> error("unknown message part type: $jsonObject")
        }
    }

    private fun buildContents(messages: List<UIMessage>): JsonArray {
        return buildJsonArray {
            messages
                .filter { it.role != MessageRole.SYSTEM && it.isValidToUpload() }
                .forEachIndexed { index, message ->
                    add(buildJsonObject {
                        put("role", commonRoleToGoogleRole(message.role))
                        putJsonArray("parts") {
                            for (part in message.parts) {
                                when (part) {
                                    is UIMessagePart.Text -> {
                                        add(buildJsonObject {
                                            put("text", part.text)
                                        })
                                    }

                                    is UIMessagePart.Image -> {
                                        part.encodeBase64(false).onSuccess { base64Data ->
                                            add(buildJsonObject {
                                                put("inline_data", buildJsonObject {
                                                    put("mime_type", "image/png")
                                                    put("data", base64Data)
                                                })
                                            })
                                        }
                                    }

                                    else -> {
                                        // Unsupported part type
                                    }
                                }
                            }
                        }
                    })
                }
        }
    }
}