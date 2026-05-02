package com.eterultimate.eteruee.ai.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import com.eterultimate.eteruee.ai.provider.CustomBody
import com.eterultimate.eteruee.ai.provider.CustomHeader
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.internal.http.RealResponseBody

fun List<CustomHeader>.toHeaders(): Headers {
    return Headers.Builder().apply {
        this@toHeaders
            .filter { it.name.isNotBlank() }
            .forEach {
                add(it.name, it.value)
            }
    }.build()
}

fun Request.Builder.configureReferHeaders(url: String): Request.Builder {
    val httpUrl = url.toHttpUrl()
    return when (httpUrl.host) {
        "aihubmix.com" -> {
            addHeader("APP-Code", "DKHA9468")
        }

        "openrouter.ai" -> {
            this
                .addHeader("X-Title", "EterUee")
                .addHeader("HTTP-Referer", "https://eteruee.com")
        }

        else -> this
    }
}

fun ResponseBody.stringSafe(): String? {
    return when (this) {
        is RealResponseBody -> string()
        else -> null
    }
}

fun JsonObject.mergeCustomBody(bodies: List<CustomBody>): JsonObject {
    if (bodies.isEmpty()) return this

    val content = toMutableMap()
    bodies.forEach { body ->
        if (body.key.isNotBlank()) {
            // 濡傛灉宸插瓨鍦ㄧ浉鍚岄敭涓斾袱鑰呴兘鏄疛sonObject锛屽垯闇€瑕侀€掑綊鍚堝苟
            val existingValue = content[body.key]
            val newValue = body.value

            if (existingValue is JsonObject && newValue is JsonObject) {
                // 閫掑綊鍚堝苟涓や釜JsonObject
                content[body.key] = mergeJsonObjects(existingValue, newValue)
            } else {
                // 鐩存帴鏇挎崲鎴栨坊鍔?
                content[body.key] = newValue
            }
        }
    }
    return JsonObject(content)
}

/**
 * 閫掑綊鍚堝苟涓や釜JsonObject
 */
private fun mergeJsonObjects(base: JsonObject, overlay: JsonObject): JsonObject {
    val result = base.toMutableMap()

    for ((key, value) in overlay) {
        val baseValue = result[key]

        result[key] = if (baseValue is JsonObject && value is JsonObject) {
            // 濡傛灉涓よ€呴兘鏄疛sonObject锛岄€掑綊鍚堝苟
            mergeJsonObjects(baseValue, value)
        } else {
            // 鍚﹀垯浣跨敤鏂板€兼浛鎹㈡棫鍊?
            value
        }
    }

    return JsonObject(result)
}

/**
 * 浠?JsonElement 涓Щ闄ゆ垨淇濈暀鎸囧畾鐨勯敭
 * @param keys 瑕佹搷浣滅殑閿垪琛?
 * @param keepOnly 濡傛灉涓?true锛屽垯鍙繚鐣欐寚瀹氱殑閿紱濡傛灉涓?false锛屽垯绉婚櫎鎸囧畾鐨勯敭
 * @return 澶勭悊鍚庣殑 JsonElement
 */
fun JsonElement.removeElements(keys: List<String>, keepOnly: Boolean = false): JsonElement {
    return when (this) {
        is JsonObject -> {
            val newContent = if (keepOnly) {
                // 鍙繚鐣欐寚瀹氱殑閿紙涓旈敭瀛樺湪锛?
                keys.mapNotNull { key ->
                    get(key)?.let { key to it }
                }.toMap()
            } else {
                // 绉婚櫎鎸囧畾鐨勯敭
                toMap().filterKeys { key -> key !in keys }
            }

            // 閫掑綊澶勭悊宓屽鐨?JsonElement
            JsonObject(newContent.mapValues { (_, value) ->
                value.removeElements(keys, keepOnly)
            })
        }

        is JsonArray -> {
            JsonArray(map { it.removeElements(keys, keepOnly) })
        }

        else -> this // 鍩烘湰绫诲瀷鐩存帴杩斿洖
    }
}

