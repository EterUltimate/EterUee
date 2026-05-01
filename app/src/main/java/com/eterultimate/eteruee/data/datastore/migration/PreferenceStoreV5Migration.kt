package com.eterultimate.eteruee.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.eterultimate.eteruee.data.datastore.SettingsStore

/**
 * V5 杩佺Щ锛氬己鍒跺皢鎵€鏈夌敤鎴蜂富棰樻洿鏂颁负 Cyberpunk
 * 
 * 鍘熷洜锛氶」鐩凡瀹屽叏閲嶆瀯涓哄崟涓€ Cyberpunk 涓婚绯荤粺锛?
 * 闇€瑕佺‘淇濇墍鏈夌敤鎴凤紙鍖呮嫭涔嬪墠宸插畬鎴?V4 杩佺Щ鐨勭敤鎴凤級閮戒娇鐢ㄦ纭殑涓婚銆?
 */
class PreferenceStoreV5Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 5
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        
        // 寮哄埗鍏抽棴鍔ㄦ€侀鑹诧紝浣跨敤 Cyberpunk 涓婚
        prefs[SettingsStore.DYNAMIC_COLOR] = false
        
        // 寮哄埗璁剧疆涓婚涓?Cyberpunk锛堣鐩栦换浣曟棫鐨勪富棰?ID锛?
        prefs[SettingsStore.THEME_ID] = "cyberpunk"
        
        // 鏇存柊鐗堟湰鍙?
        prefs[SettingsStore.VERSION] = 5
        
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}

