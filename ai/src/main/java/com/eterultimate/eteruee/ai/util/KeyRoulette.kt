package com.eterultimate.eteruee.ai.util

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

interface KeyRoulette {
    fun next(keys: String, providerId: String = ""): String

    companion object {
        fun default(): KeyRoulette = DefaultKeyRoulette()

        /**
         * LRU 杞锛屾寔涔呭寲瀛樺偍鍒?cacheDir/lru_key_roulette.json
         * 閫氳繃 providerId 鍖哄垎鍚岀被鍨嬬殑澶氫釜 provider 瀹炰緥锛屽湪 next() 璋冪敤鏃朵紶鍏?
         */
        fun lru(context: Context): KeyRoulette = LruKeyRoulette(context)
    }
}

private val SPLIT_KEY_REGEX = "[\\s,]+".toRegex() // 绌烘牸鎹㈣鍜岄€楀彿

private fun splitKey(key: String): List<String> {
    return key
        .split(SPLIT_KEY_REGEX)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private class DefaultKeyRoulette : KeyRoulette {
    override fun next(keys: String, providerId: String): String {
        val keyList = splitKey(keys)
        return if (keyList.isNotEmpty()) {
            keyList.random()
        } else {
            keys
        }
    }
}

private const val LRU_CACHE_FILE = "lru_key_roulette.json"
private const val EXPIRE_DURATION_MS = 24 * 60 * 60 * 1000L // 1 澶?

// 鍏ㄥ眬鏂囦欢閿侊紝闃叉澶氫釜 provider 瀹炰緥骞跺彂璇诲啓鍚屼竴鏂囦欢
private object LruFileLock

// 鏂囦欢缁撴瀯: Map<providerId, Map<apiKey, lastUsedTimestamp>>
private typealias LruCache = Map<String, Map<String, Long>>

private class LruKeyRoulette(
    private val context: Context,
) : KeyRoulette {

    override fun next(keys: String, providerId: String): String {
        val keyList = splitKey(keys)
        if (keyList.isEmpty()) return keys

        synchronized(LruFileLock) {
            val now = System.currentTimeMillis()
            val allCache = loadCache().toMutableMap()

            // 鍙栨湰 provider 鐨勮褰曪紝杩囨护鎺夊凡杩囨湡鏉＄洰鍜屼笉鍦ㄥ綋鍓?key 鍒楄〃涓殑鏉＄洰
            val providerCache = (allCache[providerId] ?: emptyMap())
                .filter { (k, lastUsed) -> k in keyList && now - lastUsed < EXPIRE_DURATION_MS }
                .toMutableMap()

            // 浼樺厛閫変粠鏈娇鐢ㄧ殑 key锛屽惁鍒欓€夋渶涔呮湭浣跨敤鐨?
            val selected = keyList.firstOrNull { it !in providerCache }
                ?: providerCache.minByOrNull { it.value }!!.key

            providerCache[selected] = now
            allCache[providerId] = providerCache

            // 娓呯悊鏁翠釜 provider 鏉＄洰鍧囧凡杩囨湡鐨勮褰?
            allCache.entries.removeIf { (id, cache) ->
                id != providerId && cache.values.all { now - it >= EXPIRE_DURATION_MS }
            }

            saveCache(allCache)
            return selected
        }
    }

    private fun loadCache(): LruCache {
        return try {
            val file = File(context.cacheDir, LRU_CACHE_FILE)
            if (!file.exists()) return emptyMap()
            Json.decodeFromString(file.readText())
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveCache(cache: LruCache) {
        try {
            File(context.cacheDir, LRU_CACHE_FILE).writeText(Json.encodeToString(cache))
        } catch (_: Exception) {
        }
    }
}

