package com.eterultimate.eteruee.workspace

import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSandboxManagerTest {
    @Test
    fun defaultWorkspaceCreatesWorkspaceSandboxLayout() {
        val baseDir = Files.createTempDirectory("eteruee-workspace-test").toFile()
        try {
            val manager = WorkspaceSandboxManager(baseDir)
            val sandbox = manager.defaultWorkspace()

            assertEquals(DEFAULT_WORKSPACE_ROOT, sandbox.root)
            assertTrue(sandbox.filesDir.path.endsWith("${File.separator}default${File.separator}files"))
            assertTrue(sandbox.linuxDir.path.endsWith("${File.separator}default${File.separator}linux"))
            assertTrue(sandbox.tempDir.path.endsWith("${File.separator}default${File.separator}tmp"))
        } finally {
            baseDir.deleteRecursively()
        }
    }

    @Test
    fun resolveFilesPathRejectsEscapingSandbox() {
        val baseDir = Files.createTempDirectory("eteruee-workspace-test").toFile()
        try {
            val manager = WorkspaceSandboxManager(baseDir)
            val error = runCatching {
                manager.resolveFilesPath("../outside")
            }.exceptionOrNull()

            assertTrue(error is IllegalArgumentException)
        } finally {
            baseDir.deleteRecursively()
        }
    }
}
