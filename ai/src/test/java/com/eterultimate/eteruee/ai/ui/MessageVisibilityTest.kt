package com.eterultimate.eteruee.ai.ui

import com.eterultimate.eteruee.ai.core.MessageRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageVisibilityTest {
    @Test
    fun `client tool is visible UI content`() {
        val parts = listOf(
            UIMessagePart.Tool(
                toolCallId = "call_1",
                toolName = "ask_user",
                input = "{}",
            )
        )

        assertFalse(parts.isEmptyUIMessage())
    }

    @Test
    fun `empty tool-only message is treated as empty`() {
        val parts = listOf(
            UIMessagePart.Tool(
                toolCallId = "call_1",
                toolName = "ask_user",
                input = "{}",
            )
        )

        assertFalse(UIMessage(role = MessageRole.ASSISTANT, parts = parts, modelId = null).parts.isEmptyUIMessage())
    }
}
