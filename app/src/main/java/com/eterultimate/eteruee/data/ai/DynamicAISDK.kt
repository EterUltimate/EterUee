package com.eterultimate.eteruee.data.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import com.eterultimate.eteruee.ai.provider.Model
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

    private suspend fun resolveModel(requestModel: Model): Pair<AISDK, Model> {
        val settings = settingsStore.settingsFlow.first()
        val model = settings.providers
            .asSequence()
            .flatMap { it.models.asSequence() }
            .firstOrNull {
                it.id == requestModel.id ||
                    (requestModel.modelId.isNotBlank() && it.modelId == requestModel.modelId) ||
                    (requestModel.displayName.isNotBlank() && it.displayName == requestModel.displayName)
            }
            ?: requestModel
        val providerSetting = model.findProvider(settings.providers)
            ?: throw IllegalStateException("Provider not found for model: ${requestModel.modelId}")

        return DefaultAISDK(providerManager, providerSetting) to model
    }

    override suspend fun generateText(request: GenerateTextRequest): GenerateTextResult {
        val (sdk, model) = resolveModel(request.model)
        return sdk.generateText(request.copy(model = model))
    }

    override fun streamText(request: StreamTextRequest): Flow<TextChunk> {
        // 由于 streamText 不是 suspend 的，我们需要在 flow 中解析 SDK
        return kotlinx.coroutines.flow.flow {
            val (sdk, model) = resolveModel(request.model)
            sdk.streamText(request.copy(model = model)).collect {
                emit(it)
            }
        }
    }

    override suspend fun generateObject(request: GenerateObjectRequest): JsonObject {
        val (sdk, model) = resolveModel(request.model)
        return sdk.generateObject(request.copy(model = model))
    }
}
