package com.eterultimate.eteruee.web.routes

import android.content.Context
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.sync.webdav.WebDavSync
import com.eterultimate.eteruee.web.BadRequestException
import com.eterultimate.eteruee.web.dto.BackupRestoreResponse
import com.eterultimate.eteruee.web.dto.BackupStatusDto
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val BACKUP_CONTENT_TYPE = "application/zip"
private const val MAX_BACKUP_UPLOAD_SIZE_BYTES = 512L * 1024 * 1024

fun Route.backupRoutes(
    context: Context,
    settingsStore: SettingsStore,
    webDavSync: WebDavSync,
) {
    route("/backup") {
        get("/status") {
            val settings = settingsStore.settingsFlow.value
            val s3Configured = settings.s3Config.endpoint.isNotBlank() && settings.s3Config.bucket.isNotBlank()
            call.respond(
                BackupStatusDto(
                    lastBackupTime = settings.backupReminderConfig.lastBackupTime,
                    webDavConfigured = settings.webDavConfig.url.isNotBlank(),
                    s3Configured = s3Configured,
                    includedItems = settings.webDavConfig.items.map { it.name },
                )
            )
        }

        get("/export") {
            val backupFile = webDavSync.prepareBackupFile(settingsStore.settingsFlow.value.webDavConfig.copy())
            recordBackupTime(settingsStore)

            call.response.header(
                HttpHeaders.ContentDisposition,
                "attachment; filename=\"${backupFile.name}\""
            )
            call.response.header(HttpHeaders.ContentLength, backupFile.length().toString())
            call.respondOutputStream(contentType = ContentType.parse(BACKUP_CONTENT_TYPE)) {
                try {
                    FileInputStream(backupFile).use { input ->
                        input.copyTo(this)
                    }
                } finally {
                    backupFile.delete()
                }
            }
        }

        post("/restore") {
            val uploadedBackup = call.receiveBackupFile(context)
            try {
                webDavSync.restoreFromLocalFile(uploadedBackup.file, settingsStore.settingsFlow.value.webDavConfig)
                call.respond(
                    HttpStatusCode.OK,
                    BackupRestoreResponse(
                        status = "restored",
                        fileName = uploadedBackup.fileName,
                        size = uploadedBackup.size,
                    )
                )
            } finally {
                uploadedBackup.file.delete()
            }
        }
    }
}

private suspend fun recordBackupTime(settingsStore: SettingsStore) {
    settingsStore.update { settings ->
        settings.copy(
            backupReminderConfig = settings.backupReminderConfig.copy(
                lastBackupTime = System.currentTimeMillis()
            )
        )
    }
}

private data class UploadedBackupFile(
    val file: File,
    val fileName: String,
    val size: Long,
)

private suspend fun io.ktor.server.application.ApplicationCall.receiveBackupFile(
    context: Context,
): UploadedBackupFile {
    val multipart = receiveMultipart()
    var uploadedFile: UploadedBackupFile? = null

    while (true) {
        val part = multipart.readPart() ?: break
        try {
            if (part is PartData.FileItem) {
                if (uploadedFile != null) {
                    uploadedFile.file.delete()
                    throw BadRequestException("Only one backup file can be uploaded")
                }

                val originalFileName = part.originalFileName
                    ?.takeIf { it.isNotBlank() }
                    ?: "backup.zip"
                val fileName = sanitizeBackupFileName(originalFileName)
                val tempFile = File(context.cacheDir, "web_restore_${System.currentTimeMillis()}_$fileName")

                val size = try {
                    copyPartToFile(part, tempFile, MAX_BACKUP_UPLOAD_SIZE_BYTES)
                } catch (e: Throwable) {
                    tempFile.delete()
                    throw e
                }
                if (size == 0L) {
                    tempFile.delete()
                    throw BadRequestException("Uploaded backup file is empty")
                }

                uploadedFile = UploadedBackupFile(
                    file = tempFile,
                    fileName = fileName,
                    size = size,
                )
            }
        } finally {
            part.dispose()
        }
    }

    return uploadedFile ?: throw BadRequestException("No backup file uploaded")
}

private suspend fun copyPartToFile(
    part: PartData.FileItem,
    file: File,
    maxBytes: Long,
): Long = withContext(Dispatchers.IO) {
    val input = part.provider()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var totalBytes = 0L

    FileOutputStream(file).use { output ->
        while (true) {
            val read = input.readAvailable(buffer, 0, buffer.size)
            if (read <= 0) break

            totalBytes += read
            if (totalBytes > maxBytes) {
                throw BadRequestException("Backup file too large: max ${maxBytes / (1024 * 1024)} MB")
            }

            output.write(buffer, 0, read)
        }
    }

    totalBytes
}

private fun sanitizeBackupFileName(fileName: String): String {
    return fileName
        .substringAfterLast('/')
        .substringAfterLast('\\')
        .replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
        .ifBlank { "backup.zip" }
}
