package com.eterultimate.eteruee.runtime

import java.io.File
import java.net.ServerSocket

object NativeRuntime {
    private val nativeLoaded: Boolean = isAndroidRuntime() && runCatching {
        System.loadLibrary("eteruee_runtime")
        true
    }.getOrDefault(false)

    private external fun nativeDeleteTree(path: String): Int

    private external fun nativeClearDirectory(path: String): Int

    private external fun nativeIsTcpPortAvailable(port: Int): Boolean

    fun deleteDirectoryTree(directory: File): Boolean {
        if (!directory.exists()) return true
        if (!nativeLoaded) return directory.deleteRecursively()

        val nativeDeleted = runCatching {
            nativeDeleteTree(directory.absolutePath) == 0 || !directory.exists()
        }.getOrDefault(false)

        return nativeDeleted || directory.deleteRecursively()
    }

    fun clearDirectory(directory: File): Boolean {
        if (nativeLoaded) {
            val nativeCleared = runCatching {
                nativeClearDirectory(directory.absolutePath) == 0 && directory.isDirectory
            }.getOrDefault(false)
            if (nativeCleared) return true
        }
        return clearDirectoryJvm(directory)
    }

    fun isTcpPortAvailable(port: Int): Boolean {
        if (port !in 1..65535) return false
        if (nativeLoaded) {
            val nativeAvailable = runCatching {
                nativeIsTcpPortAvailable(port)
            }.getOrNull()
            if (nativeAvailable != null) return nativeAvailable
        }
        return isTcpPortAvailableJvm(port)
    }

    private fun isTcpPortAvailableJvm(port: Int): Boolean {
        return try {
            ServerSocket(port).use { true }
        } catch (_: Exception) {
            false
        }
    }

    private fun clearDirectoryJvm(directory: File): Boolean {
        if (directory.exists() && !directory.isDirectory) {
            if (!directory.delete()) return false
        }
        if (!directory.exists() && !directory.mkdirs()) return false

        var success = true
        directory.listFiles()?.forEach { child ->
            success = child.deleteRecursively() && success
        }
        return success && directory.isDirectory
    }

    private fun isAndroidRuntime(): Boolean {
        return System.getProperty("java.vm.name")
            ?.contains("Dalvik", ignoreCase = true) == true
    }
}
