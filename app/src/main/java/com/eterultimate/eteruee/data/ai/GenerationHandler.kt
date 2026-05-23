package com.eterultimate.eteruee.data.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.put
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.core.ReasoningLevel
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.core.merge
import com.eterultimate.eteruee.ai.provider.CustomBody
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.provider.Provider
import com.eterultimate.eteruee.ai.provider.ProviderManager
import com.eterultimate.eteruee.ai.provider.ProviderSetting
import com.eterultimate.eteruee.ai.provider.TextGenerationParams
import com.eterultimate.eteruee.ai.registry.ModelRegistry
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.ToolExecutor
import com.eterultimate.eteruee.ai.sdk.ToolResult
import com.eterultimate.eteruee.ai.sdk.streamTextWithSubagent
import com.eterultimate.eteruee.ai.subagent.DefaultSubagentToolExecutor
import com.eterultimate.eteruee.ai.subagent.PlanStepResult
import com.eterultimate.eteruee.ai.subagent.SubagentPlan
import com.eterultimate.eteruee.ai.subagent.SubagentTextChunk
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.ai.ui.ToolApprovalState
import com.eterultimate.eteruee.ai.ui.handleMessageChunk
import com.eterultimate.eteruee.ai.ui.limitContext
import com.eterultimate.eteruee.data.ai.transformers.InputMessageTransformer
import com.eterultimate.eteruee.data.ai.transformers.MessageTransformer
import com.eterultimate.eteruee.data.ai.transformers.OutputMessageTransformer
import com.eterultimate.eteruee.data.ai.transformers.onGenerationFinish
import com.eterultimate.eteruee.data.ai.transformers.transforms
import com.eterultimate.eteruee.data.ai.transformers.visualTransforms
import com.eterultimate.eteruee.data.ai.tools.buildMemoryTools
import com.eterultimate.eteruee.data.datastore.Settings
import com.eterultimate.eteruee.data.datastore.findModelById
import com.eterultimate.eteruee.data.datastore.findProvider
import com.eterultimate.eteruee.data.model.Assistant
import com.eterultimate.eteruee.data.model.AssistantMemory
import com.eterultimate.eteruee.data.repository.ConversationRepository
import com.eterultimate.eteruee.data.repository.MemoryRepository
import com.eterultimate.eteruee.utils.applyPlaceholders
import java.util.Locale
import kotlin.time.Clock
import kotlin.uuid.Uuid

private const val TAG = "GenerationHandler"

@Serializable
sealed interface GenerationChunk {
    data class Messages(
        val messages: List<UIMessage>
    ) : GenerationChunk
}

class GenerationHandler(
    private val context: Context,
    private val providerManager: ProviderManager,
    private val aiSDK: AISDK,
    private val json: Json,
    private val memoryRepo: MemoryRepository,
    private val conversationRepo: ConversationRepository,
    private val aiLoggingManager: AILoggingManager,
) {
    fun generateText(
        settings: Settings,
        model: Model,
        messages: List<UIMessage>,
        inputTransformers: List<InputMessageTransformer> = emptyList(),
        outputTransformers: List<OutputMessageTransformer> = emptyList(),
        assistant: Assistant,
        memories: List<AssistantMemory>? = null,
        tools: List<Tool> = emptyList(),
        maxSteps: Int = 256,
        processingStatus: MutableStateFlow<String?> = MutableStateFlow(null),
        conversationSystemPrompt: String? = null,
        conversationModeInjectionIds: Set<Uuid> = emptySet(),
        conversationLorebookIds: Set<Uuid> = emptySet(),
    ): Flow<GenerationChunk> = flow {
        val provider = model.findProvider(settings.providers) ?: error("Provider not found")
        val providerImpl = providerManager.getProviderByType(provider)

        var messages: List<UIMessage> = messages

        for (stepIndex in 0 until maxSteps) {
            Log.i(TAG, "streamText: start step #$stepIndex (${model.id})")

            val toolsInternal = buildList {
                Log.i(TAG, "generateInternal: build tools($assistant)")
                if (assistant.enableMemory == true) {
                    val memoryAssistantId = if (assistant.useGlobalMemory) {
                        MemoryRepository.GLOBAL_MEMORY_ID
                    } else {
                        assistant.id.toString()
                    }
                    buildMemoryTools(
                        json = json,
                        onCreation = { content ->
                            memoryRepo.addMemory(memoryAssistantId, content)
                        },
                        onUpdate = { id, content ->
                            memoryRepo.updateContent(id, content)
                        },
                        onDelete = { id ->
                            memoryRepo.deleteMemory(id)
                        }
                    ).let(this::addAll)
                }
                addAll(tools)
            }

            // Check if we have tool calls ready to continue after user interaction.
            val pendingTools = messages.lastOrNull()?.getTools()?.filter {
                it.canResumeExecution
            } ?: emptyList()

            if (settings.enableSubagent && pendingTools.isEmpty()) {
                messages = generateWithSubagent(
                    settings = settings,
                    model = model,
                    messages = messages,
                    inputTransformers = inputTransformers,
                    outputTransformers = outputTransformers,
                    assistant = assistant,
                    memories = memories ?: emptyList(),
                    tools = toolsInternal,
                    provider = provider,
                    processingStatus = processingStatus,
                    conversationSystemPrompt = conversationSystemPrompt,
                    conversationModeInjectionIds = conversationModeInjectionIds,
                    conversationLorebookIds = conversationLorebookIds,
                    onUpdateMessages = {
                        messages = it
                        emit(GenerationChunk.Messages(it))
                    }
                )
                break
            }

            val toolsToProcess: List<UIMessagePart.Tool>

            // Skip generation if we have approved/denied tool calls to handle
            if (pendingTools.isEmpty()) {
                generateInternal(
                    assistant = assistant,
                    settings = settings,
                    messages = messages,
                    onUpdateMessages = {
                        messages = it.transforms(
                            transformers = outputTransformers,
                            context = context,
                            model = model,
                            assistant = assistant,
                            settings = settings
                        )
                        emit(
                            GenerationChunk.Messages(
                                messages.visualTransforms(
                                    transformers = outputTransformers,
                                    context = context,
                                    model = model,
                                    assistant = assistant,
                                    settings = settings
                                )
                            )
                        )
                    },
                    transformers = inputTransformers,
                    model = model,
                    providerImpl = providerImpl,
                    provider = provider,
                    tools = toolsInternal,
                    memories = memories ?: emptyList(),
                    stream = assistant.streamOutput,
                    processingStatus = processingStatus,
                    conversationSystemPrompt = conversationSystemPrompt,
                    conversationModeInjectionIds = conversationModeInjectionIds,
                    conversationLorebookIds = conversationLorebookIds,
                )
                messages = messages.visualTransforms(
                    transformers = outputTransformers,
                    context = context,
                    model = model,
                    assistant = assistant,
                    settings = settings
                )
                messages = messages.onGenerationFinish(
                    transformers = outputTransformers,
                    context = context,
                    model = model,
                    assistant = assistant,
                    settings = settings
                )
                messages = messages.slice(0 until messages.lastIndex) + messages.last().copy(
                    finishedAt = Clock.System.now()
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                )
                emit(GenerationChunk.Messages(messages))

                val tools = messages.last().getTools().filter { !it.isExecuted }
                if (tools.isEmpty()) {
                    // no tool calls, break
                    break
                }

                // Check for tools that need approval
                var hasPendingApproval = false
                val updatedTools = tools.map { tool ->
                    val toolDef = toolsInternal.find { it.name == tool.toolName }
                    when {
                        // Tool needs approval and state is Auto -> set to Pending
                        toolDef?.needsApproval == true && tool.approvalState is ToolApprovalState.Auto -> {
                            hasPendingApproval = true
                            tool.copy(approvalState = ToolApprovalState.Pending)
                        }
                        // State is Pending -> keep waiting
                        tool.approvalState is ToolApprovalState.Pending -> {
                            hasPendingApproval = true
                            tool
                        }

                        else -> tool
                    }
                }

                // If any tools were updated to Pending, update the message and break
                if (updatedTools != tools) {
                    val lastMessage = messages.last()
                    val updatedParts = lastMessage.parts.map { part ->
                        if (part is UIMessagePart.Tool) {
                            updatedTools.find { it.toolCallId == part.toolCallId } ?: part
                        } else {
                            part
                        }
                    }
                    messages = messages.dropLast(1) + lastMessage.copy(parts = updatedParts)
                    emit(GenerationChunk.Messages(messages))
                }

                // If there are pending approvals, break and wait for user
                if (hasPendingApproval) {
                    Log.i(TAG, "generateText: waiting for tool approval")
                    break
                }

                toolsToProcess = updatedTools
            } else {
                // Resuming after user interaction - use the resumable tools directly.
                Log.i(TAG, "generateText: resuming with ${pendingTools.size} resumable tools")
                toolsToProcess = messages.last().getTools().filter { it.canResumeExecution }
            }

            // Handle tools (execute approved tools, handle denied tools)
            val executedTools = arrayListOf<UIMessagePart.Tool>()
            toolsToProcess.forEach { tool ->
                when (tool.approvalState) {
                    is ToolApprovalState.Denied -> {
                        // Tool was denied by user
                        val reason = (tool.approvalState as ToolApprovalState.Denied).reason
                        executedTools += tool.copy(
                            output = listOf(
                                UIMessagePart.Text(
                                    json.encodeToString(
                                        buildJsonObject {
                                            put(
                                                "error",
                                                JsonPrimitive("Tool execution denied by user. Reason: ${reason.ifBlank { "No reason provided" }}")
                                            )
                                        }
                                    )
                                )
                            )
                        )
                    }

                    is ToolApprovalState.Answered -> {
                        // Tool was answered by user (e.g., ask_user tool)
                        val answer = (tool.approvalState as ToolApprovalState.Answered).answer
                        executedTools += tool.copy(
                            output = listOf(
                                UIMessagePart.Text(answer)
                            )
                        )
                    }

                    is ToolApprovalState.Pending -> {
                        // Should not reach here, but just in case
                    }

                    else -> {
                        // Auto or Approved - execute the tool
                        runCatching {
                            val toolDef = toolsInternal.find { toolDef -> toolDef.name == tool.toolName }
                                ?: error("Tool ${tool.toolName} not found")
                            val args = runCatching {
                                json.parseToJsonElement(tool.input.ifBlank { "{}" })
                            }.getOrElse {
                                error("Invalid tool arguments JSON for ${tool.toolName}: ${it.message}")
                            }
                            Log.i(TAG, "generateText: executing tool ${toolDef.name} with args: $args")
                            val result = toolDef.execute(args)
                            executedTools += tool.copy(output = result)
                        }.onFailure {
                            it.printStackTrace()
                            executedTools += tool.copy(
                                output = listOf(
                                    UIMessagePart.Text(
                                        json.encodeToString(
                                            buildJsonObject {
                                                put(
                                                    "error",
                                                    JsonPrimitive(buildString {
                                                        append("[${it.javaClass.name}] ${it.message}")
                                                        append("\n${it.stackTraceToString()}")
                                                    })
                                                )
                                            }
                                        )
                                    )
                                )
                            )
                        }
                    }
                }
            }

            if (executedTools.isEmpty()) {
                // No results to add (all tools were pending)
                break
            }

            // Update last message with executed tools (NOT create TOOL message)
            val lastMessage = messages.last()
            val updatedParts = lastMessage.parts.map { part ->
                if (part is UIMessagePart.Tool) {
                    executedTools.find { it.toolCallId == part.toolCallId } ?: part
                } else part
            }
            messages = messages.dropLast(1) + lastMessage.copy(parts = updatedParts)
            emit(
                GenerationChunk.Messages(
                    messages.transforms(
                        transformers = outputTransformers,
                        context = context,
                        model = model,
                        assistant = assistant,
                        settings = settings
                    )
                )
            )
        }

    }.flowOn(Dispatchers.IO)

    private suspend fun generateInternal(
        assistant: Assistant,
        settings: Settings,
        messages: List<UIMessage>,
        onUpdateMessages: suspend (List<UIMessage>) -> Unit,
        transformers: List<MessageTransformer>,
        model: Model,
        providerImpl: Provider<ProviderSetting>,
        provider: ProviderSetting,
        tools: List<Tool>,
        memories: List<AssistantMemory>,
        stream: Boolean,
        processingStatus: MutableStateFlow<String?> = MutableStateFlow(null),
        conversationSystemPrompt: String? = null,
        conversationModeInjectionIds: Set<Uuid> = emptySet(),
        conversationLorebookIds: Set<Uuid> = emptySet(),
    ) {
        val internalMessages = buildInternalMessages(
            assistant = assistant,
            settings = settings,
            messages = messages,
            transformers = transformers,
            model = model,
            tools = tools,
            memories = memories,
            processingStatus = processingStatus,
            conversationSystemPrompt = conversationSystemPrompt,
            conversationModeInjectionIds = conversationModeInjectionIds,
            conversationLorebookIds = conversationLorebookIds,
        )

        var messages: List<UIMessage> = messages
        val params = TextGenerationParams(
            model = model,
            temperature = assistant.temperature,
            topP = assistant.topP,
            maxTokens = assistant.maxTokens,
            tools = tools,
            reasoningLevel = assistant.reasoningLevel,
            customHeaders = buildList {
                addAll(assistant.customHeaders)
                addAll(model.customHeaders)
            },
            customBody = buildList {
                addAll(assistant.customBodies)
                addAll(model.customBodies)
            }
        )
        if (stream) {
            aiLoggingManager.addLog(
                AILogging.Generation(
                    params = params,
                    messages = messages,
                    providerSetting = provider,
                    stream = true
                )
            )
            providerImpl.streamText(
                providerSetting = provider,
                messages = internalMessages,
                params = params
            ).collect {
                messages = messages.handleMessageChunk(chunk = it, model = model)
                it.usage?.let { usage ->
                    messages = messages.mapIndexed { index, message ->
                        if (index == messages.lastIndex) {
                            message.copy(usage = message.usage.merge(usage))
                        } else {
                            message
                        }
                    }
                }
                onUpdateMessages(messages)
            }
        } else {
            aiLoggingManager.addLog(
                AILogging.Generation(
                    params = params,
                    messages = messages,
                    providerSetting = provider,
                    stream = false
                )
            )
            val chunk = providerImpl.generateText(
                providerSetting = provider,
                messages = internalMessages,
                params = params,
            )
            messages = messages.handleMessageChunk(chunk = chunk, model = model)
            chunk.usage?.let { usage ->
                messages = messages.mapIndexed { index, message ->
                    if (index == messages.lastIndex) {
                        message.copy(
                            usage = message.usage.merge(usage)
                        )
                    } else {
                        message
                    }
                }
            }
            onUpdateMessages(messages)
        }
    }

    private suspend fun generateWithSubagent(
        settings: Settings,
        model: Model,
        messages: List<UIMessage>,
        inputTransformers: List<InputMessageTransformer>,
        outputTransformers: List<OutputMessageTransformer>,
        assistant: Assistant,
        memories: List<AssistantMemory>,
        tools: List<Tool>,
        provider: ProviderSetting,
        processingStatus: MutableStateFlow<String?>,
        conversationSystemPrompt: String?,
        conversationModeInjectionIds: Set<Uuid>,
        conversationLorebookIds: Set<Uuid>,
        onUpdateMessages: suspend (List<UIMessage>) -> Unit,
    ): List<UIMessage> {
        val executableTools = tools.filterNot { it.needsApproval }
        if (executableTools.size < tools.size) {
            Log.i(TAG, "generateWithSubagent: skipped ${tools.size - executableTools.size} approval-required tools")
        }
        val internalMessages = buildInternalMessages(
            assistant = assistant,
            settings = settings,
            messages = messages,
            transformers = inputTransformers,
            model = model,
            tools = executableTools,
            memories = memories,
            processingStatus = processingStatus,
            conversationSystemPrompt = conversationSystemPrompt,
            conversationModeInjectionIds = conversationModeInjectionIds,
            conversationLorebookIds = conversationLorebookIds,
        )

        val params = TextGenerationParams(
            model = model,
            temperature = assistant.temperature,
            topP = assistant.topP,
            maxTokens = assistant.maxTokens,
            tools = executableTools,
            reasoningLevel = assistant.reasoningLevel,
            customHeaders = buildList {
                addAll(assistant.customHeaders)
                addAll(model.customHeaders)
            },
            customBody = buildList {
                addAll(assistant.customBodies)
                addAll(model.customBodies)
            }
        )
        aiLoggingManager.addLog(
            AILogging.Generation(
                params = params,
                messages = internalMessages,
                providerSetting = provider,
                stream = assistant.streamOutput
            )
        )

        val toolExecutor = DefaultSubagentToolExecutor(InMemoryToolExecutor(executableTools, json))
        var outputMessages = ensureAssistantMessage(messages)
        var generatedText = outputMessages.last().toText()

        aiSDK.streamTextWithSubagent(
            request = com.eterultimate.eteruee.ai.sdk.StreamTextRequest(
                model = model,
                messages = internalMessages,
                temperature = assistant.temperature,
                topP = assistant.topP,
                maxTokens = assistant.maxTokens,
                tools = executableTools,
                customHeaders = params.customHeaders,
                customBody = params.customBody,
            ),
            toolExecutor = toolExecutor
        ).collect { chunk ->
            processingStatus.value = when (chunk) {
                is SubagentTextChunk.PlanGenerating -> chunk.status
                is SubagentTextChunk.Status -> chunk.status
                is SubagentTextChunk.StepExecuting -> "正在执行工具: ${chunk.toolName}"
                else -> processingStatus.value
            }

            outputMessages = when (chunk) {
                is SubagentTextChunk.PlanGenerated -> {
                    outputMessages.updateAssistantSubagentPlan(chunk.plan)
                }

                is SubagentTextChunk.StepExecuting -> {
                    outputMessages.updateAssistantSubagentStep(chunk.stepId, UIMessagePart.StepStatus.RUNNING)
                }

                is SubagentTextChunk.StepCompleted -> {
                    outputMessages.updateAssistantSubagentResult(chunk.stepId, chunk.result)
                }

                is SubagentTextChunk.PlanExecuted -> {
                    outputMessages.updateAssistantSubagentExecuting(false)
                }

                is SubagentTextChunk.TextDelta -> {
                    generatedText += chunk.text
                    outputMessages.updateAssistantText(generatedText)
                }

                is SubagentTextChunk.Usage -> {
                    outputMessages.updateLastAssistant { message ->
                        message.copy(usage = message.usage.merge(chunk.tokenUsage))
                    }
                }

                is SubagentTextChunk.Error -> {
                    outputMessages.updateAssistantText(chunk.error)
                }

                else -> outputMessages
            }

            val transformed = outputMessages.transforms(
                transformers = outputTransformers,
                context = context,
                model = model,
                assistant = assistant,
                settings = settings
            ).visualTransforms(
                transformers = outputTransformers,
                context = context,
                model = model,
                assistant = assistant,
                settings = settings
            )
            onUpdateMessages(transformed)
        }

        processingStatus.value = null
        outputMessages = outputMessages.visualTransforms(
            transformers = outputTransformers,
            context = context,
            model = model,
            assistant = assistant,
            settings = settings
        ).onGenerationFinish(
            transformers = outputTransformers,
            context = context,
            model = model,
            assistant = assistant,
            settings = settings
        )

        val finalMessages = outputMessages.slice(0 until outputMessages.lastIndex) + outputMessages.last().copy(
            finishedAt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        )
        onUpdateMessages(finalMessages)
        return finalMessages
    }

    private suspend fun buildInternalMessages(
        assistant: Assistant,
        settings: Settings,
        messages: List<UIMessage>,
        transformers: List<MessageTransformer>,
        model: Model,
        tools: List<Tool>,
        memories: List<AssistantMemory>,
        processingStatus: MutableStateFlow<String?> = MutableStateFlow(null),
        conversationSystemPrompt: String? = null,
        conversationModeInjectionIds: Set<Uuid> = emptySet(),
        conversationLorebookIds: Set<Uuid> = emptySet(),
    ): List<UIMessage> {
        return buildList {
            val system = buildString {
                val effectiveSystemPrompt =
                    if (assistant.allowConversationSystemPrompt && !conversationSystemPrompt.isNullOrBlank()) {
                        conversationSystemPrompt
                    } else {
                        assistant.systemPrompt
                    }
                if (effectiveSystemPrompt.isNotBlank()) {
                    append(effectiveSystemPrompt)
                }
                if (assistant.enableMemory) {
                    appendLine()
                    append(buildMemoryPrompt(memories = memories))
                }
                if (assistant.enableRecentChatsReference) {
                    appendLine()
                    append(buildRecentChatsPrompt(assistant, conversationRepo))
                }
                tools.forEach { tool ->
                    appendLine()
                    append(tool.systemPrompt(model, messages))
                }
            }
            if (system.isNotBlank()) add(UIMessage.system(prompt = system))
            addAll(messages.limitContext(assistant.contextMessageSize))
        }.transforms(
            transformers = transformers,
            context = context,
            model = model,
            assistant = assistant,
            settings = settings,
            conversationModeInjectionIds = conversationModeInjectionIds,
            conversationLorebookIds = conversationLorebookIds,
            processingStatus = processingStatus,
        )
    }

    private class InMemoryToolExecutor(
        private val tools: List<Tool>,
        private val json: Json
    ) : ToolExecutor {
        override suspend fun execute(toolCallId: String, toolName: String, arguments: String): ToolResult {
            return runCatching {
                val tool = tools.find { it.name == toolName }
                    ?: throw IllegalArgumentException("Tool not found: $toolName")
                val args = json.parseToJsonElement(arguments.ifBlank { "{}" })
                val outputParts = tool.execute(args)
                ToolResult(
                    toolCallId = toolCallId,
                    result = outputParts.joinToString("\n") { part ->
                        when (part) {
                            is UIMessagePart.Text -> part.text
                            else -> json.encodeToString(part)
                        }
                    }
                )
            }.getOrElse { error ->
                ToolResult(
                    toolCallId = toolCallId,
                    result = "Error: ${error.message}",
                    isError = true
                )
            }
        }
    }

    private fun ensureAssistantMessage(messages: List<UIMessage>): List<UIMessage> {
        if (messages.lastOrNull()?.role == MessageRole.ASSISTANT) {
            return messages
        }
        return messages + UIMessage(
            id = Uuid.random(),
            role = MessageRole.ASSISTANT,
            parts = emptyList()
        )
    }

    private fun List<UIMessage>.updateLastAssistant(transform: (UIMessage) -> UIMessage): List<UIMessage> {
        val index = indexOfLast { it.role == MessageRole.ASSISTANT }
        if (index == -1) return this
        return mapIndexed { messageIndex, message ->
            if (messageIndex == index) transform(message) else message
        }
    }

    private fun List<UIMessage>.updateAssistantText(text: String): List<UIMessage> {
        return updateLastAssistant { message ->
            val partsWithoutText = message.parts.filterNot { it is UIMessagePart.Text }
            message.copy(parts = partsWithoutText + UIMessagePart.Text(text))
        }
    }

    private fun List<UIMessage>.updateAssistantSubagentPlan(plan: SubagentPlan): List<UIMessage> {
        return updateLastAssistant { message ->
            val planPart = UIMessagePart.SubagentPlan(
                planId = plan.planId,
                planText = plan.planText,
                reasoning = plan.reasoning,
                steps = plan.steps.map { step ->
                    UIMessagePart.PlanStep(
                        stepId = step.stepId,
                        toolName = step.toolName,
                        description = step.description,
                        arguments = step.arguments,
                        status = UIMessagePart.StepStatus.PENDING,
                        dependsOn = step.dependsOn
                    )
                },
                isExecuting = plan.steps.isNotEmpty()
            )
            val parts = message.parts.filterNot { it is UIMessagePart.SubagentPlan } + planPart
            message.copy(parts = parts)
        }
    }

    private fun List<UIMessage>.updateAssistantSubagentStep(
        stepId: String,
        status: UIMessagePart.StepStatus
    ): List<UIMessage> {
        return updateLastAssistant { message ->
            message.copy(
                parts = message.parts.map { part ->
                    if (part is UIMessagePart.SubagentPlan) {
                        part.copy(
                            steps = part.steps.map { step ->
                                if (step.stepId == stepId) step.copy(status = status) else step
                            }
                        )
                    } else {
                        part
                    }
                }
            )
        }
    }

    private fun List<UIMessage>.updateAssistantSubagentResult(
        stepId: String,
        result: PlanStepResult
    ): List<UIMessage> {
        val status = if (result.isError) UIMessagePart.StepStatus.FAILED else UIMessagePart.StepStatus.COMPLETED
        return updateLastAssistant { message ->
            message.copy(
                parts = message.parts.map { part ->
                    if (part is UIMessagePart.SubagentPlan) {
                        part.copy(
                            steps = part.steps.map { step ->
                                if (step.stepId == stepId) step.copy(status = status) else step
                            },
                            executionResults = part.executionResults.filterNot {
                                it.stepId == result.stepId
                            } + UIMessagePart.PlanStepResult(
                                stepId = result.stepId,
                                result = result.result,
                                isError = result.isError
                            )
                        )
                    } else {
                        part
                    }
                }
            )
        }
    }

    private fun List<UIMessage>.updateAssistantSubagentExecuting(isExecuting: Boolean): List<UIMessage> {
        return updateLastAssistant { message ->
            message.copy(
                parts = message.parts.map { part ->
                    if (part is UIMessagePart.SubagentPlan) {
                        part.copy(isExecuting = isExecuting)
                    } else {
                        part
                    }
                }
            )
        }
    }

    fun translateText(
        settings: Settings,
        sourceText: String,
        targetLanguage: Locale,
        onStreamUpdate: ((String) -> Unit)? = null
    ): Flow<String> = flow {
        val model = settings.providers.findModelById(settings.translateModeId)
            ?: error("Translation model not found")
        val provider = model.findProvider(settings.providers)
            ?: error("Translation provider not found")

        val providerHandler = providerManager.getProviderByType(provider)

        if (!ModelRegistry.QWEN_MT.match(model.modelId)) {
            // Use regular translation with prompt
            val prompt = settings.translatePrompt.applyPlaceholders(
                "source_text" to sourceText,
                "target_lang" to targetLanguage.toString(),
            )

            var messages = listOf(UIMessage.user(prompt))
            var translatedText = ""

            providerHandler.streamText(
                providerSetting = provider,
                messages = messages,
                params = TextGenerationParams(
                    model = model,
                    reasoningLevel = ReasoningLevel.fromBudgetTokens(settings.translateThinkingBudget),
                ),
            ).collect { chunk ->
                messages = messages.handleMessageChunk(chunk)
                translatedText = messages.lastOrNull()?.toText() ?: ""

                if (translatedText.isNotBlank()) {
                    onStreamUpdate?.invoke(translatedText)
                    emit(translatedText)
                }
            }
        } else {
            // Use Qwen MT model with special translation options
            val messages = listOf(UIMessage.user(sourceText))
            val chunk = providerHandler.generateText(
                providerSetting = provider,
                messages = messages,
                params = TextGenerationParams(
                    model = model,
                    temperature = 0.3f,
                    topP = 0.95f,
                    customBody = listOf(
                        CustomBody(
                            key = "translation_options",
                            value = buildJsonObject {
                                put("source_lang", JsonPrimitive("auto"))
                                put(
                                    "target_lang",
                                    JsonPrimitive(targetLanguage.getDisplayLanguage(Locale.ENGLISH))
                                )
                            }
                        )
                    )
                ),
            )
            val translatedText = chunk.choices.firstOrNull()?.message?.toText() ?: ""

            if (translatedText.isNotBlank()) {
                onStreamUpdate?.invoke(translatedText)
                emit(translatedText)
            }
        }
    }.flowOn(Dispatchers.IO)
}
