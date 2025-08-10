package me.rerere.common.android

import android.content.Context
import java.io.File

val Context.appTempFolder: File
    get() {
        val dir = File(cacheDir, "temp")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
