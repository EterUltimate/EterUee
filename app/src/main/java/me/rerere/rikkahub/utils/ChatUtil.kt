package me.rerere.rikkahub.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.ai.ui.UIMessage
import java.net.URLDecoder
import kotlin.uuid.Uuid

private const val TAG = "ChatUtil"

fun String.urlDecode(): String {
    return URLDecoder.decode(this, "UTF-8")
}

fun navigateToChatPage(
    navController: NavController,
    chatId: Uuid = Uuid.random()
) {
    Log.i(TAG, "navigateToChatPage: navigate to $chatId")
    navController.navigate("chat/${chatId}") {
        popUpTo(0) {
            inclusive = true
        }
        launchSingleTop = true
    }
}

fun Context.copyMessageToClipboard(message: UIMessage) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(
        "Chat Message",
        message.toText()
    )
    clipboard.setPrimaryClip(clip)
    Log.i(TAG, "copyMessageToClipboard: Copied message to clipboard: ${message.toText()}")
}

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

fun Context.deleteAllChatFiles() {
    val dir = this.filesDir.resolve("upload")
    if (dir.exists()) {
        dir.deleteRecursively()
    }
}

suspend fun Context.countChatFiles(): Pair<Int, Long> = withContext(Dispatchers.IO) {
    val dir = filesDir.resolve("upload")
    if (!dir.exists()) {
        return@withContext Pair(0, 0)
    }
    val files = dir.listFiles() ?: return@withContext Pair(0, 0)
    val count = files.size
    val size = files.sumOf { it.length() }
    Pair(count, size)
}