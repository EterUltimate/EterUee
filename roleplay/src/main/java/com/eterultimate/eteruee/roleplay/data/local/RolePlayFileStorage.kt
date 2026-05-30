package com.eterultimate.eteruee.roleplay.data.local

import android.content.Context
import android.net.Uri
import com.eterultimate.eteruee.roleplay.data.model.*
import com.eterultimate.eteruee.roleplay.data.serialization.RoleplayJson
import com.eterultimate.eteruee.roleplay.data.tavern.TavernChatCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * RolePlay 模块文件存储管理器
 * 
 * 目录结构:
 * /data/user/0/com.eterultimate.eteruee/files/roleplay/
 * ├── characters/
 * │   ├── {character_id}/
 * │   │   ├── character.json
 * │   │   └── avatar.png
 * ├── chats/
 * │   ├── {character_id}/
 * │   │   ├── {chat_id}.jsonl
 * │   │   └── branches/{chat_id}_{branch_id}.jsonl
 * │   └── groups/
 * │       └── {group_id}/
 * │           ├── {chat_id}.jsonl
 * │           └── branches/{chat_id}_{branch_id}.jsonl
 * ├── worldinfos/
 * │   └── {worldinfo_id}.json
 * ├── groups/
 * │   └── {group_id}/
 * │       ├── group.json
 * │       └── avatars/
 * └── backups/
 *     ├── characters/
 *     └── chats/
 */
class RolePlayFileStorage(private val context: Context) {
    private val baseDir = context.filesDir.resolve("roleplay").apply { mkdirs() }
    
    // JSON 序列化器
    private val json = RoleplayJson
    
    // ==================== 角色卡文件操作 ====================
    
    /**
     * 获取角色目录
     */
    fun getCharacterDir(characterId: kotlin.uuid.Uuid): File {
        return baseDir.resolve("characters").resolve(characterId.toString()).apply { mkdirs() }
    }
    
    /**
     * 保存角色卡JSON
     */
    suspend fun saveCharacterJson(character: Character, avatarPath: String?) = withContext(Dispatchers.IO) {
        val dir = getCharacterDir(character.id)
        val jsonFile = dir.resolve("character.json")
        
        // 更新avatarUrl为本地路径
        val characterWithLocalAvatar = if (avatarPath != null) {
            character.copy(avatarUrl = avatarPath)
        } else {
            character
        }
        
        jsonFile.writeText(json.encodeToString(characterWithLocalAvatar))
    }
    
    /**
     * 加载角色卡JSON
     */
    suspend fun loadCharacterJson(characterId: kotlin.uuid.Uuid): Character? = withContext(Dispatchers.IO) {
        val jsonFile = getCharacterDir(characterId).resolve("character.json")
        return@withContext if (jsonFile.exists()) {
            try {
                json.decodeFromString<Character>(jsonFile.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }
    
    /**
     * 保存角色头像
     */
    suspend fun saveCharacterAvatar(characterId: kotlin.uuid.Uuid, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val dir = getCharacterDir(characterId)
            val avatarFile = dir.resolve("avatar.png")
            
            context.contentResolver.openInputStream(uri)?.use { input ->
                avatarFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            avatarFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveCharacterAvatarBytes(characterId: kotlin.uuid.Uuid, bytes: ByteArray): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = getCharacterDir(characterId)
                val avatarFile = dir.resolve("avatar.png")
                avatarFile.writeBytes(bytes)
                avatarFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    
    /**
     * 删除角色目录
     */
    suspend fun deleteCharacterDir(characterId: kotlin.uuid.Uuid) = withContext(Dispatchers.IO) {
        getCharacterDir(characterId).deleteRecursively()
    }
    
    // ==================== 聊天文件操作 ====================
    
    /**
     * 获取角色聊天目录
     */
    fun getCharacterChatDir(characterId: kotlin.uuid.Uuid): File {
        return baseDir.resolve("chats").resolve(characterId.toString()).apply { mkdirs() }
    }
    
    /**
     * 获取群组聊天目录
     */
    fun getGroupChatDir(groupId: kotlin.uuid.Uuid): File {
        return baseDir.resolve("chats").resolve("groups").resolve(groupId.toString()).apply { mkdirs() }
    }
    
    /**
     * 获取聊天JSONL文件
     */
    fun getChatFile(characterId: kotlin.uuid.Uuid, chatId: kotlin.uuid.Uuid): File {
        val dir = getCharacterChatDir(characterId)
        return dir.resolve("${chatId}.jsonl")
    }

    /**
     * 获取角色聊天分支 JSONL 文件
     */
    fun getChatBranchFile(
        characterId: kotlin.uuid.Uuid,
        chatId: kotlin.uuid.Uuid,
        branchId: kotlin.uuid.Uuid
    ): File {
        val dir = getCharacterChatDir(characterId).resolve("branches").apply { mkdirs() }
        return dir.resolve("${chatId}_${branchId}.jsonl")
    }
    
    /**
     * 获取群组聊天JSONL文件
     */
    fun getGroupChatFile(groupId: kotlin.uuid.Uuid, chatId: kotlin.uuid.Uuid): File {
        val dir = getGroupChatDir(groupId)
        return dir.resolve("${chatId}.jsonl")
    }

    /**
     * 获取群组聊天分支 JSONL 文件
     */
    fun getGroupChatBranchFile(
        groupId: kotlin.uuid.Uuid,
        chatId: kotlin.uuid.Uuid,
        branchId: kotlin.uuid.Uuid
    ): File {
        val dir = getGroupChatDir(groupId).resolve("branches").apply { mkdirs() }
        return dir.resolve("${chatId}_${branchId}.jsonl")
    }

    /**
     * 删除聊天的所有分支文件
     */
    suspend fun deleteChatBranchFiles(chatFile: File, chatId: kotlin.uuid.Uuid) = withContext(Dispatchers.IO) {
        chatFile.parentFile
            ?.resolve("branches")
            ?.listFiles { file -> file.name.startsWith("${chatId}_") && file.name.endsWith(".jsonl") }
            ?.forEach { it.delete() }
    }
    
    /**
     * 追加消息到JSONL文件
     */
    suspend fun appendMessageToFile(file: File, message: ChatMessage) = withContext(Dispatchers.IO) {
        FileWriter(file, true).use { writer ->
            writer.write(json.encodeToString(message))
            writer.write("\n")
        }
    }
    
    /**
     * 流式读取JSONL文件(窗口加载)
     */
    suspend fun loadMessagesWindowed(
        file: File,
        offset: Int = 0,
        limit: Int = 50
    ): List<ChatMessage> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        
        try {
            BufferedReader(FileReader(file)).useLines { lines ->
                lines.drop(offset)
                    .take(limit)
                    .filter { it.isNotBlank() }
                    .mapNotNull(::decodeJsonlMessageLine)
                    .toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 加载所有消息从JSONL文件
     */
    suspend fun loadMessagesFromJsonl(file: File): List<ChatMessage> = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext emptyList()
        
        try {
            BufferedReader(FileReader(file)).useLines { lines ->
                lines.filter { it.isNotBlank() }
                    .mapNotNull(::decodeJsonlMessageLine)
                    .toList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 保存消息列表到JSONL文件(覆盖写入)
     */
    suspend fun saveMessagesToJsonl(file: File, messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        FileWriter(file).use { writer ->
            messages.forEach { message ->
                writer.write(json.encodeToString(message))
                writer.write("\n")
            }
        }
    }
    
    /**
     * 获取JSONL文件行数
     */
    suspend fun getChatLineCount(file: File): Int = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext 0
        
        try {
            BufferedReader(FileReader(file)).useLines { lines ->
                lines.count { it.isNotBlank() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    private fun decodeJsonlMessageLine(line: String): ChatMessage? {
        return runCatching { json.decodeFromString<ChatMessage>(line) }
            .recoverCatching {
                val root = json.parseToJsonElement(line).jsonObject
                if (root.isTavernChatHeader()) {
                    null
                } else {
                    TavernChatCodec.decodeMessage(root)
                }
            }
            .getOrNull()
    }

    private fun JsonObject.isTavernChatHeader(): Boolean {
        return containsKey("user_name") || containsKey("chat_metadata")
    }
    
    /**
     * 删除聊天文件
     */
    suspend fun deleteChatFile(file: File) = withContext(Dispatchers.IO) {
        file.delete()
    }
    
    // ==================== 世界书文件操作 ====================
    
    /**
     * 获取世界书目录
     */
    fun getWorldInfoDir(): File {
        return baseDir.resolve("worldinfos").apply { mkdirs() }
    }
    
    /**
     * 保存世界书JSON
     */
    suspend fun saveWorldInfoJson(worldInfo: WorldInfo) = withContext(Dispatchers.IO) {
        val dir = getWorldInfoDir()
        val jsonFile = dir.resolve("${worldInfo.id}.json")
        jsonFile.writeText(json.encodeToString(worldInfo))
    }
    
    /**
     * 加载世界书JSON
     */
    suspend fun loadWorldInfoJson(worldInfoId: kotlin.uuid.Uuid): WorldInfo? = withContext(Dispatchers.IO) {
        val jsonFile = getWorldInfoDir().resolve("${worldInfoId}.json")
        return@withContext if (jsonFile.exists()) {
            try {
                json.decodeFromString<WorldInfo>(jsonFile.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }
    
    /**
     * 删除世界书文件
     */
    suspend fun deleteWorldInfoFile(worldInfoId: kotlin.uuid.Uuid) = withContext(Dispatchers.IO) {
        getWorldInfoDir().resolve("${worldInfoId}.json").delete()
    }
    
    // ==================== 群组文件操作 ====================
    
    /**
     * 获取群组目录
     */
    fun getGroupDir(groupId: kotlin.uuid.Uuid): File {
        return baseDir.resolve("groups").resolve(groupId.toString()).apply { mkdirs() }
    }
    
    /**
     * 保存群组JSON
     */
    suspend fun saveGroupJson(group: Group) = withContext(Dispatchers.IO) {
        val dir = getGroupDir(group.id)
        val jsonFile = dir.resolve("group.json")
        jsonFile.writeText(json.encodeToString(group))
    }
    
    /**
     * 加载群组JSON
     */
    suspend fun loadGroupJson(groupId: kotlin.uuid.Uuid): Group? = withContext(Dispatchers.IO) {
        val jsonFile = getGroupDir(groupId).resolve("group.json")
        return@withContext if (jsonFile.exists()) {
            try {
                json.decodeFromString<Group>(jsonFile.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else null
    }
    
    /**
     * 删除群组目录
     */
    suspend fun deleteGroupDir(groupId: kotlin.uuid.Uuid) = withContext(Dispatchers.IO) {
        getGroupDir(groupId).deleteRecursively()
    }
    
    // ==================== 备份操作 ====================
    
    /**
     * 获取备份目录
     */
    fun getBackupDir(type: String): File {
        return baseDir.resolve("backups").resolve(type).apply { mkdirs() }
    }
    
    /**
     * 备份聊天文件
     */
    suspend fun backupChatFile(sourceFile: File, chatTitle: String): File? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        
        try {
            val backupDir = getBackupDir("chats")
            val timestamp = System.currentTimeMillis()
            val sanitizedName = chatTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val backupFile = backupDir.resolve("${sanitizedName}_${timestamp}.jsonl")
            
            sourceFile.copyTo(backupFile, overwrite = true)
            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
