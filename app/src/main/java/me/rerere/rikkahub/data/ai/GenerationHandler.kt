package me.rerere.rikkahub.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.core.SchemaBuilder
import me.rerere.ai.core.Tool
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.Provider
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.MessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.handleMessageChunk
import me.rerere.ai.ui.transforms
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantMemory
import kotlin.uuid.Uuid

private const val TAG = "GenerationHandler"

class GenerationHandler(private val context: Context, private val json: Json) {
    fun streamText(
        settings: Settings,
        model: Model,
        messages: List<UIMessage>,
        transformers: List<MessageTransformer> = emptyList(),
        assistant: (() -> Assistant)? = null,
        tools: List<Tool> = emptyList(),
        onCreationMemory: ((String) -> AssistantMemory)? = null,
        onUpdateMemory: ((Uuid, String) -> AssistantMemory)? = null,
        onDeleteMemory: ((Uuid) -> Unit)? = null,
    ): Flow<List<UIMessage>> = flow {
        val provider = model.findProvider(settings.providers) ?: error("Provider not found")
        val providerImpl = ProviderManager.getProviderByType(provider)

        var messages: List<UIMessage> = messages

        generateInternal(
            assistant,
            messages,
            {
                messages = it
                emit(messages)
            },
            transformers,
            model,
            providerImpl,
            provider,
            onCreationMemory,
            onUpdateMemory,
            onDeleteMemory,
            tools,
            stream = true
        )
    }.flowOn(Dispatchers.IO)

    private suspend fun generateInternal(
        assistant: (() -> Assistant)?,
        messages: List<UIMessage>,
        onUpdateMessages: suspend (List<UIMessage>) -> Unit,
        transformers: List<MessageTransformer>,
        model: Model,
        providerImpl: Provider<ProviderSetting>,
        provider: ProviderSetting,
        onCreationMemory: ((String) -> AssistantMemory)?,
        onUpdateMemory: ((Uuid, String) -> AssistantMemory)?,
        onDeleteMemory: ((Uuid) -> Unit)?,
        tools: List<Tool>,
        stream: Boolean
    ) {
        val assistantRef = assistant?.invoke()
        val internalMessages = buildList {
            if (assistantRef != null) {
                // 如果存在助手，构造系统消息
                add(UIMessage.system(buildString {
                    // 如果助手有系统提示，则添加到消息中
                    if (assistantRef.systemPrompt.isNotBlank()) {
                        append(assistantRef.systemPrompt)
                    }

                    // 记忆
                    if (assistantRef.enableMemory) {
                        append(buildMemoryPrompt(assistantRef))
                    }
                }))
            }
            addAll(messages)
        }.transforms(transformers, context, model)

        var messages: List<UIMessage> = messages
        val params = TextGenerationParams(
            model = model,
            temperature = assistantRef?.temperature,
            tools = buildList {
                Log.i(TAG, "generateInternal: build tools($assistantRef)")
                if (assistantRef?.enableMemory == true) {
                    checkNotNull(onCreationMemory)
                    checkNotNull(onUpdateMemory)
                    checkNotNull(onDeleteMemory)
                    buildMemoryTools(
                        onCreation = onCreationMemory,
                        onUpdate = onUpdateMemory,
                        onDelete = onDeleteMemory
                    ).let(this::addAll)
                }
                addAll(tools)
            }
        )
        if (stream) {
            providerImpl.streamText(
                providerSetting = provider,
                messages = internalMessages,
                params = params
            ).collect {
                messages = messages.handleMessageChunk(it)
                onUpdateMessages(messages)
            }
        } else {
            messages = messages.handleMessageChunk(
                providerImpl.generateText(
                    providerSetting = provider,
                    messages = internalMessages,
                    params = params,
                )
            )
            onUpdateMessages(messages)
        }
    }

    private fun buildMemoryTools(
        onCreation: (String) -> AssistantMemory,
        onUpdate: (Uuid, String) -> AssistantMemory,
        onDelete: (Uuid) -> Unit
    ) = listOf(
        Tool.Function(
            name = "edit_memory",
            description = "create or update memory item, if the id is empty, create a new memory item, otherwise update the memory item",
            parameters = SchemaBuilder.obj(
                "id" to SchemaBuilder.str(),
                "content" to SchemaBuilder.str(),
                required = listOf("content")
            ),
            execute = {
                val params = it.jsonObject
                val id = params["id"]?.jsonPrimitive?.contentOrNull?.let { Uuid.parse(it) }
                val content =
                    params["content"]?.jsonPrimitive?.contentOrNull ?: error("content is required")
                json.encodeToJsonElement(
                    AssistantMemory.serializer(), if (id == null) {
                        onCreation(content)
                    } else {
                        onUpdate(id, content)
                    }
                )
            }
        ),
        Tool.Function(
            name = "delete_memory",
            description = "delete memory item",
            parameters = SchemaBuilder.obj(
                "id" to SchemaBuilder.str(),
                required = listOf("id")
            ),
            execute = {
                val params = it.jsonObject
                val id = params["id"]?.jsonPrimitive?.contentOrNull?.let { Uuid.parse(it) }
                    ?: error("id is required")
                onDelete(id)
                JsonPrimitive(true)
            }
        )
    )

    private fun buildMemoryPrompt(assistant: Assistant) = buildString {
        append(
            """
            ## Memories
            The following content within <memories> contains some memories related to the user, which you can use to better
            understand user preferences. At the same time, you can also actively call tools to create or update memories.
        """.trimIndent()
        )
        append("<memories>\n")
        assistant.memories.forEach { memory ->
            append("<item>\n")
            append("<id>${memory.id}</id>")
            append("<content>${memory.content}</content>")
            append("</item>\n")
        }
        append("</memories>\n")
    }
}