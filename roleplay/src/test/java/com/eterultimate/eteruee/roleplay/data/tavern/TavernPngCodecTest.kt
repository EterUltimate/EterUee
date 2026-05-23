package com.eterultimate.eteruee.roleplay.data.tavern

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32

class TavernPngCodecTest {
    @Test
    fun `write and read V2 chara metadata`() {
        val json = """{"spec":"chara_card_v2","data":{"name":"V2"}}"""

        val png = TavernPngCodec.writeCharacterJson(minimalPng(), json)

        assertEquals(json, TavernPngCodec.readCharacterJson(png))
    }

    @Test
    fun `reader prefers V3 ccv3 metadata over V2 chara metadata`() {
        val v2 = """{"spec":"chara_card_v2","data":{"name":"V2"}}"""
        val v3 = """{"spec":"chara_card_v3","data":{"name":"V3"}}"""

        val png = TavernPngCodec.writeCharacterJson(minimalPng(), v2, v3)

        assertEquals(v3, TavernPngCodec.readCharacterJson(png))
    }

    private fun minimalPng(): ByteArray {
        return ByteArrayOutputStream().use { output ->
            output.write(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A))
            writeChunk(output, "IHDR", byteArrayOf(
                0, 0, 0, 1,
                0, 0, 0, 1,
                8,
                6,
                0,
                0,
                0
            ))
            writeChunk(output, "IEND", byteArrayOf())
            output.toByteArray()
        }
    }

    private fun writeChunk(output: ByteArrayOutputStream, type: String, data: ByteArray) {
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        output.write(intToBytes(data.size))
        output.write(typeBytes)
        output.write(data)
        val crc = CRC32()
        crc.update(typeBytes)
        crc.update(data)
        output.write(intToBytes(crc.value.toInt()))
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            ((value ushr 24) and 0xFF).toByte(),
            ((value ushr 16) and 0xFF).toByte(),
            ((value ushr 8) and 0xFF).toByte(),
            (value and 0xFF).toByte()
        )
    }
}
