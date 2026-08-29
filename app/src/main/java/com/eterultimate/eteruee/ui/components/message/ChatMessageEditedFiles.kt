package com.eterultimate.eteruee.ui.components.message

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.model.Assistant
import com.eterultimate.eteruee.workspace.WORKSPACE_MOUNT_PATH
import com.eterultimate.eteruee.workspace.WorkspaceSandboxManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.File02
import me.rerere.hugeicons.stroke.FileImport
import me.rerere.hugeicons.stroke.Share08
import org.koin.compose.koinInject
import java.io.File
import java.io.OutputStream

private const val DEFAULT_VISIBLE_COUNT = 3
private val WORKSPACE_FILE_TOOL_NAMES = setOf("workspace_write_file", "workspace_edit_file")

@OptIn(ExperimentalLayoutApi::class)
@Suppress("UNUSED_PARAMETER")
@Composable
internal fun EditedFilesList(
    parts: List<UIMessagePart>,
    assistant: Assistant?,
) {
    val editedFiles = remember(parts) {
        parts.filterIsInstance<UIMessagePart.Tool>()
            .filter { it.toolName in WORKSPACE_FILE_TOOL_NAMES && it.isExecuted }
            .mapNotNull { tool ->
                tool.inputAsJson().jsonObject["path"]?.jsonPrimitive?.contentOrNull
            }
            .mapNotNull { path ->
                val relativePath = resolveWorkspaceRelativePath(path) ?: return@mapNotNull null
                EditedWorkspaceFile(
                    displayPath = path,
                    relativePath = relativePath,
                    fileName = fileNameOf(path),
                )
            }
            .distinctBy { it.relativePath }
    }
    if (editedFiles.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val workspaceSandboxManager: WorkspaceSandboxManager = koinInject()

    var selectedFile by remember { mutableStateOf<EditedWorkspaceFile?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val visibleFiles = if (expanded) editedFiles else editedFiles.take(DEFAULT_VISIBLE_COUNT)
    val hasMore = editedFiles.size > DEFAULT_VISIBLE_COUNT

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri ->
        val file = selectedFile.also { selectedFile = null } ?: return@rememberLauncherForActivityResult
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val outputStream = context.contentResolver.openOutputStream(uri) ?: return@runCatching
                exportWorkspaceFile(workspaceSandboxManager, file, outputStream)
            }
        }
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        visibleFiles.forEach { file ->
            Surface(
                onClick = { selectedFile = file },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = HugeIcons.File02,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = file.fileName,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 200.dp),
                    )
                }
            }
        }
        if (hasMore && !expanded) {
            Surface(
                onClick = { expanded = true },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = "+${editedFiles.size - DEFAULT_VISIBLE_COUNT}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }

    selectedFile?.let { file ->
        ModalBottomSheet(
            onDismissRequest = { selectedFile = null },
            sheetState = rememberBottomSheetState(
                initialValue = SheetValue.Hidden,
                enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = file.fileName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Card(
                    onClick = {
                        selectedFile?.let {
                            exportLauncher.launch(it.fileName)
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = HugeIcons.FileImport,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp),
                        )
                        Text(
                            text = stringResource(R.string.common_export),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
                Card(
                    onClick = {
                        val selected = selectedFile ?: return@Card
                        selectedFile = null
                        scope.launch {
                            runCatching {
                                val dir = File(context.cacheDir, "workspace_share").apply { mkdirs() }
                                val cacheFile = copyWorkspaceFileToCache(workspaceSandboxManager, selected, dir)
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    cacheFile,
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/octet-stream"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, null))
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = HugeIcons.Share08,
                            contentDescription = null,
                            modifier = Modifier.padding(4.dp),
                        )
                        Text(
                            text = stringResource(R.string.common_share),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun exportWorkspaceFile(
    workspaceSandboxManager: WorkspaceSandboxManager,
    file: EditedWorkspaceFile,
    outputStream: OutputStream,
) = withContext(Dispatchers.IO) {
    val source = workspaceSandboxManager.resolveFilesPath(file.relativePath)
    require(source.isFile) { "Workspace file does not exist: ${file.displayPath}" }
    outputStream.use { output ->
        source.inputStream().use { input ->
            input.copyTo(output)
        }
    }
}

private suspend fun copyWorkspaceFileToCache(
    workspaceSandboxManager: WorkspaceSandboxManager,
    file: EditedWorkspaceFile,
    dir: File,
): File = withContext(Dispatchers.IO) {
    val source = workspaceSandboxManager.resolveFilesPath(file.relativePath)
    require(source.isFile) { "Workspace file does not exist: ${file.displayPath}" }
    val target = File(dir, file.fileName)
    source.inputStream().use { input ->
        target.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    target
}

private fun resolveWorkspaceRelativePath(path: String): String? {
    val normalized = path
        .replace('\\', '/')
        .trim()
        .trimEnd('/')
    if (normalized.isBlank() || normalized == WORKSPACE_MOUNT_PATH) return null
    return when {
        normalized.startsWith("$WORKSPACE_MOUNT_PATH/") ->
            normalized.removePrefix("$WORKSPACE_MOUNT_PATH/").trimStart('/').takeIf { it.isNotBlank() }

        normalized.startsWith("/") -> null

        else -> normalized.trimStart('/').takeIf { it.isNotBlank() }
    }
}

private fun fileNameOf(path: String): String =
    path.replace('\\', '/').trimEnd('/').substringAfterLast('/').ifBlank { "workspace-file" }

private data class EditedWorkspaceFile(
    val displayPath: String,
    val relativePath: String,
    val fileName: String,
)
