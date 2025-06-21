package me.rerere.rikkahub.data.sync

import android.content.Context
import android.util.Log
import at.bitfire.dav4jvm.BasicDigestAuthHandler
import at.bitfire.dav4jvm.DavCollection
import at.bitfire.dav4jvm.exception.NotFoundException
import at.bitfire.dav4jvm.property.DisplayName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.WebDavConfig
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val TAG = "DataSync"

class DataSync(
    private val settingsStore: SettingsStore,
    private val json: Json,
    private val context: Context,
) {
    suspend fun testWebdav(webDavConfig: WebDavConfig) {
        val davCollection = DavCollection(
            httpClient = webDavConfig.requireClient(),
            location = webDavConfig.url.toHttpUrl(),
        )

        withContext(Dispatchers.IO) {
            davCollection.propfind(
                depth = 1,
                DisplayName.NAME,
            ) { response, relation ->
                Log.i(TAG, "testWebdav: $response | $relation")
            }
        }
    }

    suspend fun backupToWebDav(webDavConfig: WebDavConfig) = withContext(Dispatchers.IO) {
        val file = prepareBackupFile(webDavConfig)
        val collection = webDavConfig.requireCollection()
        collection.ensureCollectionExists() // ensure collection exists
        val target = webDavConfig.requireCollection(file.name)
        target.put(
            body = file.asRequestBody(),
        ) { response ->
            Log.i(TAG, "backupToWebDav: $response")
        }
    }

    suspend fun restoreFromWebDav(webDavConfig: WebDavConfig) = withContext(Dispatchers.IO) {
        val collection = webDavConfig.requireCollection()
        collection.ensureCollectionExists() // ensure collection exists

    }

    private fun prepareBackupFile(webDavConfig: WebDavConfig): File {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val backupFile = File(
            context.cacheDir,
            "backup_$timestamp.zip"
        )
        if (backupFile.exists()) {
            backupFile.delete()
        }

        // 创建zip文件并备份数据库
        ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
            addVirtualFileToZip(
                zipOut = zipOut,
                name = "settings.json",
                content = json.encodeToString(settingsStore.settingsFlow.value)
            )

            if (webDavConfig.items.contains(WebDavConfig.BackupItem.DATABASE)) {
                // 备份主数据库文件
                val dbFile = context.getDatabasePath("rikka_hub")
                if (dbFile.exists()) {
                    addFileToZip(zipOut, dbFile, "rikka_hub.db")
                }

                // 备份数据库的WAL文件（如果存在）
                val walFile = File(dbFile.parentFile, "rikka_hub-wal")
                if (walFile.exists()) {
                    addFileToZip(zipOut, walFile, "rikka_hub-wal")
                }

                // 备份数据库的SHM文件（如果存在）
                val shmFile = File(dbFile.parentFile, "rikka_hub-shm")
                if (shmFile.exists()) {
                    addFileToZip(zipOut, shmFile, "rikka_hub-shm")
                }
            }

            if (webDavConfig.items.contains(WebDavConfig.BackupItem.FILES)) {
                // TODO: Save chat files to zip
            }
        }

        return backupFile
    }
}

private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
    FileInputStream(file).use { fis ->
        val zipEntry = ZipEntry(entryName)
        zipOut.putNextEntry(zipEntry)
        fis.copyTo(zipOut)
        zipOut.closeEntry()
        Log.d(TAG, "addFileToZip: Added $entryName (${file.length()} bytes) to zip")
    }
}

private fun addVirtualFileToZip(zipOut: ZipOutputStream, name: String, content: String) {
    val zipEntry = ZipEntry(name)
    zipOut.putNextEntry(zipEntry)
    zipOut.write(content.toByteArray())
    zipOut.closeEntry()
    Log.i(TAG, "addVirtualFileToZip: $name （${content.length} bytes）")
}

private fun WebDavConfig.requireClient(): OkHttpClient {
    val authHandler = BasicDigestAuthHandler(
        domain = null,
        username = this.username,
        password = this.password
    )
    val okHttpClient = OkHttpClient.Builder()
        .followRedirects(false)
        .authenticator(authHandler)
        .addNetworkInterceptor(authHandler)
        .build()
    return okHttpClient
}

private suspend fun WebDavConfig.requireCollection(path: String? = null): DavCollection {
    val location = buildString {
        append(this@requireCollection.url.trimEnd('/'))
        append("/")
        append(this@requireCollection.path.trim('/'))
        append("/")
        if (path != null) {
            append(path.trim('/'))
        }
    }.toHttpUrl()
    val davCollection = DavCollection(
        httpClient = this.requireClient(),
        location = location,
    )
    return davCollection
}

private suspend fun DavCollection.ensureCollectionExists() = withContext(Dispatchers.IO) {
    try {
        propfind(depth = 0) { response, relation ->
            Log.i(TAG, "ensureCollectionExists: $response $relation")
        }
    } catch (e: NotFoundException) {
        mkCol(null) { res ->
            Log.i(TAG, "ensureCollectionExists: $res")
        }
    }
}
