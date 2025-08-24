package me.rerere.rikkahub.ui.pages.imggen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import me.rerere.ai.provider.ImageGenerationParams
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.ui.ImageGenerationItem
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.repository.GenMediaRepository
import me.rerere.rikkahub.utils.createImageFileFromBase64
import me.rerere.rikkahub.utils.getImagesDir
import me.rerere.rikkahub.utils.listImageFiles
import java.io.File
import kotlin.uuid.Uuid

@Serializable
data class GeneratedImage(
    val id: String,
    val prompt: String,
    val filePath: String,
    val timestamp: Long,
    val model: String
)

class ImgGenVM(
    context: Application,
    val settingsStore: SettingsStore,
    val providerManager: ProviderManager,
    val genMediaRepository: GenMediaRepository,
) : AndroidViewModel(context) {
    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt

    private val _numberOfImages = MutableStateFlow(1)
    val numberOfImages: StateFlow<Int> = _numberOfImages

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _generatedImages = MutableStateFlow<List<GeneratedImage>>(emptyList())
    val generatedImages: StateFlow<List<GeneratedImage>> = _generatedImages

    init {
        loadGeneratedImages()
    }

    fun updatePrompt(prompt: String) {
        _prompt.value = prompt
    }

    fun updateNumberOfImages(count: Int) {
        _numberOfImages.value = count.coerceIn(1, 4)
    }

    fun clearError() {
        _error.value = null
    }

    fun generateImage() {
        viewModelScope.launch {
            try {
                _isGenerating.value = true
                _error.value = null

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
                    numOfImages = _numberOfImages.value
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
                        id = Uuid.random().toString(),
                        prompt = _prompt.value,
                        filePath = imageFile.absolutePath,
                        timestamp = System.currentTimeMillis(),
                        model = model.displayName
                    )
                    newImages.add(generatedImage)
                }

                _generatedImages.value = newImages + _generatedImages.value

            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate image", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun saveImageToStorage(item: ImageGenerationItem, prompt: String, modelName: String, index: Int): File {
        val context = getApplication<Application>()
        val imagesDir = context.getImagesDir()

        val timestamp = System.currentTimeMillis()
        val filename = "${timestamp}_${modelName}_$index.png"
        val imageFile = File(imagesDir, filename)

        return context.createImageFileFromBase64(item.data, imageFile.absolutePath)
    }

    fun loadGeneratedImages() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val imageFiles = context.listImageFiles()

                val images = imageFiles.map { file ->
                    // Parse filename to extract metadata
                    val parts = file.nameWithoutExtension.split("_")
                    val timestamp = parts.getOrNull(0)?.toLongOrNull() ?: file.lastModified()
                    val model = if (parts.size > 1) parts.subList(1, parts.size - 1).joinToString("_") else "Unknown"

                    GeneratedImage(
                        id = file.name,
                        prompt = "Generated Image",
                        filePath = file.absolutePath,
                        timestamp = timestamp,
                        model = model
                    )
                }.sortedByDescending { it.timestamp }

                _generatedImages.value = images
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load generated images", e)
            }
        }
    }

    fun deleteImage(image: GeneratedImage) {
        viewModelScope.launch {
            try {
                val file = File(image.filePath)
                if (file.exists() && file.delete()) {
                    _generatedImages.value = _generatedImages.value.filter { it.id != image.id }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete image", e)
                _error.value = "Failed to delete image"
            }
        }
    }

    companion object {
        private const val TAG = "ImgGenVM"
    }
}
