package com.eterultimate.eteruee.roleplay.data.tavern

import com.eterultimate.eteruee.ai.core.MessageRole
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.uuid.Uuid

class TavernChatCodecTest {
    @Test
    fun `decode JSONL header and messages`() {
        val characterId = Uuid.random()
        val payload = TavernChatCodec.decodeJsonl(
            sequenceOf(
                """{"user_name":"User","character_name":"Mira","create_date":"2026-05-23","chat_metadata":{"chat_id_hash":"abc","variables":{"mood":"calm"}}}""",
                """{"name":"User","is_user":true,"is_system":false,"send_date":1710000000000,"mes":"Hello","extra":{"token_count":3}}""",
                """{"name":"Mira","is_user":false,"is_system":false,"send_date":"May 23, 2026 3:10PM","mes":"Hi","extra":{"model":"test-model","swipes":["Hi","Hello"]}}"""
            ),
            characterId = characterId
        )

        assertEquals(characterId, payload.metadata.characterId)
        assertEquals("User", payload.metadata.userName)
        assertEquals("Mira", payload.metadata.characterName)
        assertEquals("abc", payload.metadata.tavernChatId)
        assertEquals(mapOf("mood" to "calm"), payload.metadata.variables)
        assertEquals(2, payload.messages.size)
        assertEquals(MessageRole.USER, payload.messages[0].role)
        assertEquals(3, payload.messages[0].tokenCount)
        assertEquals(MessageRole.ASSISTANT, payload.messages[1].role)
        assertEquals("test-model", payload.messages[1].model)
        assertEquals(listOf("Hi", "Hello"), payload.messages[1].swipeAlternatives)
    }
}
