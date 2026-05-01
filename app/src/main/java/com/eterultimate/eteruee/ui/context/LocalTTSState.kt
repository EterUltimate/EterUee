package com.eterultimate.eteruee.ui.context

import androidx.compose.runtime.compositionLocalOf
import com.eterultimate.eteruee.ui.hooks.CustomTtsState

val LocalTTSState = compositionLocalOf<CustomTtsState> { error("Not provided yet") }

