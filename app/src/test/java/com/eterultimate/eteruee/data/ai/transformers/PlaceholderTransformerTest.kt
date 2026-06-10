package com.eterultimate.eteruee.data.ai.transformers

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceholderTransformerTest {
    private val placeholders = mapOf(
        "cur_datetime" to PlaceholderInfo(
            displayName = {},
            cachePolicy = PlaceholderCachePolicy.RUNTIME,
            resolver = { error("not used by pure test") }
        ),
        "battery_level" to PlaceholderInfo(
            displayName = {},
            cachePolicy = PlaceholderCachePolicy.RUNTIME,
            resolver = { error("not used by pure test") }
        ),
        "model_name" to PlaceholderInfo(
            displayName = {},
            resolver = { error("not used by pure test") }
        )
    )

    @Test
    fun `runtime system placeholders should ride tail context`() {
        val messages = listOf(
            UIMessage.system("Model {{model_name}}\nTime {{cur_datetime}}\nBattery {battery_level}"),
            UIMessage.user("Hello")
        )

        val result = applyPlaceholdersForCache(
            messages = messages,
            placeholders = placeholders,
            resolvePlaceholder = { key, _ ->
                when (key) {
                    "cur_datetime" -> "Jun 11, 2026, 10:30:00 AM"
                    "battery_level" -> "88"
                    "model_name" -> "DeepSeek"
                    else -> error("unexpected key $key")
                }
            }
        )

        assertEquals(3, result.size)
        val systemText = result[0].toText()
        assertEquals(MessageRole.SYSTEM, result[0].role)
        assertTrue(systemText.contains("Model DeepSeek"))
        assertTrue(systemText.contains("<runtime_context>cur_datetime</runtime_context>"))
        assertTrue(systemText.contains("<runtime_context>battery_level</runtime_context>"))
        assertFalse(systemText.contains("Jun 11, 2026"))
        assertFalse(systemText.contains("88"))

        val runtimeText = result[1].toText()
        assertEquals(MessageRole.USER, result[1].role)
        assertTrue(runtimeText.contains("- battery_level: 88"))
        assertTrue(runtimeText.contains("- cur_datetime: Jun 11, 2026, 10:30:00 AM"))
        assertEquals("Hello", result[2].toText())
    }

    @Test
    fun `runtime user placeholders should resolve in place`() {
        val messages = listOf(UIMessage.user("Current time: {{cur_datetime}}"))

        val result = applyPlaceholdersForCache(
            messages = messages,
            placeholders = placeholders,
            resolvePlaceholder = { key, _ ->
                when (key) {
                    "cur_datetime" -> "Jun 11, 2026, 10:30:00 AM"
                    else -> error("unexpected key $key")
                }
            }
        )

        assertEquals(1, result.size)
        assertEquals("Current time: Jun 11, 2026, 10:30:00 AM", result[0].toText())
    }

    @Test
    fun `runtime context should not split user tool-call adjacency`() {
        val messages = listOf(
            UIMessage.system("Time {{cur_datetime}}"),
            UIMessage.user("Call a tool"),
            UIMessage(
                role = MessageRole.ASSISTANT,
                parts = listOf(
                    UIMessagePart.Tool(
                        toolCallId = "call_1",
                        toolName = "search_web",
                        input = "{}",
                        output = emptyList()
                    )
                )
            )
        )

        val result = applyPlaceholdersForCache(
            messages = messages,
            placeholders = placeholders,
            resolvePlaceholder = { key, _ ->
                when (key) {
                    "cur_datetime" -> "Jun 11, 2026, 10:30:00 AM"
                    else -> error("unexpected key $key")
                }
            }
        )

        val expectedRuntimeContext = """
            <runtime_context>
            Dynamic values referenced by the stable system prompt:
            - cur_datetime: Jun 11, 2026, 10:30:00 AM
            </runtime_context>
        """.trimIndent()
        assertEquals(expectedRuntimeContext, result[1].toText())
        assertEquals("Call a tool", result[2].toText())
        assertTrue(result[3].getTools().isNotEmpty())
    }
}
