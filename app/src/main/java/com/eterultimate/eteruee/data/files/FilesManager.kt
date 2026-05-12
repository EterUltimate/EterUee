package com.eterultimate.eteruee.data.files

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import androidx.core.net.toUri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.common.android.Logging
import com.eterultimate.eteruee.AppScope
import com.eterultimate.eteruee.data.db.entity.ManagedFileEntity
import com.eterultimate.eteruee.data.repository.FilesRepository
import com.eterultimate.eteruee.utils.exportImage
import com.eterultimate.eteruee.utils.exportImageFile
import com.eterultimate.eteruee.utils.getActivity
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class FilesManager(
    private val context: Context,
    private val repository: FilesRepository,
    private val appScope: AppScope,
) {
    companion object {
        private const val TAG = "FilesManager"
    }

    suspend fun saveUploadFromUri(
        uri: Uri,
        displayName: String? = null,
        mimeType: String? = null,
    ): ManagedFileEntity = withContext(Dispatchers.IO) {
        val resolvedName = displayName ?: getFileNameFromUri(uri) ?: "file"
        val resolvedMime = mimeType ?: getFileMimeType(uri) ?: "application/octet-stream"
        val target = createTargetFile(FileFolders.UPLOAD, resolvedName, resolvedMime)
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        val now = System.currentTimeMillis()
        repository.insert(
            ManagedFileEntity(
                folder = FileFolders.UPLOAD,
                relativePath = "${FileFolders.UPLOAD}/${target.name}",
                displayName = resolvedName,
                mimeType = resolvedMime,
                sizeBytes = target.length(),
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun saveUploadFromBytes(
        bytes: ByteArray,
        displayName: String,
        mimeType: String = "application/octet-stream",
    ): ManagedFileEntity = withContext(Dispatchers.IO) {
        val target = createTargetFile(FileFolders.UPLOAD, displayName, mimeType)
        target.writeBytes(bytes)
        val now = System.currentTimeMillis()
        repository.insert(
            ManagedFileEntity(
                folder = FileFolders.UPLOAD,
                relativePath = "${FileFolders.UPLOAD}/${target.name}",
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = target.length(),
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    suspend fun saveUploadText(
        text: String,
        displayName: String = "pasted_text.txt",
        mimeType: String = "text/plain",
    ): ManagedFileEntity = withContext(Dispatchers.IO) {
        val target = createTargetFile(FileFolders.UPLOAD, displayName, mimeType)
        target.writeText(text)
        val now = System.currentTimeMillis()
        repository.insert(
            ManagedFileEntity(
                folder = FileFolders.UPLOAD,
                relativePath = "${FileFolders.UPLOAD}/${target.name}",
                displayName = displayName,
                mimeType = mimeType,
                sizeBytes = target.length(),
                createdAt = now,
                updatedAt = now,
            )
        )
    }

    fun observe(folder: String = FileFolders.UPLOAD): Flow<List<ManagedFileEntity>> =
        repository.listByFolder(folder)

    suspend fun list(folder: String = FileFolders.UPLOAD): List<ManagedFileEntity> =
        repository.listByFolder(folder).first()

    suspend fun get(id: Long): ManagedFileEntity? = repository.getById(id)

    suspend fun getByRelativePath(relativePath: String): ManagedFileEntity? = repository.getByPath(relativePath)

    fun getFile(entity: ManagedFileEntity): File =
        File(context.filesDir, entity.relativePath)

    fun createChatFilesByContents(uris: List<Uri>): List<Uri> {
        val newUris = mutableListOf<Uri>()
        val dir = context.filesDir.resolve(FileFolders.UPLOAD)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        uris.forEach { uri ->
            runCatching {
                val sourceName = getFileNameFromUri(uri) ?: uri.lastPathSegment ?: "file"
                val sourceMime = getFileMimeType(uri)
                val fileName = buildUuidFileName(displayName = sourceName, mimeType = sourceMime)
                val file = dir.resolve(fileName)
                if (!file.exists()) {
                    file.createNewFile()
                }
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: error("Failed to open input stream for $uri")
                inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val guessedMime = sourceMime ?: guessMimeType(file, sourceName)
                trackUploadFile(file = file, displayName = sourceName, mimeType = guessedMime)
                newUris.add(file.toUri())
            }.onFailure {
                it.printStackTrace()
                Log.e(TAG, "createChatFilesByContents: Failed to save file from $uri", it)
                Logging.log(
                    TAG,
                    "createChatFilesByContents: Failed to save file from $uri ${it.message} | ${it.stackTraceToString()}"
                )
            }
        }
        return newUris
    }

    fun createChatFilesByByteArrays(byteArrays: List<ByteArray>): List<Uri> {
        val newUris = mutableListOf<Uri>()
        val dir = context.filesDir.resolve(FileFolders.UPLOAD)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        byteArrays.forEach { byteArray ->
            val fileName = buildUuidFileName(displayName = "image.png", mimeType = "image/png")
            val file = dir.resolve(fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            val newUri = file.toUri()
            file.outputStream().use { outputStream ->
                outputStream.write(byteArray)
            }
            trackUploadFile(file = file, displayName = "image.png", mimeType = "image/png")
            newUris.add(newUri)
        }
        return newUris
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun convertBase64ImagePartToLocalFile(message: UIMessage): UIMessage =
        withContext(Dispatchers.IO) {
            message.copy(
                parts = message.parts.map { part ->
                    when (part) {
                        is UIMessagePart.Image -> {
                            if (part.url.startsWith("data:image")) {
                                val sourceByteArray = Base64.decode(part.url.substringAfter("base64,").toByteArray())
                                val bitmap = BitmapFactory.decodeByteArray(sourceByteArray, 0, sourceByteArray.size)
                                val byteArray = FileUtils.compressBitmapToPng(bitmap)
                                val urls = createChatFilesByByteArrays(listOf(byteArray))
                                Log.i(
                                    TAG,
                                    "convertBase64ImagePartToLocalFile: convert base64 img to ${urls.joinToString(", ")}"
                                )
                                part.copy(
                                    url = urls.first().toString(),
                                )
                            } else {
                                part
                            }
                        }

                        else -> part
                    }
                }
            )
        }

    fun deleteChatFiles(uris: List<Uri>) {
        val relativePaths = mutableSetOf<String>()
        uris.filter { it.toString().startsWith("file:") }.forEach { uri ->
            val file = uri.toFile()
            getRelativePathInFilesDir(file)?.let { relativePaths.add(it) }
            if (file.exists()) {
                file.delete()
            }
        }
        if (relativePaths.isNotEmpty()) {
            appScope.launch(Dispatchers.IO) {
                relativePaths.forEach { path ->
                    repository.deleteByPath(path)
                }
            }
        }
    }

    suspend fun countChatFiles(): Pair<Int, Long> = withContext(Dispatchers.IO) {
        val dir = context.filesDir.resolve(FileFolders.UPLOAD)
        if (!dir.exists()) {
            return@withContext Pair(0, 0)
        }
        val files = dir.listFiles() ?: return@withContext Pair(0, 0)
        val count = files.size
        val size = files.sumOf { it.length() }
        Pair(count, size)
    }

    fun createChatTextFile(text: String): UIMessagePart.Document {
        val dir = context.filesDir.resolve(FileFolders.UPLOAD)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val fileName = buildUuidFileName(displayName = "pasted_text.txt", mimeType = "text/plain")
        val file = dir.resolve(fileName)
        file.writeText(text)
        trackUploadFile(file = file, displayName = "pasted_text.txt", mimeType = "text/plain")
        return UIMessagePart.Document(
            url = file.toUri().toString(),
            fileName = "pasted_text.txt",
            mime = "text/plain"
        )
    }

    fun getImagesDir(): File {
        val dir = context.filesDir.resolve("images")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getVideosDir(): File {
        val dir = context.filesDir.resolve("videos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun createImageFileFromBase64(base64Data: String, filePath: String): File {
        val data = if (base64Data.startsWith("data:image")) {
            base64Data.substringAfter("base64,")
        } else {
            base64Data
        }

        val byteArray = Base64.decode(data.toByteArray())
        val file = File(filePath)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArray)
        return file
    }

    fun listImageFiles(): List<File> {
        val imagesDir = getImagesDir()
        return imagesDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp") }
            ?.toList()
            ?: emptyList()
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun saveMessageImage(activityContext: Context, image: String) = withContext(Dispatchers.IO) {
        val activity = requireNotNull(activityContext.getActivity()) { "Activity not found" }
        when {
            image.startsWith("data:image") -> {
                val byteArray = Base64.decode(image.substringAfter("base64,").toByteArray())
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                activityContext.exportImage(activity, bitmap)
            }

            image.startsWith("file:") -> {
                val file = image.toUri().toFile()
                activityContext.exportImageFile(activity, file)
            }

            image.startsWith("/") -> {
                activityContext.exportImageFile(activity, File(image))
            }

            image.startsWith("http") -> {
                runCatching {
                    val url = URL(image)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connect()

                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                        activityContext.exportImage(activity, bitmap)
                    } else {
                        Log.e(
                            TAG,
                            "saveMessageImage: Failed to download image from $image, response code: ${connection.responseCode}"
                        )
                    }
                }.getOrNull()
            }

            else -> error("Invalid image format")
        }
    }

    suspend fun syncFolder(folder: String = FileFolders.UPLOAD): Int = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, folder)
        if (!dir.exists()) return@withContext 0
        val files = dir.listFiles()?.filter { it.isFile } ?: return@withContext 0
        var inserted = 0
        files.forEach { file ->
            val relativePath = "${folder}/${file.name}"
            val existing = repository.getByPath(relativePath)
            if (existing == null) {
                val now = System.currentTimeMillis()
                val displayName = file.name
                val mimeType = guessMimeType(file, displayName)
                repository.insert(
                    ManagedFileEntity(
                        folder = folder,
                        relativePath = relativePath,
                        displayName = displayName,
                        mimeType = mimeType,
                        sizeBytes = file.length(),
                        createdAt = file.lastModified().takeIf { it > 0 } ?: now,
                        updatedAt = now,
                    )
                )
                inserted += 1
            }
        }
        inserted
    }

    suspend fun delete(id: Long, deleteFromDisk: Boolean = true): Boolean = withContext(Dispatchers.IO) {
        val entity = repository.getById(id) ?: return@withContext false
        if (deleteFromDisk) {
            runCatching { getFile(entity).delete() }
        }
        repository.deleteById(id) > 0
    }

    private fun createTargetFile(folder: String, displayName: String, mimeType: String?): File {
        val dir = File(context.filesDir, folder)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, FileUtils.buildUuidFileName(displayName = displayName, mimeType = mimeType))
    }

    private fun buildUuidFileName(displayName: String?, mimeType: String?): String =
        FileUtils.buildUuidFileName(displayName, mimeType)

    private fun trackUploadFile(file: File, displayName: String, mimeType: String) {
        val relativePath = "${FileFolders.UPLOAD}/${file.name}"
        appScope.launch(Dispatchers.IO) {
            runCatching {
                val existing = repository.getByPath(relativePath)
                if (existing != null) {
                    return@runCatching
                }
                val now = System.currentTimeMillis()
                repository.insert(
                    ManagedFileEntity(
                        folder = FileFolders.UPLOAD,
                        relativePath = relativePath,
                        displayName = displayName,
                        mimeType = mimeType,
                        sizeBytes = file.length(),
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            }.onFailure {
                Log.e(TAG, "trackUploadFile: Failed to track file ${file.absolutePath}", it)
                Logging.log(
                    TAG,
                    "trackUploadFile: Failed to track file ${file.absolutePath} ${it.message} | ${it.stackTraceToString()}"
                )
            }
        }
    }

    private fun getRelativePathInFilesDir(file: File): String? =
        FileUtils.getRelativePathInFilesDir(context.filesDir, file)

    fun getFileNameFromUri(uri: Uri): String? =
        FileUtils.getFileNameFromUri(context, uri)

    fun getFileMimeType(uri: Uri): String? =
        FileUtils.getFileMimeType(context, uri)

    private fun guessMimeType(file: File, fileName: String): String =
        FileUtils.guessMimeType(file, fileName)
}

object FileFolders {
    const val UPLOAD = "upload"
    const val SKILLS = "skills"
}

