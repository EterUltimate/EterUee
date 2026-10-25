package com.eterultimate.eteruee.ui.pages.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.eterultimate.eteruee.data.datastore.Settings
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.ai.mcp.McpManager

class SettingVM(
    private val settingsStore: SettingsStore,
    private val mcpManager: McpManager
) :
    ViewModel() {
    val settings: StateFlow<Settings> = settingsStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, Settings(init = true, providers = emptyList()))

    // 基于最新设置做转换后写回，避免 UI 手里的快照过期时覆盖并发更新（rikkahub 3462d9ee1）
    fun updateSettings(transform: (Settings) -> Settings) {
        viewModelScope.launch {
            settingsStore.update(transform)
        }
    }
}

