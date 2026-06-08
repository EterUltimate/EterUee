package com.eterultimate.eteruee.data.ai.tools

import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.provider.ProviderManager
import com.eterultimate.eteruee.ai.provider.VideoGenerationParams
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.datastore.findModelById
import com.eterultimate.eteruee.data.datastore.findProvider
import com.eterultimate.eteruee.data.db.entity.GenMediaEntity
import com.eterultimate.eteruee.data.files.FilesManager
import com.eterultimate.eteruee.data.repository.GenMediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object VideoGenerationTools {
    fun createVideoGenerationTool(
        settingsStore: SettingsStore,
        providerManager: ProviderManager,
        filesManager: FilesManager,
        genMediaRepository: GenMediaRepository,
    ): Tool = Tool(
        name = "generate_video",
        description = """
            Generate a video using EterUee's configured video generation model.
            The generated video is saved to the local media library and returned as a local file path.
            Use this only when the user asks to create or generate a video.
        """.trimIndent().replace("\n", " "),
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("prompt", buildJsonObject {
                        put("type", "string")
                        put("description", "The video generation prompt")
                    })
                    put("aspectRatio", buildJsonObject {
                        put("type", "string")
                        put("enum", buildJsonArray {
                            add(JsonPrimitive("16:9"))
                            add(JsonPrimitive("9:16"))
                            add(JsonPrimitive("1:1"))
                        })
                        put("description", "Video aspect ratio. Default: 16:9")
                        put("default", "16:9")
                    })
                    put("durationSeconds", buildJsonObject {
                        put("type", "integer")
                        put("description", "Video duration in seconds, from 3 to 10. Default: 5")
                        put("default", 5)
                    })
                    put("resolution", buildJsonObject {
                        put("type", "string")
                        put("enum", buildJsonArray {
                            add(JsonPrimitive("480p"))
                            add(JsonPrimitive("720p"))
                            add(JsonPrimitive("1080p"))
                        })
                        put("description", "Output video resolution. Default: 720p")
                        put("default", "720p")
                    })
                    put("generateAudio", buildJsonObject {
                        put("type", "boolean")
                        put("description", "Whether to generate audio for the video. Default: false")
                        put("default", false)
                    })
                    put("seed", buildJsonObject {
                        put("type", "integer")
                        put("description", "Optional random seed for reproducible generation")
                    })
                    put("negativePrompt", buildJsonObject {
                        put("type", "string")
                        put("description", "Optional negative prompt describing what to avoid")
                    })
                },
                required = listOf("prompt")
            )
        },
        needsApproval = true,
        execute = { params ->
            executeVideoGeneration(
                params = params,
                settingsStore = settingsStore,
                providerManager = providerManager,
                filesManager = filesManager,
                genMediaRepository = genMediaRepository,
            )
        }
    )

    private suspend fun executeVideoGeneration(
        params: JsonElement,
        settingsStore: SettingsStore,
        providerManager: ProviderManager,
        filesManager: FilesManager,
        genMediaRepository: GenMediaRepository,
    ): List<UIMessagePart> = withContext(Dispatchers.IO) {
        val jsonObject = params.jsonObject
        val prompt = jsonObject["prompt"]?.jsonPrimitive?.contentOrNull?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: error("prompt is required")
        val aspectRatio = jsonObject["aspectRatio"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it in SUPPORTED_ASPECT_RATIOS }
            ?: "16:9"
        val durationSeconds = jsonObject["durationSeconds"]?.jsonPrimitive?.intOrNull
            ?.coerceIn(3, 10)
            ?: 5
        val resolution = jsonObject["resolution"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it in SUPPORTED_RESOLUTIONS }
            ?: "720p"
        val generateAudio = jsonObject["generateAudio"]?.jsonPrimitive?.booleanOrNull ?: false
        val seed = jsonObject["seed"]?.jsonPrimitive?.intOrNull
        val negativePrompt = jsonObject["negativePrompt"]?.jsonPrimitive?.contentOrNull
            ?.takeIf { it.isNotBlank() }

        val settings = settingsStore.settingsFlow.first()
        val model = settings.findModelById(settings.videoGenerationModelId)
            ?: error("No video generation model selected")
        val provider = model.findProvider(settings.providers)
            ?: error("Provider not found")
        val providerSetting = settings.providers.find { it.id == provider.id }
            ?: error("Provider setting not found")

        val result = providerManager.getProviderByType(providerSetting).generateVideo(
            providerSetting = providerSetting,
            params = VideoGenerationParams(
                model = model,
                prompt = prompt,
                aspectRatio = aspectRatio,
                durationSeconds = durationSeconds,
                resolution = resolution,
                generateAudio = generateAudio,
                seed = seed,
                negativePrompt = negativePrompt,
                customHeaders = model.customHeaders,
                customBody = model.customBodies,
            )
        )

        if (result.items.isEmpty()) {
            error("Video generation returned no videos")
        }

        val timestamp = System.currentTimeMillis()
        val modelName = model.displayName.ifBlank { model.modelId }
        val safeModelName = modelName.toSafeFilename()
        val savedVideos = result.items.mapIndexed { index, item ->
            val file = File(filesManager.getVideosDir(), "${timestamp}_${safeModelName}_$index.mp4")
            val createdFile = saveVideoData(item.videoUrl, file)
            val relativePath = "videos/${createdFile.name}"
            genMediaRepository.insertMedia(
                GenMediaEntity(
                    path = relativePath,
                    modelId = modelName,
                    prompt = prompt,
                    createAt = timestamp,
                    type = GenMediaEntity.TYPE_VIDEO_GENERATION,
                )
            )
            SavedVideo(
                file = createdFile,
                relativePath = relativePath,
                sourceType = item.videoUrl.sourceType(),
                coverUrl = item.coverUrl,
            )
        }

        val payload = buildJsonObject {
            put("success", true)
            put("model", modelName)
            put("count", savedVideos.size)
            put("parameters", buildJsonObject {
                put("aspectRatio", aspectRatio)
                put("durationSeconds", durationSeconds)
                put("resolution", resolution)
                put("generateAudio", generateAudio)
                seed?.let { put("seed", it) }
                negativePrompt?.let { put("negativePrompt", it) }
            })
            put("videos", buildJsonArray {
                savedVideos.forEach { saved ->
                    add(
                        buildJsonObject {
                            put("path", saved.file.absolutePath)
                            put("relativePath", saved.relativePath)
                            put("sizeBytes", saved.file.length())
                            put("sourceType", saved.sourceType)
                            saved.coverUrl?.let { put("coverUrl", it) }
                        }
                    )
                }
            })
        }

        listOf(UIMessagePart.Text(payload.toString()))
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun saveVideoData(videoData: String, target: File): File {
        target.parentFile?.mkdirs()
        when {
            videoData.startsWith("http://", ignoreCase = true) ||
                videoData.startsWith("https://", ignoreCase = true) -> {
                downloadVideo(videoData, target)
            }

            videoData.startsWith("/") -> {
                File(videoData).copyTo(target, overwrite = true)
            }

            videoData.startsWith("data:", ignoreCase = true) -> {
                val base64Data = videoData.substringAfter("base64,", missingDelimiterValue = "")
                require(base64Data.isNotBlank()) { "Unsupported data URL video payload" }
                target.writeBytes(Base64.decode(base64Data.toByteArray()))
            }

            else -> {
                target.writeBytes(Base64.decode(videoData.toByteArray()))
            }
        }
        return target
    }

    private fun downloadVideo(url: String, target: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 20_000
            readTimeout = 10 * 60_000
            instanceFollowRedirects = true
        }
        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("Failed to download video: HTTP ${connection.responseCode}")
            }
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun String.toSafeFilename(): String {
        val normalized = replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_')
        return normalized.ifBlank { "video_model" }.take(60)
    }

    private fun String.sourceType(): String = when {
        startsWith("http://", ignoreCase = true) || startsWith("https://", ignoreCase = true) -> "url"
        startsWith("data:", ignoreCase = true) -> "data_url"
        startsWith("/") -> "file"
        else -> "base64"
    }

    private data class SavedVideo(
        val file: File,
        val relativePath: String,
        val sourceType: String,
        val coverUrl: String?,
    )

    private val SUPPORTED_ASPECT_RATIOS = setOf("16:9", "9:16", "1:1")
    private val SUPPORTED_RESOLUTIONS = setOf("480p", "720p", "1080p")
}
