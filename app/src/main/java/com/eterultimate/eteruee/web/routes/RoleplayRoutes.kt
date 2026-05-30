package com.eterultimate.eteruee.web.routes

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.sdk.AISDK
import com.eterultimate.eteruee.ai.sdk.StreamTextRequest
import com.eterultimate.eteruee.ai.sdk.TextChunk
import com.eterultimate.eteruee.ai.sdk.ToolExecutor
import com.eterultimate.eteruee.ai.sdk.ToolResult
import com.eterultimate.eteruee.ai.sdk.streamTextWithTools
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.ai.tools.LocalTools
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.datastore.findModelById
import com.eterultimate.eteruee.data.datastore.getCurrentAssistant
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.data.model.ChatMetadata
import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.data.model.GroupMember
import com.eterultimate.eteruee.roleplay.data.model.MessageNode
import com.eterultimate.eteruee.roleplay.data.model.Preset
import com.eterultimate.eteruee.roleplay.data.model.PresetType
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.data.tavern.TavernCharacterCardFormat
import com.eterultimate.eteruee.roleplay.data.tavern.TavernPresetCodec
import com.eterultimate.eteruee.roleplay.data.tavern.TavernWorldInfoCodec
import com.eterultimate.eteruee.roleplay.domain.service.CharacterService
import com.eterultimate.eteruee.roleplay.domain.service.ChatService
import com.eterultimate.eteruee.roleplay.domain.service.GroupService
import com.eterultimate.eteruee.roleplay.domain.service.PresetService
import com.eterultimate.eteruee.roleplay.domain.service.WorldInfoService
import com.eterultimate.eteruee.utils.JsonInstant
import com.eterultimate.eteruee.web.BadRequestException
import com.eterultimate.eteruee.web.NotFoundException
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.utils.io.readAvailable
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import kotlin.uuid.Uuid

private const val MAX_ROLEPLAY_IMPORT_BYTES = 25 * 1024 * 1024

fun Route.roleplayRoutes(
    aiSDK: AISDK,
    settingsStore: SettingsStore,
    characterService: CharacterService,
    chatService: ChatService,
    groupService: GroupService,
    presetService: PresetService,
    worldInfoService: WorldInfoService,
    localTools: LocalTools,
) {
    route("/roleplay") {
        get("/summary") {
            val characters = characterService.getAllCharactersList().first()
            val groups = groupService.getAllGroups().first()
            val worlds = worldInfoService.getAllWorldInfos().first()
            val presets = presetService.getAllPresetsList()
            call.respond(
                RoleplaySummaryResponse(
                    characters = characters,
                    groups = groups,
                    worldInfos = worlds,
                    presets = presets.map { it.toDto() },
                )
            )
        }

        route("/characters") {
            get {
                call.respond(characterService.getAllCharactersList().first())
            }
            post("/import") {
                val upload = call.receiveRoleplayUpload()
                val fileName = upload.fileName.orEmpty()
                val character = if (upload.bytes.isPng() || fileName.endsWith(".png", ignoreCase = true)) {
                    characterService.importPngCharacter(upload.bytes).getOrThrow()
                } else {
                    characterService.importJsonCharacter(upload.bytes.toString(Charsets.UTF_8)).getOrThrow()
                }.requireName("Character name")
                call.respond(HttpStatusCode.Created, CharacterImportResponse(character))
            }
            post {
                val request = call.receive<SaveCharacterRequest>()
                val now = Instant.now()
                val character = Character(
                    name = request.name.trim(),
                    description = request.description,
                    personality = request.personality,
                    scenario = request.scenario,
                    firstMessage = request.firstMessage,
                    messageExamples = request.messageExamples,
                    systemPrompt = request.systemPrompt,
                    postHistoryInstructions = request.postHistoryInstructions,
                    creator = request.creator,
                    creatorNotes = request.creatorNotes,
                    tags = request.tags,
                    alternateGreetings = request.alternateGreetings,
                    characterBook = request.characterBook,
                    extensions = request.extensions,
                    createdAt = now,
                    updatedAt = now,
                ).requireName("Character name")
                val saved = characterService.createCharacter(character, avatarUri = null)
                    .getOrThrow()
                call.respond(HttpStatusCode.Created, saved)
            }
            get("/{id}/export.json") {
                val id = call.parameters["id"].toUuid("character id")
                val character = findCharacter(characterService, id)
                val format = call.request.queryParameters["format"].toTavernCharacterFormat(
                    default = TavernCharacterCardFormat.V3
                )
                val json = characterService.exportJsonCharacterString(id, format).getOrThrow()
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    attachmentDisposition("${character.name.safeFileStem()}-character.json")
                )
                call.respondBytes(json.toByteArray(Charsets.UTF_8), ContentType.Application.Json)
            }
            get("/{id}/export.png") {
                val id = call.parameters["id"].toUuid("character id")
                val character = findCharacter(characterService, id)
                val format = call.request.queryParameters["format"].toTavernCharacterFormat(
                    default = TavernCharacterCardFormat.V3
                )
                val png = characterService.exportPngCharacterBytes(id, format).getOrThrow()
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    attachmentDisposition("${character.name.safeFileStem()}-character.png")
                )
                call.respondBytes(png, ContentType.Image.PNG)
            }
            get("/{id}") {
                call.respond(findCharacter(characterService, call.parameters["id"].toUuid("character id")))
            }
            put("/{id}") {
                val id = call.parameters["id"].toUuid("character id")
                val existing = findCharacter(characterService, id)
                val request = call.receive<SaveCharacterRequest>()
                val updated = existing.copy(
                    name = request.name.trim(),
                    description = request.description,
                    personality = request.personality,
                    scenario = request.scenario,
                    firstMessage = request.firstMessage,
                    messageExamples = request.messageExamples,
                    systemPrompt = request.systemPrompt,
                    postHistoryInstructions = request.postHistoryInstructions,
                    creator = request.creator,
                    creatorNotes = request.creatorNotes,
                    tags = request.tags,
                    alternateGreetings = request.alternateGreetings,
                    characterBook = request.characterBook,
                    extensions = request.extensions,
                    updatedAt = Instant.now(),
                ).requireName("Character name")
                call.respond(characterService.updateCharacter(updated, avatarUri = null).getOrThrow())
            }
            delete("/{id}") {
                characterService.deleteCharacter(call.parameters["id"].toUuid("character id")).getOrThrow()
                call.respond(HttpStatusCode.OK, mapOf("status" to "deleted"))
            }
            post("/{id}/favorite") {
                val favorite = characterService.toggleFavorite(call.parameters["id"].toUuid("character id")).getOrThrow()
                call.respond(mapOf("favorite" to favorite))
            }
            post("/{id}/chats") {
                val characterId = call.parameters["id"].toUuid("character id")
                findCharacter(characterService, characterId)
                val request = call.receive<CreateChatRequest>()
                val chat = chatService.createChat(
                    characterId = characterId,
                    groupId = null,
                    title = request.title,
                ).getOrThrow()
                call.respond(HttpStatusCode.Created, chat)
            }
            get("/{id}/chats") {
                val characterId = call.parameters["id"].toUuid("character id")
                findCharacter(characterService, characterId)
                call.respond(chatService.getChatsByCharacter(characterId).first())
            }
        }

        route("/chats") {
            get("/{id}") {
                call.respond(findChat(chatService, call.parameters["id"].toUuid("chat id")))
            }
            delete("/{id}") {
                chatService.deleteChat(call.parameters["id"].toUuid("chat id")).getOrThrow()
                call.respond(HttpStatusCode.OK, mapOf("status" to "deleted"))
            }
            put("/{id}/title") {
                val request = call.receive<UpdateChatTitleRequest>()
                chatService.updateChatTitle(
                    chatId = call.parameters["id"].toUuid("chat id"),
                    title = request.title.trim().ifBlank { throw BadRequestException("Title must not be blank") },
                ).getOrThrow()
                call.respond(mapOf("status" to "updated"))
            }
            post("/{id}/pin") {
                val pinned = chatService.togglePin(call.parameters["id"].toUuid("chat id")).getOrThrow()
                call.respond(mapOf("pinned" to pinned))
            }
            get("/{id}/messages") {
                val chatId = call.parameters["id"].toUuid("chat id")
                findChat(chatService, chatId)
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                if (offset < 0) throw BadRequestException("offset must be >= 0")
                if (limit !in 1..500) throw BadRequestException("limit must be in 1..500")
                val nodes = chatService.loadMessages(chatId, offset, limit)
                call.respond(RoleplayMessagesResponse(nodes = nodes, count = chatService.getMessageCount(chatId)))
            }
            post("/{id}/messages") {
                val chatId = call.parameters["id"].toUuid("chat id")
                findChat(chatService, chatId)
                val request = call.receive<AppendMessageRequest>()
                val content = request.content.trim()
                if (content.isBlank()) throw BadRequestException("Message content must not be blank")
                val message = when (request.role) {
                    MessageRole.USER -> chatService.appendUserMessage(chatId, content)
                    MessageRole.ASSISTANT -> chatService.appendAssistantMessage(chatId, content)
                    else -> throw BadRequestException("Only USER and ASSISTANT messages can be appended")
                }.getOrThrow()
                call.respond(HttpStatusCode.Created, message)
            }
            put("/{id}/messages/{messageId}") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val messageId = call.parameters["messageId"].toUuid("message id")
                val request = call.receive<UpdateMessageRequest>()
                chatService.editMessageContent(chatId, messageId, request.content).getOrThrow()
                call.respond(mapOf("status" to "updated"))
            }
            delete("/{id}/messages/{messageId}") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val messageId = call.parameters["messageId"].toUuid("message id")
                chatService.deleteMessageById(chatId, messageId).getOrThrow()
                call.respond(mapOf("status" to "deleted"))
            }
            post("/{id}/clear") {
                chatService.clearAllMessages(call.parameters["id"].toUuid("chat id")).getOrThrow()
                call.respond(mapOf("status" to "cleared"))
            }
            post("/{id}/generate") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val chat = findChat(chatService, chatId)
                val request = call.receive<GenerateRoleplayRequest>()
                request.userMessage?.trim()?.takeIf { it.isNotEmpty() }?.let { message ->
                    chatService.appendUserMessage(chatId, message).getOrThrow()
                }
                call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
                    runCatching {
                        val settings = settingsStore.settingsFlow.first()
                        val character = characterService.getCharacterById(chat.characterId)
                        val model = resolveRoleplayModel(settingsStore, request.modelId)
                        val tools = localTools.getTools(settings.getCurrentAssistant().localTools)
                        val messages = buildRoleplayUiMessages(
                            chatService = chatService,
                            chat = chat,
                            character = character,
                            systemPrompt = request.systemPrompt,
                        )
                        val streamRequest = StreamTextRequest(
                            model = model,
                            messages = messages,
                            temperature = request.temperature,
                            maxTokens = request.maxTokens,
                            tools = tools,
                        )
                        val fullText = StringBuilder()
                        val stream = if (tools.isEmpty()) {
                            aiSDK.streamText(streamRequest)
                        } else {
                            aiSDK.streamTextWithTools(streamRequest, RoleplayLocalToolExecutor(tools))
                        }
                        stream.collect { chunk ->
                            when (chunk) {
                                is TextChunk.TextDelta -> {
                                    fullText.append(chunk.text)
                                    val payload = RoleplayGenerationEvent("delta", chunk.text, null, null)
                                    writeStringUtf8("data: ${JsonInstant.encodeToString(payload)}\n\n")
                                    flush()
                                }

                                is TextChunk.Finish -> {
                                    val message = chatService.appendAssistantMessage(chatId, fullText.toString()).getOrThrow()
                                    val payload = RoleplayGenerationEvent("complete", null, message, null)
                                    writeStringUtf8("data: ${JsonInstant.encodeToString(payload)}\n\n")
                                    flush()
                                }

                                is TextChunk.ToolCall -> {
                                    val payload = RoleplayGenerationEvent(
                                        type = "tool_call",
                                        toolCall = RoleplayToolCallEvent(
                                            toolCallId = chunk.toolCallId,
                                            toolName = chunk.toolName,
                                            arguments = chunk.arguments,
                                        ),
                                    )
                                    writeStringUtf8("data: ${JsonInstant.encodeToString(payload)}\n\n")
                                    flush()
                                }

                                else -> Unit
                            }
                        }
                    }.onFailure { error ->
                        val payload = RoleplayGenerationEvent(
                            type = "error",
                            error = error.message ?: error.javaClass.simpleName,
                        )
                        writeStringUtf8("data: ${JsonInstant.encodeToString(payload)}\n\n")
                        flush()
                    }
                }
            }
            post("/{id}/branches") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val request = call.receive<CreateBranchRequest>()
                val branchId = chatService.createBranch(chatId, request.fromMessageIndex).getOrThrow()
                call.respond(mapOf("branchId" to branchId.toString()))
            }
            get("/{id}/branches") {
                val chatId = call.parameters["id"].toUuid("chat id")
                findChat(chatService, chatId)
                call.respond(chatService.getBranches(chatId))
            }
            post("/{id}/branches/{branchId}/select") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val branchId = call.parameters["branchId"].toUuid("branch id")
                chatService.switchToBranch(chatId, branchId).getOrThrow()
                call.respond(mapOf("status" to "selected"))
            }
            delete("/{id}/branches/{branchId}") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val branchId = call.parameters["branchId"].toUuid("branch id")
                chatService.deleteBranch(chatId, branchId).getOrThrow()
                call.respond(mapOf("status" to "deleted"))
            }
            post("/{id}/messages/{index}/swipes") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val index = call.parameters["index"]?.toIntOrNull()
                    ?: throw BadRequestException("message index must be an integer")
                val request = call.receive<UpdateMessageRequest>()
                chatService.addSwipeAlternative(chatId, index, request.content).getOrThrow()
                call.respond(mapOf("status" to "created"))
            }
            post("/{id}/messages/{index}/swipes/next") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val index = call.parameters["index"]?.toIntOrNull()
                    ?: throw BadRequestException("message index must be an integer")
                chatService.nextSwipe(chatId, index).getOrThrow()
                call.respond(mapOf("status" to "selected"))
            }
            post("/{id}/messages/{index}/swipes/previous") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val index = call.parameters["index"]?.toIntOrNull()
                    ?: throw BadRequestException("message index must be an integer")
                chatService.previousSwipe(chatId, index).getOrThrow()
                call.respond(mapOf("status" to "selected"))
            }
            delete("/{id}/messages/index/{index}") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val index = call.parameters["index"]?.toIntOrNull()
                    ?: throw BadRequestException("message index must be an integer")
                chatService.deleteMessageNode(chatId, index).getOrThrow()
                call.respond(mapOf("status" to "deleted"))
            }
            put("/{id}/messages/index/{index}") {
                val chatId = call.parameters["id"].toUuid("chat id")
                val index = call.parameters["index"]?.toIntOrNull()
                    ?: throw BadRequestException("message index must be an integer")
                val request = call.receive<UpdateMessageRequest>()
                chatService.editMessage(chatId, index, request.content).getOrThrow()
                call.respond(mapOf("status" to "updated"))
            }
        }

        route("/groups") {
            get {
                call.respond(groupService.getAllGroups().first())
            }
            post {
                val request = call.receive<SaveGroupRequest>()
                val group = groupService.createGroup(
                    name = request.name.trim().ifBlank { throw BadRequestException("Group name must not be blank") },
                    description = request.description,
                ).getOrThrow()
                call.respond(HttpStatusCode.Created, group)
            }
            put("/{id}") {
                val existing = findGroup(groupService, call.parameters["id"].toUuid("group id"))
                val request = call.receive<SaveGroupRequest>()
                val updated = existing.copy(
                    name = request.name.trim().ifBlank { throw BadRequestException("Group name must not be blank") },
                    description = request.description,
                )
                call.respond(groupService.updateGroup(updated).getOrThrow())
            }
            delete("/{id}") {
                groupService.deleteGroup(call.parameters["id"].toUuid("group id")).getOrThrow()
                call.respond(mapOf("status" to "deleted"))
            }
            post("/{id}/members") {
                val groupId = call.parameters["id"].toUuid("group id")
                val request = call.receive<GroupMember>()
                call.respond(groupService.addMember(groupId, request).getOrThrow())
            }
            put("/{id}/members/{characterId}") {
                val groupId = call.parameters["id"].toUuid("group id")
                val characterId = call.parameters["characterId"].toUuid("character id")
                val request = call.receive<GroupMember>()
                call.respond(groupService.updateMember(groupId, characterId, request).getOrThrow())
            }
            delete("/{id}/members/{characterId}") {
                val groupId = call.parameters["id"].toUuid("group id")
                val characterId = call.parameters["characterId"].toUuid("character id")
                call.respond(groupService.removeMember(groupId, characterId).getOrThrow())
            }
            post("/{id}/members/{characterId}/active") {
                val groupId = call.parameters["id"].toUuid("group id")
                val characterId = call.parameters["characterId"].toUuid("character id")
                call.respond(groupService.toggleMemberActive(groupId, characterId).getOrThrow())
            }
            post("/{id}/chats") {
                val groupId = call.parameters["id"].toUuid("group id")
                val request = call.receive<CreateChatRequest>()
                val chatId = groupService.createGroupChat(groupId, request.title).getOrThrow()
                call.respond(HttpStatusCode.Created, findChat(chatService, chatId))
            }
            get("/{id}/chats") {
                call.respond(groupService.getGroupChats(call.parameters["id"].toUuid("group id")))
            }
        }

        route("/world-infos") {
            get {
                call.respond(worldInfoService.getAllWorldInfos().first())
            }
            post("/import") {
                val upload = call.receiveRoleplayUpload()
                val fallbackName = upload.fileName.fallbackNameFromFile("Imported Lorebook")
                val now = Instant.now()
                val decoded = TavernWorldInfoCodec.decode(
                    jsonString = upload.bytes.toString(Charsets.UTF_8),
                    fallbackName = fallbackName
                )
                val worldInfo = decoded.copy(
                    id = Uuid.random(),
                    createdAt = now,
                    updatedAt = now,
                )
                val saved = worldInfoService.saveWorldInfo(worldInfo.requireName()).getOrThrow()
                call.respond(HttpStatusCode.Created, WorldInfoImportResponse(saved))
            }
            post {
                val request = call.receive<SaveWorldInfoRequest>()
                val world = if (request.template != null) {
                    request.template.copy(
                        id = Uuid.random(),
                        name = request.template.name.trim().ifBlank {
                            throw BadRequestException("World info name must not be blank")
                        },
                        updatedAt = Instant.now(),
                    )
                } else {
                    WorldInfo(
                        name = request.name.trim().ifBlank { throw BadRequestException("World info name must not be blank") },
                        description = request.description,
                    )
                }
                call.respond(HttpStatusCode.Created, worldInfoService.saveWorldInfo(world).getOrThrow())
            }
            get("/{id}/export.json") {
                val world = findWorldInfo(worldInfoService, call.parameters["id"].toUuid("world info id"))
                val json = TavernWorldInfoCodec.encodeWorldInfo(world)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    attachmentDisposition("${world.name.safeFileStem()}-world.json")
                )
                call.respondBytes(json.toByteArray(Charsets.UTF_8), ContentType.Application.Json)
            }
            put("/{id}") {
                val id = call.parameters["id"].toUuid("world info id")
                findWorldInfo(worldInfoService, id)
                val request = call.receive<WorldInfo>()
                call.respond(worldInfoService.updateWorldInfo(request.copy(id = id)).getOrThrow())
            }
            delete("/{id}") {
                worldInfoService.deleteWorldInfo(call.parameters["id"].toUuid("world info id")).getOrThrow()
                call.respond(mapOf("status" to "deleted"))
            }
            post("/{id}/entries") {
                val worldId = call.parameters["id"].toUuid("world info id")
                call.respond(worldInfoService.addEntry(worldId, call.receive<WorldInfoEntry>()).getOrThrow())
            }
            put("/{id}/entries/{entryId}") {
                val worldId = call.parameters["id"].toUuid("world info id")
                val entryId = call.parameters["entryId"].toUuid("entry id")
                val request = call.receive<WorldInfoEntry>()
                call.respond(worldInfoService.updateEntry(worldId, request.copy(id = entryId)).getOrThrow())
            }
            delete("/{id}/entries/{entryId}") {
                val worldId = call.parameters["id"].toUuid("world info id")
                val entryId = call.parameters["entryId"].toUuid("entry id")
                call.respond(worldInfoService.deleteEntry(worldId, entryId).getOrThrow())
            }
            post("/{id}/entries/{entryId}/enabled") {
                val worldId = call.parameters["id"].toUuid("world info id")
                val entryId = call.parameters["entryId"].toUuid("entry id")
                call.respond(worldInfoService.toggleEntryEnabled(worldId, entryId).getOrThrow())
            }
            post("/{id}/match") {
                val request = call.receive<MatchWorldInfoRequest>()
                val matches = worldInfoService.scanAndMatchEntries(
                    worldInfoId = call.parameters["id"].toUuid("world info id"),
                    recentMessages = request.messages,
                    scanDepth = request.scanDepth,
                )
                call.respond(matches)
            }
        }

        route("/presets") {
            get {
                val type = call.request.queryParameters["type"]?.trim()?.takeIf { it.isNotEmpty() }
                    ?.let { runCatching { PresetType.valueOf(it.uppercase()) }.getOrNull() }
                val presets = if (type == null) {
                    presetService.getAllPresetsList()
                } else {
                    presetService.getPresetsByTypeList(type)
                }
                call.respond(presets.map { it.toDto() })
            }
            post("/import") {
                val upload = call.receiveRoleplayUpload()
                val preset = TavernPresetCodec.decode(
                    jsonString = upload.bytes.toString(Charsets.UTF_8),
                    fallbackName = upload.fileName.fallbackNameFromFile("Imported Preset")
                ).withFreshIdentity()
                val saved = presetService.savePreset(preset).getOrThrow()
                call.respond(HttpStatusCode.Created, PresetImportResponse(saved.toDto()))
            }
            post {
                val preset = call.receive<SavePresetRequest>().toPreset().withFreshIdentity()
                call.respond(HttpStatusCode.Created, presetService.savePreset(preset).getOrThrow().toDto())
            }
            get("/{id}/export.json") {
                val preset = presetService.getPresetById(call.parameters["id"].toUuid("preset id"))
                    ?: throw NotFoundException("Preset not found")
                val json = TavernPresetCodec.encode(preset)
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    attachmentDisposition("${preset.name.safeFileStem()}-preset.json")
                )
                call.respondBytes(json.toByteArray(Charsets.UTF_8), ContentType.Application.Json)
            }
            put("/{id}") {
                val id = call.parameters["id"].toUuid("preset id")
                val existing = presetService.getPresetById(id) ?: throw NotFoundException("Preset not found")
                val request = call.receive<SavePresetRequest>()
                val preset = request.toPreset().copy(id = id, createdAt = existing.createdAt, updatedAt = Instant.now())
                    .requireName()
                call.respond(presetService.savePreset(preset).getOrThrow().toDto())
            }
            delete("/{id}") {
                presetService.deletePreset(call.parameters["id"].toUuid("preset id")).getOrThrow()
                call.respond(mapOf("status" to "deleted"))
            }
        }
    }
}

@Serializable
data class RoleplaySummaryResponse(
    val characters: List<Character>,
    val groups: List<Group>,
    val worldInfos: List<WorldInfo>,
    val presets: List<RoleplayPresetDto>,
)

@Serializable
data class CharacterImportResponse(
    val item: Character,
)

@Serializable
data class WorldInfoImportResponse(
    val item: WorldInfo,
)

@Serializable
data class PresetImportResponse(
    val item: RoleplayPresetDto,
)

@Serializable
data class SaveCharacterRequest(
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "",
    val messageExamples: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val creator: String = "",
    val creatorNotes: String = "",
    val tags: List<String> = emptyList(),
    val alternateGreetings: List<String> = emptyList(),
    val characterBook: JsonElement? = null,
    val extensions: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class CreateChatRequest(
    val title: String = "",
)

@Serializable
data class UpdateChatTitleRequest(val title: String)

@Serializable
data class AppendMessageRequest(
    val role: MessageRole = MessageRole.USER,
    val content: String,
)

@Serializable
data class UpdateMessageRequest(val content: String)

@Serializable
data class GenerateRoleplayRequest(
    val providerId: String,
    val modelId: String,
    val systemPrompt: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val userMessage: String? = null,
)

@Serializable
data class CreateBranchRequest(
    val fromMessageIndex: Int,
)

@Serializable
data class RoleplayMessagesResponse(
    val nodes: List<MessageNode>,
    val count: Int,
)

@Serializable
data class RoleplayGenerationEvent(
    val type: String,
    val delta: String? = null,
    val message: ChatMessage? = null,
    val error: String? = null,
    val toolCall: RoleplayToolCallEvent? = null,
)

@Serializable
data class RoleplayToolCallEvent(
    val toolCallId: String,
    val toolName: String,
    val arguments: String,
)

@Serializable
data class SaveGroupRequest(
    val name: String,
    val description: String = "",
)

@Serializable
data class SaveWorldInfoRequest(
    val name: String = "",
    val description: String = "",
    val template: WorldInfo? = null,
)

@Serializable
data class MatchWorldInfoRequest(
    val messages: List<String>,
    val scanDepth: Int = 4,
)

@Serializable
data class RoleplayPresetDto(
    val id: String,
    val name: String,
    val description: String,
    val type: PresetType,
    val parameters: Map<String, JsonElement>,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class SavePresetRequest(
    val name: String,
    val description: String = "",
    val type: PresetType = PresetType.OPENAI,
    val parameters: Map<String, JsonElement> = emptyMap(),
) {
    fun toPreset(): Preset {
        return Preset(
            name = name.trim(),
            description = description,
            type = type,
            parameters = parameters,
        )
    }
}

private suspend fun findCharacter(service: CharacterService, id: Uuid): Character {
    return service.getCharacterById(id) ?: throw NotFoundException("Character not found")
}

private suspend fun findChat(service: ChatService, id: Uuid): ChatMetadata {
    return service.getChatById(id) ?: throw NotFoundException("Chat not found")
}

private suspend fun findGroup(service: GroupService, id: Uuid): Group {
    return service.getGroupById(id) ?: throw NotFoundException("Group not found")
}

private suspend fun findWorldInfo(service: WorldInfoService, id: Uuid): WorldInfo {
    return service.getWorldInfoById(id) ?: throw NotFoundException("World info not found")
}

private data class RoleplayUpload(
    val bytes: ByteArray,
    val fileName: String?,
)

private suspend fun ApplicationCall.receiveRoleplayUpload(): RoleplayUpload {
    val multipart = receiveMultipart()
    var upload: RoleplayUpload? = null

    while (true) {
        val part = multipart.readPart() ?: break
        try {
            if (part is PartData.FileItem && upload == null) {
                val bytes = readUploadBytes(part, MAX_ROLEPLAY_IMPORT_BYTES)
                if (bytes.isEmpty()) throw BadRequestException("Uploaded file is empty")
                upload = RoleplayUpload(
                    bytes = bytes,
                    fileName = part.originalFileName?.sanitizeUploadFileName(),
                )
            }
        } finally {
            part.dispose()
        }
    }

    return upload ?: throw BadRequestException("No file uploaded")
}

private suspend fun readUploadBytes(part: PartData.FileItem, maxBytes: Int): ByteArray {
    val input = part.provider()
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0

    while (true) {
        val read = input.readAvailable(buffer, 0, buffer.size)
        if (read <= 0) break

        totalBytes += read
        if (totalBytes > maxBytes) {
            throw BadRequestException("File too large: max ${maxBytes / (1024 * 1024)} MB")
        }

        output.write(buffer, 0, read)
    }

    return output.toByteArray()
}

private fun Character.requireName(field: String): Character {
    if (name.isBlank()) throw BadRequestException("$field must not be blank")
    return this
}

private fun WorldInfo.requireName(): WorldInfo {
    if (name.isBlank()) throw BadRequestException("World info name must not be blank")
    return this
}

private fun Preset.requireName(): Preset {
    if (name.isBlank()) throw BadRequestException("Preset name must not be blank")
    return this
}

private fun Preset.withFreshIdentity(): Preset {
    return copy(id = Uuid.random(), createdAt = Instant.now(), updatedAt = Instant.now()).requireName()
}

private fun Preset.toDto(): RoleplayPresetDto {
    return RoleplayPresetDto(
        id = id.toString(),
        name = name,
        description = description,
        type = type,
        parameters = parameters.mapValues { (_, value) ->
            value as? JsonElement ?: JsonPrimitive(value.toString())
        },
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}

private fun ByteArray.isPng(): Boolean {
    val signature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    return size >= signature.size && signature.indices.all { index -> this[index] == signature[index] }
}

private fun String?.toTavernCharacterFormat(default: TavernCharacterCardFormat): TavernCharacterCardFormat {
    return when (this?.trim()?.lowercase()) {
        null, "" -> default
        "v1", "1" -> TavernCharacterCardFormat.V1
        "v2", "2" -> TavernCharacterCardFormat.V2
        "v3", "3" -> TavernCharacterCardFormat.V3
        else -> throw BadRequestException("Unsupported character card format")
    }
}

private fun String?.fallbackNameFromFile(defaultName: String): String {
    return this
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.substringBeforeLast('.')
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: defaultName
}

private fun String.sanitizeUploadFileName(): String {
    return substringAfterLast('/').substringAfterLast('\\')
        .replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
        .trim()
        .ifBlank { "file" }
}

private fun String.safeFileStem(): String {
    return trim()
        .replace(Regex("[\\\\/:*?\"<>|\\r\\n]+"), "_")
        .take(80)
        .ifBlank { "untitled" }
}

private fun attachmentDisposition(fileName: String): String {
    val sanitized = fileName.sanitizeUploadFileName()
    val fallback = sanitized
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .ifBlank { "download" }
    val encoded = URLEncoder
        .encode(sanitized, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
    return "attachment; filename=\"$fallback\"; filename*=UTF-8''$encoded"
}

private suspend fun resolveRoleplayModel(
    settingsStore: SettingsStore,
    requestedModelId: String,
): Model {
    val settings = settingsStore.settingsFlow.first()
    val trimmed = requestedModelId.trim()
    val requestedUuid = runCatching { Uuid.parse(trimmed) }.getOrNull()
    if (requestedUuid != null) {
        settings.findModelById(requestedUuid)?.let { return it }
    }

    settings.providers
        .asSequence()
        .flatMap { it.models.asSequence() }
        .firstOrNull { model ->
            model.modelId == trimmed || model.displayName == trimmed
        }
        ?.let { return it }

    val fallbackId = settings.assistants
        .firstOrNull { it.id == settings.assistantId }
        ?.chatModelId
        ?: settings.chatModelId
    return settings.findModelById(fallbackId)
        ?: throw BadRequestException("Model not found")
}

private class RoleplayLocalToolExecutor(
    private val tools: List<Tool>,
) : ToolExecutor {
    override suspend fun execute(toolCallId: String, toolName: String, arguments: String): ToolResult {
        val tool = tools.find { it.name == toolName }
            ?: return ToolResult(toolCallId, "Error: Tool not found: $toolName", isError = true)

        if (tool.needsApproval) {
            return ToolResult(
                toolCallId = toolCallId,
                result = "Error: Tool $toolName requires explicit approval and cannot run inside the roleplay stream.",
                isError = true,
            )
        }

        return runCatching {
            val args = JsonInstant.parseToJsonElement(arguments)
            val output = tool.execute(args).joinToString("\n") { part ->
                when (part) {
                    is UIMessagePart.Text -> part.text
                    else -> JsonInstant.encodeToString(part)
                }
            }
            ToolResult(toolCallId, output)
        }.getOrElse { error ->
            ToolResult(toolCallId, "Error: ${error.message ?: error.javaClass.simpleName}", isError = true)
        }
    }
}

private suspend fun buildRoleplayUiMessages(
    chatService: ChatService,
    chat: ChatMetadata,
    character: Character?,
    systemPrompt: String,
): List<UIMessage> {
    val prompt = listOf(
        systemPrompt.trim(),
        character?.systemPrompt?.trim().orEmpty(),
        character?.description?.takeIf { it.isNotBlank() }?.let { "Description:\n$it" }.orEmpty(),
        character?.personality?.takeIf { it.isNotBlank() }?.let { "Personality:\n$it" }.orEmpty(),
        character?.scenario?.takeIf { it.isNotBlank() }?.let { "Scenario:\n$it" }.orEmpty(),
        character?.postHistoryInstructions?.trim().orEmpty(),
    ).filter { it.isNotBlank() }.joinToString("\n\n")

    val history = chatService
        .loadMessages(chat.chatId, offset = 0, limit = 500)
        .mapNotNull { it.getCurrentMessage() }
        .map { message ->
            UIMessage(
                id = message.id,
                role = message.role,
                parts = listOf(UIMessagePart.Text(message.content)),
            )
        }

    val firstGreeting = character?.firstMessage
        ?.trim()
        ?.takeIf { it.isNotEmpty() && history.none { message -> message.role == MessageRole.ASSISTANT } }
        ?.let { UIMessage.assistant(it) }

    return buildList {
        if (prompt.isNotBlank()) {
            add(UIMessage.system(prompt))
        }
        if (firstGreeting != null) {
            add(firstGreeting)
        }
        addAll(history)
    }
}
