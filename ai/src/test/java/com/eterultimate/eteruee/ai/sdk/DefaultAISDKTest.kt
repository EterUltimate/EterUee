package com.eterultimate.eteruee.ai.sdk

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.core.TokenUsage
import com.eterultimate.eteruee.ai.ui.MessageChunk
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessageChoice
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultAISDKTest {
    @Test
    fun `text delta chunk should not be converted to finish`() {
        val chunk = MessageChunk(
            id = "chatcmpl-1",
            model = "test-model",
            choices = listOf(
                UIMessageChoice(
                    index = 0,
                    delta = UIMessage(
                        role = MessageRole.ASSISTANT,
                        parts = listOf(UIMessagePart.Text("hello"))
                    ),
                    message = null,
                    finishReason = null
                )
            )
        )

        assertEquals(TextChunk.TextDelta("hello"), chunk.toTextChunk())
    }

    @Test
    fun `chunk with finish reason should be converted to finish`() {
        val chunk = MessageChunk(
            id = "chatcmpl-1",
            model = "test-model",
            choices = listOf(
                UIMessageChoice(
                    index = 0,
                    delta = UIMessage(
                        role = MessageRole.ASSISTANT,
                        parts = emptyList()
                    ),
                    message = null,
                    finishReason = "stop"
                )
            )
        )

        assertEquals(TextChunk.Finish, chunk.toTextChunk())
    }

    @Test
    fun `stream should emit usage before delayed finish`() = runBlocking {
        val usage = TokenUsage(
            promptTokens = 10,
            completionTokens = 5,
            totalTokens = 15,
        )
        val textChunk = MessageChunk(
            id = "chatcmpl-1",
            model = "test-model",
            choices = listOf(
                UIMessageChoice(
                    index = 0,
                    delta = UIMessage(
                        role = MessageRole.ASSISTANT,
                        parts = listOf(UIMessagePart.Text("hello"))
                    ),
                    message = null,
                    finishReason = null
                )
            )
        )
        val finishChunk = MessageChunk(
            id = "chatcmpl-1",
            model = "test-model",
            choices = listOf(
                UIMessageChoice(
                    index = 0,
                    delta = UIMessage(
                        role = MessageRole.ASSISTANT,
                        parts = emptyList()
                    ),
                    message = null,
                    finishReason = "stop"
                )
            )
        )
        val usageChunk = MessageChunk(
            id = "chatcmpl-1",
            model = "test-model",
            choices = emptyList(),
            usage = usage
        )

        assertEquals(
            listOf(
                TextChunk.TextDelta("hello"),
                TextChunk.Usage(usage),
                TextChunk.Finish,
            ),
            flowOf(textChunk, finishChunk, usageChunk).toTextChunkFlow().toList()
        )
    }
}
