package me.rerere.ai.provider.providers

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.Provider
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.MessageChunk
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.util.await
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

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
        val url = "https://generativelanguage.googleapis.com/v1beta/models".toHttpUrl()
            .newBuilder()
            .addQueryParameter("key", providerSetting.apiKey)
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val response = client.newCall(request).await()

        return if (response.isSuccessful) {
            TODO()
        } else {
            emptyList()
        }
    }

    override suspend fun generateText(
        providerSetting: ProviderSetting.Google,
        messages: List<UIMessage>,
        params: TextGenerationParams
    ): MessageChunk {
        TODO("Not yet implemented")
    }

    override suspend fun streamText(
        providerSetting: ProviderSetting.Google,
        messages: List<UIMessage>,
        params: TextGenerationParams
    ): Flow<MessageChunk> {
        TODO("Not yet implemented")
    }
}