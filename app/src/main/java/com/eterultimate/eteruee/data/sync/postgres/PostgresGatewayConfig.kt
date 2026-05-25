package com.eterultimate.eteruee.data.sync.postgres

import kotlinx.serialization.Serializable

@Serializable
data class PostgresGatewayConfig(
    val baseUrl: String = "",
    val accessToken: String = "",
    val namespace: String = "default",
    val items: List<BackupItem> = listOf(
        BackupItem.DATABASE,
        BackupItem.FILES
    ),
) {
    @Serializable
    enum class BackupItem {
        DATABASE,
        FILES,
    }
}
