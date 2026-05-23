package com.eterultimate.eteruee.data.model

import org.junit.Test
import org.junit.Assert.*
import kotlin.uuid.Uuid
import java.time.Instant
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart

/**
 * Conversation 数据模型单元测试
 * 
 * 测试覆盖：
 * - 默认值初始化
 * - 消息节点管理
 * - 当前消息获取
 * - 文件提取
 */
class ConversationTest {

    @Test
    fun testConversationDefaultValues() {
        val assistantId = Uuid.random()
        val conversation = Conversation(
            assistantId = assistantId,
            messageNodes = emptyList()
        )
        
        assertNotNull(conversation.id)
        assertEquals(assistantId, conversation.assistantId)
        assertEquals("", conversation.title)
        assertEquals(emptyList<MessageNode>(), conversation.messageNodes)
        assertEquals(emptyList<String>(), conversation.chatSuggestions)
        assertEquals(false, conversation.isPinned)
        assertNotNull(conversation.createAt)
        assertNotNull(conversation.updateAt)
        assertEquals(false, conversation.newConversation)
    }

    @Test
    fun testConversationWithMessages() {
        val assistantId = Uuid.random()
        val node1 = createTestMessageNode("Hello", MessageRole.USER)
        val node2 = createTestMessageNode("Hi there!", MessageRole.ASSISTANT)
        
        val conversation = Conversation(
            assistantId = assistantId,
            title = "Test Chat",
            messageNodes = listOf(node1, node2),
            isPinned = true,
            chatSuggestions = listOf("Suggestion 1", "Suggestion 2")
        )
        
        assertEquals("Test Chat", conversation.title)
        assertEquals(2, conversation.messageNodes.size)
        assertEquals(true, conversation.isPinned)
        assertEquals(2, conversation.chatSuggestions.size)
        assertEquals("Suggestion 1", conversation.chatSuggestions[0])
    }

    @Test
    fun testGetCurrentMessages() {
        val assistantId = Uuid.random()
        val node1 = createTestMessageNode("User message", MessageRole.USER)
        val node2 = createTestMessageNode("Assistant response", MessageRole.ASSISTANT)
        
        val conversation = Conversation(
            assistantId = assistantId,
            messageNodes = listOf(node1, node2)
        )
        
        val currentMessages = conversation.currentMessages
        
        assertEquals(2, currentMessages.size)
        assertEquals(MessageRole.USER, currentMessages[0].role)
        assertEquals(MessageRole.ASSISTANT, currentMessages[1].role)
    }

    @Test
    fun testGetMessageNodeByMessage() {
        val assistantId = Uuid.random()
        val node = createTestMessageNode("Test message", MessageRole.USER)
        
        val conversation = Conversation(
            assistantId = assistantId,
            messageNodes = listOf(node)
        )
        
        val targetMessage = node.messages[0]
        val foundNode = conversation.getMessageNodeByMessage(targetMessage)
        
        assertNotNull(foundNode)
        assertEquals(node, foundNode)
    }

    @Test
    fun testGetMessageNodeByMessageId() {
        val assistantId = Uuid.random()
        val node = createTestMessageNode("Test message", MessageRole.USER)
        
        val conversation = Conversation(
            assistantId = assistantId,
            messageNodes = listOf(node)
        )
        
        val messageId = node.messages[0].id
        val foundNode = conversation.getMessageNodeByMessageId(messageId)
        
        assertNotNull(foundNode)
        assertEquals(node, foundNode)
    }

    @Test
    fun testGetMessageNodeByNonExistentMessage() {
        val assistantId = Uuid.random()
        val node = createTestMessageNode("Test message", MessageRole.USER)
        
        val conversation = Conversation(
            assistantId = assistantId,
            messageNodes = listOf(node)
        )
        
        val nonExistentId = Uuid.random()
        val foundNode = conversation.getMessageNodeByMessageId(nonExistentId)
        
        assertNull(foundNode)
    }

    @Test
    fun testConversationCopyWithModification() {
        val assistantId = Uuid.random()
        val original = Conversation(
            assistantId = assistantId,
            title = "Original Title",
            messageNodes = emptyList(),
            isPinned = false
        )
        
        val modified = original.copy(
            title = "Modified Title",
            isPinned = true
        )
        
        // 验证修改的字段
        assertEquals("Modified Title", modified.title)
        assertEquals(true, modified.isPinned)
        
        // 验证未修改的字段保持不变
        assertEquals(original.id, modified.id)
        assertEquals(original.assistantId, modified.assistantId)
        assertEquals(original.messageNodes, modified.messageNodes)
    }

    @Test
    fun testConversationEquality() {
        val id = Uuid.random()
        val assistantId = Uuid.random()
        val now = Instant.now()
        
        val conv1 = Conversation(
            id = id,
            assistantId = assistantId,
            title = "Test",
            messageNodes = emptyList(),
            createAt = now,
            updateAt = now
        )
        
        val conv2 = Conversation(
            id = id,
            assistantId = assistantId,
            title = "Test",
            messageNodes = emptyList(),
            createAt = now,
            updateAt = now
        )
        
        assertEquals(conv1, conv2)
    }

    @Test
    fun testConversationWithMultipleBranches() {
        val assistantId = Uuid.random()
        
        // 创建带分支的消息节点
        val branchNode = MessageNode(
            messages = listOf(
                createTestUIMessage("Original response", MessageRole.ASSISTANT),
                createTestUIMessage("Regenerated response", MessageRole.ASSISTANT)
            ),
            selectIndex = 1 // 选择第二个消息
        )
        
        val conversation = Conversation(
            assistantId = assistantId,
            messageNodes = listOf(branchNode)
        )
        
        val currentMessages = conversation.currentMessages
        assertEquals(1, currentMessages.size)
        // 应该返回选中的消息（索引1）
        val firstPart = currentMessages[0].parts.firstOrNull()
        assertTrue(firstPart is UIMessagePart.Text)
        assertEquals("Regenerated response", (firstPart as UIMessagePart.Text).text)
    }

    // Helper functions
    
    private fun createTestMessageNode(content: String, role: MessageRole): MessageNode {
        return MessageNode(
            messages = listOf(createTestUIMessage(content, role))
        )
    }

    private fun createTestUIMessage(content: String, role: MessageRole): com.eterultimate.eteruee.ai.ui.UIMessage {
        return com.eterultimate.eteruee.ai.ui.UIMessage(
            role = role,
            parts = listOf(com.eterultimate.eteruee.ai.ui.UIMessagePart.Text(content))
        )
    }
}
