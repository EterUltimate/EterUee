package com.eterultimate.eteruee.data.sync.importer

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.core.TokenUsage
import com.eterultimate.eteruee.ai.provider.Modality
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.provider.ModelAbility
import com.eterultimate.eteruee.ai.provider.ProviderSetting
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.model.Conversation
import com.eterultimate.eteruee.data.model.MessageNode
import com.eterultimate.eteruee.utils.JsonInstant
import com.eterultimate.eteruee.utils.JsonInstantPretty
import java.io.File
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.UUID
import kotlin.time.Instant as KotlinInstant
import kotlin.uuid.Uuid

/**
 * Chatbox exports its data as a single JSON object that is close to its local storage layout.
 *
 * Expected top-level shape:
 * - `settings`: Chatbox app settings. Provider credentials are under
 *   `settings.providers.openai`, `settings.providers.claude`, and `settings.providers.gemini`.
 * - `chat-sessions-list`: lightweight conversation index. Each item normally contains `id`, `name`, `type`,
 *   and optionally `picUrl`.
 * - `session:<id>`: full conversation payload for the matching item in `chat-sessions-list`.
 *   Important fields are `id`, `name`, `threadName`, `settings`, `messages`, and `messageForksHash`.
 *
 * Message shape inside `session:<id>.messages`:
 * - `role`: `system`, `user`, `assistant`, or `tool`.
 * - `contentParts`: ordered message parts. Supported part types here are:
 *   - `text` -> [UIMessagePart.Text]
 *   - `reasoning` -> [UIMessagePart.Reasoning]
 *   - `tool-call` -> [UIMessagePart.Tool]
 *   - `image` -> dropped intentionally; Chatbox JSON usually stores only a `storageKey`, not image bytes.
 * - `timestamp`: epoch milliseconds.
 * - `usage`: token usage in Chatbox's field names.
 *
 * Import mapping:
 * - One Chatbox `session:<id>` becomes one EterUee [Conversation].
 * - Leading `system` messages are merged into [Conversation.customSystemPrompt].
 * - Each remaining Chatbox message becomes one [MessageNode] with one [UIMessage].
 * - Stable UUIDs are derived from Chatbox ids so importing the same file again can skip existing conversations.
 */
object ChatboxImporter {
    fun import(file: File, assistantId: Uuid, providers: List<ProviderSetting>): ChatboxImportPayload {
        val root = JsonInstant.parseToJsonElement(file.readText()).jsonObject
        val importedProviders = importProviders(root)
        return ChatboxImportPayload(
            providers = importedProviders,
            conversations = importConversations(root, assistantId, importedProviders + providers),
        )
    }

    fun importProviders(file: File): List<ProviderSetting> {
        val root = JsonInstant.parseToJsonElement(file.readText()).jsonObject
        return importProviders(root)
    }

    private fun importProviders(root: JsonObject): List<ProviderSetting> {
        val importProviders = arrayListOf<ProviderSetting>()
        val settingsObj = root["settings"]?.jsonObjectOrNull ?: return emptyList()
        settingsObj["providers"]?.jsonObjectOrNull?.let { providers ->
            providers["openai"]?.jsonObjectOrNull?.let { openai ->
                val apiHost = openai["apiHost"]?.asString ?: "https://api.openai.com"
                val apiKey = openai["apiKey"]?.asString ?: ""
                val models = openai["models"]?.jsonArrayOrNull?.map { element ->
                    val model = element.jsonObject
                    val modelId = model["modelId"]?.asString ?: ""
                    val capabilities = model["capabilities"]?.jsonArrayOrNull
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                        ?: emptyList()
                    Model(
                        modelId = modelId,
                        displayName = modelId,
                        inputModalities = buildList {
                            add(Modality.TEXT)
                            if (capabilities.contains("vision")) {
                                add(Modality.IMAGE)
                            }
                        },
                        abilities = buildList {
                            if (capabilities.contains("tool_use")) {
                                add(ModelAbility.TOOL)
                            }
                            if (capabilities.contains("reasoning")) {
                                add(ModelAbility.REASONING)
                            }
                        }
                    )
                } ?: emptyList()
                if (apiKey.isNotBlank()) {
                    importProviders.add(
                        ProviderSetting.OpenAI(
                            name = "OpenAI",
                            baseUrl = "${apiHost.trimEnd('/')}/v1",
                            apiKey = apiKey,
                            models = models,
                        )
                    )
                }
            }
            providers["claude"]?.jsonObjectOrNull?.let { claude ->
                val apiHost = claude["apiHost"]?.asString ?: "https://api.anthropic.com"
                val apiKey = claude["apiKey"]?.asString ?: ""
                if (apiKey.isNotBlank()) {
                    importProviders.add(
                        ProviderSetting.Claude(
                            name = "Claude",
                            baseUrl = "${apiHost.trimEnd('/')}/v1",
                            apiKey = apiKey,
                        )
                    )
                }
            }
            providers["gemini"]?.jsonObjectOrNull?.let { gemini ->
                val apiHost = gemini["apiHost"]?.asString ?: "https://generativelanguage.googleapis.com"
                val apiKey = gemini["apiKey"]?.asString ?: ""
                if (apiKey.isNotBlank()) {
                    importProviders.add(
                        ProviderSetting.Google(
                            name = "Gemini",
                            baseUrl = "${apiHost.trimEnd('/')}/v1beta",
                            apiKey = apiKey,
                        )
                    )
                }
            }
        }
        return importProviders
    }

    private fun importConversations(
        root: JsonObject,
        assistantId: Uuid,
        providers: List<ProviderSetting>,
    ): ChatboxConversationImport {
        var skippedImageParts = 0
        var skippedEmptyMessages = 0
        val conversations = sessionObjects(root).mapNotNull { session ->
            val sessionId = session["id"]?.asString ?: return@mapNotNull null
            val messages = session["messages"]?.jsonArrayOrNull ?: return@mapNotNull null
            val sessionSettings = session["settings"]?.jsonObjectOrNull
            val sessionModelId = sessionSettings?.get("modelId")?.asString
            val sessionProvider = sessionSettings?.get("provider")?.asString
            val title = session["threadName"]?.asString
                ?.takeIf { it.isNotBlank() }
                ?: session["name"]?.asString?.takeIf { it.isNotBlank() }
                ?: sessionId

            var customSystemPrompt: String? = null
            var reachedConversationMessages = false
            val nodes = messages.mapNotNull { element ->
                val message = element.jsonObject
                val role = message["role"]?.asString?.toMessageRole() ?: return@mapNotNull null
                if (role == MessageRole.SYSTEM && !reachedConversationMessages) {
                    val systemPrompt = extractText(message).trim()
                    if (systemPrompt.isNotBlank()) {
                        customSystemPrompt = listOfNotNull(customSystemPrompt, systemPrompt).joinToString("\n\n")
                    }
                    return@mapNotNull null
                }
                reachedConversationMessages = true

                val parseResult = parseParts(message)
                skippedImageParts += parseResult.skippedImageParts
                if (parseResult.parts.isEmpty()) {
                    skippedEmptyMessages++
                    return@mapNotNull null
                }

                val messageId = message["id"]?.asString ?: "${sessionId}:${message.hashCode()}"
                MessageNode(
                    id = stableUuid("chatbox:node:$sessionId:$messageId"),
                    messages = listOf(
                        UIMessage(
                            id = stableUuid("chatbox:message:$messageId"),
                            role = role,
                            parts = parseResult.parts,
                            createdAt = millisToLocalDateTime(message["timestamp"]?.asLong),
                            modelId = resolveModelId(
                                providers = providers,
                                providerName = message["aiProvider"]?.asString ?: sessionProvider,
                                modelId = sessionModelId,
                                modelName = message["model"]?.asString
                            ),
                            usage = parseUsage(message["usage"]?.jsonObjectOrNull),
                        )
                    ),
                    selectIndex = 0
                )
            }

            if (nodes.isEmpty()) {
                return@mapNotNull null
            }

            val timestamps = messages.mapNotNull { it.jsonObject["timestamp"]?.asLong }
            Conversation(
                id = stableUuid("chatbox:session:$sessionId"),
                assistantId = assistantId,
                title = title,
                messageNodes = nodes,
                createAt = timestamps.minOrNull()?.let { Instant.ofEpochMilli(it) } ?: Instant.now(),
                updateAt = timestamps.maxOrNull()?.let { Instant.ofEpochMilli(it) } ?: Instant.now(),
                customSystemPrompt = customSystemPrompt,
            )
        }

        return ChatboxConversationImport(
            conversations = conversations,
            skippedImageParts = skippedImageParts,
            skippedEmptyMessages = skippedEmptyMessages,
        )
    }

    private fun sessionObjects(root: JsonObject): List<JsonObject> {
        val idsFromList = root["chat-sessions-list"]?.jsonArrayOrNull
            ?.mapNotNull { it.jsonObject["id"]?.asString }
            ?: emptyList()
        val listedSessions = idsFromList.mapNotNull { root["session:$it"]?.jsonObjectOrNull }
        val listedIds = idsFromList.toSet()
        val extraSessions = root.entries
            .asSequence()
            .filter { it.key.startsWith("session:") }
            .filter { it.key.removePrefix("session:") !in listedIds }
            .mapNotNull { it.value.jsonObjectOrNull }
            .toList()
        return listedSessions + extraSessions
    }

    private fun parseParts(message: JsonObject): ChatboxPartParseResult {
        var skippedImageParts = 0
        val parts = message["contentParts"]?.jsonArrayOrNull
            ?.mapNotNull { part ->
                when (val type = part.jsonObject["type"]?.asString) {
                    "text" -> part.jsonObject["text"]?.asString
                        ?.takeIf { it.isNotBlank() }
                        ?.let { UIMessagePart.Text(it) }

                    "reasoning" -> part.jsonObject["text"]?.asString
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            UIMessagePart.Reasoning(
                                reasoning = it,
                                createdAt = part.jsonObject["startTime"]?.asLong
                                    ?.let(KotlinInstant::fromEpochMilliseconds)
                                    ?: KotlinInstant.fromEpochMilliseconds(message["timestamp"]?.asLong ?: 0L),
                                finishedAt = part.jsonObject["startTime"]?.asLong?.let { start ->
                                    KotlinInstant.fromEpochMilliseconds(
                                        start + (part.jsonObject["duration"]?.asLong ?: 0L)
                                    )
                                }
                            )
                        }

                    "tool-call" -> parseToolPart(part.jsonObject)
                    "image" -> {
                        skippedImageParts++
                        null
                    }

                    else -> {
                        if (type != null) {
                            UIMessagePart.Text(JsonInstantPretty.encodeToString(part))
                        } else {
                            null
                        }
                    }
                }
            }
            ?: emptyList()

        if (parts.isNotEmpty()) {
            return ChatboxPartParseResult(parts, skippedImageParts)
        }

        return ChatboxPartParseResult(
            parts = message["content"]?.asString
                ?.takeIf { it.isNotBlank() }
                ?.let { listOf(UIMessagePart.Text(it)) }
                ?: emptyList(),
            skippedImageParts = skippedImageParts
        )
    }

    private fun parseToolPart(part: JsonObject): UIMessagePart.Tool? {
        val toolCallId = part["toolCallId"]?.asString ?: return null
        val toolName = part["toolName"]?.asString ?: return null
        val args = part["args"] ?: JsonObject(emptyMap())
        val result = part["result"]
        return UIMessagePart.Tool(
            toolCallId = toolCallId,
            toolName = toolName,
            input = JsonInstant.encodeToString(args),
            output = result?.let {
                listOf(
                    UIMessagePart.Text(
                        when (it) {
                            is JsonPrimitive -> it.contentOrNull ?: it.toString()
                            else -> JsonInstantPretty.encodeToString(it)
                        }
                    )
                )
            } ?: emptyList()
        )
    }

    private fun extractText(message: JsonObject): String {
        val fromParts = message["contentParts"]?.jsonArrayOrNull
            ?.mapNotNull { part ->
                val obj = part.jsonObject
                if (obj["type"]?.asString == "text") {
                    obj["text"]?.asString
                } else {
                    null
                }
            }
            ?.joinToString("\n")
        return fromParts?.takeIf { it.isNotBlank() } ?: message["content"]?.asString.orEmpty()
    }

    private fun parseUsage(usage: JsonObject?): TokenUsage? {
        usage ?: return null
        val promptTokens = usage["inputTokens"]?.asInt ?: 0
        val completionTokens = usage["outputTokens"]?.asInt ?: 0
        val cachedTokens = usage["cachedInputTokens"]?.asInt
            ?: usage["inputTokenDetails"]?.jsonObjectOrNull?.get("cacheReadTokens")?.asInt
            ?: 0
        val totalTokens = usage["totalTokens"]?.asInt
            ?: (promptTokens + completionTokens)
        if (promptTokens == 0 && completionTokens == 0 && cachedTokens == 0 && totalTokens == 0) {
            return null
        }
        return TokenUsage(
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            cachedTokens = cachedTokens,
            totalTokens = totalTokens,
        )
    }

    private fun resolveModelId(
        providers: List<ProviderSetting>,
        providerName: String?,
        modelId: String?,
        modelName: String?,
    ): Uuid? {
        val providerMatches = providers.filter { provider ->
            providerName.isNullOrBlank() ||
                provider.name.equals(providerName, ignoreCase = true) ||
                provider.providerTypeName().equals(providerName, ignoreCase = true)
        }.takeIf { it.isNotEmpty() } ?: providers

        return providerMatches
            .asSequence()
            .flatMap { it.models.asSequence() }
            .firstOrNull { model ->
                listOfNotNull(modelId, modelName).any { imported ->
                    model.modelId.equals(imported, ignoreCase = true) ||
                        model.displayName.equals(imported, ignoreCase = true) ||
                        imported.contains(model.modelId, ignoreCase = true) ||
                        model.displayName.takeIf { it.isNotBlank() }?.let {
                            imported.contains(it, ignoreCase = true)
                        } == true
                }
            }
            ?.id
    }

    private fun String.toMessageRole(): MessageRole? = when (this) {
        "system" -> MessageRole.SYSTEM
        "user" -> MessageRole.USER
        "assistant" -> MessageRole.ASSISTANT
        "tool" -> MessageRole.TOOL
        else -> null
    }

    private fun ProviderSetting.providerTypeName(): String = when (this) {
        is ProviderSetting.OpenAI -> "openai"
        is ProviderSetting.Google -> "gemini"
        is ProviderSetting.Claude -> "claude"
    }

    private fun millisToLocalDateTime(timestamp: Long?) =
        KotlinInstant.fromEpochMilliseconds(timestamp ?: System.currentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())

    private fun stableUuid(value: String): Uuid =
        Uuid.parse(UUID.nameUUIDFromBytes(value.toByteArray(StandardCharsets.UTF_8)).toString())

    private val JsonElement.jsonObjectOrNull: JsonObject?
        get() = this as? JsonObject

    private val JsonElement.jsonArrayOrNull: JsonArray?
        get() = this as? JsonArray

    private val JsonElement.asString: String?
        get() = (this as? JsonPrimitive)?.contentOrNull

    private val JsonElement.asLong: Long?
        get() = (this as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

    private val JsonElement.asInt: Int?
        get() = (this as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
}

data class ChatboxImportPayload(
    val providers: List<ProviderSetting>,
    val conversations: ChatboxConversationImport,
)

data class ChatboxConversationImport(
    val conversations: List<Conversation>,
    val skippedImageParts: Int,
    val skippedEmptyMessages: Int,
)

data class ChatboxPartParseResult(
    val parts: List<UIMessagePart>,
    val skippedImageParts: Int,
)
