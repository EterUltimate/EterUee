package me.rerere.rikkahub.utils

import java.net.URLDecoder

fun String.urlDecode(): String {
    return URLDecoder.decode(this, "UTF-8")
}

fun Number.toFixed(digits: Int = 0) = "%.${digits}f".format(this)