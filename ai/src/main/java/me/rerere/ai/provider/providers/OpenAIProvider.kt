package me.rerere.ai.provider.providers

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Provider
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.MessageChunk
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageChoice
import me.rerere.ai.ui.UIMessagePart
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.concurrent.TimeUnit

object OpenAIProvider : Provider<ProviderSetting.OpenAI> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("X-Title", "RikkaHub")
                .addHeader("HTTP-Referer", "https://github.com/re-ovo/rikkahub")
                .build()
            chain.proceed(request)
        }
        .build()

    override suspend fun generateText(
        providerSetting: ProviderSetting,
        conversation: Conversation,
        params: TextGenerationParams
    ): MessageChunk {
        if (providerSetting !is ProviderSetting.OpenAI) {
            throw IllegalArgumentException("ProviderSetting must be OpenAI")
        }

        val requestBody = buildChatCompletionRequest(conversation, params)
        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        println(json.encodeToString(requestBody))

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get response: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body?.string() ?: ""
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject

        // 从 JsonObject 中提取必要的信息
        val id = bodyJson["id"]?.jsonPrimitive?.content ?: ""
        val model = bodyJson["model"]?.jsonPrimitive?.content ?: ""
        val choice = bodyJson["choices"]?.jsonArray?.get(0)?.jsonObject ?: error("choices is null")

        val message = choice["message"]?.jsonObject ?: throw Exception("message is null")
        val finishReason = choice["finish_reason"]
            ?.jsonPrimitive
            ?.content
            ?: "unknown"

        return MessageChunk(
            id = id,
            model = model,
            choices = listOf(
                UIMessageChoice(
                    index = 0,
                    delta = null,
                    message = parseMessage(message),
                    finishReason = finishReason
                )
            )
        )
    }

    override suspend fun streamText(
        providerSetting: ProviderSetting,
        conversation: Conversation,
        params: TextGenerationParams
    ): Flow<MessageChunk> = callbackFlow {
        if (providerSetting !is ProviderSetting.OpenAI) {
            throw IllegalArgumentException("ProviderSetting must be OpenAI")
        }

        val requestBody = buildChatCompletionRequest(conversation, params, stream = true)
        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/chat/completions")
            .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        println(requestBody)

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    eventSource.cancel()
                    return
                }
                data
                    .trim()
                    .split("\n")
                    .filter { it.isNotBlank() }
                    .map { json.parseToJsonElement(it).jsonObject}
                    .forEach {
                        println(it)
                        val id = it["id"]?.jsonPrimitive?.content ?: ""
                        val model = it["model"]?.jsonPrimitive?.content ?: ""
                        val choices = it["choices"]?.jsonArray ?: JsonArray(emptyList())
                        if(choices.isEmpty()) return@forEach
                        val choice = choices[0].jsonObject
                        val message = choice["delta"]?.jsonObject ?: choice["message"]?.jsonObject ?: throw Exception("delta/message is null")
                        val finishReason = choice["finish_reason"]?.jsonPrimitive?.content ?: "unknown"
                        val messageChunk = MessageChunk(
                            id = id,
                            model = model,
                            choices = listOf(
                                UIMessageChoice(
                                    index = 0,
                                    delta = parseMessage(message),
                                    message = null,
                                    finishReason = finishReason
                                )
                            )
                        )
                        trySend(messageChunk)
                    }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                t?.printStackTrace()
                eventSource.cancel()
                throw Exception("Stream failed: ${t?.message}, response: ${response?.body?.string()}")
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = EventSources.createFactory(client).newEventSource(request, listener)

        awaitClose {
            println("[awaitClose] 关闭eventSource ")
            eventSource.cancel()
        }
    }

    private fun buildChatCompletionRequest(
        conversation: Conversation,
        params: TextGenerationParams,
        stream: Boolean = false
    ): JsonObject {
        return buildJsonObject {
            put("model", params.model.name)
            put("messages", buildMessages(conversation.messages))
            put("temperature", params.temperature)
            put("top_p", params.topP)
            put("stream", stream)
        }
    }

    private fun buildMessages(messages: List<UIMessage>) = buildJsonArray {
        messages.forEach { message ->
            add(buildJsonObject {
                put("role", JsonPrimitive(message.role.name.lowercase()))
                putJsonArray("content") {
                    message.parts.forEach { part ->
                        val partJson = buildJsonObject {
                            when (part) {
                                is UIMessagePart.Text -> {
                                    put("type", "text")
                                    put("text", part.text)
                                }

                                is UIMessagePart.Image -> {
                                    put("type", "image_url")
                                    put("image_url", buildJsonObject {
                                        put("url", part.url)
                                    })
                                }

                                else -> {
                                    println("message part not supported: $part")
                                    // DO NOTHING
                                }
                            }
                        }
                        add(partJson)
                    }
                }
            })
        }
    }

    private fun parseMessage(jsonObject: JsonObject): UIMessage {
        val role = MessageRole.valueOf(jsonObject["role"]?.jsonPrimitive?.content?.uppercase() ?: "ASSISTANT")
        val reasoning = jsonObject["reasoning_content"]?.jsonPrimitive?.content

        // 也许支持其他模态的输出content? 暂时只支持文本吧
        val content = jsonObject["content"]?.jsonPrimitive?.content ?: ""

        return UIMessage(
            role = role,
            parts = buildList {
                if (reasoning != null) {
                    add(UIMessagePart.Reasoning(reasoning))
                }
                add(UIMessagePart.Text(content))
            },
        )
    }
}