package com.eterultimate.eteruee.data.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import com.eterultimate.eteruee.ai.provider.ProviderManager
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.DefaultAISDK
import com.eterultimate.eteruee.ai.sdk.GenerateObjectRequest
import com.eterultimate.eteruee.ai.sdk.GenerateTextRequest
import com.eterultimate.eteruee.ai.sdk.GenerateTextResult
import com.eterultimate.eteruee.ai.sdk.StreamTextRequest
import com.eterultimate.eteruee.ai.sdk.TextChunk
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.datastore.findProvider

/**
 * 动态 AISDK 实现
 * 根据请求中的模型动态选择对应的 Provider
 */
class DynamicAISDK(
    private val providerManager: ProviderManager,
    private val settingsStore: SettingsStore
) : AISDK {

    private suspend fun getSDK(requestModelId: com.eterultimate.eteruee.ai.provider.Model): AISDK {
        val settings = settingsStore.settingsFlow.first()
        val providerSetting = requestModelId.findProvider(settings.providers)
            ?: throw IllegalStateException("Provider not found for model: ${requestModelId.modelId}")

        return DefaultAISDK(providerManager, providerSetting)
    }

    override suspend fun generateText(request: GenerateTextRequest): GenerateTextResult {
        return getSDK(request.model).generateText(request)
    }

    override fun streamText(request: StreamTextRequest): Flow<TextChunk> {
        // 由于 streamText 不是 suspend 的，我们需要在 flow 中解析 SDK
        return kotlinx.coroutines.flow.flow {
            val sdk = getSDK(request.model)
            sdk.streamText(request).collect {
                emit(it)
            }
        }
    }

    override suspend fun generateObject(request: GenerateObjectRequest): JsonObject {
        return getSDK(request.model).generateObject(request)
    }
}
