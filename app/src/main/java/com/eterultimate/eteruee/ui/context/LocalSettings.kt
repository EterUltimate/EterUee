package com.eterultimate.eteruee.ui.context

import androidx.compose.runtime.compositionLocalOf
import com.eterultimate.eteruee.data.datastore.Settings

// compositionLocalOf：设置变化只重组读取了 LocalSettings 的节点，
// staticCompositionLocalOf 会让任何一次写设置都重组整棵界面树（rikkahub d1c81bf99）
val LocalSettings = compositionLocalOf<Settings> {
    error("No SettingsStore provided")
}
