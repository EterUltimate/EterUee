package com.eterultimate.eteruee.shared

actual val currentPlatformFamily: PlatformFamily
    get() {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> PlatformFamily.WINDOWS
            osName.contains("mac") || osName.contains("darwin") -> PlatformFamily.MACOS
            osName.contains("linux") -> PlatformFamily.LINUX
            else -> PlatformFamily.LINUX
        }
    }
