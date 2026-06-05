package com.eterultimate.eteruee.runtime

import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
import org.junit.Assume.assumeNoException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeRuntimeTest {
    @Test
    fun deleteDirectoryTreeRemovesNestedFiles() {
        val root = Files.createTempDirectory("eteruee-native-runtime-").toFile()
        val nested = File(root, "a/b/c").apply { mkdirs() }
        File(nested, "payload.txt").writeText("payload")

        assertTrue(NativeRuntime.deleteDirectoryTree(root))
        assertFalse(root.exists())
    }

    @Test
    fun clearDirectoryRemovesNestedFilesAndKeepsRoot() {
        val root = Files.createTempDirectory("eteruee-native-runtime-").toFile()
        val nested = File(root, "a/b/c").apply { mkdirs() }
        File(nested, "payload.txt").writeText("payload")

        assertTrue(NativeRuntime.clearDirectory(root))
        assertTrue(root.isDirectory)
        assertFalse(File(root, "a").exists())
    }

    @Test
    fun clearDirectoryCreatesMissingRoot() {
        val parent = Files.createTempDirectory("eteruee-native-runtime-").toFile()
        val root = File(parent, "missing-temp")

        assertTrue(NativeRuntime.clearDirectory(root))
        assertTrue(root.isDirectory)

        parent.deleteRecursively()
    }

    @Test
    fun clearDirectoryDeletesSymlinkWithoutClearingTarget() {
        val parent = Files.createTempDirectory("eteruee-native-runtime-")
        val target = Files.createDirectory(parent.resolve("target"))
        Files.writeString(target.resolve("payload.txt"), "payload")
        val link = parent.resolve("temp-link")

        try {
            Files.createSymbolicLink(link, target)
        } catch (e: Exception) {
            assumeNoException(e)
        }

        assertTrue(NativeRuntime.clearDirectory(link.toFile()))
        assertTrue(Files.isDirectory(link))
        assertFalse(Files.isSymbolicLink(link))
        assertTrue(Files.exists(target.resolve("payload.txt")))

        parent.toFile().deleteRecursively()
    }

    @Test
    fun isTcpPortAvailableReflectsBoundPort() {
        val server = ServerSocket(0)
        val port = server.localPort

        try {
            assertFalse(NativeRuntime.isTcpPortAvailable(port))
        } finally {
            server.close()
        }

        assertTrue(NativeRuntime.isTcpPortAvailable(port))
    }
}
