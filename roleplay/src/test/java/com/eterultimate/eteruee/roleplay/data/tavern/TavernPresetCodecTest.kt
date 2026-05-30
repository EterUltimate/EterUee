package com.eterultimate.eteruee.roleplay.data.tavern

import com.eterultimate.eteruee.roleplay.data.model.PresetType
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernPresetCodecTest {
    @Test
    fun `decode raw SillyTavern style preset keeps parameters`() {
        val preset = TavernPresetCodec.decode(
            """
            {
              "temperature": 0.85,
              "top_p": 0.9,
              "stream_openai": true,
              "claude_use_sysprompt": true,
              "assistant_prefill": "Understood.",
              "custom_model": "test-model"
            }
            """.trimIndent(),
            fallbackName = "Sauce Preset"
        )

        assertEquals("Sauce Preset", preset.name)
        assertEquals(PresetType.CLAUDE, preset.type)
        assertEquals(0.85f, (preset.parameters["temperature"] as JsonPrimitive).floatOrNull ?: 0f, 0.001f)
        assertTrue((preset.parameters["stream_openai"] as JsonPrimitive).booleanOrNull == true)
        assertEquals("test-model", (preset.parameters["custom_model"] as JsonPrimitive).contentOrNull)
    }

    @Test
    fun `encode preset emits importable JSON root`() {
        val preset = TavernPresetCodec.decode(
            """
            {
              "name": "OpenAI preset",
              "type": "OPENAI",
              "parameters": {
                "temperature": 0.7,
                "max_tokens": 2048
              }
            }
            """.trimIndent()
        )

        val encoded = com.eterultimate.eteruee.roleplay.data.serialization.RoleplayJson
            .parseToJsonElement(TavernPresetCodec.encode(preset))
            .jsonObject

        assertEquals("OpenAI preset", encoded["name"]?.jsonPrimitiveOrNull()?.contentOrNull)
        assertEquals("OPENAI", encoded["type"]?.jsonPrimitiveOrNull()?.contentOrNull)
        assertEquals(2048, encoded["max_tokens"]?.jsonPrimitiveOrNull()?.contentOrNull?.toInt())
    }

    private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveOrNull() =
        this as? JsonPrimitive
}
