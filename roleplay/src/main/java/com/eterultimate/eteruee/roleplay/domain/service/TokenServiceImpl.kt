package com.eterultimate.eteruee.roleplay.domain.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TokenService 默认实现
 * 
 * 使用启发式方法估算 Token 数量：
 * - 英文：约 4 个字符 = 1 token
 * - 中文/日文：约 1-2 个字符 = 1 token
 * - 混合文本：根据语言比例动态计算
 */
class TokenServiceImpl : TokenService {
    
    companion object {
        // 平均每个 token 的字符数（英文）
        private const val AVG_CHARS_PER_TOKEN_EN = 4.0
        
        // 平均每个 token 的字符数（中文/日文）
        private const val AVG_CHARS_PER_TOKEN_CN = 1.5
        
        // 中英文分界阈值（CJK 字符 Unicode 范围）
        private val CJK_REGEX = Regex("[\\u4e00-\\u9fff\\u3040-\\u30ff\\u3400-\\u4dbf\\uf900-\\ufaff]")
    }
    
    override fun estimateTokens(text: String): Int {
        if (text.isEmpty()) return 0
        
        // 统计 CJK 字符和非 CJK 字符的数量
        var cjkCount = 0
        var otherCount = 0
        
        for (char in text) {
            if (CJK_REGEX.matches(char.toString())) {
                cjkCount++
            } else {
                otherCount++
            }
        }
        
        // 分别计算两种语言的 token 数
        val cjkTokens = (cjkCount / AVG_CHARS_PER_TOKEN_CN).toInt()
        val otherTokens = (otherCount / AVG_CHARS_PER_TOKEN_EN).toInt()
        
        // 加上标点符号和空格的额外开销
        val punctuationCount = text.count { it.isWhitespace() || it in "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~" }
        val punctuationTokens = (punctuationCount / 2).toInt()
        
        return cjkTokens + otherTokens + punctuationTokens + 1 // +1 作为安全余量
    }
    
    override fun calculateTotalTokens(messages: List<String>): Int {
        return messages.sumOf { estimateTokens(it) }
    }
    
    override fun isExceedingLimit(text: String, maxTokens: Int): Boolean {
        return estimateTokens(text) > maxTokens
    }
    
    override suspend fun truncateToTokens(text: String, maxTokens: Int): String {
        return withContext(Dispatchers.Default) {
            if (maxTokens <= 0 || text.isEmpty()) {
                return@withContext ""
            }
            
            val estimatedTokens = estimateTokens(text)
            if (estimatedTokens <= maxTokens) {
                return@withContext text
            }
            
            // 二分查找合适的截断点
            var low = 0
            var high = text.length
            var result = ""
            
            while (low <= high) {
                val mid = (low + high) / 2
                val truncated = text.substring(0, mid)
                val tokens = estimateTokens(truncated)
                
                if (tokens <= maxTokens) {
                    result = truncated
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            
            // 确保不在单词中间截断（尝试找到最近的空格或标点）
            val lastSpace = result.lastIndexOf(' ')
            val punctuations = listOf('.', ',', '!', '?', ';', ':')
            val lastPunct = punctuations.map { result.lastIndexOf(it) }.maxOrNull() ?: -1
            val cutPoint = maxOf(lastSpace, lastPunct)
            
            if (cutPoint > result.length / 2) {
                result.substring(0, cutPoint + 1)
            } else {
                result
            }
        }
    }
}
