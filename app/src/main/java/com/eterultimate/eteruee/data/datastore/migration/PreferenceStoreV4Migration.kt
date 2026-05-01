package com.eterultimate.eteruee.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import com.eterultimate.eteruee.data.datastore.SettingsStore

/**
 * V4 杩佺Щ锛氬己鍒跺叧闂姩鎬侀鑹诧紝鍚敤 Cyberpunk 涓婚
 * 
 * 鍘熷洜锛氶」鐩凡閲嶆瀯涓哄彧淇濈暀 Cyberpunk 涓婚锛屼絾涔嬪墠淇濆瓨鐨勭敤鎴疯缃腑
 * dynamicColor 鍙兘涓?true锛屽鑷翠娇鐢ㄧ郴缁熷姩鎬侀鑹茶€岄潪 Cyberpunk 涓婚銆?
 */
class PreferenceStoreV4Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 4
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        
        // 寮哄埗鍏抽棴鍔ㄦ€侀鑹?
        prefs[SettingsStore.DYNAMIC_COLOR] = false
        
        // 寮哄埗璁剧疆涓婚涓?Cyberpunk锛堥」鐩凡閲嶆瀯涓哄崟涓€涓婚锛?
        prefs[SettingsStore.THEME_ID] = "cyberpunk"
        
        // 鏇存柊鐗堟湰鍙?
        prefs[SettingsStore.VERSION] = 4
        
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}

