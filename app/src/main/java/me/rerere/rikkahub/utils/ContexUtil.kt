package me.rerere.rikkahub.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.core.net.toUri

/**
 * Read clipboard data as text
 */
fun Context.readClipboardText(): String {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = clipboardManager.primaryClip ?: return ""
    val item = clip.getItemAt(0) ?: return ""
    return item.text.toString()
}

/**
 * Write text into clipboard
 */
fun Context.writeClipboardText(text: String) {
    val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("text", text))
}

/**
 * Open a url
 */
fun Context.openUrl(url: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
    intent.data = url.toUri()
    startActivity(intent)
}

fun Context.getActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    return null
}

fun Context.getComponentActivity(): ComponentActivity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) {
            return context
        }
        context = context.baseContext
    }
    return null
}