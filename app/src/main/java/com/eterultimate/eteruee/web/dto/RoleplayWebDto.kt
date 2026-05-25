package com.eterultimate.eteruee.web.dto

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.data.model.ChatMetadata
import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.data.model.GroupMember
import com.eterultimate.eteruee.roleplay.data.model.Preset
import com.eterultimate.eteruee.roleplay.data.model.PresetType
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class RoleplayOverviewDto(
    val characterCount: Int,
    val groupCount: Int,
    val worldInfoCount: Int,
    val presetCount: Int,
)

@Serializable
data class RoleplayCharacterDto(
    val id: String,
    val name: String,
    val description: String,
    val personality: String,
    val scenario: String,
    val firstMessage: String,
    val messageExamples: String,
    val systemPrompt: String,
    val postHistoryInstructions: String,
    val avatarUrl: String?,
    val creator: String,
    val creatorNotes: String,
    val tags: List<String>,
    val talkativeness: Float,
    val alternateGreetings: List<String>,
    val characterVersion: String,
    val favorite: Boolean,
    val chatCount: Int,
    val lastChatAt: Long?,
    val spec: String,
    val specVersion: String,
    val characterBook: JsonElement?,
    val extensions: Map<String, JsonElement>,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class UpsertRoleplayCharacterRequest(
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
    val talkativeness: Float = 0.5f,
    val alternateGreetings: List<String> = emptyList(),
)

@Serializable
data class RoleplayChatDto(
    val id: String,
    val characterId: String,
    val groupId: String?,
    val title: String,
    val userName: String,
    val characterName: String,
    val createDate: String,
    val tavernChatId: String,
    val messageCount: Int,
    val pinned: Boolean,
    val activeBranchId: String?,
    val rootNodes: List<String>,
    val variables: Map<String, String>,
    val tavernMetadata: JsonElement?,
    val extensions: Map<String, JsonElement>,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class RoleplayChatDetailDto(
    val chat: RoleplayChatDto,
    val character: RoleplayCharacterDto?,
    val group: RoleplayGroupDto?,
    val messages: List<RoleplayMessageDto>,
)

@Serializable
data class CreateRoleplayChatRequest(
    val characterId: String? = null,
    val groupId: String? = null,
    val title: String = "",
)

@Serializable
data class UpdateRoleplayChatTitleRequest(
    val title: String,
)

@Serializable
data class AppendRoleplayMessageRequest(
    val content: String,
    val role: String = "user",
)

@Serializable
data class RoleplayMessageDto(
    val id: String,
    val role: String,
    val content: String,
    val timestamp: Long,
    val tavernName: String,
    val tavernSendDate: String,
    val model: String?,
    val tokenCount: Int?,
    val swipeAlternatives: List<String>,
    val speakerId: String?,
    val speakerName: String?,
    val extra: Map<String, JsonElement>,
)

@Serializable
data class RoleplayGroupDto(
    val id: String,
    val name: String,
    val description: String,
    val members: List<RoleplayGroupMemberDto>,
    val activeMembers: List<String>,
    val avatarUrl: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class RoleplayGroupMemberDto(
    val characterId: String,
    val name: String,
    val priority: Int,
    val responseProbability: Float,
    val forcedResponse: Boolean,
)

@Serializable
data class RoleplayWorldInfoDto(
    val id: String,
    val name: String,
    val description: String,
    val entries: List<RoleplayWorldInfoEntryDto>,
    val scanDepth: Int,
    val scanTrigger: String,
    val selectiveLogic: String,
    val extensions: Map<String, JsonElement>,
    val originalData: JsonElement?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class RoleplayWorldInfoEntryDto(
    val id: String,
    val key: String,
    val keys: List<String>,
    val secondaryKeys: List<String>,
    val comment: String,
    val content: String,
    val constant: Boolean,
    val selective: Boolean,
    val order: Int,
    val position: String,
    val tavernPosition: Int,
    val enabled: Boolean,
    val probability: Float,
    val useProbability: Boolean,
    val depth: Int,
    val role: Int,
    val displayIndex: Int,
    val extensions: Map<String, JsonElement>,
)

@Serializable
data class CreateRoleplayWorldInfoRequest(
    val name: String,
    val description: String = "",
)

@Serializable
data class CreateRoleplayGroupRequest(
    val name: String,
    val description: String = "",
)

@Serializable
data class RoleplayPresetDto(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val parameters: JsonObject,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class UpsertRoleplayPresetRequest(
    val name: String,
    val description: String = "",
    val type: String = PresetType.OPENAI.name,
    val parameters: JsonObject = JsonObject(emptyMap()),
)

fun Character.toRoleplayDto() = RoleplayCharacterDto(
    id = id.toString(),
    name = name,
    description = description,
    personality = personality,
    scenario = scenario,
    firstMessage = firstMessage,
    messageExamples = messageExamples,
    systemPrompt = systemPrompt,
    postHistoryInstructions = postHistoryInstructions,
    avatarUrl = avatarUrl,
    creator = creator,
    creatorNotes = creatorNotes,
    tags = tags,
    talkativeness = talkativeness,
    alternateGreetings = alternateGreetings,
    characterVersion = characterVersion,
    favorite = favorite,
    chatCount = chatCount,
    lastChatAt = lastChatAt?.toEpochMilli(),
    spec = spec,
    specVersion = specVersion,
    characterBook = characterBook,
    extensions = extensions,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun ChatMetadata.toRoleplayDto() = RoleplayChatDto(
    id = chatId.toString(),
    characterId = characterId.toString(),
    groupId = groupId?.toString(),
    title = title,
    userName = userName,
    characterName = characterName,
    createDate = createDate,
    tavernChatId = tavernChatId,
    messageCount = messageCount,
    pinned = pinned,
    activeBranchId = activeBranchId?.toString(),
    rootNodes = rootNodes.map { it.toString() },
    variables = variables,
    tavernMetadata = tavernMetadata,
    extensions = extensions,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun ChatMessage.toRoleplayDto() = RoleplayMessageDto(
    id = id.toString(),
    role = role.name.lowercase(),
    content = content,
    timestamp = timestamp.toEpochMilli(),
    tavernName = tavernName,
    tavernSendDate = tavernSendDate,
    model = model,
    tokenCount = tokenCount,
    swipeAlternatives = swipeAlternatives,
    speakerId = speakerId?.toString(),
    speakerName = speakerName,
    extra = extra,
)

fun Group.toRoleplayDto() = RoleplayGroupDto(
    id = id.toString(),
    name = name,
    description = description,
    members = members.map { it.toRoleplayDto() },
    activeMembers = activeMembers.map { it.toString() },
    avatarUrl = avatarUrl,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun GroupMember.toRoleplayDto() = RoleplayGroupMemberDto(
    characterId = characterId.toString(),
    name = name,
    priority = priority,
    responseProbability = responseProbability,
    forcedResponse = forcedResponse,
)

fun WorldInfo.toRoleplayDto() = RoleplayWorldInfoDto(
    id = id.toString(),
    name = name,
    description = description,
    entries = entries.map { it.toRoleplayDto() },
    scanDepth = scanDepth,
    scanTrigger = scanTrigger.name,
    selectiveLogic = selectiveLogic.name,
    extensions = extensions,
    originalData = originalData,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun WorldInfoEntry.toRoleplayDto() = RoleplayWorldInfoEntryDto(
    id = id.toString(),
    key = key,
    keys = keys,
    secondaryKeys = secondaryKeys,
    comment = comment,
    content = content,
    constant = constant,
    selective = selective,
    order = order,
    position = position.name,
    tavernPosition = tavernPosition,
    enabled = enabled,
    probability = probability,
    useProbability = useProbability,
    depth = depth,
    role = role,
    displayIndex = displayIndex,
    extensions = extensions,
)

fun Preset.toRoleplayDto() = RoleplayPresetDto(
    id = id.toString(),
    name = name,
    description = description,
    type = type.name,
    parameters = parameters.toJsonObject(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun UpsertRoleplayCharacterRequest.toCharacter(existing: Character? = null): Character {
    val now = Instant.now()
    return (existing ?: Character()).copy(
        name = name.trim(),
        description = description,
        personality = personality,
        scenario = scenario,
        firstMessage = firstMessage,
        messageExamples = messageExamples,
        systemPrompt = systemPrompt,
        postHistoryInstructions = postHistoryInstructions,
        creator = creator,
        creatorNotes = creatorNotes,
        tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
        talkativeness = talkativeness.coerceIn(0f, 1f),
        alternateGreetings = alternateGreetings,
        updatedAt = now,
        createdAt = existing?.createdAt ?: now,
    )
}

fun UpsertRoleplayPresetRequest.toPreset(existing: Preset? = null): Preset {
    val now = Instant.now()
    return (existing ?: Preset()).copy(
        name = name.trim(),
        description = description,
        type = runCatching { PresetType.valueOf(type.uppercase()) }.getOrDefault(PresetType.OPENAI),
        parameters = parameters.toPrimitiveMap(),
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
    )
}

fun String.toRoleplayMessageRole(): MessageRole {
    return when (trim().lowercase()) {
        "user" -> MessageRole.USER
        "assistant", "character" -> MessageRole.ASSISTANT
        "system" -> MessageRole.SYSTEM
        "tool" -> MessageRole.TOOL
        else -> MessageRole.USER
    }
}

private fun Map<String, Any>.toJsonObject(): JsonObject {
    return JsonObject(mapValues { (_, value) -> value.toJsonElement() })
}

private fun Any.toJsonElement(): JsonElement {
    return when (this) {
        is Boolean -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        is JsonElement -> this
        else -> JsonPrimitive(toString())
    }
}

private fun JsonObject.toPrimitiveMap(): Map<String, Any> {
    return mapValues { (_, value) ->
        when (value) {
            JsonNull -> ""
            is JsonPrimitive -> value.booleanOrNull
                ?: value.longOrNull
                ?: value.doubleOrNull
                ?: value.content
            else -> value.toString()
        }
    }
}
