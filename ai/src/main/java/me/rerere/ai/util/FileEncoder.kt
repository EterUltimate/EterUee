package me.rerere.ai.util

import android.util.Base64
import androidx.core.net.toUri
import me.rerere.ai.ui.UIMessagePart
import java.io.File

fun UIMessagePart.Image.encodeBase64(): Result<String> = runCatching {
    if(this.url.startsWith("file://")) {
        val filePath = this.url.toUri().path ?: throw IllegalArgumentException("Invalid file URI: ${this.url}")
        val file = File(filePath)
        if (file.exists()) {
            val bytes = file.readBytes()
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/*;base64,$encoded"
        } else {
            throw IllegalArgumentException("File does not exist: ${this.url}")
        }
    } else {
        throw IllegalArgumentException("Unsupported URL format: $url")
    }
}