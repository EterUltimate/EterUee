package com.eterultimate.eteruee.roleplay.data.tavern

import com.eterultimate.eteruee.roleplay.data.model.Character
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TavernCharacterCodecTest {
    @Test
    fun `encode emits selected V1 layout`() {
        val root = TavernCharacterCodec.toJsonElement(sampleCharacter(), TavernCharacterCardFormat.V1)

        assertFalse(root.containsKey("spec"))
        assertFalse(root.containsKey("data"))
        assertEquals("Mira", root.string("name"))
        assertEquals("Warm opening", root.string("first_mes"))
        assertEquals(2, root["tags"]?.jsonArrayOrEmpty()?.size)
    }

    @Test
    fun `encode emits selected V2 layout`() {
        val root = TavernCharacterCodec.toJsonElement(sampleCharacter(), TavernCharacterCardFormat.V2)
        val data = root["data"]!!.jsonObject

        assertEquals("chara_card_v2", root.string("spec"))
        assertEquals("2.0", root.string("spec_version"))
        assertEquals("Mira", data.string("name"))
        assertEquals("Keep scenes grounded.", data.string("system_prompt"))
        assertTrue(data["character_book"] is JsonObject)
    }

    @Test
    fun `encode emits selected V3 layout`() {
        val root = TavernCharacterCodec.toJsonElement(sampleCharacter(), TavernCharacterCardFormat.V3)

        assertEquals("chara_card_v3", root.string("spec"))
        assertEquals("3.0", root.string("spec_version"))
    }

    @Test
    fun `decode preserves V3 extensions and character book`() {
        val encoded = TavernCharacterCodec.encode(sampleCharacter(), TavernCharacterCardFormat.V3)
        val decoded = TavernCharacterCodec.decode(encoded)

        assertEquals("Mira", decoded.name)
        assertEquals(listOf("soft", "glass"), decoded.tags)
        assertEquals(0.72f, decoded.talkativeness, 0.001f)
        assertTrue(decoded.favorite)
        assertEquals("chara_card_v3", decoded.spec)
        assertEquals("3.0", decoded.specVersion)
        assertTrue(decoded.characterBook is JsonObject)
    }

    @Test
    fun `decode V1 top level card`() {
        val decoded = TavernCharacterCodec.decode(
            """
            {
              "name": "Ari",
              "description": "Archivist",
              "personality": "Concise",
              "scenario": "Library",
              "first_mes": "Welcome.",
              "mes_example": "<START>",
              "creator": "tester",
              "tags": ["minimal"]
            }
            """.trimIndent()
        )

        assertEquals("Ari", decoded.name)
        assertEquals("Welcome.", decoded.firstMessage)
        assertEquals(listOf("minimal"), decoded.tags)
        assertEquals("chara_card_v1", decoded.spec)
        assertEquals("1.0", decoded.specVersion)
    }

    private fun sampleCharacter(): Character {
        return Character(
            name = "Mira",
            description = "A calm hitech companion.",
            personality = "Precise, warm, observant.",
            scenario = "A quiet room with translucent displays.",
            firstMessage = "Warm opening",
            messageExamples = "<START>\n{{char}}: Example",
            systemPrompt = "Keep scenes grounded.",
            postHistoryInstructions = "Preserve continuity.",
            creator = "EterUee",
            creatorNotes = "Imported from test data.",
            tags = listOf("soft", "glass"),
            talkativeness = 0.72f,
            alternateGreetings = listOf("Alt one", "Alt two"),
            characterVersion = "1.2.3",
            createdAt = Instant.parse("2026-05-23T00:00:00Z"),
            updatedAt = Instant.parse("2026-05-23T00:00:00Z"),
            favorite = true,
            characterBook = JsonObject(
                mapOf(
                    "entries" to JsonArray(
                        listOf(
                            JsonObject(
                                mapOf(
                                    "keys" to JsonArray(listOf(JsonPrimitive("memory"))),
                                    "content" to JsonPrimitive("Remember important facts.")
                                )
                            )
                        )
                    )
                )
            ),
            extensions = mapOf("vendor" to JsonPrimitive("eteruee"))
        )
    }

    private fun JsonObject.string(key: String): String? {
        return this[key]?.jsonPrimitiveOrNull()?.contentOrNull
    }

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveOrNull() =
        this as? JsonPrimitive

    private fun kotlinx.serialization.json.JsonElement.jsonArrayOrEmpty() =
        this as? JsonArray ?: JsonArray(emptyList())
}
