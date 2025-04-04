package me.rerere.ai.provider.providers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Provider
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.MessageChunk
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageChoice
import me.rerere.ai.ui.UIMessageContent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.util.UUID
import java.util.concurrent.TimeUnit

class OpenAIProvider : Provider<ProviderSetting.OpenAI> {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
    
    override suspend fun generateText(
        providerSetting: ProviderSetting,
        conversation: Conversation,
        params: TextGenerationParams
    ): MessageChunk {
        if (providerSetting !is ProviderSetting.OpenAI) {
            throw IllegalArgumentException("ProviderSetting must be OpenAI")
        }
        
        val requestBody = buildChatCompletionRequest(conversation, params, providerSetting)
        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        println(json.encodeToString(requestBody))
            
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("Failed to get response: ${response.code} ${response.body?.string()}")
        }

        val chatCompletion = json.decodeFromString(ChatCompletion.serializer(), response.body?.string() ?: "")
        
        return MessageChunk(
            id = chatCompletion.id,
            model = chatCompletion.model,
            choices = chatCompletion.choices.map { choice ->
                UIMessageChoice(
                    index = choice.index,
                    delta = null,
                    message = UIMessageContent.Text(choice.message.content ?: ""),
                    finishReason = choice.finishReason
                )
            }
        )
    }

    override suspend fun streamText(
        providerSetting: ProviderSetting,
        conversation: Conversation,
        params: TextGenerationParams
    ): Flow<MessageChunk> = flow {
        if (providerSetting !is ProviderSetting.OpenAI) {
            throw IllegalArgumentException("ProviderSetting must be OpenAI")
        }
//
//        val requestBody = buildChatCompletionRequest(conversation, params, providerSetting, stream = true)
//        val request = Request.Builder()
//            .url("${providerSetting.baseUrl}/v1/chat/completions")
//            .addHeader("Authorization", "Bearer ${providerSetting.apiKey}")
//            .addHeader("Content-Type", "application/json")
//            .post(json.encodeToString(ChatCompletionRequest.serializer(), requestBody).toRequestBody("application/json".toMediaType()))
//            .build()
//
//        val listener = object : EventSourceListener() {
//            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
//                if (data == "[DONE]") {
//                    eventSource.cancel()
//                    return
//                }
//
//                try {
//                    val chatCompletionChunk = json.decodeFromString(ChatCompletionChunk.serializer(), data)
//                    val content = chatCompletionChunk.choices.firstOrNull()?.delta?.content
//
//                    val messageChunk = MessageChunk(
//                        id = chatCompletionChunk.id,
//                        model = chatCompletionChunk.model,
//                        choices = chatCompletionChunk.choices.map { choice ->
//                            UIMessageChoice(
//                                index = choice.index,
//                                delta = if (content != null) UIMessageContent.Text(content) else null,
//                                message = null,
//                                finishReason = choice.finishReason
//                            )
//                        }
//                    )
//
//                    emit(messageChunk)
//                } catch (e: Exception) {
//                    eventSource.cancel()
//                    throw e
//                }
//            }
//
//            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
//                eventSource.cancel()
//                throw Exception("Stream failed: ${t?.message}, response: ${response?.body?.string()}")
//            }
//        }
//
//        EventSources.createFactory(client).newEventSource(request, listener)
    }
    
    private fun buildChatCompletionRequest(
        conversation: Conversation,
        params: TextGenerationParams,
        providerSetting: ProviderSetting.OpenAI,
        stream: Boolean = false
    ): ChatCompletionRequest {
        val messages = conversation.messages.map { uiMessage ->
            val content = uiMessage.content.filterIsInstance<UIMessageContent.Text>()
                .joinToString("\n") { it.text }
            ChatMessage(
                role = uiMessage.role.name.lowercase(),
                content = content
            )
        }
        
        // 从providerSetting中获取第一个可用模型
        val modelName = providerSetting.models.firstOrNull()?.name ?: "gpt-4o"
        
        return ChatCompletionRequest(
            model = modelName,
            messages = messages,
            temperature = params.temperature,
            topP = params.topP,
            presencePenalty = params.presencePenalty,
            frequencyPenalty = params.frequencyPenalty,
            stream = stream
        )
    }
    
    @Serializable
    private data class ChatCompletionRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Float,
        @SerialName("top_p")
        val topP: Float,
        @SerialName("presence_penalty")
        val presencePenalty: Float,
        @SerialName("frequency_penalty")
        val frequencyPenalty: Float,
        val stream: Boolean = false
    )
    
    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: String
    )
    
    @Serializable
    private data class ChatCompletion(
        val id: String,
        val model: String,
        val choices: List<ChatCompletionChoice>
    )
    
    @Serializable
    private data class ChatCompletionChoice(
        val index: Int,
        val message: ChatMessage,
        @SerialName("finish_reason")
        val finishReason: String?
    )
    
    @Serializable
    private data class ChatCompletionChunk(
        val id: String,
        val model: String,
        val choices: List<ChatCompletionChunkChoice>
    )
    
    @Serializable
    private data class ChatCompletionChunkChoice(
        val index: Int,
        val delta: ChatCompletionDelta,
        @SerialName("finish_reason")
        val finishReason: String?
    )
    
    @Serializable
    private data class ChatCompletionDelta(
        val role: String? = null,
        val content: String? = null
    )
}