package com.eterultimate.eteruee.data.model

import org.junit.Test
import org.junit.Assert.*
import kotlin.uuid.Uuid
import com.eterultimate.eteruee.ai.provider.CustomHeader
import com.eterultimate.eteruee.ai.provider.CustomBody
import com.eterultimate.eteruee.ai.ui.UIMessage

/**
 * Assistant 数据模型单元测试
 * 
 * 测试覆盖：
 * - 默认值初始化
 * - 自定义配置
 * - 字段验证
 */
class AssistantTest {

    @Test
    fun testAssistantDefaultValues() {
        val assistant = Assistant()
        
        assertEquals("", assistant.name)
        assertEquals(Avatar.Dummy, assistant.avatar)
        assertEquals(false, assistant.useAssistantAvatar)
        assertEquals(emptyList<Uuid>(), assistant.tags)
        assertEquals("", assistant.systemPrompt)
        assertEquals(null, assistant.temperature)
        assertEquals(null, assistant.topP)
        assertEquals(0, assistant.contextMessageSize)
        assertEquals(true, assistant.streamOutput)
        assertEquals(false, assistant.enableMemory)
        assertEquals(false, assistant.useGlobalMemory)
        assertEquals("{{ message }}", assistant.messageTemplate)
        assertEquals(emptyList<UIMessage>(), assistant.presetMessages)
        assertEquals(emptySet<Uuid>(), assistant.quickMessageIds)
        assertEquals(emptyList<AssistantRegex>(), assistant.regexes)
        assertEquals(emptyList<CustomHeader>(), assistant.customHeaders)
        assertEquals(emptyList<CustomBody>(), assistant.customBodies)
        assertEquals(emptySet<Uuid>(), assistant.mcpServers)
        assertEquals(emptySet<Uuid>(), assistant.modeInjectionIds)
        assertEquals(emptySet<Uuid>(), assistant.lorebookIds)
        assertEquals(emptySet<String>(), assistant.enabledSkills)
        assertEquals(false, assistant.enableTimeReminder)
    }

    @Test
    fun testAssistantCustomValues() {
        val id = Uuid.random()
        val chatModelId = Uuid.random()
        val tag1 = Uuid.random()
        val tag2 = Uuid.random()
        
        val assistant = Assistant(
            id = id,
            chatModelId = chatModelId,
            name = "Test Assistant",
            avatar = Avatar.Emoji("🤖"),
            useAssistantAvatar = true,
            tags = listOf(tag1, tag2),
            systemPrompt = "You are a helpful assistant",
            temperature = 0.7f,
            topP = 0.9f,
            contextMessageSize = 10,
            streamOutput = false,
            enableMemory = true,
            useGlobalMemory = true,
            enableRecentChatsReference = true,
            messageTemplate = "{{ time }}\n{{ message }}",
            reasoningLevel = com.eterultimate.eteruee.ai.core.ReasoningLevel.HIGH,
            maxTokens = 2048,
            background = "gradient_background",
            backgroundOpacity = 0.8f,
            enabledSkills = setOf("web-search", "code-execution"),
            enableTimeReminder = true
        )
        
        assertEquals(id, assistant.id)
        assertEquals(chatModelId, assistant.chatModelId)
        assertEquals("Test Assistant", assistant.name)
        assertEquals(Avatar.Emoji("🤖"), assistant.avatar)
        assertEquals(true, assistant.useAssistantAvatar)
        assertEquals(2, assistant.tags.size)
        assertEquals("You are a helpful assistant", assistant.systemPrompt)
        assertEquals(0.7f, assistant.temperature)
        assertEquals(0.9f, assistant.topP)
        assertEquals(10, assistant.contextMessageSize)
        assertEquals(false, assistant.streamOutput)
        assertEquals(true, assistant.enableMemory)
        assertEquals(true, assistant.useGlobalMemory)
        assertEquals(true, assistant.enableRecentChatsReference)
        assertEquals("{{ time }}\n{{ message }}", assistant.messageTemplate)
        assertEquals(com.eterultimate.eteruee.ai.core.ReasoningLevel.HIGH, assistant.reasoningLevel)
        assertEquals(2048, assistant.maxTokens)
        assertEquals("gradient_background", assistant.background)
        assertEquals(0.8f, assistant.backgroundOpacity)
        assertEquals(setOf("web-search", "code-execution"), assistant.enabledSkills)
        assertEquals(true, assistant.enableTimeReminder)
    }

    @Test
    fun testAssistantWithRegexes() {
        val id1 = Uuid.random()
        val id2 = Uuid.random()
        val regex1 = AssistantRegex(
            id = id1,
            name = "Test Regex",
            findRegex = "test.*pattern",
            replaceString = "replacement"
        )
        val regex2 = AssistantRegex(
            id = id2,
            name = "Another Regex",
            findRegex = "another.*regex",
            replaceString = "another_replacement"
        )
        
        val assistant = Assistant(
            regexes = listOf(regex1, regex2)
        )
        
        assertEquals(2, assistant.regexes.size)
        assertEquals("test.*pattern", assistant.regexes[0].findRegex)
        assertEquals("replacement", assistant.regexes[0].replaceString)
    }

    @Test
    fun testAssistantWithLocalTools() {
        val assistant = Assistant(
            localTools = listOf(
                com.eterultimate.eteruee.data.ai.tools.LocalToolOption.TimeInfo,
                com.eterultimate.eteruee.data.ai.tools.LocalToolOption.JavascriptEngine
            )
        )
        
        assertEquals(2, assistant.localTools.size)
        assertTrue(assistant.localTools.contains(com.eterultimate.eteruee.data.ai.tools.LocalToolOption.TimeInfo))
    }

    @Test
    fun testAssistantCopyWithModification() {
        val original = Assistant(
            name = "Original",
            temperature = 0.5f
        )
        
        val modified = original.copy(
            name = "Modified",
            temperature = 0.8f,
            enableMemory = true
        )
        
        // 验证修改的字段
        assertEquals("Modified", modified.name)
        assertEquals(0.8f, modified.temperature)
        assertEquals(true, modified.enableMemory)
        
        // 验证未修改的字段保持不变
        assertEquals(original.id, modified.id)
        assertEquals(original.systemPrompt, modified.systemPrompt)
        assertEquals(original.streamOutput, modified.streamOutput)
    }

    @Test
    fun testAssistantEquality() {
        val id = Uuid.random()
        val assistant1 = Assistant(id = id, name = "Test")
        val assistant2 = Assistant(id = id, name = "Test")
        
        // data class 应该基于所有字段进行相等性比较
        assertEquals(assistant1, assistant2)
    }
}
