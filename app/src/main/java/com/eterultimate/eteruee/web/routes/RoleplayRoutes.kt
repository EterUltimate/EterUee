package com.eterultimate.eteruee.web.routes

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.domain.service.CharacterService
import com.eterultimate.eteruee.roleplay.domain.service.ChatService
import com.eterultimate.eteruee.roleplay.domain.service.GroupService
import com.eterultimate.eteruee.roleplay.domain.service.PresetService
import com.eterultimate.eteruee.roleplay.domain.service.WorldInfoService
import com.eterultimate.eteruee.web.BadRequestException
import com.eterultimate.eteruee.web.NotFoundException
import com.eterultimate.eteruee.web.dto.AppendRoleplayMessageRequest
import com.eterultimate.eteruee.web.dto.CreateRoleplayChatRequest
import com.eterultimate.eteruee.web.dto.CreateRoleplayGroupRequest
import com.eterultimate.eteruee.web.dto.CreateRoleplayWorldInfoRequest
import com.eterultimate.eteruee.web.dto.RoleplayChatDetailDto
import com.eterultimate.eteruee.web.dto.RoleplayOverviewDto
import com.eterultimate.eteruee.web.dto.UpdateRoleplayChatTitleRequest
import com.eterultimate.eteruee.web.dto.UpsertRoleplayCharacterRequest
import com.eterultimate.eteruee.web.dto.UpsertRoleplayPresetRequest
import com.eterultimate.eteruee.web.dto.toCharacter
import com.eterultimate.eteruee.web.dto.toPreset
import com.eterultimate.eteruee.web.dto.toRoleplayDto
import com.eterultimate.eteruee.web.dto.toRoleplayMessageRole
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.first

fun Route.roleplayRoutes(
    characterService: CharacterService,
    chatService: ChatService,
    worldInfoService: WorldInfoService,
    groupService: GroupService,
    presetService: PresetService,
) {
    route("/roleplay") {
        get("/overview") {
            val characters = characterService.getAllCharactersList().first()
            val groups = groupService.getAllGroups().first()
            val worldInfos = worldInfoService.getAllWorldInfos().first()
            val presets = presetService.getAllPresetsList()
            call.respond(
                RoleplayOverviewDto(
                    characterCount = characters.size,
                    groupCount = groups.size,
                    worldInfoCount = worldInfos.size,
                    presetCount = presets.size,
                )
            )
        }

        route("/characters") {
            get {
                val query = call.request.queryParameters["query"]?.trim().orEmpty()
                val characters = if (query.isBlank()) {
                    characterService.getAllCharactersList().first()
                } else {
                    characterService.searchCharacters(query)
                }
                call.respond(characters.map { it.toRoleplayDto() })
            }

            post {
                val request = call.receive<UpsertRoleplayCharacterRequest>()
                if (request.name.isBlank()) {
                    throw BadRequestException("Character name must not be blank")
                }
                val character = request.toCharacter()
                val created = characterService.createCharacter(character, avatarUri = null).getOrThrow()
                call.respond(HttpStatusCode.Created, created.toRoleplayDto())
            }

            get("/{id}") {
                val id = call.parameters["id"].toUuid("character id")
                val character = characterService.getCharacterById(id)
                    ?: throw NotFoundException("Character not found")
                call.respond(character.toRoleplayDto())
            }

            put("/{id}") {
                val id = call.parameters["id"].toUuid("character id")
                val existing = characterService.getCharacterById(id)
                    ?: throw NotFoundException("Character not found")
                val request = call.receive<UpsertRoleplayCharacterRequest>()
                if (request.name.isBlank()) {
                    throw BadRequestException("Character name must not be blank")
                }
                val updated = characterService.updateCharacter(request.toCharacter(existing), avatarUri = null).getOrThrow()
                call.respond(HttpStatusCode.OK, updated.toRoleplayDto())
            }

            delete("/{id}") {
                val id = call.parameters["id"].toUuid("character id")
                characterService.getCharacterById(id) ?: throw NotFoundException("Character not found")
                characterService.deleteCharacter(id).getOrThrow()
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{id}/favorite") {
                val id = call.parameters["id"].toUuid("character id")
                characterService.getCharacterById(id) ?: throw NotFoundException("Character not found")
                val favorite = characterService.toggleFavorite(id).getOrThrow()
                call.respond(HttpStatusCode.OK, mapOf("favorite" to favorite))
            }

            get("/{id}/chats") {
                val id = call.parameters["id"].toUuid("character id")
                characterService.getCharacterById(id) ?: throw NotFoundException("Character not found")
                call.respond(chatService.getChatsByCharacter(id).first().map { it.toRoleplayDto() })
            }
        }

        route("/groups") {
            get {
                call.respond(groupService.getAllGroups().first().map { it.toRoleplayDto() })
            }

            post {
                val request = call.receive<CreateRoleplayGroupRequest>()
                if (request.name.isBlank()) {
                    throw BadRequestException("Group name must not be blank")
                }
                val group = groupService.createGroup(request.name.trim(), request.description).getOrThrow()
                call.respond(HttpStatusCode.Created, group.toRoleplayDto())
            }

            get("/{id}") {
                val id = call.parameters["id"].toUuid("group id")
                val group = groupService.getGroupById(id) ?: throw NotFoundException("Group not found")
                call.respond(group.toRoleplayDto())
            }

            delete("/{id}") {
                val id = call.parameters["id"].toUuid("group id")
                groupService.getGroupById(id) ?: throw NotFoundException("Group not found")
                groupService.deleteGroup(id).getOrThrow()
                call.respond(HttpStatusCode.NoContent)
            }

            get("/{id}/chats") {
                val id = call.parameters["id"].toUuid("group id")
                groupService.getGroupById(id) ?: throw NotFoundException("Group not found")
                call.respond(chatService.getChatsByGroup(id).first().map { it.toRoleplayDto() })
            }
        }

        route("/chats") {
            post {
                val request = call.receive<CreateRoleplayChatRequest>()
                val groupId = request.groupId?.toUuid("group id")
                val characterId = request.characterId?.toUuid("character id")

                val chat = if (groupId != null) {
                    groupService.getGroupById(groupId) ?: throw NotFoundException("Group not found")
                    val chatId = groupService.createGroupChat(groupId, request.title).getOrThrow()
                    chatService.getChatById(chatId) ?: throw NotFoundException("Chat not found")
                } else {
                    val targetCharacterId = characterId
                        ?: throw BadRequestException("characterId or groupId is required")
                    characterService.getCharacterById(targetCharacterId)
                        ?: throw NotFoundException("Character not found")
                    chatService.createChat(targetCharacterId, title = request.title).getOrThrow()
                }

                call.respond(HttpStatusCode.Created, chat.toRoleplayDto())
            }

            get("/{id}") {
                val id = call.parameters["id"].toUuid("chat id")
                val chat = chatService.getChatById(id) ?: throw NotFoundException("Chat not found")
                val character = characterService.getCharacterById(chat.characterId)
                val group = chat.groupId?.let { groupService.getGroupById(it) }
                val messages = chatService.loadMessages(id, offset = 0, limit = 500)
                    .mapNotNull { it.getCurrentMessage() }
                call.respond(
                    RoleplayChatDetailDto(
                        chat = chat.toRoleplayDto(),
                        character = character?.toRoleplayDto(),
                        group = group?.toRoleplayDto(),
                        messages = messages.map { it.toRoleplayDto() },
                    )
                )
            }

            delete("/{id}") {
                val id = call.parameters["id"].toUuid("chat id")
                chatService.getChatById(id) ?: throw NotFoundException("Chat not found")
                chatService.deleteChat(id).getOrThrow()
                call.respond(HttpStatusCode.NoContent)
            }

            post("/{id}/title") {
                val id = call.parameters["id"].toUuid("chat id")
                val request = call.receive<UpdateRoleplayChatTitleRequest>()
                if (request.title.isBlank()) {
                    throw BadRequestException("Title must not be blank")
                }
                chatService.getChatById(id) ?: throw NotFoundException("Chat not found")
                chatService.updateChatTitle(id, request.title.trim()).getOrThrow()
                call.respond(HttpStatusCode.OK, mapOf("status" to "updated"))
            }

            post("/{id}/pin") {
                val id = call.parameters["id"].toUuid("chat id")
                chatService.getChatById(id) ?: throw NotFoundException("Chat not found")
                val pinned = chatService.togglePin(id).getOrThrow()
                call.respond(HttpStatusCode.OK, mapOf("pinned" to pinned))
            }

            get("/{id}/messages") {
                val id = call.parameters["id"].toUuid("chat id")
                chatService.getChatById(id) ?: throw NotFoundException("Chat not found")
                val offset = call.request.queryParameters["offset"]?.toIntOrNull() ?: 0
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 200
                if (offset < 0 || limit !in 1..500) {
                    throw BadRequestException("offset must be >= 0 and limit must be in 1..500")
                }
                val messages = chatService.loadMessages(id, offset = offset, limit = limit)
                    .mapNotNull { it.getCurrentMessage() }
                call.respond(messages.map { it.toRoleplayDto() })
            }

            post("/{id}/messages") {
                val id = call.parameters["id"].toUuid("chat id")
                val request = call.receive<AppendRoleplayMessageRequest>()
                if (request.content.isBlank()) {
                    throw BadRequestException("Message content must not be blank")
                }
                chatService.getChatById(id) ?: throw NotFoundException("Chat not found")
                val message = when (request.role.toRoleplayMessageRole()) {
                    MessageRole.USER -> chatService.appendUserMessage(id, request.content).getOrThrow()
                    MessageRole.ASSISTANT -> chatService.appendAssistantMessage(id, request.content).getOrThrow()
                    MessageRole.SYSTEM, MessageRole.TOOL -> throw BadRequestException("Only user and assistant messages can be appended")
                }
                call.respond(HttpStatusCode.Created, message.toRoleplayDto())
            }

            delete("/{id}/messages/{messageId}") {
                val id = call.parameters["id"].toUuid("chat id")
                val messageId = call.parameters["messageId"].toUuid("message id")
                chatService.getChatById(id) ?: throw NotFoundException("Chat not found")
                chatService.deleteMessageById(id, messageId).getOrThrow()
                call.respond(HttpStatusCode.NoContent)
            }
        }

        route("/world-infos") {
            get {
                call.respond(worldInfoService.getAllWorldInfos().first().map { it.toRoleplayDto() })
            }

            post {
                val request = call.receive<CreateRoleplayWorldInfoRequest>()
                if (request.name.isBlank()) {
                    throw BadRequestException("World info name must not be blank")
                }
                val worldInfo = worldInfoService.createWorldInfo(request.name.trim(), request.description).getOrThrow()
                call.respond(HttpStatusCode.Created, worldInfo.toRoleplayDto())
            }

            get("/{id}") {
                val id = call.parameters["id"].toUuid("world info id")
                val worldInfo = worldInfoService.getWorldInfoById(id)
                    ?: throw NotFoundException("World info not found")
                call.respond(worldInfo.toRoleplayDto())
            }

            delete("/{id}") {
                val id = call.parameters["id"].toUuid("world info id")
                worldInfoService.getWorldInfoById(id) ?: throw NotFoundException("World info not found")
                worldInfoService.deleteWorldInfo(id).getOrThrow()
                call.respond(HttpStatusCode.NoContent)
            }
        }

        route("/presets") {
            get {
                val type = call.request.queryParameters["type"]?.trim()?.uppercase()
                val presets = if (type.isNullOrBlank()) {
                    presetService.getAllPresetsList()
                } else {
                    val presetType = runCatching {
                        com.eterultimate.eteruee.roleplay.data.model.PresetType.valueOf(type)
                    }.getOrElse {
                        throw BadRequestException("Unsupported preset type")
                    }
                    presetService.getPresetsByTypeList(presetType)
                }
                call.respond(presets.map { it.toRoleplayDto() })
            }

            post {
                val request = call.receive<UpsertRoleplayPresetRequest>()
                if (request.name.isBlank()) {
                    throw BadRequestException("Preset name must not be blank")
                }
                val preset = presetService.savePreset(request.toPreset()).getOrThrow()
                call.respond(HttpStatusCode.Created, preset.toRoleplayDto())
            }

            get("/{id}") {
                val id = call.parameters["id"].toUuid("preset id")
                val preset = presetService.getPresetById(id) ?: throw NotFoundException("Preset not found")
                call.respond(preset.toRoleplayDto())
            }

            put("/{id}") {
                val id = call.parameters["id"].toUuid("preset id")
                val existing = presetService.getPresetById(id) ?: throw NotFoundException("Preset not found")
                val request = call.receive<UpsertRoleplayPresetRequest>()
                if (request.name.isBlank()) {
                    throw BadRequestException("Preset name must not be blank")
                }
                val preset = presetService.savePreset(request.toPreset(existing)).getOrThrow()
                call.respond(HttpStatusCode.OK, preset.toRoleplayDto())
            }

            delete("/{id}") {
                val id = call.parameters["id"].toUuid("preset id")
                presetService.getPresetById(id) ?: throw NotFoundException("Preset not found")
                presetService.deletePreset(id).getOrThrow()
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
