package com.eterultimate.eteruee.ui.pages.imggen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import com.eterultimate.eteruee.ai.provider.ImageEditParams
import com.eterultimate.eteruee.ai.provider.ImageGenerationParams
import com.eterultimate.eteruee.ai.provider.ProviderManager
import com.eterultimate.eteruee.ai.provider.ReferenceImage
import com.eterultimate.eteruee.ai.provider.VideoGenerationParams
import com.eterultimate.eteruee.ai.ui.ImageAspectRatio
import com.eterultimate.eteruee.ai.ui.ImageGenerationItem
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.datastore.findModelById
import com.eterultimate.eteruee.data.datastore.findProvider
import com.eterultimate.eteruee.data.db.entity.GenMediaEntity
import com.eterultimate.eteruee.data.files.FilesManager
import com.eterultimate.eteruee.data.repository.GenMediaRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class GeneratedImage(
    val id: Int,
    val prompt: String,
    val filePath: String,
    val timestamp: Long,
    val model: String
)

private fun GenMediaEntity.toGeneratedImage(filesManager: FilesManager): GeneratedImage {
    val dir = if (type == GenMediaEntity.TYPE_VIDEO_GENERATION) {
        filesManager.getVideosDir()
    } else {
        filesManager.getImagesDir()
    }
    val prefix = if (type == GenMediaEntity.TYPE_VIDEO_GENERATION) "videos/" else "images/"
    val fullPath = File(dir, this.path.removePrefix(prefix)).absolutePath

    return GeneratedImage(
        id = this.id,
        prompt = this.prompt,
        filePath = fullPath,
        timestamp = this.createAt,
        model = this.modelId
    )
}

class ImgGenVM(
    context: Application,
    val settingsStore: SettingsStore,
    val providerManager: ProviderManager,
    val genMediaRepository: GenMediaRepository,
    private val filesManager: FilesManager,
) : AndroidViewModel(context) {
    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    private val _numberOfImages = MutableStateFlow(1)
    val numberOfImages: StateFlow<Int> = _numberOfImages

    private val _aspectRatio = MutableStateFlow(ImageAspectRatio.SQUARE)
    val aspectRatio: StateFlow<ImageAspectRatio> = _aspectRatio

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating
    private var cancelJob: Job? = null

    // Video generation mode
    private val _isVideoMode = MutableStateFlow(false)
    val isVideoMode: StateFlow<Boolean> = _isVideoMode

    private val _videoDuration = MutableStateFlow(5)
    val videoDuration: StateFlow<Int> = _videoDuration

    private val _videoResolution = MutableStateFlow("720p")
    val videoResolution: StateFlow<String> = _videoResolution

    private val _videoAspectRatio = MutableStateFlow("16:9")
    val videoAspectRatio: StateFlow<String> = _videoAspectRatio

    private val _generateAudio = MutableStateFlow(false)
    val generateAudio: StateFlow<Boolean> = _generateAudio

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _currentGeneratedImages = MutableStateFlow<List<GeneratedImage>>(emptyList())
    val currentGeneratedImages: StateFlow<List<GeneratedImage>> = _currentGeneratedImages

    private val _referenceImages = MutableStateFlow<List<String>>(emptyList())
    val referenceImages: StateFlow<List<String>> = _referenceImages

    val pager = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = { genMediaRepository.getAllMedia() }
    )
    val generatedImages: Flow<PagingData<GeneratedImage>> = pager.flow
        .map { pagingData ->
            pagingData.map { entity -> entity.toGeneratedImage(filesManager) }
        }
        .cachedIn(viewModelScope)

    fun updatePrompt(prompt: String) {
        _prompt.value = prompt
    }

    fun updateNumberOfImages(count: Int) {
        _numberOfImages.value = count.coerceIn(1, 4)
    }

    fun updateAspectRatio(aspectRatio: ImageAspectRatio) {
        _aspectRatio.value = aspectRatio
    }

    fun setVideoMode(enabled: Boolean) {
        _isVideoMode.value = enabled
    }

    fun updateVideoDuration(seconds: Int) {
        _videoDuration.value = seconds.coerceIn(3, 10)
    }

    fun updateVideoResolution(resolution: String) {
        _videoResolution.value = resolution
    }

    fun updateVideoAspectRatio(aspectRatio: String) {
        _videoAspectRatio.value = aspectRatio
    }

    fun updateGenerateAudio(enabled: Boolean) {
        _generateAudio.value = enabled
    }

    fun addReferenceImages(paths: List<String>) {
        _referenceImages.value = (_referenceImages.value + paths).distinct().take(MAX_REFERENCE_IMAGES)
    }

    fun removeReferenceImage(path: String) {
        _referenceImages.value = _referenceImages.value.filterNot { it == path }
        deleteReferenceFiles(listOf(path))
    }

    fun clearReferenceImages() {
        deleteReferenceFiles(_referenceImages.value)
        _referenceImages.value = emptyList()
    }

    private fun deleteReferenceFiles(paths: List<String>) {
        paths.forEach { path ->
            runCatching {
                val file = File(path)
                if (file.exists()) file.delete()
            }
        }
    }

    fun startNewSession() {
        cancelJob?.cancel()
        clearReferenceImages()
        _prompt.value = ""
        _currentGeneratedImages.value = emptyList()
        _error.value = null
        _isGenerating.value = false
    }

    fun clearError() {
        _error.value = null
    }

    fun generateImage() {
        if (isVideoMode.value) {
            generateVideo()
            return
        }
        if(prompt.value.isBlank()) return
        cancelJob?.cancel()
        cancelJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _error.value = null
                _currentGeneratedImages.value = emptyList()

                val settings = settingsStore.settingsFlow.first()
                val model = settings.findModelById(settings.imageGenerationModelId)
                    ?: throw IllegalStateException("No model selected")

                val provider = model.findProvider(settings.providers)
                    ?: throw IllegalStateException("Provider not found")

                val providerSetting = settings.providers.find { it.id == provider.id }
                    ?: throw IllegalStateException("Provider setting not found")

                val params = ImageGenerationParams(
                    model = model,
                    prompt = _prompt.value,
                    numOfImages = _numberOfImages.value,
                    aspectRatio = _aspectRatio.value,
                    customHeaders = model.customHeaders,
                    customBody = model.customBodies
                )

                val result = providerManager.getProviderByType(provider)
                    .generateImage(providerSetting, params)

                val newImages = mutableListOf<GeneratedImage>()

                result.items.forEachIndexed { index, item ->
                    val imageFile = saveImageToStorage(
                        item = item,
                        prompt = _prompt.value,
                        modelName = model.displayName,
                        index = index
                    )
                    val generatedImage = GeneratedImage(
                        id = 0, // Will be updated after database insertion
                        prompt = _prompt.value,
                        filePath = imageFile.absolutePath,
                        timestamp = System.currentTimeMillis(),
                        model = model.displayName
                    )
                    newImages.add(generatedImage)
                }

                _currentGeneratedImages.value = newImages
            } catch (e: Exception) {
                if(e is CancellationException) return@launch
                Log.e(TAG, "Failed to generate image", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateVideo() {
        if (prompt.value.isBlank()) return
        cancelJob?.cancel()
        cancelJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _error.value = null
                _currentGeneratedImages.value = emptyList()

                val settings = settingsStore.settingsFlow.first()
                val model = settings.findModelById(settings.videoGenerationModelId)
                    ?: throw IllegalStateException("No video model selected")

                val provider = model.findProvider(settings.providers)
                    ?: throw IllegalStateException("Provider not found")

                val providerSetting = settings.providers.find { it.id == provider.id }
                    ?: throw IllegalStateException("Provider setting not found")

                val refImages = _referenceImages.value.map { path ->
                    // Convert local file paths to data URIs or upload as needed
                    // For Seedance API, image_url supports base64 data URI or HTTP URL
                    val file = File(path)
                    if (file.exists()) {
                        val bytes = file.readBytes()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        val mimeType = when (file.extension.lowercase()) {
                            "jpg", "jpeg" -> "image/jpeg"
                            "webp" -> "image/webp"
                            else -> "image/png"
                        }
                        ReferenceImage(
                            url = "data:$mimeType;base64,$base64",
                            role = null // Will be set based on position: first=first_frame, rest=default
                        )
                    } else {
                        ReferenceImage(url = path)
                    }
                }.let { refs ->
                    // If there are reference images, mark the first as first_frame
                    if (refs.size > 1) {
                        refs.mapIndexed { index, ref ->
                            if (index == 0) ref.copy(role = "first_frame")
                            else if (index == refs.size - 1) ref.copy(role = "last_frame")
                            else ref
                        }
                    } else if (refs.size == 1) {
                        refs.mapIndexed { _, ref -> ref.copy(role = "first_frame") }
                    } else {
                        refs
                    }
                }

                val params = VideoGenerationParams(
                    model = model,
                    prompt = _prompt.value,
                    referenceImages = refImages,
                    aspectRatio = _videoAspectRatio.value,
                    durationSeconds = _videoDuration.value,
                    resolution = _videoResolution.value,
                    generateAudio = _generateAudio.value,
                    customHeaders = model.customHeaders,
                    customBody = model.customBodies
                )

                val result = providerManager.getProviderByType(provider)
                    .generateVideo(providerSetting, params)

                val newImages = mutableListOf<GeneratedImage>()

                result.items.forEachIndexed { index, item ->
                    val videoFile = saveVideoToStorage(
                        videoUrl = item.videoUrl,
                        prompt = _prompt.value,
                        modelName = model.displayName,
                        index = index
                    )
                    val generatedImage = GeneratedImage(
                        id = 0,
                        prompt = _prompt.value,
                        filePath = videoFile.absolutePath,
                        timestamp = System.currentTimeMillis(),
                        model = model.displayName
                    )
                    newImages.add(generatedImage)
                }

                _currentGeneratedImages.value = newImages
                clearReferenceImages()
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                Log.e(TAG, "Failed to generate video", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun editImage() {
        if (prompt.value.isBlank() || referenceImages.value.isEmpty()) return
        cancelJob?.cancel()
        cancelJob = viewModelScope.launch {
            try {
                _isGenerating.value = true
                _error.value = null
                _currentGeneratedImages.value = emptyList()

                val settings = settingsStore.settingsFlow.first()
                val model = settings.findModelById(settings.imageGenerationModelId)
                    ?: throw IllegalStateException("No model selected")

                val provider = model.findProvider(settings.providers)
                    ?: throw IllegalStateException("Provider not found")

                val providerSetting = settings.providers.find { it.id == provider.id }
                    ?: throw IllegalStateException("Provider setting not found")

                val sourceImages = _referenceImages.value
                val params = ImageEditParams(
                    model = model,
                    prompt = _prompt.value,
                    images = sourceImages,
                    numOfImages = _numberOfImages.value,
                    aspectRatio = _aspectRatio.value,
                    customHeaders = model.customHeaders,
                    customBody = model.customBodies
                )

                val result = providerManager.getProviderByType(provider)
                    .editImage(providerSetting, params)

                val newImages = mutableListOf<GeneratedImage>()

                result.items.forEachIndexed { index, item ->
                    val imageFile = saveImageToStorage(
                        item = item,
                        prompt = _prompt.value,
                        modelName = model.displayName,
                        index = index,
                        type = GenMediaEntity.TYPE_IMAGE_EDIT,
                    )
                    val generatedImage = GeneratedImage(
                        id = 0,
                        prompt = _prompt.value,
                        filePath = imageFile.absolutePath,
                        timestamp = System.currentTimeMillis(),
                        model = model.displayName
                    )
                    newImages.add(generatedImage)
                }

                _currentGeneratedImages.value = newImages
                clearReferenceImages()
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                Log.e(TAG, "Failed to edit image", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun cancelGeneration() {
        cancelJob?.cancel()
        clearReferenceImages()
    }

    private suspend fun saveImageToStorage(
        item: ImageGenerationItem,
        prompt: String,
        modelName: String,
        index: Int,
        type: String = GenMediaEntity.TYPE_IMAGE_GENERATION,
        sourcePaths: String? = null,
    ): File {
        val imagesDir = filesManager.getImagesDir()

        val timestamp = System.currentTimeMillis()
        val filename = "${timestamp}_${modelName}_$index.png"
        val imageFile = File(imagesDir, filename)

        val createdFile = filesManager.createImageFileFromBase64(item.data, imageFile.absolutePath)

        // Save to database with relative path
        val relativePath = "images/${imageFile.name}"
        val entity = GenMediaEntity(
            path = relativePath,
            modelId = modelName,
            prompt = prompt,
            createAt = timestamp,
            type = type,
            sourcePaths = sourcePaths,
        )
        genMediaRepository.insertMedia(entity)

        return createdFile
    }

    private suspend fun saveVideoToStorage(
        videoUrl: String,
        prompt: String,
        modelName: String,
        index: Int,
    ): File {
        val videosDir = filesManager.getVideosDir()

        val timestamp = System.currentTimeMillis()
        val filename = "${timestamp}_${modelName}_$index.mp4"
        val videoFile = File(videosDir, filename)

        // Download video from URL
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(videoUrl)
                .get()
                .build()
            val response = OkHttpClient().newCall(request).execute()
            if (!response.isSuccessful) {
                error("Failed to download video: ${response.code}")
            }
            response.body.byteStream().use { input ->
                videoFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }

        // Save to database with relative path
        val relativePath = "videos/${videoFile.name}"
        val entity = GenMediaEntity(
            path = relativePath,
            modelId = modelName,
            prompt = prompt,
            createAt = timestamp,
            type = GenMediaEntity.TYPE_VIDEO_GENERATION,
        )
        genMediaRepository.insertMedia(entity)

        return videoFile
    }

    fun deleteImage(image: GeneratedImage) {
        viewModelScope.launch {
            try {
                // Delete from database first
                genMediaRepository.deleteMedia(image.id)

                // Then delete the file
                val file = File(image.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete image", e)
                _error.value = "Failed to delete image"
            }
        }
    }

    companion object {
        private const val TAG = "ImgGenVM"
        private const val MAX_REFERENCE_IMAGES = 16
    }
}

