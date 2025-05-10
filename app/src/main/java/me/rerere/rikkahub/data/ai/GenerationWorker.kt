package me.rerere.rikkahub.data.ai

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.transformers.MessageTimeTransformer
import me.rerere.ai.ui.transformers.PlaceholderTransformer
import me.rerere.ai.ui.transformers.SearchTextTransformer
import me.rerere.ai.ui.transformers.ThinkTagTransformer
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.data.repository.MemoryRepository
import me.rerere.rikkahub.utils.JsonInstant

private const val TAG = "GenerationWorker"

private val inputTransformers by lazy {
    listOf(
        SearchTextTransformer,
        PlaceholderTransformer,
    )
}

private val outputTransformers by lazy {
    listOf(
        ThinkTagTransformer,
    )
}

class GenerationWorker(
    appContext: Context,
    params: WorkerParameters,
    private val handler: GenerationHandler,
    private val settingsStore: SettingsStore,
    private val conversationRepo: ConversationRepository,
    private val memoryRepository: MemoryRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val settings =
            JsonInstant.decodeFromString<Settings>(checkNotNull(inputData.getString("settings")))
        val model =
            JsonInstant.decodeFromString<Model>(checkNotNull(inputData.getString("model")))
        val messages =
            JsonInstant.decodeFromString<List<UIMessage>>(checkNotNull(inputData.getString("messages")))
        val assistant =
            JsonInstant.decodeFromString<Assistant?>(inputData.getString("assistant") ?: "null")

        handler.streamText(
            settings = settings,
            model = model,
            messages = messages,
            assistant = assistant,
            memories = {
                if (assistant != null) memoryRepository.getMemoriesOfAssistant(assistant.id.toString()) else emptyList()
            },
            onCreationMemory = {
                memoryRepository.addMemory(assistant!!.id.toString(), it)
            },
            onUpdateMemory = { id, content ->
                memoryRepository.updateContent(id, content)
            },
            onDeleteMemory = {
                memoryRepository.deleteMemory(it)
            },
            inputTransformers = buildList {
                addAll(inputTransformers)
                if (assistant?.enableMessageTime == true) add(MessageTimeTransformer)
            },
            outputTransformers = outputTransformers,
            maxSteps = 5,
        ).collect { chunk ->
            when (chunk) {
                is GenerationChunk.Messages -> {

                }

                is GenerationChunk.TokenUsage -> {

                }
            }
        }

        return Result.success()
    }
}