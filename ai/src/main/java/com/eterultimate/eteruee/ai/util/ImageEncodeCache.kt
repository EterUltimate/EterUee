package com.eterultimate.eteruee.ai.util

import android.util.Log
import java.io.File
import java.security.MessageDigest

/**
 * 历史图片编码结果的磁盘缓存（rikkahub#2292 同型缺陷）。
 *
 * FileEncoder.compressAndEncode 是纯函数，数据库里只存原图路径，每轮请求（含多轮工具调用）
 * 都会对同一张历史图片重复解码、JPEG 重编码、base64 转换，图片多时每轮明显卡顿。
 * 这里按 原图路径 + lastModified + 文件大小 + 编码参数 做缓存键，把编码出的 JPEG base64 字节
 * 落到 cacheDir，命中时跳过解码与编码，只读缓存文件。
 *
 * 原图被修改（mtime/大小变化）时键自然失效；缓存只增不减到容量上限后按最后修改时间清理。
 */
object ImageEncodeCache {
    private const val TAG = "ImageEncodeCache"
    private const val ENCODER_VERSION = "v1"
    private const val MAX_CACHE_BYTES = 256L * 1024 * 1024
    private const val PRUNE_INTERVAL = 32

    private var cacheDir: File? = null
    private var putCount = 0

    /** 应用启动时注入 cacheDir；未初始化时缓存静默禁用（直接走原编码路径） */
    fun init(dir: File) {
        cacheDir = dir.apply { mkdirs() }
        pruneLocked()
    }

    internal fun cacheKey(path: String, lastModified: Long, length: Long, quality: Int): String {
        // 仅作缓存键使用（非安全用途），选 SHA-256 避免 CodeQL 对 MD5 的密码学告警
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$path|$lastModified|$length|$quality|$ENCODER_VERSION".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Synchronized
    fun get(key: String): ByteArray? {
        val dir = cacheDir ?: return null
        return runCatching {
            val file = File(dir, key)
            if (file.isFile) file.readBytes() else null
        }.getOrElse {
            Log.w(TAG, "read encoded image cache failed", it)
            null
        }
    }

    @Synchronized
    fun put(key: String, bytes: ByteArray) {
        val dir = cacheDir ?: return
        runCatching {
            File(dir, key).writeBytes(bytes)
            if (++putCount % PRUNE_INTERVAL == 0) pruneLocked()
        }.onFailure {
            Log.w(TAG, "write encoded image cache failed", it)
        }
    }

    /** 超出容量上限时按最后修改时间从旧到新删除 */
    private fun pruneLocked() {
        val dir = cacheDir ?: return
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_CACHE_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= MAX_CACHE_BYTES) return
            if (file.delete()) total -= file.length()
        }
    }
}
