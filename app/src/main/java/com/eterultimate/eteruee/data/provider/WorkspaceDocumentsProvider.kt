package com.eterultimate.eteruee.data.provider

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.provider.DocumentsContract.Root
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.workspace.DEFAULT_WORKSPACE_ROOT
import com.eterultimate.eteruee.workspace.WorkspaceSandboxManager
import org.koin.core.context.GlobalContext
import java.io.File

/**
 * Exposes the managed workspace files directory through Android's Storage Access Framework.
 */
class WorkspaceDocumentsProvider : DocumentsProvider() {
    private fun manager(): WorkspaceSandboxManager = GlobalContext.get().get()

    override fun onCreate(): Boolean = true

    override fun queryRoots(projection: Array<String>?): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION)
        val ctx = context ?: return cursor
        cursor.newRow().apply {
            add(Root.COLUMN_ROOT_ID, ROOT_ID)
            add(Root.COLUMN_DOCUMENT_ID, ROOT_DOC_ID)
            add(Root.COLUMN_TITLE, ctx.getString(R.string.app_name))
            add(Root.COLUMN_FLAGS, Root.FLAG_LOCAL_ONLY or Root.FLAG_SUPPORTS_IS_CHILD)
            add(Root.COLUMN_ICON, R.mipmap.ic_launcher)
            add(Root.COLUMN_MIME_TYPES, "*/*")
        }
        return cursor
    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val target = parseDocId(documentId)
        if (target.isRoot) {
            cursor.newRow().apply {
                add(Document.COLUMN_DOCUMENT_ID, ROOT_DOC_ID)
                add(Document.COLUMN_DISPLAY_NAME, context?.getString(R.string.app_name))
                add(Document.COLUMN_MIME_TYPE, Document.MIME_TYPE_DIR)
                add(Document.COLUMN_FLAGS, 0)
                add(Document.COLUMN_SIZE, null)
                add(Document.COLUMN_LAST_MODIFIED, null)
            }
        } else {
            addFileRow(cursor, resolveFile(target.relPath))
        }
        return cursor
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION)
        val parent = parseDocId(parentDocumentId)
        if (parent.isRoot) {
            addFileRow(cursor, workspaceFilesDir())
        } else {
            val dir = resolveFile(parent.relPath)
            if (dir.isDirectory) {
                dir.listFiles()
                    .orEmpty()
                    .filter { !it.name.startsWith(".l2s.") }
                    .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                    .forEach { addFileRow(cursor, it) }
            }
        }
        return cursor
    }

    override fun openDocument(
        documentId: String,
        mode: String,
        signal: CancellationSignal?,
    ): ParcelFileDescriptor {
        val target = parseDocId(documentId)
        require(!target.isRoot) { "Cannot open root as a document" }
        return ParcelFileDescriptor.open(resolveFile(target.relPath), ParcelFileDescriptor.parseMode(mode))
    }

    override fun createDocument(
        parentDocumentId: String,
        mimeType: String,
        displayName: String,
    ): String {
        val parent = parseDocId(parentDocumentId)
        require(!parent.isRoot) { "Cannot create document at root" }
        val parentDir = resolveFile(parent.relPath)
        require(parentDir.isDirectory) { "Parent is not a directory" }
        val target = uniqueChild(parentDir, displayName)
        if (mimeType == Document.MIME_TYPE_DIR) {
            require(target.mkdir()) { "Failed to create directory: $displayName" }
        } else {
            require(target.createNewFile()) { "Failed to create file: $displayName" }
        }
        notifyChange(parentDocumentId)
        return buildDocId(relPathOf(target))
    }

    override fun deleteDocument(documentId: String) {
        val target = parseDocId(documentId)
        require(!target.isRoot && target.relPath.isNotEmpty()) { "Cannot delete this document" }
        val file = resolveFile(target.relPath)
        val ok = if (file.isDirectory) file.deleteRecursively() else file.delete()
        require(ok) { "Failed to delete: $documentId" }
        notifyChange(buildDocId(target.relPath.substringBeforeLast('/', "")))
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val target = parseDocId(documentId)
        require(!target.isRoot && target.relPath.isNotEmpty()) { "Cannot rename this document" }
        val file = resolveFile(target.relPath)
        val dest = File(file.parentFile, displayName.replace('/', '_'))
        require(!dest.exists()) { "Target already exists: $displayName" }
        require(file.renameTo(dest)) { "Failed to rename: $documentId" }
        notifyChange(buildDocId(target.relPath.substringBeforeLast('/', "")))
        return buildDocId(relPathOf(dest))
    }

    override fun getDocumentType(documentId: String): String {
        val target = parseDocId(documentId)
        if (target.isRoot) return Document.MIME_TYPE_DIR
        return mimeOf(resolveFile(target.relPath))
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        val parent = parseDocId(parentDocumentId)
        val child = parseDocId(documentId)
        if (child.isRoot) return false
        if (parent.isRoot) return true
        if (parent.relPath.isEmpty()) return true
        return child.relPath == parent.relPath || child.relPath.startsWith(parent.relPath + "/")
    }

    private fun addFileRow(cursor: MatrixCursor, file: File) {
        val relPath = relPathOf(file)
        val isDir = file.isDirectory
        val flags = when {
            relPath.isEmpty() -> Document.FLAG_DIR_SUPPORTS_CREATE
            isDir -> Document.FLAG_DIR_SUPPORTS_CREATE or
                Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME
            else -> Document.FLAG_SUPPORTS_WRITE or
                Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME
        }
        cursor.newRow().apply {
            add(Document.COLUMN_DOCUMENT_ID, buildDocId(relPath))
            add(
                Document.COLUMN_DISPLAY_NAME,
                if (relPath.isEmpty()) DEFAULT_WORKSPACE_ROOT else file.name
            )
            add(Document.COLUMN_MIME_TYPE, if (isDir) Document.MIME_TYPE_DIR else mimeOf(file))
            add(Document.COLUMN_FLAGS, flags)
            add(Document.COLUMN_SIZE, if (isDir) null else file.length())
            add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
        }
    }

    private fun mimeOf(file: File): String {
        if (file.isDirectory) return Document.MIME_TYPE_DIR
        val ext = file.extension.lowercase()
        return ext.takeIf { it.isNotEmpty() }
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it) }
            ?: "application/octet-stream"
    }

    private fun uniqueChild(parent: File, name: String): File {
        val safe = name.replace('/', '_').ifBlank { "untitled" }
        var candidate = File(parent, safe)
        if (!candidate.exists()) return candidate
        val stem = candidate.nameWithoutExtension
        val ext = candidate.extension.let { if (it.isNotEmpty()) ".$it" else "" }
        var n = 1
        do {
            candidate = File(parent, "$stem ($n)$ext")
            n++
        } while (candidate.exists())
        return candidate
    }

    private fun workspaceFilesDir(): File = manager().defaultWorkspace().filesDir.canonicalFile

    private fun relPathOf(file: File): String =
        file.canonicalFile.relativeTo(workspaceFilesDir()).path.replace(File.separatorChar, '/')

    private fun resolveFile(relPath: String): File {
        val base = workspaceFilesDir()
        base.mkdirs()
        val normalized = relPath.trim().trimStart('/')
        require(!normalized.contains('\u0000')) { "Path contains invalid character" }
        if (normalized.isEmpty()) return base
        val target = File(base, normalized).canonicalFile
        require(target.path == base.path || target.path.startsWith(base.path + File.separator)) {
            "Path escapes workspace root: $relPath"
        }
        return target
    }

    private fun parseDocId(documentId: String): DocId {
        if (documentId == ROOT_DOC_ID) return DocId(isRoot = true, relPath = "")
        require(documentId.startsWith(DOC_PREFIX)) { "Invalid documentId: $documentId" }
        return DocId(isRoot = false, relPath = documentId.removePrefix(DOC_PREFIX))
    }

    private fun buildDocId(relPath: String): String =
        if (relPath.isEmpty()) DOC_PREFIX else "$DOC_PREFIX$relPath"

    private fun notifyChange(parentDocumentId: String) {
        val ctx = context ?: return
        val uri = DocumentsContract.buildChildDocumentsUri(
            ctx.packageName + ".documents",
            parentDocumentId,
        )
        ctx.contentResolver.notifyChange(uri, null)
    }

    private data class DocId(
        val isRoot: Boolean,
        val relPath: String,
    )

    companion object {
        private const val ROOT_ID = "eteruee_workspace"
        private const val ROOT_DOC_ID = "root"
        private const val DOC_PREFIX = "ws/"

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            Root.COLUMN_ROOT_ID,
            Root.COLUMN_FLAGS,
            Root.COLUMN_TITLE,
            Root.COLUMN_DOCUMENT_ID,
            Root.COLUMN_ICON,
            Root.COLUMN_MIME_TYPES,
        )

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_FLAGS,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
        )
    }
}
