package com.eterultimate.eteruee.roleplay.domain.service

import android.content.Context
import java.io.File
import java.time.Instant

/**
 * 备份服务接口
 */
interface BackupService {
    /**
     * 导出聊天为 JSON 格式
     */
    suspend fun exportChatAsJson(
        context: Context,
        chatId: kotlin.uuid.Uuid,
        characterName: String
    ): Result<File>
    
    /**
     * 导出聊天为 TXT 格式
     */
    suspend fun exportChatAsTxt(
        context: Context,
        chatId: kotlin.uuid.Uuid,
        characterName: String
    ): Result<File>
    
    /**
     * 导出聊天为 HTML 格式
     */
    suspend fun exportChatAsHtml(
        context: Context,
        chatId: kotlin.uuid.Uuid,
        characterName: String
    ): Result<File>
    
    /**
     * 导出角色数据（包含所有聊天）
     */
    suspend fun exportCharacterData(
        context: Context,
        characterId: kotlin.uuid.Uuid,
        characterName: String
    ): Result<File>
    
    /**
     * 获取导出的文件名
     */
    fun getExportFileName(characterName: String, format: ExportFormat): String
    
    /**
     * 导出格式枚举
     */
    enum class ExportFormat {
        JSON, TXT, HTML
    }
}
