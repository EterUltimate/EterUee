package me.rerere.ai.ui

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import me.rerere.ai.core.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTest {

    @Test
    fun `limitContext with size 0 should return original list`() {
        val messages = createTestMessages(5)
        val result = messages.limitContext(0)
        assertEquals(messages, result)
    }

    @Test
    fun `limitContext with negative size should return original list`() {
        val messages = createTestMessages(5)
        val result = messages.limitContext(-1)
        assertEquals(messages, result)
    }

    @Test
    fun `limitContext with size greater than list size should return original list`() {
        val messages = createTestMessages(3)
        val result = messages.limitContext(5)
        assertEquals(messages, result)
    }

    @Test
    fun `limitContext with normal size should return last N messages`() {
        val messages = createTestMessages(5)
        val result = messages.limitContext(3)
        assertEquals(3, result.size)
        assertEquals(messages.subList(2, 5), result)
    }

    @Test
    fun `limitContext should include tool call when first message has tool result`() {
        val messages = mutableListOf<UIMessage>()

        // Message 0: User message
        messages.add(UIMessage(
            role = MessageRole.USER,
            parts = listOf(UIMessagePart.Text("Hello"))
        ))

        // Message 1: Assistant with tool call
        messages.add(UIMessage(
            role = MessageRole.ASSISTANT,
            parts = listOf(UIMessagePart.ToolCall("call_1", "search", "{}"))
        ))

        // Message 2: Tool result
        messages.add(UIMessage(
            role = MessageRole.TOOL,
            parts = listOf(UIMessagePart.ToolResult("call_1", "search", JsonPrimitive("result"), JsonPrimitive("{}")))
        ))

        // Message 3: Assistant response
        messages.add(UIMessage(
            role = MessageRole.ASSISTANT,
            parts = listOf(UIMessagePart.Text("Based on the search result..."))
        ))

        // Message 4: User message
        messages.add(UIMessage(
            role = MessageRole.USER,
            parts = listOf(UIMessagePart.Text("Thanks"))
        ))

        // Limit to 2 messages, but should include the tool call message
        val result = messages.limitContext(3)

        // Should include message 1 (tool call), 2 (tool result), 3 (assistant), 4 (user)
        assertEquals(4, result.size)
        assertEquals(messages.subList(1, 5), result)
    }

    @Test
    fun `limitContext without tool result should work normally`() {
        val messages = mutableListOf<UIMessage>()

        // Message 0: User message
        messages.add(UIMessage(
            role = MessageRole.USER,
            parts = listOf(UIMessagePart.Text("Hello 1"))
        ))

        // Message 1: Assistant response
        messages.add(UIMessage(
            role = MessageRole.ASSISTANT,
            parts = listOf(UIMessagePart.Text("Response 1"))
        ))

        // Message 2: User message
        messages.add(UIMessage(
            role = MessageRole.USER,
            parts = listOf(UIMessagePart.Text("Hello 2"))
        ))

        // Message 3: Assistant response
        messages.add(UIMessage(
            role = MessageRole.ASSISTANT,
            parts = listOf(UIMessagePart.Text("Response 2"))
        ))

        val result = messages.limitContext(2)

        assertEquals(2, result.size)
        assertEquals(messages.subList(2, 4), result)
    }

    @Test
    fun `limitContext should handle multiple tool calls correctly`() {
        val messages = mutableListOf<UIMessage>()

        // Message 0: User message
        messages.add(UIMessage(
            role = MessageRole.USER,
            parts = listOf(UIMessagePart.Text("Hello"))
        ))

        // Message 1: Assistant with multiple tool calls
        messages.add(UIMessage(
            role = MessageRole.ASSISTANT,
            parts = listOf(
                UIMessagePart.ToolCall("call_1", "search", "{}"),
                UIMessagePart.ToolCall("call_2", "calculate", "{}")
            )
        ))

        // Message 2: Tool results
        messages.add(UIMessage(
            role = MessageRole.TOOL,
            parts = listOf(
                UIMessagePart.ToolResult("call_1", "search", JsonPrimitive("result1"), JsonPrimitive("{}")),
                UIMessagePart.ToolResult("call_2", "calculate", JsonPrimitive("result2"), JsonPrimitive("{}"))
            )
        ))

        // Message 3: Final response
        messages.add(UIMessage(
            role = MessageRole.ASSISTANT,
            parts = listOf(UIMessagePart.Text("Final answer"))
        ))

        val result = messages.limitContext(2)

        // Should include the tool call message, tool result, and final response
        assertEquals(3, result.size)
        assertEquals(messages.subList(1, 4), result)
    }

    private fun createTestMessages(count: Int): List<UIMessage> {
        return (0 until count).map { i ->
            UIMessage(
                role = if (i % 2 == 0) MessageRole.USER else MessageRole.ASSISTANT,
                parts = listOf(UIMessagePart.Text("Message $i"))
            )
        }
    }
}
