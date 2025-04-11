package me.rerere.rikkahub.utils

import android.content.Context
import androidx.core.net.toUri

fun Context.openUrl(url: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
    intent.data = url.toUri()
    startActivity(intent)
}