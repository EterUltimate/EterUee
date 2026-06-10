package com.eterultimate.eteruee.data.ai.transformers

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeReminderTransformerTest {
    @Test
    fun `inserted reminder should use following message timestamp`() {
        val firstCreatedAt = LocalDateTime(2026, 6, 11, 8, 0)
        val secondCreatedAt = LocalDateTime(2026, 6, 11, 10, 0)
        val messages = listOf(
            UIMessage(
                role = MessageRole.USER,
                parts = listOf(UIMessagePart.Text("First")),
                createdAt = firstCreatedAt
            ),
            UIMessage(
                role = MessageRole.USER,
                parts = listOf(UIMessagePart.Text("Second")),
                createdAt = secondCreatedAt
            )
        )

        val result = applyTimeReminder(messages)

        assertEquals(3, result.size)
        assertTrue(result[1].toText().contains("<time_reminder>"))
        assertEquals(secondCreatedAt, result[1].createdAt)
        assertEquals(secondCreatedAt, result[2].createdAt)
    }
}
