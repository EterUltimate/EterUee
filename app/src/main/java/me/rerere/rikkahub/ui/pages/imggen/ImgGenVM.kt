package me.rerere.rikkahub.ui.pages.imggen

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
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
import me.rerere.rikkahub.data.db.entity.GenMediaEntity
import me.rerere.rikkahub.data.repository.GenMediaRepository
import me.rerere.rikkahub.utils.createImageFileFromBase64
import me.rerere.rikkahub.utils.getImagesDir
import java.io.File

@Serializable
data class GeneratedImage(
    val id: Int,
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

    val generatedImages: Flow<PagingData<GeneratedImage>> = Pager(
        config = PagingConfig(pageSize = 20, enablePlaceholders = false),
        pagingSourceFactory = {
            GeneratedImagePagingSource(repository = genMediaRepository, context = getApplication())
        }
    ).flow

    // No init needed - PagingSource handles loading automatically

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
                        id = 0, // Will be updated after database insertion
                        prompt = _prompt.value,
                        filePath = imageFile.absolutePath,
                        timestamp = System.currentTimeMillis(),
                        model = model.displayName
                    )
                    newImages.add(generatedImage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate image", e)
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private suspend fun saveImageToStorage(
        item: ImageGenerationItem,
        prompt: String,
        modelName: String,
        index: Int
    ): File {
        val context = getApplication<Application>()
        val imagesDir = context.getImagesDir()

        val timestamp = System.currentTimeMillis()
        val filename = "${timestamp}_${modelName}_$index.png"
        val imageFile = File(imagesDir, filename)

        val createdFile = context.createImageFileFromBase64(item.data, imageFile.absolutePath)

        // Save to database with relative path
        val relativePath = "images/${imageFile.name}"
        val entity = GenMediaEntity(
            path = relativePath,
            modelId = modelName,
            prompt = prompt,
            createAt = timestamp
        )
        genMediaRepository.insertMedia(entity)

        return createdFile
    }

    fun deleteImage(image: GeneratedImage) {
        viewModelScope.launch {
            try {
                val file = File(image.filePath)
                if (file.exists() && file.delete()) {
                    // Delete from database if it has a valid ID
                    if (image.id > 0) {
                        genMediaRepository.deleteMedia(image.id)
                    }
                    // PagingSource will automatically update UI
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
