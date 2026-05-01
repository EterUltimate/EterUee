package com.eterultimate.eteruee.ai.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

class HttpException(
    message: String
) : RuntimeException(message)

fun JsonElement.parseErrorDetail(): HttpException {
    return when (this) {
        is JsonObject -> {
            // 灏濊瘯鑾峰彇甯歌鐨勯敊璇瓧娈?
            val errorFields = listOf("error", "detail", "message", "description")

            // 鏌ユ壘绗竴涓瓨鍦ㄧ殑閿欒瀛楁
            val foundField = errorFields.firstOrNull { this[it] != null }

            if (foundField != null) {
                // 閫掑綊瑙ｆ瀽鎵惧埌鐨勫瓧娈靛€?
                this[foundField]!!.parseErrorDetail()
            } else {
                // 濡傛灉娌℃湁鎵惧埌浠讳綍閿欒瀛楁锛屽簭鍒楀寲鏁翠釜瀵硅薄
                HttpException(Json.encodeToString(JsonElement.serializer(), this))
            }
        }

        is JsonArray -> {
            if (this.isEmpty()) {
                HttpException("Unknown error: Empty JSON array")
            } else {
                // 閫掑綊瑙ｆ瀽鏁扮粍鐨勭涓€涓厓绱?
                this.first().parseErrorDetail()
            }
        }

        is JsonPrimitive -> {
            // 瀵逛簬鍩烘湰绫诲瀷锛岀洿鎺ヤ娇鐢ㄥ叾鍐呭
            HttpException(this.jsonPrimitive.content)
        }

        else -> {
            // 鍏朵粬鎯呭喌锛屽簭鍒楀寲鏁翠釜鍏冪礌
            HttpException(Json.encodeToString(JsonElement.serializer(), this))
        }
    }
}

