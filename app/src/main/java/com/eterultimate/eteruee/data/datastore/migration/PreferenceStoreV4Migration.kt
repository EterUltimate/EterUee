package com.eterultimate.eteruee.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.eterultimate.eteruee.data.datastore.SettingsStore

/**
 * V4 迁移保留版本号推进。主题不再在迁移中强制覆盖，交由显示设置页管理。
 */
class PreferenceStoreV4Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 4
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        prefs[SettingsStore.VERSION] = 4
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}
