package com.eterultimate.eteruee.workspace

import android.content.Context
import java.io.File

const val DEFAULT_WORKSPACE_ROOT = "default"
const val WORKSPACE_MOUNT_PATH = "/workspace"
private val WORKSPACE_ROOT_REGEX = Regex("[A-Za-z0-9._-]+")

class WorkspaceSandboxManager internal constructor(
    private val baseDir: File,
) {
    constructor(context: Context) : this(File(context.filesDir, "workspace"))

    init {
        ensureWorkspace(DEFAULT_WORKSPACE_ROOT)
    }

    fun defaultWorkspace(): WorkspaceSandbox = ensureWorkspace(DEFAULT_WORKSPACE_ROOT)

    fun ensureWorkspace(root: String = DEFAULT_WORKSPACE_ROOT): WorkspaceSandbox {
        require(root.matches(WORKSPACE_ROOT_REGEX)) {
            "Invalid workspace root name: $root"
        }
        val workspaceDir = File(baseDir, root)
        val filesDir = File(workspaceDir, "files").apply { mkdirs() }
        val linuxDir = File(workspaceDir, "linux").apply { mkdirs() }
        val tempDir = File(workspaceDir, "tmp").apply { mkdirs() }
        return WorkspaceSandbox(
            root = root,
            workspaceDir = workspaceDir,
            filesDir = filesDir,
            linuxDir = linuxDir,
            tempDir = tempDir,
        )
    }

    fun resolveFilesPath(
        path: String?,
        root: String = DEFAULT_WORKSPACE_ROOT,
    ): File {
        val sandbox = ensureWorkspace(root)
        return resolveInside(sandbox.filesDir, path)
    }

    fun cleanupTemp(root: String = DEFAULT_WORKSPACE_ROOT) {
        val sandbox = ensureWorkspace(root)
        sandbox.tempDir.deleteRecursively()
        sandbox.tempDir.mkdirs()
    }

    private fun resolveInside(root: File, path: String?): File {
        val normalized = path
            ?.replace('\\', '/')
            ?.trim()
            ?.trimStart('/')
            ?.takeIf { it.isNotBlank() }
            ?: "."
        require(!normalized.contains('\u0000')) { "Path contains invalid character" }

        val rootFile = root.canonicalFile
        val target = if (normalized == ".") {
            rootFile
        } else {
            File(rootFile, normalized).canonicalFile
        }
        require(target.path == rootFile.path || target.path.startsWith(rootFile.path + File.separator)) {
            "Path escapes workspace sandbox: $path"
        }
        return target
    }
}

data class WorkspaceSandbox(
    val root: String,
    val workspaceDir: File,
    val filesDir: File,
    val linuxDir: File,
    val tempDir: File,
)
