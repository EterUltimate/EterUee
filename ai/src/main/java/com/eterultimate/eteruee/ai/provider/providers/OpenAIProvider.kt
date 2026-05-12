package com.eterultimate.eteruee.ai.provider.providers

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import com.eterultimate.eteruee.ai.provider.EmbeddingGenerationParams
import com.eterultimate.eteruee.ai.provider.EmbeddingGenerationResult
import com.eterultimate.eteruee.ai.provider.ImageEditParams
import com.eterultimate.eteruee.ai.provider.ImageGenerationParams
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.provider.Provider
import com.eterultimate.eteruee.ai.provider.ProviderSetting
import com.eterultimate.eteruee.ai.provider.ReferenceImage
import com.eterultimate.eteruee.ai.provider.TextGenerationParams
import com.eterultimate.eteruee.ai.provider.VideoGenerationParams
import com.eterultimate.eteruee.ai.provider.providers.openai.ChatCompletionsAPI
import com.eterultimate.eteruee.ai.provider.providers.openai.ResponseAPI
import com.eterultimate.eteruee.ai.ui.ImageAspectRatio
import com.eterultimate.eteruee.ai.ui.ImageGenerationItem
import com.eterultimate.eteruee.ai.ui.ImageGenerationResult
import com.eterultimate.eteruee.ai.ui.VideoGenerationItem
import com.eterultimate.eteruee.ai.ui.VideoGenerationResult
import com.eterultimate.eteruee.ai.ui.MessageChunk
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.util.KeyRoulette
import com.eterultimate.eteruee.ai.util.json
import com.eterultimate.eteruee.ai.util.mergeCustomBody
import com.eterultimate.eteruee.ai.util.toHeaders
import com.eterultimate.eteruee.common.http.await
import com.eterultimate.eteruee.common.http.getByKey
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAIProvider(
    private val client: OkHttpClient,
    context: Context? = null
) : Provider<ProviderSetting.OpenAI> {
    private val keyRoulette = if (context != null) KeyRoulette.lru(context) else KeyRoulette.default()

    private val chatCompletionsAPI = ChatCompletionsAPI(client = client, keyRoulette = keyRoulette)
    private val responseAPI = ResponseAPI(client = client, keyRoulette = keyRoulette)


    override suspend fun listModels(providerSetting: ProviderSetting.OpenAI): List<Model> =
        withContext(Dispatchers.IO) {
            val key = keyRoulette.next(providerSetting.apiKey, providerSetting.id.toString())
            val request = Request.Builder()
                .url("${providerSetting.baseUrl}/models")
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            val response = client.newCall(request).await()
            if (!response.isSuccessful) {
                error("Failed to get models: ${response.code} ${response.body?.string()}")
            }

            val bodyStr = response.body?.string() ?: ""
            val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
            val data = bodyJson["data"]?.jsonArray ?: return@withContext emptyList()

            data.mapNotNull { modelJson ->
                val modelObj = modelJson.jsonObject
                val id = modelObj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null

                Model(
                    modelId = id,
                    displayName = id,
                )
            }
        }

    override suspend fun getBalance(providerSetting: ProviderSetting.OpenAI): String = withContext(Dispatchers.IO) {
        val key = keyRoulette.next(providerSetting.apiKey, providerSetting.id.toString())
        val url = if (providerSetting.balanceOption.apiPath.startsWith("http")) {
            providerSetting.balanceOption.apiPath
        } else {
            "${providerSetting.baseUrl}${providerSetting.balanceOption.apiPath}"
        }
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $key")
            .get()
            .build()
        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            error("Failed to get balance: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body.string()
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
        val value = bodyJson.getByKey(providerSetting.balanceOption.resultPath)
        val digitalValue = value.toFloatOrNull()
        if(digitalValue != null) {
            "%.2f".format(digitalValue)
        } else {
            value
        }
    }

    override suspend fun streamText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams
    ): Flow<MessageChunk> = if (providerSetting.useResponseApi) {
        responseAPI.streamText(
            providerSetting = providerSetting,
            messages = messages,
            params = params
        )
    } else {
        chatCompletionsAPI.streamText(
            providerSetting = providerSetting,
            messages = messages,
            params = params
        )
    }

    override suspend fun generateText(
        providerSetting: ProviderSetting.OpenAI,
        messages: List<UIMessage>,
        params: TextGenerationParams
    ): MessageChunk = if (providerSetting.useResponseApi) {
        responseAPI.generateText(
            providerSetting = providerSetting,
            messages = messages,
            params = params
        )
    } else {
        chatCompletionsAPI.generateText(
            providerSetting = providerSetting,
            messages = messages,
            params = params
        )
    }

    override suspend fun generateEmbedding(
        providerSetting: ProviderSetting.OpenAI,
        params: EmbeddingGenerationParams
    ): EmbeddingGenerationResult = withContext(Dispatchers.IO) {
        require(params.input.isNotEmpty()) { "Embedding input cannot be empty" }

        val key = keyRoulette.next(providerSetting.apiKey, providerSetting.id.toString())
        val requestBody = json.encodeToString(
            buildJsonObject {
                put("model", params.model.modelId)
                if (params.input.size == 1) {
                    put("input", params.input.first())
                } else {
                    putJsonArray("input") {
                        params.input.forEach { add(JsonPrimitive(it)) }
                    }
                }
                params.dimensions?.let { put("dimensions", it) }
            }.mergeCustomBody(params.customBody)
        )

        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/embeddings")
            .headers(params.customHeaders.toHeaders())
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            error("Failed to generate embedding: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body?.string() ?: ""
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
        val data = bodyJson["data"]?.jsonArray ?: error("No data in response")
        val model = bodyJson["model"]?.jsonPrimitive?.contentOrNull ?: params.model.modelId

        val embeddings = data.map { embeddingJson ->
            val embeddingArray = embeddingJson.jsonObject["embedding"]?.jsonArray
                ?: error("No embedding in response")
            embeddingArray.map { it.jsonPrimitive.content.toFloat() }
        }

        EmbeddingGenerationResult(
            model = model,
            embeddings = embeddings
        )
    }

    override suspend fun generateImage(
        providerSetting: ProviderSetting,
        params: ImageGenerationParams
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        require(providerSetting is ProviderSetting.OpenAI) {
            "Expected OpenAI provider setting"
        }

        val key = keyRoulette.next(providerSetting.apiKey, providerSetting.id.toString())

        val requestBody = json.encodeToString(
            buildJsonObject {
                put("model", params.model.modelId)
                put("prompt", params.prompt)
                put("n", params.numOfImages)
                put(
                    "size", when (params.aspectRatio) {
                        ImageAspectRatio.SQUARE -> "1024x1024"
                        ImageAspectRatio.LANDSCAPE -> "1536x1024"
                        ImageAspectRatio.PORTRAIT -> "1024x1536"
                    }
                )
            }.mergeCustomBody(params.customBody)
        )

        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/images/generations")
            .headers(params.customHeaders.toHeaders())
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            error("Failed to generate image: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body?.string() ?: ""
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
        val data = bodyJson["data"]?.jsonArray ?: error("No data in response")

        val items = data.map { imageJson ->
            val imageObj = imageJson.jsonObject
            val b64Json = imageObj["b64_json"]?.jsonPrimitive?.contentOrNull
                ?: error("No b64_json in response")

            ImageGenerationItem(
                data = b64Json,
                mimeType = "image/png"
            )
        }

        ImageGenerationResult(items = items)
    }

    override suspend fun editImage(
        providerSetting: ProviderSetting,
        params: ImageEditParams
    ): ImageGenerationResult = withContext(Dispatchers.IO) {
        require(providerSetting is ProviderSetting.OpenAI) {
            "Expected OpenAI provider setting"
        }
        require(params.images.isNotEmpty()) {
            "At least one image is required"
        }

        val key = keyRoulette.next(providerSetting.apiKey, providerSetting.id.toString())
        val bodyBuilder = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("model", params.model.modelId)
            .addFormDataPart("prompt", params.prompt)
            .addFormDataPart("n", params.numOfImages.toString())
            .addFormDataPart(
                "size", when (params.aspectRatio) {
                    ImageAspectRatio.SQUARE -> "1024x1024"
                    ImageAspectRatio.LANDSCAPE -> "1536x1024"
                    ImageAspectRatio.PORTRAIT -> "1024x1536"
                }
            )

        val imageFieldName = if (params.images.size == 1) "image" else "image[]"
        params.images.forEach { path ->
            val imageFile = java.io.File(path)
            require(imageFile.exists()) {
                "Image file does not exist: $path"
            }
            val extension = imageFile.extension.lowercase()
            require(extension in setOf("png", "jpg", "jpeg", "webp")) {
                "Unsupported image file type for OpenAI edit: $extension"
            }
            val mediaType = when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                else -> "image/png"
            }
            bodyBuilder.addFormDataPart(
                imageFieldName,
                imageFile.name,
                imageFile.readBytes().toRequestBody(mediaType.toMediaType())
            )
        }

        params.customBody.forEach { customBody ->
            val value = when (val element = customBody.value) {
                is kotlinx.serialization.json.JsonPrimitive -> element.contentOrNull ?: element.toString()
                else -> element.toString()
            }
            bodyBuilder.addFormDataPart(customBody.key, value)
        }

        val request = Request.Builder()
            .url("${providerSetting.baseUrl}/images/edits")
            .headers(params.customHeaders.toHeaders())
            .addHeader("Authorization", "Bearer $key")
            .post(bodyBuilder.build())
            .build()

        val response = client.newCall(request).await()
        if (!response.isSuccessful) {
            error("Failed to edit image: ${response.code} ${response.body?.string()}")
        }

        val bodyStr = response.body?.string() ?: ""
        val bodyJson = json.parseToJsonElement(bodyStr).jsonObject
        val data = bodyJson["data"]?.jsonArray ?: error("No data in response")

        val items = data.map { imageJson ->
            val imageObj = imageJson.jsonObject
            val b64Json = imageObj["b64_json"]?.jsonPrimitive?.contentOrNull
                ?: error("No b64_json in response")

            ImageGenerationItem(
                data = b64Json,
                mimeType = "image/png"
            )
        }

        ImageGenerationResult(items = items)
    }

    override suspend fun generateVideo(
        providerSetting: ProviderSetting,
        params: VideoGenerationParams
    ): VideoGenerationResult = withContext(Dispatchers.IO) {
        require(providerSetting is ProviderSetting.OpenAI) {
            "Expected OpenAI provider setting"
        }

        val key = keyRoulette.next(providerSetting.apiKey, providerSetting.id.toString())

        // Build content array
        val contentArray = buildJsonArray {
            add(buildJsonObject {
                put("type", "text")
                put("text", params.prompt)
            })
            params.referenceImages.forEach { ref ->
                add(buildJsonObject {
                    put("type", "image_url")
                    put("url", ref.url)
                    ref.role?.let { put("role", it) }
                })
            }
        }

        // Build request body
        val requestBodyObj = buildJsonObject {
            put("model", params.model.modelId)
            put("content", contentArray)
        }

        // Build parameters object
        val parametersObj = buildJsonObject {
            put("generateAudio", params.generateAudio)
            put("durationSeconds", params.durationSeconds)
            put("aspectRatio", params.aspectRatio)
            put("resolution", params.resolution)
            params.seed?.let { put("seed", it) }
            params.negativePrompt?.let { put("negativePrompt", it) }
        }

        val finalBody = buildJsonObject {
            requestBodyObj.forEach { (k, v) -> put(k, v) }
            put("parameters", parametersObj)
        }.mergeCustomBody(params.customBody)

        val submitBody = json.encodeToString(finalBody)

        // Step 1: Submit task
        val submitRequest = Request.Builder()
            .url("${providerSetting.baseUrl}/contents/generations/tasks")
            .headers(params.customHeaders.toHeaders())
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(submitBody.toRequestBody("application/json".toMediaType()))
            .build()

        val submitResponse = client.newCall(submitRequest).await()
        if (!submitResponse.isSuccessful) {
            error("Failed to submit video generation task: ${submitResponse.code} ${submitResponse.body?.string()}")
        }

        val submitBodyStr = submitResponse.body?.string() ?: ""
        val submitJson = json.parseToJsonElement(submitBodyStr).jsonObject
        val taskId = submitJson["id"]?.jsonPrimitive?.contentOrNull
            ?: error("No task id in submit response")

        // Step 2: Poll for result
        val maxPollDurationMs = 10 * 60 * 1000L // 10 minutes max
        val pollIntervalMs = 5000L // 5 seconds between polls
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < maxPollDurationMs) {
            val pollRequest = Request.Builder()
                .url("${providerSetting.baseUrl}/contents/generations/tasks/$taskId")
                .headers(params.customHeaders.toHeaders())
                .addHeader("Authorization", "Bearer $key")
                .get()
                .build()

            val pollResponse = client.newCall(pollRequest).await()
            if (!pollResponse.isSuccessful) {
                error("Failed to poll video task: ${pollResponse.code} ${pollResponse.body?.string()}")
            }

            val pollBodyStr = pollResponse.body?.string() ?: ""
            val pollJson = json.parseToJsonElement(pollBodyStr).jsonObject
            val status = pollJson["status"]?.jsonPrimitive?.contentOrNull ?: "unknown"

            when (status) {
                "succeeded" -> {
                    val content = pollJson["content"]?.jsonObject
                        ?: error("No content in succeeded response")
                    val videoUrl = content["video_url"]?.jsonPrimitive?.contentOrNull
                        ?: error("No video_url in succeeded response")
                    val coverUrl = content["cover_url"]?.jsonPrimitive?.contentOrNull

                    return@withContext VideoGenerationResult(
                        items = listOf(
                            VideoGenerationItem(
                                videoUrl = videoUrl,
                                coverUrl = coverUrl
                            )
                        )
                    )
                }
                "failed" -> {
                    val errorMsg = pollJson["error"]?.toString() ?: "Unknown error"
                    error("Video generation failed: $errorMsg")
                }
                "cancelled" -> error("Video generation was cancelled")
                "expired" -> error("Video generation task expired")
                // queued, running → continue polling
            }

            kotlinx.coroutines.delay(pollIntervalMs)
        }

        error("Video generation timed out after ${maxPollDurationMs / 1000} seconds")
    }
}

