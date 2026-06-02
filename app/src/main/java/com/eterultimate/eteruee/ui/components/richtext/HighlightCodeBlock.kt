package com.eterultimate.eteruee.ui.components.richtext

import android.content.ClipData
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.eterultimate.eteruee.highlight.HighlightText
import com.eterultimate.eteruee.highlight.HighlightTextColorPalette
import com.eterultimate.eteruee.highlight.Highlighter
import com.eterultimate.eteruee.highlight.LocalHighlighter
import com.eterultimate.eteruee.highlight.buildHighlightText
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.ArrowUp01
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Download04
import me.rerere.hugeicons.stroke.Eye
import me.rerere.hugeicons.stroke.Play
import me.rerere.hugeicons.stroke.Cancel01
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.Screen
import com.eterultimate.eteruee.data.ai.tools.executeJavaScriptCode
import com.eterultimate.eteruee.data.ai.tools.executePythonScript
import com.eterultimate.eteruee.ui.context.LocalNavController
import com.eterultimate.eteruee.ui.context.LocalSettings
import com.eterultimate.eteruee.ui.context.Navigator
import com.eterultimate.eteruee.ui.modifier.onClick
import com.eterultimate.eteruee.ui.theme.AtomOneDarkPalette
import com.eterultimate.eteruee.ui.theme.AtomOneLightPalette
import com.eterultimate.eteruee.ui.theme.JetbrainsMono
import com.eterultimate.eteruee.ui.theme.LocalDarkMode
import com.eterultimate.eteruee.utils.base64Encode
import com.eterultimate.eteruee.utils.toDp
import kotlin.time.Clock

private const val COLLAPSE_LINES = 10

@Composable
fun HighlightCodeBlock(
    code: String,
    language: String,
    modifier: Modifier = Modifier,
    completeCodeBlock: Boolean = true,
    style: TextStyle? = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
) {
    val darkMode = LocalDarkMode.current
    val colorPalette = if (darkMode) AtomOneDarkPalette else AtomOneLightPalette
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val context = LocalContext.current
    val settings = LocalSettings.current
    val colorScheme = MaterialTheme.colorScheme
    val normalizedLanguage = remember(language) { language.trim().lowercase() }

    var isExpanded by remember(settings.displaySetting.codeBlockAutoCollapse) {
        mutableStateOf(!settings.displaySetting.codeBlockAutoCollapse)
    }
    val autoWrap = settings.displaySetting.codeBlockAutoWrap
    val showLineNumbers = settings.displaySetting.showLineNumbers

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(code.toByteArray())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    var showExecResult by remember { mutableStateOf(false) }
    var execResult by remember { mutableStateOf("") }
    var isExecuting by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            HighlightCodeActions(
                language = language,
                scope = scope,
                clipboardManager = clipboardManager,
                code = code,
                createDocumentLauncher = createDocumentLauncher,
                navController = navController,
                colorScheme = colorScheme,
                completeCodeBlock = completeCodeBlock,
                normalizedLanguage = normalizedLanguage,
                isExecuting = isExecuting,
                onExecute = {
                    isExecuting = true
                    showExecResult = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            val result = when (normalizedLanguage) {
                                "javascript", "js" -> executeJavaScriptCode(code)
                                "python", "py" -> executePythonScript(code)
                                else -> return@launch
                            }
                            val output = buildString {
                                result.result?.let { appendLine(it) }
                                if (result.logs.isNotEmpty()) {
                                    appendLine("--- Logs ---")
                                    result.logs.forEach { appendLine(it) }
                                }
                                result.error?.let {
                                    appendLine("--- Error ---")
                                    appendLine(it)
                                }
                            }.trim()
                            withContext(Dispatchers.Main) {
                                execResult = output.ifBlank { "No output" }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                execResult = e.message ?: e.javaClass.simpleName
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                isExecuting = false
                            }
                        }
                    }
                },
            )
        }
        Column(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
        ) {
            when {
                completeCodeBlock && normalizedLanguage == "mermaid" -> {
                    Mermaid(
                        code = code,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                else -> {
                    val textStyle = LocalTextStyle.current.merge(style)
                    val codeLines = remember(code) { code.lines() }
                    val collapsedCode = remember(codeLines) { codeLines.take(COLLAPSE_LINES).joinToString("\n") }
                    val displayCode = if (isExpanded) code else collapsedCode
                    val displayLines = remember(displayCode) { displayCode.lines() }

                    // 如果显示行号且自动换行，需要逐行渲染以保持对齐
                    when {
                        showLineNumbers && autoWrap -> {
                            CodeBlockWithLineNumbersWrapped(
                                displayLines = displayLines,
                                language = language,
                                textStyle = textStyle,
                                colorPalette = colorPalette,
                            )
                        }
                        else -> {
                            CodeBlockDefault(
                                displayCode = displayCode,
                                displayLines = displayLines,
                                language = language,
                                textStyle = textStyle,
                                colorPalette = colorPalette,
                                autoWrap = autoWrap,
                                showLineNumbers = showLineNumbers,
                                scrollState = scrollState,
                            )
                        }
                    }

                    // Execution result
                    if (showExecResult) {
                        Spacer(Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.code_block_result),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = HugeIcons.Cancel01,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .onClick { showExecResult = false }
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            if (isExecuting) {
                                Text(
                                    text = stringResource(id = R.string.code_block_executing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                SelectionContainer {
                                    Text(
                                        text = execResult,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = JetbrainsMono,
                                            fontSize = 11.sp,
                                            lineHeight = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    // 代码折叠按钮
                    if (settings.displaySetting.codeBlockAutoCollapse && codeLines.size > COLLAPSE_LINES) {
                        Box(
                            modifier = Modifier
                                .onClick {
                                    isExpanded = !isExpanded
                                }
                                .fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) HugeIcons.ArrowUp01 else HugeIcons.ArrowDown01,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(textStyle.fontSize.toDp())
                                )
                                Text(
                                    text = if (isExpanded) {
                                        stringResource(id = R.string.code_block_collapse)
                                    } else {
                                        stringResource(id = R.string.code_block_expand)
                                    },
                                    fontSize = textStyle.fontSize,
                                    lineHeight = textStyle.lineHeight,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CodeBlockWithLineNumbersWrapped(
    displayLines: List<String>,
    language: String,
    textStyle: TextStyle,
    colorPalette: HighlightTextColorPalette,
) {
    val lineNumberWidth = remember(displayLines.size) {
        displayLines.size.toString().length
    }
    SelectionContainer {
        Column {
            displayLines.forEachIndexed { index, line ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (index + 1).toString().padStart(lineNumberWidth, ' '),
                        fontSize = textStyle.fontSize,
                        lineHeight = textStyle.lineHeight,
                        fontFamily = JetbrainsMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        softWrap = false,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    HighlightText(
                        code = line,
                        language = language,
                        fontSize = textStyle.fontSize,
                        lineHeight = textStyle.lineHeight,
                        colors = colorPalette,
                        overflow = TextOverflow.Visible,
                        softWrap = true,
                        fontFamily = JetbrainsMono,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockDefault(
    displayCode: String,
    displayLines: List<String>,
    language: String,
    textStyle: TextStyle,
    colorPalette: HighlightTextColorPalette,
    autoWrap: Boolean,
    showLineNumbers: Boolean,
    scrollState: ScrollState,
) {
    Row(
        modifier = Modifier.then(
            if (autoWrap) {
                Modifier
            } else {
                Modifier.horizontalScroll(scrollState)
            }
        )
    ) {
        // 行号列
        if (showLineNumbers) {
            val lineNumberWidth = remember(displayLines.size) {
                displayLines.size.toString().length
            }
            Column(
                modifier = Modifier.padding(end = 8.dp)
            ) {
                displayLines.forEachIndexed { index, _ ->
                    Text(
                        text = (index + 1).toString().padStart(lineNumberWidth, ' '),
                        fontSize = textStyle.fontSize,
                        lineHeight = textStyle.lineHeight,
                        fontFamily = JetbrainsMono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        softWrap = false,
                    )
                }
            }
        }

        // 代码列
        SelectionContainer {
            HighlightText(
                code = displayCode,
                language = language,
                modifier = Modifier.animateContentSize(),
                fontSize = textStyle.fontSize,
                lineHeight = textStyle.lineHeight,
                colors = colorPalette,
                overflow = TextOverflow.Visible,
                softWrap = autoWrap,
                fontFamily = JetbrainsMono
            )
        }
    }
}

@Composable
private fun HighlightCodeActions(
    language: String,
    scope: CoroutineScope,
    clipboardManager: Clipboard,
    code: String,
    createDocumentLauncher: ManagedActivityResultLauncher<String, Uri?>,
    navController: Navigator,
    colorScheme: ColorScheme = MaterialTheme.colorScheme,
    completeCodeBlock: Boolean = true,
    normalizedLanguage: String = language.trim().lowercase(),
    isExecuting: Boolean = false,
    onExecute: () -> Unit = {},
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = language,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
                .copy(alpha = 0.5f),
        )
        Spacer(Modifier.weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconSize = 16.dp
            val iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

            Icon(
                imageVector = HugeIcons.Download04,
                contentDescription = stringResource(id = R.string.chat_page_save),
                tint = iconTint,
                modifier = Modifier
                    .clip(RoundedCornerShape(0.dp))
                    .onClick {
                        val extension = when (normalizedLanguage) {
                            "kotlin" -> "kt"
                            "java" -> "java"
                            "python" -> "py"
                            "javascript" -> "js"
                            "typescript" -> "ts"
                            "cpp", "c++" -> "cpp"
                            "c" -> "c"
                            "html" -> "html"
                            "css" -> "css"
                            "xml" -> "xml"
                            "json" -> "json"
                            "yaml", "yml" -> "yml"
                            "markdown", "md" -> "md"
                            "sql" -> "sql"
                            "sh", "bash" -> "sh"
                            "svg" -> "svg"
                            else -> "txt"
                        }
                        createDocumentLauncher.launch(
                            "code_${
                                Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            }.$extension"
                        )
                    }
                    .padding(4.dp)
                    .size(iconSize)
            )

            Icon(
                imageVector = HugeIcons.Copy01,
                contentDescription = stringResource(id = R.string.code_block_copy),
                tint = iconTint,
                modifier = Modifier
                    .clip(RoundedCornerShape(0.dp))
                    .onClick {
                        scope.launch {
                            clipboardManager.setClipEntry(ClipEntry(ClipData.newPlainText("code", code)))
                        }
                    }
                    .padding(4.dp)
                    .size(iconSize)
            )

            // Preview button for HTML, SVG, Markdown
            if (completeCodeBlock &&
                (normalizedLanguage == "html" || normalizedLanguage == "svg" ||
                    normalizedLanguage == "markdown" || normalizedLanguage == "md")
            ) {
                Icon(
                    imageVector = HugeIcons.Eye,
                    contentDescription = stringResource(id = R.string.code_block_preview),
                    tint = iconTint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp))
                        .onClick {
                            val content = when (normalizedLanguage) {
                                "svg" -> {
                                    """<!DOCTYPE html><html><body style="margin:0;display:flex;justify-content:center;align-items:center;min-height:100vh;">$code</body></html>"""
                                }
                                "markdown", "md" -> {
                                    buildMarkdownPreviewHtml(
                                        context = context,
                                        markdown = code,
                                        colorScheme = colorScheme
                                    )
                                }
                                else -> code
                            }
                            navController.navigate(Screen.WebView(content = content.base64Encode()))
                        }
                        .padding(4.dp)
                        .size(iconSize)
                )
            }

            // Execute button for JavaScript and Python
            if (completeCodeBlock &&
                (normalizedLanguage == "javascript" || normalizedLanguage == "js" ||
                    normalizedLanguage == "python" || normalizedLanguage == "py")
            ) {
                Icon(
                    imageVector = HugeIcons.Play,
                    contentDescription = stringResource(id = R.string.code_block_execute),
                    tint = if (isExecuting) MaterialTheme.colorScheme.primary else iconTint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp))
                        .onClick {
                            if (!isExecuting) onExecute()
                        }
                        .padding(4.dp)
                        .size(iconSize)
                )
            }
        }
    }
}

class HighlightCodeVisualTransformation(
    val language: String,
    val highlighter: Highlighter,
    val darkMode: Boolean
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val annotatedString = try {
            val colorPalette = if (darkMode) AtomOneDarkPalette else AtomOneLightPalette
            if (text.text.isEmpty()) {
                AnnotatedString("")
            } else {
                runBlocking {
                    val tokens = highlighter.highlight(text.text, language)
                    buildAnnotatedString {
                        tokens.forEach { token ->
                            buildHighlightText(token, colorPalette)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AnnotatedString(text.text)
        }

        return TransformedText(
            text = annotatedString,
            offsetMapping = OffsetMapping.Identity
        )
    }

    companion object {
        @Composable
        fun regex() = HighlightCodeVisualTransformation(
            language = "regex",
            highlighter = LocalHighlighter.current,
            darkMode = LocalDarkMode.current,
        )
    }
}
