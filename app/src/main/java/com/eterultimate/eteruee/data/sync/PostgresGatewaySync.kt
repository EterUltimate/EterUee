package com.eterultimate.eteruee.data.sync

import android.util.Log
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.datastore.WebDavConfig
import com.eterultimate.eteruee.data.sync.postgres.PostgresGatewayClient
import com.eterultimate.eteruee.data.sync.postgres.PostgresGatewayConfig
import com.eterultimate.eteruee.data.sync.webdav.WebDavSync
import com.eterultimate.eteruee.utils.fileSizeToString

private const val TAG = "PostgresGatewaySync"

class PostgresGatewaySync(
    private val settingsStore: SettingsStore,
    private val webDavSync: WebDavSync,
    private val httpClient: HttpClient,
) {
    private fun getClient(config: PostgresGatewayConfig): PostgresGatewayClient {
        return PostgresGatewayClient(config, httpClient)
    }

    suspend fun testConnection(config: PostgresGatewayConfig) = withContext(Dispatchers.IO) {
        getClient(config).testConnection().getOrThrow()
        Log.i(TAG, "testConnection: Gateway connection successful")
    }

    suspend fun backup(config: PostgresGatewayConfig) = withContext(Dispatchers.IO) {
        val file = webDavSync.prepareBackupFile(config.toWebDavBackupConfig())
        try {
            getClient(config).uploadBackup(file).getOrThrow()
            Log.i(TAG, "backup: Uploaded ${file.name} (${file.length().fileSizeToString()})")
        } finally {
            file.delete()
        }
    }

    private fun PostgresGatewayConfig.toWebDavBackupConfig(): WebDavConfig {
        return settingsStore.settingsFlow.value.webDavConfig.copy(
            items = items.map {
                when (it) {
                    PostgresGatewayConfig.BackupItem.DATABASE -> WebDavConfig.BackupItem.DATABASE
                    PostgresGatewayConfig.BackupItem.FILES -> WebDavConfig.BackupItem.FILES
                }
            }
        )
    }
}
