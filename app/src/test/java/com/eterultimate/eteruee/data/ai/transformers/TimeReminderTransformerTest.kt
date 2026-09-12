package com.eterultimate.eteruee.data.ai.transformers

import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `synthetic reminder should be marked as synthetic`() {
        val firstCreatedAt = LocalDateTime(2026, 6, 11, 8, 0)
        val secondCreatedAt = LocalDateTime(2026, 6, 11, 12, 0)
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

        assertTrue(result[1].isSynthetic)
        assertFalse(result[0].isSynthetic)
        assertFalse(result[2].isSynthetic)
    }

    @Test
    fun `custom interval should use strict threshold and format minutes`() {
        val messages = listOf(
            UIMessage(
                role = MessageRole.USER,
                parts = listOf(UIMessagePart.Text("Hello")),
                createdAt = LocalDateTime(2026, 2, 22, 10, 0, 0)
            ),
            UIMessage(
                role = MessageRole.USER,
                parts = listOf(UIMessagePart.Text("World")),
                createdAt = LocalDateTime(2026, 2, 22, 10, 30, 0)
            )
        )

        assertEquals(2, applyTimeReminder(messages, intervalMinutes = 30).size)
        val result = applyTimeReminder(messages, intervalMinutes = 15)
        assertEquals(3, result.size)
        assertTrue(result[1].toText().contains("30 min since last message"))
    }

    @Test
    fun `large interval should not overflow or inject prematurely`() {
        val messages = listOf(
            UIMessage(
                role = MessageRole.USER,
                parts = listOf(UIMessagePart.Text("Hello")),
                createdAt = LocalDateTime(2026, 2, 22, 10, 0, 0)
            ),
            UIMessage(
                role = MessageRole.USER,
                parts = listOf(UIMessagePart.Text("World")),
                createdAt = LocalDateTime(2026, 2, 22, 12, 0, 0)
            )
        )

        assertEquals(2, applyTimeReminder(messages, intervalMinutes = 180).size)
        assertEquals(2, applyTimeReminder(messages, intervalMinutes = Int.MAX_VALUE).size)
    }
}
