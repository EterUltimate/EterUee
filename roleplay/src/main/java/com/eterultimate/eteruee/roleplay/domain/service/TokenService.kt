package com.eterultimate.eteruee.roleplay.domain.service

/**
 * Token 计数服务
 * 用于估算和统计消息的 Token 数量
 */
interface TokenService {
    
    /**
     * 估算文本的 Token 数量
     * @param text 输入文本
     * @return 估算的 Token 数量
     */
    fun estimateTokens(text: String): Int
    
    /**
     * 计算多条消息的总 Token 数
     * @param messages 消息列表
     * @return 总 Token 数
     */
    fun calculateTotalTokens(messages: List<String>): Int
    
    /**
     * 检查消息是否超过 Token 限制
     * @param text 输入文本
     * @param maxTokens 最大 Token 限制
     * @return 是否超出限制
     */
    fun isExceedingLimit(text: String, maxTokens: Int): Boolean
    
    /**
     * 截断文本到指定 Token 数
     * @param text 输入文本
     * @param maxTokens 最大 Token 数
     * @return 截断后的文本
     */
    suspend fun truncateToTokens(text: String, maxTokens: Int): String
}
