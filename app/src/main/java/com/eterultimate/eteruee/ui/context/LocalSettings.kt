package com.eterultimate.eteruee.ui.context

import androidx.compose.runtime.staticCompositionLocalOf
import com.eterultimate.eteruee.data.datastore.Settings

val LocalSettings = staticCompositionLocalOf<Settings> {
    error("No SettingsStore provided")
}

