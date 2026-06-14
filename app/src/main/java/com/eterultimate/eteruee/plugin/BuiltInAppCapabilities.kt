package com.eterultimate.eteruee.plugin

import android.content.Context
import androidx.core.net.toUri
import com.eterultimate.eteruee.BuildConfig
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.ai.tools.LocalTools
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.datastore.getCurrentAssistant
import com.eterultimate.eteruee.data.files.FilesManager
import com.eterultimate.eteruee.data.model.Assistant
import com.eterultimate.eteruee.data.repository.ConversationRepository
import com.eterultimate.eteruee.device.DeviceAgentManager
import com.eterultimate.eteruee.linux.LinuxEnvironmentManager
import com.eterultimate.eteruee.service.ChatService
import com.eterultimate.eteruee.utils.JsonInstant
import com.eterultimate.eteruee.web.BadRequestException
import com.eterultimate.eteruee.web.NotFoundException
import com.eterultimate.eteruee.web.dto.PagedResult
import com.eterultimate.eteruee.web.dto.toDto
import com.eterultimate.eteruee.web.dto.toListDto
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put

private const val MAX_FILE_READ_BYTES = 1 * 1024 * 1024
private val SAFE_TOOL_CALLS = setOf("get_time_info")

fun createBuiltInAppCapabilityRegistry(
    context: Context,
    settingsStore: SettingsStore,
    conversationRepo: ConversationRepository,
    chatService: ChatService,
    filesManager: FilesManager,
    localTools: LocalTools,
    deviceAgentManager: DeviceAgentManager,
    linuxEnvironmentManager: LinuxEnvironmentManager,
): AppCapabilityRegistry = AppCapabilityRegistry(
    listOf(
        AppInfoCapability(),
        SettingsGetCapability(settingsStore),
        AssistantListCapability(settingsStore),
        ConversationListCapability(settingsStore, conversationRepo, chatService),
        ConversationGetCapability(settingsStore, conversationRepo, chatService),
        ChatSendCapability(settingsStore, conversationRepo, chatService),
        FilesGetCapability(context, filesManager),
        ToolsListCapability(settingsStore, localTools),
        ToolsCallCapability(settingsStore, localTools),
        DeviceStatusCapability(deviceAgentManager),
        DeviceShizukuRequestPermissionCapability(deviceAgentManager),
        DeviceAdbShellCapability(deviceAgentManager),
        LinuxStatusCapability(linuxEnvironmentManager),
        LinuxPrepareCapability(linuxEnvironmentManager),
        LinuxInstallCapability(linuxEnvironmentManager),
        LinuxShellCapability(linuxEnvironmentManager),
    )
)

fun defaultPluginPermissions(): Set<PluginPermission> = setOf(
    PluginPermission.ConversationRead,
    PluginPermission.AssistantRead,
    PluginPermission.SettingsRead,
    PluginPermission.FilesRead,
    PluginPermission.ToolsRead,
    PluginPermission.ToolsExecute,
    PluginPermission.DeviceRead,
)

class AppInfoCapability : AppCapability {
    override val id = "app.info"
    override val description = "Return application and plugin gateway metadata."
    override val inputSchema = emptyObjectSchema()
    override val permissions = emptySet<PluginPermission>()

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement = buildJsonObject {
        put("name", "EterUee")
        put("versionName", BuildConfig.VERSION_NAME)
        put("versionCode", BuildConfig.VERSION_CODE)
        put("pluginGateway", buildJsonObject {
            put("protocol", "eteruee-plugin-rpc")
            put("version", 1)
            put("capabilityMode", "external-websocket")
        })
    }
}

class SettingsGetCapability(
    private val settingsStore: SettingsStore,
) : AppCapability {
    override val id = "settings.get"
    override val description = "Return a redacted summary of app settings relevant to plugins."
    override val inputSchema = emptyObjectSchema()
    override val permissions = setOf(PluginPermission.SettingsRead)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val settings = settingsStore.settingsFlow.first()
        val currentAssistant = settings.getCurrentAssistant()
        return buildJsonObject {
            put("currentAssistantId", settings.assistantId.toString())
            put("currentAssistantName", currentAssistant.name)
            put("assistantCount", settings.assistants.size)
            put("mcpServerCount", settings.mcpServers.size)
            put("webServer", buildJsonObject {
                put("enabled", settings.webServerEnabled)
                put("port", settings.webServerPort)
                put("jwtEnabled", settings.webServerJwtEnabled)
                put("localhostOnly", settings.webServerLocalhostOnly)
            })
            put("features", buildJsonObject {
                put("webSearch", settings.enableWebSearch)
                put("subagent", settings.enableSubagent)
                put("developerMode", settings.developerMode)
            })
        }
    }
}

class AssistantListCapability(
    private val settingsStore: SettingsStore,
) : AppCapability {
    override val id = "assistant.list"
    override val description = "List assistants with non-secret metadata and enabled extension handles."
    override val inputSchema = emptyObjectSchema()
    override val permissions = setOf(PluginPermission.AssistantRead)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val settings = settingsStore.settingsFlow.first()
        return buildJsonObject {
            put("currentAssistantId", settings.assistantId.toString())
            put(
                "items",
                JsonArray(settings.assistants.map { assistant ->
                    assistant.toPluginJson(isCurrent = assistant.id == settings.assistantId)
                })
            )
        }
    }
}

class ConversationListCapability(
    private val settingsStore: SettingsStore,
    private val conversationRepo: ConversationRepository,
    private val chatService: ChatService,
) : AppCapability {
    override val id = "conversation.list"
    override val description = "List conversations for the current assistant with pagination."
    override val inputSchema = objectSchema(
        properties = buildJsonObject {
            put("offset", integerSchema("Zero-based paging offset."))
            put("limit", integerSchema("Page size from 1 to 100. Defaults to 20."))
            put("query", stringSchema("Optional title search query."))
        }
    )
    override val permissions = setOf(PluginPermission.ConversationRead)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val offset = input.optionalInt("offset") ?: 0
        val limit = input.optionalInt("limit") ?: 20
        val query = input.optionalString("query").orEmpty().trim()
        require(offset >= 0) { "offset must be >= 0" }
        require(limit in 1..100) { "limit must be in 1..100" }

        val settings = settingsStore.settingsFlow.first()
        val page = if (query.isBlank()) {
            conversationRepo.getConversationsOfAssistantPage(settings.assistantId, offset, limit)
        } else {
            conversationRepo.searchConversationsOfAssistantPage(settings.assistantId, query, offset, limit)
        }
        val generationJobs = chatService.getConversationJobs().first()
        return JsonInstant.encodeToJsonElement(
            PagedResult(
                items = page.items.map { conversation ->
                    conversation.toListDto(isGenerating = generationJobs[conversation.id] != null)
                },
                nextOffset = page.nextOffset
            )
        )
    }
}

class ConversationGetCapability(
    private val settingsStore: SettingsStore,
    private val conversationRepo: ConversationRepository,
    private val chatService: ChatService,
) : AppCapability {
    override val id = "conversation.get"
    override val description = "Read one conversation from the current assistant."
    override val inputSchema = objectSchema(
        properties = buildJsonObject {
            put("id", stringSchema("Conversation UUID."))
        },
        required = listOf("id")
    )
    override val permissions = setOf(PluginPermission.ConversationRead)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val id = input.requiredUuid("id")
        val settings = settingsStore.settingsFlow.first()
        val conversation = conversationRepo.getConversationById(id)
            ?: throw NotFoundException("Conversation not found")
        if (conversation.assistantId != settings.assistantId) {
            throw NotFoundException("Conversation not found")
        }
        val isGenerating = chatService.getGenerationJobStateFlow(id).first() != null
        return JsonInstant.encodeToJsonElement(conversation.toDto(isGenerating))
    }
}

class ChatSendCapability(
    private val settingsStore: SettingsStore,
    private val conversationRepo: ConversationRepository,
    private val chatService: ChatService,
) : AppCapability {
    override val id = "chat.send"
    override val description = "Append a user message to a conversation and optionally start generation."
    override val inputSchema = objectSchema(
        properties = buildJsonObject {
            put("conversationId", stringSchema("Optional conversation UUID. A new UUID is created when omitted."))
            put("text", stringSchema("User text to send."))
            put("parts", arraySchema("Optional UIMessagePart array. Used when text is omitted."))
            put("answer", booleanSchema("Whether to start assistant generation. Defaults to true."))
        }
    )
    override val permissions = setOf(PluginPermission.ConversationWrite)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val conversationId = input.optionalString("conversationId")
            ?.takeIf { it.isNotBlank() }
            ?.let { parseUuid(it, "conversationId") }
            ?: Uuid.random()
        val answer = input.optionalBoolean("answer") ?: true
        val parts = input.toMessageParts()
        if (parts.isEmpty()) {
            throw BadRequestException("chat.send requires text or parts")
        }

        val existing = conversationRepo.getConversationById(conversationId)
        if (existing != null) {
            val currentAssistantId = settingsStore.settingsFlow.first().assistantId
            if (existing.assistantId != currentAssistantId) {
                throw NotFoundException("Conversation not found")
            }
        }

        chatService.initializeConversation(conversationId)
        chatService.sendMessage(conversationId, parts, answer = answer)

        return buildJsonObject {
            put("conversationId", conversationId.toString())
            put("status", "accepted")
        }
    }
}

class FilesGetCapability(
    private val appContext: Context,
    private val filesManager: FilesManager,
) : AppCapability {
    override val id = "files.get"
    override val description = "Read metadata for uploaded files, optionally including small file content."
    override val inputSchema = objectSchema(
        properties = buildJsonObject {
            put("id", integerSchema("Managed file id."))
            put("path", stringSchema("Managed relative path under the app files directory."))
            put("includeContent", booleanSchema("Include file content when the file is at most 1 MiB."))
        }
    )
    override val permissions = setOf(PluginPermission.FilesRead)

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val includeContent = input.optionalBoolean("includeContent") ?: false
        val entity = input.optionalLong("id")?.let { filesManager.get(it) }
            ?: input.optionalString("path")?.let { path ->
                if (path.contains("..") || path.startsWith("/")) {
                    throw BadRequestException("Invalid file path")
                }
                filesManager.getByRelativePath(path)
            }
            ?: throw BadRequestException("files.get requires id or path")

        val file = filesManager.getFile(entity)
        if (!file.canonicalPath.startsWith(appContext.filesDir.canonicalPath) || !file.isFile) {
            throw NotFoundException("File not found")
        }

        return buildJsonObject {
            put("id", entity.id)
            put("folder", entity.folder)
            put("relativePath", entity.relativePath)
            put("displayName", entity.displayName)
            put("mimeType", entity.mimeType)
            put("sizeBytes", entity.sizeBytes)
            put("createdAt", entity.createdAt)
            put("updatedAt", entity.updatedAt)
            put("url", file.toUri().toString())
            if (includeContent) {
                if (file.length() > MAX_FILE_READ_BYTES) {
                    throw BadRequestException("File is too large to include content")
                }
                if (entity.mimeType.startsWith("text/") || entity.mimeType == "application/json") {
                    put("content", file.readText(Charsets.UTF_8))
                    put("contentEncoding", "utf-8")
                } else {
                    put("contentBase64", Base64.encode(file.readBytes()))
                    put("contentEncoding", "base64")
                }
            }
        }
    }
}

class ToolsListCapability(
    private val settingsStore: SettingsStore,
    private val localTools: LocalTools,
) : AppCapability {
    override val id = "tools.list"
    override val description = "List local tools enabled for the current assistant."
    override val inputSchema = emptyObjectSchema()
    override val permissions = setOf(PluginPermission.ToolsRead)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val settings = settingsStore.settingsFlow.first()
        val assistant = settings.getCurrentAssistant()
        val tools = localTools.getTools(assistant.localTools)
        return buildJsonObject {
            put("assistantId", assistant.id.toString())
            put(
                "items",
                JsonArray(tools.map { tool ->
                    buildJsonObject {
                        put("name", tool.name)
                        put("description", tool.description)
                        put("parameters", JsonInstant.encodeToJsonElement(tool.parameters()))
                        put("needsApproval", tool.needsApproval)
                        put("callableByPlugin", tool.name in SAFE_TOOL_CALLS && !tool.needsApproval)
                    }
                })
            )
        }
    }
}

class ToolsCallCapability(
    private val settingsStore: SettingsStore,
    private val localTools: LocalTools,
) : AppCapability {
    override val id = "tools.call"
    override val description = "Execute a low-risk local tool enabled for the current assistant."
    override val inputSchema = objectSchema(
        properties = buildJsonObject {
            put("name", stringSchema("Local tool name."))
            put("arguments", objectSchema(description = "Tool arguments."))
        },
        required = listOf("name")
    )
    override val permissions = setOf(PluginPermission.ToolsExecute)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val name = input.requiredString("name")
        if (name !in SAFE_TOOL_CALLS) {
            throw PluginProtocolException(
                code = "TOOL_NOT_ALLOWED",
                message = "Tool is not callable through the plugin gateway: $name"
            )
        }
        val settings = settingsStore.settingsFlow.first()
        val tool = localTools.getTools(settings.getCurrentAssistant().localTools)
            .firstOrNull { it.name == name }
            ?: throw PluginProtocolException("TOOL_NOT_FOUND", "Tool is not enabled: $name")
        if (tool.needsApproval) {
            throw PluginProtocolException("TOOL_APPROVAL_REQUIRED", "Tool requires app-side approval: $name")
        }

        val arguments = input["arguments"]?.jsonObjectOrNull ?: JsonObject(emptyMap())
        val output = tool.execute(arguments)
        return buildJsonObject {
            put("name", tool.name)
            put("output", JsonInstant.encodeToJsonElement(ListSerializer(UIMessagePart.serializer()), output))
        }
    }
}

class DeviceStatusCapability(
    private val deviceAgentManager: DeviceAgentManager,
) : AppCapability {
    override val id = "device.status"
    override val description = "Read Shizuku/WiFi ADB readiness and Android device agent status."
    override val inputSchema = emptyObjectSchema()
    override val permissions = setOf(PluginPermission.DeviceRead)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement =
        JsonInstant.encodeToJsonElement(deviceAgentManager.getStatus())
}

class DeviceShizukuRequestPermissionCapability(
    private val deviceAgentManager: DeviceAgentManager,
) : AppCapability {
    override val id = "device.shizuku.request_permission"
    override val description = "Request Shizuku API permission for EterUee. User approval may still be required in Shizuku."
    override val inputSchema = emptyObjectSchema()
    override val permissions = setOf(PluginPermission.DeviceControl)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement =
        JsonInstant.encodeToJsonElement(deviceAgentManager.requestShizukuPermission())
}

class DeviceAdbShellCapability(
    private val deviceAgentManager: DeviceAgentManager,
) : AppCapability {
    override val id = "device.adb_shell"
    override val description = "Execute an Android shell command through Shizuku ADB shell."
    override val inputSchema = shellSchema(
        defaultWorkingDir = "/data/local/tmp",
        defaultTimeout = 30,
    )
    override val permissions = setOf(PluginPermission.DeviceControl)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val command = input.requiredString("command")
        return JsonInstant.encodeToJsonElement(
            deviceAgentManager.executeAdbShell(
                command = command,
                workingDir = input.optionalString("workingDir"),
                stdin = input.optionalString("stdin"),
                timeoutSeconds = input.optionalInt("timeout") ?: 30,
            )
        )
    }
}

class LinuxStatusCapability(
    private val linuxEnvironmentManager: LinuxEnvironmentManager,
) : AppCapability {
    override val id = "linux.status"
    override val description = "Read EterUee managed Linux/proot environment readiness for Arch or Ubuntu."
    override val inputSchema = linuxDistributionSchema()
    override val permissions = setOf(PluginPermission.DeviceRead)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement =
        JsonInstant.encodeToJsonElement(linuxEnvironmentManager.getStatus(input.linuxDistribution()))
}

class LinuxPrepareCapability(
    private val linuxEnvironmentManager: LinuxEnvironmentManager,
) : AppCapability {
    override val id = "linux.prepare"
    override val description = "Prepare the selected Linux installer helper in EterUee's app-private files directory."
    override val inputSchema = linuxDistributionSchema()
    override val permissions = setOf(PluginPermission.DeviceControl)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement =
        JsonInstant.encodeToJsonElement(linuxEnvironmentManager.prepareInstallerScript(input.linuxDistribution()))
}

class LinuxInstallCapability(
    private val linuxEnvironmentManager: LinuxEnvironmentManager,
) : AppCapability {
    override val id = "linux.install"
    override val description =
        "Download Termux proot runtime packages and extract the selected Arch or Ubuntu rootfs before commands can run."
    override val inputSchema = objectSchema(
        properties = buildJsonObject {
            put("distribution", linuxDistributionProperty())
            put("timeout", integerSchema("Install timeout in seconds. Defaults to 600."))
        }
    )
    override val permissions = setOf(PluginPermission.DeviceControl)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement =
        JsonInstant.encodeToJsonElement(
            linuxEnvironmentManager.install(
                distribution = input.linuxDistribution(),
                timeoutSeconds = input.optionalInt("timeout") ?: 600,
            )
        )
}

class LinuxShellCapability(
    private val linuxEnvironmentManager: LinuxEnvironmentManager,
) : AppCapability {
    override val id = "linux.shell"
    override val description = "Execute a command inside EterUee's managed Arch or Ubuntu Linux/proot environment."
    override val inputSchema = shellSchema(
        defaultWorkingDir = "/root",
        defaultTimeout = 60,
        includeDistribution = true,
    )
    override val permissions = setOf(PluginPermission.DeviceControl)

    override suspend fun execute(input: JsonObject, context: PluginCallContext): JsonElement {
        val command = input.requiredString("command")
        return JsonInstant.encodeToJsonElement(
            linuxEnvironmentManager.execute(
                distribution = input.linuxDistribution(),
                command = command,
                workingDir = input.optionalString("workingDir"),
                stdin = input.optionalString("stdin"),
                timeoutSeconds = input.optionalInt("timeout") ?: 60,
            )
        )
    }
}

private fun Assistant.toPluginJson(isCurrent: Boolean): JsonObject = buildJsonObject {
    put("id", id.toString())
    put("name", name)
    put("isCurrent", isCurrent)
    put("chatModelId", chatModelId?.toString()?.let { JsonPrimitive(it) } ?: JsonNull)
    put("streamOutput", streamOutput)
    put("contextMessageSize", contextMessageSize)
    put("enableMemory", enableMemory)
    put("enableRecentChatsReference", enableRecentChatsReference)
    put("localTools", JsonArray(localTools.map { JsonPrimitive(it::class.simpleName ?: it.toString()) }))
    put("mcpServerIds", JsonArray(mcpServers.map { JsonPrimitive(it.toString()) }))
    put("enabledSkills", JsonArray(enabledSkills.sorted().map { JsonPrimitive(it) }))
    put("allowConversationSystemPrompt", allowConversationSystemPrompt)
    put("allowConversationPromptInjection", allowConversationPromptInjection)
}

private fun emptyObjectSchema(): JsonObject = objectSchema()

private fun objectSchema(
    description: String? = null,
    properties: JsonObject = JsonObject(emptyMap()),
    required: List<String> = emptyList(),
): JsonObject = buildJsonObject {
    put("type", "object")
    description?.let { put("description", it) }
    put("properties", properties)
    if (required.isNotEmpty()) {
        put("required", JsonArray(required.map { JsonPrimitive(it) }))
    }
}

private fun stringSchema(description: String): JsonObject = buildJsonObject {
    put("type", "string")
    put("description", description)
}

private fun integerSchema(description: String): JsonObject = buildJsonObject {
    put("type", "integer")
    put("description", description)
}

private fun booleanSchema(description: String): JsonObject = buildJsonObject {
    put("type", "boolean")
    put("description", description)
}

private fun linuxDistributionProperty(): JsonObject = stringSchema("Linux distribution: arch or ubuntu. Defaults to arch.")

private fun linuxDistributionSchema(): JsonObject = objectSchema(
    properties = buildJsonObject {
        put("distribution", linuxDistributionProperty())
    }
)

private fun arraySchema(description: String): JsonObject = buildJsonObject {
    put("type", "array")
    put("description", description)
}

private fun shellSchema(
    defaultWorkingDir: String,
    defaultTimeout: Int,
    includeDistribution: Boolean = false,
): JsonObject = objectSchema(
    properties = buildJsonObject {
        put("command", stringSchema("Shell command to execute."))
        put("workingDir", stringSchema("Working directory. Defaults to $defaultWorkingDir."))
        put("stdin", stringSchema("Optional standard input for the command."))
        put("timeout", integerSchema("Timeout in seconds. Defaults to $defaultTimeout."))
        if (includeDistribution) {
            put("distribution", linuxDistributionProperty())
        }
    },
    required = listOf("command")
)

private fun JsonObject.linuxDistribution(): String =
    optionalString("distribution")?.takeIf { it.isNotBlank() } ?: "arch"

private fun JsonObject.requiredString(key: String): String =
    optionalString(key)?.takeIf { it.isNotBlank() } ?: throw BadRequestException("$key is required")

private fun JsonObject.requiredUuid(key: String): Uuid = parseUuid(requiredString(key), key)

private fun JsonObject.optionalString(key: String): String? =
    this[key]?.jsonPrimitiveOrNull?.contentOrNull

private fun JsonObject.optionalBoolean(key: String): Boolean? =
    this[key]?.jsonPrimitiveOrNull?.let { primitive ->
        primitive.contentOrNull?.toBooleanStrictOrNull()
    }

private fun JsonObject.optionalInt(key: String): Int? =
    this[key]?.jsonPrimitiveOrNull?.intOrNull

private fun JsonObject.optionalLong(key: String): Long? =
    this[key]?.jsonPrimitiveOrNull?.contentOrNull?.toLongOrNull()

private val JsonElement.jsonPrimitiveOrNull
    get() = this as? JsonPrimitive

private val JsonElement.jsonObjectOrNull
    get() = this as? JsonObject

private fun parseUuid(value: String, field: String): Uuid =
    runCatching { Uuid.parse(value) }.getOrElse {
        throw BadRequestException("Invalid $field")
    }

private fun JsonObject.toMessageParts(): List<UIMessagePart> {
    val text = optionalString("text")?.takeIf { it.isNotBlank() }
    if (text != null) {
        return listOf(UIMessagePart.Text(text))
    }
    val parts = this["parts"]?.let { element ->
        runCatching {
            JsonInstant.decodeFromJsonElement(
                ListSerializer(UIMessagePart.serializer()),
                element.jsonArray
            )
        }.getOrElse {
            throw BadRequestException("Invalid parts")
        }
    }
    return parts.orEmpty()
}
