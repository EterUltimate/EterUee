package me.rerere.rikkahub.utils

import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlin.uuid.Uuid

fun Context.createChatFiles(uris: List<Uri>): List<Uri> {
    val newUris = mutableListOf<Uri>()
    val dir = this.filesDir.resolve("upload")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    uris.forEach { uri ->
        val fileName = Uuid.random()
        val newUri = dir
            .resolve("$fileName")
            .toUri()
        this.contentResolver.openInputStream(uri)?.use { inputStream ->
            this.contentResolver.openOutputStream(newUri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        newUris.add(newUri)
    }
    return newUris
}

fun Context.deleteChatFiles(uris: List<Uri>) {
    uris.forEach { uri ->
        val file = uri.toFile()
        if (file.exists()) {
            file.delete()
        }
    }
}