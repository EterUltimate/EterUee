package com.eterultimate.eteruee.utils

import org.apache.commons.text.StringEscapeUtils
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun String.urlEncode(): String {
    return URLEncoder.encode(this, "UTF-8")
}

fun String.urlDecode(): String {
    return URLDecoder.decode(this, "UTF-8")
}

@OptIn(ExperimentalEncodingApi::class)
fun String.base64Encode(): String {
    return Base64.encode(this.toByteArray())
}

@OptIn(ExperimentalEncodingApi::class)
fun String.base64Decode(): String {
    return String(Base64.decode(this))
}

fun String.escapeHtml(): String {
    return StringEscapeUtils.escapeHtml4(this)
}

fun String.unescapeHtml(): String {
    return StringEscapeUtils.unescapeHtml4(this)
}

fun Number.toFixed(digits: Int = 0) = "%.${digits}f".format(this)

fun String.applyPlaceholders(
    vararg placeholders: Pair<String, String>,
): String {
    var result = this
    for ((placeholder, replacement) in placeholders) {
        result = result.replace("{$placeholder}", replacement)
    }
    return result
}

fun Long.fileSizeToString(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> "${this / (1024 * 1024)} MB"
        else -> "${this / (1024 * 1024 * 1024)} GB"
    }
}

fun Int.formatNumber(): String {
    val absValue = kotlin.math.abs(this)
    val sign = if (this < 0) "-" else ""

    return when {
        absValue < 1000 -> this.toString()
        absValue < 1000000 -> {
            val value = absValue / 1000.0
            if (value == value.toInt().toDouble()) {
                "$sign${value.toInt()}K"
            } else {
                "$sign${value.toFixed(1)}K"
            }
        }

        absValue < 1000000000 -> {
            val value = absValue / 1000000.0
            if (value == value.toInt().toDouble()) {
                "$sign${value.toInt()}M"
            } else {
                "$sign${value.toFixed(1)}M"
            }
        }

        else -> {
            val value = absValue / 1000000000.0
            if (value == value.toInt().toDouble()) {
                "$sign${value.toInt()}B"
            } else {
                "$sign${value.toFixed(1)}B"
            }
        }
    }
}

fun Float.toFixed(digits: Int = 0) = "%.${digits}f".format(this)
fun Double.toFixed(digits: Int = 0) = "%.${digits}f".format(this)

/**
 * 鎻愬彇瀛楃涓蹭腑鎵€鏈夊紩鍙峰唴鐨勫唴瀹?
 * 鏀寔澶氱寮曞彿绫诲瀷锛氳嫳鏂囧弻寮曞彿 "..."銆佽嫳鏂囧崟寮曞彿 '...'銆佷腑鏂囧弻寮曞彿 "..."銆佷腑鏂囧崟寮曞彿 '...'
 * @return 鎵€鏈夊紩鍙峰唴鍐呭鐨勫垪琛?
 */
fun String.extractQuotedContent(): List<String> {
    val result = mutableListOf<String>()
    // 鍖归厤澶氱寮曞彿绫诲瀷
    val patterns = listOf(
        """"([^"]*?)"""",  // 涓枃鍙屽紩鍙?
        """'([^']*?)'""",  // 涓枃鍗曞紩鍙?
        """"([^"]*?)"""",  // 鑻辨枃鍙屽紩鍙?
        """'([^']*?)'""",  // 鑻辨枃鍗曞紩鍙?
    )
    for (pattern in patterns) {
        val regex = Regex(pattern)
        regex.findAll(this).forEach { matchResult ->
            val content = matchResult.groupValues[1]
            if (content.isNotBlank()) {
                result.add(content)
            }
        }
    }
    return result
}

/**
 * 鎻愬彇瀛楃涓蹭腑鎵€鏈夊紩鍙峰唴鐨勫唴瀹瑰苟鍚堝苟涓轰竴涓瓧绗︿覆
 * @param separator 鍒嗛殧绗︼紝榛樿涓烘崲琛?
 * @return 鍚堝苟鍚庣殑瀛楃涓诧紝濡傛灉娌℃湁寮曞彿鍐呭鍒欒繑鍥?null
 */
fun String.extractQuotedContentAsText(separator: String = "\n"): String? {
    val contents = extractQuotedContent()
    return if (contents.isNotEmpty()) {
        contents.joinToString(separator)
    } else {
        null
    }
}

