package com.eterultimate.eteruee.roleplay.data.tavern

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class TavernImportSampleTest {
    @Test
    fun `decode provided world info sample`() {
        val file = File("C:/Users/zacza/Desktop/card/工具/性格逻辑世界书3.0.json")
        assumeTrue("sample file is only available on the user's machine", file.exists())

        val worldInfo = TavernWorldInfoCodec.decode(file.readText(Charsets.UTF_8), "性格逻辑世界书3.0")

        assertTrue(worldInfo.entries.isNotEmpty())
        assertTrue(worldInfo.entries.any { it.content.isNotBlank() })
    }

    @Test
    fun `decode provided preset sample`() {
        val file = File("C:/Users/zacza/Desktop/card/工具/🚧🥣料碟預設_內部測試.json")
        assumeTrue("sample file is only available on the user's machine", file.exists())

        val preset = TavernPresetCodec.decode(file.readText(Charsets.UTF_8), "料碟預設")

        assertTrue(preset.name.isNotBlank())
        assertTrue(preset.parameters.isNotEmpty())
    }

    @Test
    fun `decode provided character json sample`() {
        val file = File("C:/Users/zacza/Desktop/card/涌流/涌流角色卡.json")
        assumeTrue("sample file is only available on the user's machine", file.exists())

        val character = TavernCharacterCodec.decode(file.readText(Charsets.UTF_8))

        assertTrue(character.name.isNotBlank())
        assertTrue(character.description.isNotBlank() || character.firstMessage.isNotBlank())
    }

    @Test
    fun `decode provided character png sample`() {
        val file = File("C:/Users/zacza/Desktop/card/工具/角色卡架构师.png")
        assumeTrue("sample file is only available on the user's machine", file.exists())

        val json = TavernPngCodec.readCharacterJson(file.readBytes())
        val character = json?.let(TavernCharacterCodec::decode)

        assertNotNull(json)
        assertNotNull(character)
        assertTrue(character!!.name.isNotBlank())
    }
}
