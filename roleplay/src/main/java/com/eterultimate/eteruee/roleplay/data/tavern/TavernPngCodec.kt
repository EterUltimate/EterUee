package com.eterultimate.eteruee.roleplay.data.tavern

import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.InflaterInputStream

object TavernPngCodec {
    private val pngSignature = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private const val CHUNK_TEXT = "tEXt"
    private const val CHUNK_ZTXT = "zTXt"
    private const val CHUNK_ITXT = "iTXt"
    private const val CHUNK_IEND = "IEND"
    private const val KEY_V2 = "chara"
    private const val KEY_V3 = "ccv3"

    fun readCharacterJson(pngData: ByteArray): String? {
        if (!hasPngSignature(pngData)) return null

        var offset = pngSignature.size
        var v2Payload: String? = null

        while (offset <= pngData.size - 12) {
            val start = offset
            val length = readUInt32(pngData, offset)
            val type = String(pngData, offset + 4, 4, Charsets.US_ASCII)
            val dataStart = offset + 8
            val dataEnd = dataStart + length
            val chunkEnd = dataEnd + 4
            if (dataEnd > pngData.size || chunkEnd > pngData.size) return null

            if (type == CHUNK_IEND) break

            if (type == CHUNK_TEXT || type == CHUNK_ZTXT || type == CHUNK_ITXT) {
                val parsed = parseTextChunk(type, pngData.copyOfRange(dataStart, dataEnd))
                if (parsed != null) {
                    val (keyword, text) = parsed
                    if (keyword.equals(KEY_V3, ignoreCase = true)) {
                        decodeBase64OrRaw(text)?.let { return it }
                    }
                    if (keyword.equals(KEY_V2, ignoreCase = true) && v2Payload == null) {
                        v2Payload = text
                    }
                }
            }

            offset = if (chunkEnd > start) chunkEnd else break
        }

        return v2Payload?.let(::decodeBase64OrRaw)
    }

    fun writeCharacterJson(
        pngData: ByteArray,
        characterJson: String,
        v3CharacterJson: String? = null
    ): ByteArray {
        require(hasPngSignature(pngData)) { "Invalid PNG signature" }

        val output = ByteArrayOutputStream(pngData.size + characterJson.length + (v3CharacterJson?.length ?: 0) + 256)
        output.write(pngSignature)

        var offset = pngSignature.size
        var wroteIend = false

        while (offset <= pngData.size - 12) {
            val start = offset
            val length = readUInt32(pngData, offset)
            val type = String(pngData, offset + 4, 4, Charsets.US_ASCII)
            val dataStart = offset + 8
            val dataEnd = dataStart + length
            val chunkEnd = dataEnd + 4
            if (dataEnd > pngData.size || chunkEnd > pngData.size) break

            if (type == CHUNK_IEND) {
                writeTextChunk(output, KEY_V2, encodeBase64(characterJson))
                v3CharacterJson?.let { writeTextChunk(output, KEY_V3, encodeBase64(it)) }
                output.write(pngData, start, chunkEnd - start)
                wroteIend = true
                break
            }

            val shouldSkip = if (type == CHUNK_TEXT || type == CHUNK_ZTXT || type == CHUNK_ITXT) {
                val keyword = parseTextChunk(type, pngData.copyOfRange(dataStart, dataEnd))?.first
                keyword.equals(KEY_V2, ignoreCase = true) || keyword.equals(KEY_V3, ignoreCase = true)
            } else {
                false
            }

            if (!shouldSkip) {
                output.write(pngData, start, chunkEnd - start)
            }

            offset = if (chunkEnd > start) chunkEnd else break
        }

        require(wroteIend) { "PNG is missing IEND chunk" }
        return output.toByteArray()
    }

    private fun hasPngSignature(bytes: ByteArray): Boolean {
        return bytes.size >= pngSignature.size && pngSignature.indices.all { bytes[it] == pngSignature[it] }
    }

    private fun readUInt32(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
            (bytes[offset + 3].toInt() and 0xFF)
    }

    private fun ByteArray.indexOfByte(value: Int, startIndex: Int = 0): Int {
        for (index in startIndex.coerceAtLeast(0) until size) {
            if (this[index].toInt() == value) return index
        }
        return -1
    }

    private fun parseTextChunk(type: String, data: ByteArray): Pair<String, String>? {
        return when (type) {
            CHUNK_TEXT -> {
                val split = data.indexOfByte(0)
                if (split < 0) return null
                String(data, 0, split, Charsets.ISO_8859_1) to
                    String(data, split + 1, data.size - split - 1, Charsets.ISO_8859_1)
            }
            CHUNK_ZTXT -> {
                val split = data.indexOfByte(0)
                if (split < 0 || split + 2 > data.size) return null
                val keyword = String(data, 0, split, Charsets.ISO_8859_1)
                if (data[split + 1].toInt() != 0) return null
                val compressed = data.copyOfRange(split + 2, data.size)
                keyword to inflateLatin1(compressed)
            }
            CHUNK_ITXT -> parseInternationalTextChunk(data)
            else -> null
        }
    }

    private fun parseInternationalTextChunk(data: ByteArray): Pair<String, String>? {
        val keywordEnd = data.indexOfByte(0)
        if (keywordEnd < 0 || keywordEnd + 2 >= data.size) return null
        val keyword = String(data, 0, keywordEnd, Charsets.ISO_8859_1)
        val compressionFlag = data[keywordEnd + 1].toInt()
        val compressionMethod = data[keywordEnd + 2].toInt()
        if (compressionFlag !in 0..1 || compressionMethod != 0) return null

        var cursor = keywordEnd + 3
        val languageEnd = data.indexOfByte(0, cursor)
        if (languageEnd < 0) return null
        cursor = languageEnd + 1
        val translatedEnd = data.indexOfByte(0, cursor)
        if (translatedEnd < 0) return null
        cursor = translatedEnd + 1

        val textBytes = data.copyOfRange(cursor, data.size)
        val text = if (compressionFlag == 1) {
            inflateUtf8(textBytes)
        } else {
            String(textBytes, Charsets.UTF_8)
        }
        return keyword to text
    }

    private fun inflateLatin1(data: ByteArray): String {
        return InflaterInputStream(data.inputStream()).use { inflater ->
            String(inflater.readBytes(), Charsets.ISO_8859_1)
        }
    }

    private fun inflateUtf8(data: ByteArray): String {
        return InflaterInputStream(data.inputStream()).use { inflater ->
            String(inflater.readBytes(), Charsets.UTF_8)
        }
    }

    private fun decodeBase64OrRaw(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) return trimmed
        return runCatching {
            String(Base64.getDecoder().decode(trimmed), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun encodeBase64(text: String): String {
        return Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
    }

    private fun writeTextChunk(output: ByteArrayOutputStream, keyword: String, text: String) {
        val data = ByteArrayOutputStream(keyword.length + 1 + text.length)
        data.write(keyword.toByteArray(Charsets.ISO_8859_1))
        data.write(0)
        data.write(text.toByteArray(Charsets.ISO_8859_1))
        writeChunk(output, CHUNK_TEXT, data.toByteArray())
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
