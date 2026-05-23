package com.eterultimate.eteruee.roleplay.data.tavern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TavernWorldInfoCodecTest {
    @Test
    fun `decode character book entries array`() {
        val worldInfo = TavernWorldInfoCodec.decode(
            """
            {
              "name": "Book",
              "entries": [
                {
                  "keys": ["alpha", "beta"],
                  "secondary_keys": ["gamma"],
                  "content": "Entry content",
                  "constant": true,
                  "selective": true,
                  "insertion_order": 25,
                  "enabled": true,
                  "position": "before_char",
                  "extensions": {
                    "probability": 80,
                    "depth": 6
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("Book", worldInfo.name)
        assertEquals(1, worldInfo.entries.size)
        assertEquals(listOf("alpha", "beta"), worldInfo.entries.first().keys)
        assertEquals(listOf("gamma"), worldInfo.entries.first().secondaryKeys)
        assertEquals(0.8f, worldInfo.entries.first().probability, 0.001f)
        assertEquals(6, worldInfo.entries.first().depth)
        assertTrue(worldInfo.entries.first().enabled)
    }

    @Test
    fun `decode world info entries object`() {
        val worldInfo = TavernWorldInfoCodec.decode(
            """
            {
              "entries": {
                "7": {
                  "uid": 7,
                  "key": ["alpha"],
                  "keysecondary": ["omega"],
                  "comment": "memo",
                  "content": "World info content",
                  "disable": true,
                  "probability": 40
                }
              }
            }
            """.trimIndent()
        )

        assertEquals(1, worldInfo.entries.size)
        assertEquals("memo", worldInfo.entries.first().comment)
        assertFalse(worldInfo.entries.first().enabled)
        assertEquals(0.4f, worldInfo.entries.first().probability, 0.001f)
    }
}
