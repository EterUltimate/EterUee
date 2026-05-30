package com.eterultimate.eteruee.runtime

import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
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
