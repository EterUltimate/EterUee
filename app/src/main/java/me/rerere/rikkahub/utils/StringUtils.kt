package me.rerere.rikkahub.utils

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
    if (this.isEmpty()) {
        return ""
    }
    val sb = StringBuilder(this.length + (this.length / 10)) // 预估容量，避免多次扩容
    for (char in this) {
        when (char) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\'' -> sb.append("&apos;")
            // 可选：处理其他一些不常见的字符，但以上5个是最核心的
            // '/' -> sb.append("&#x2F;") // OWASP 推荐，但并非所有场景都需要
            else -> sb.append(char)
        }
    }
    return sb.toString()
}

fun Number.toFixed(digits: Int = 0) = "%.${digits}f".format(this)