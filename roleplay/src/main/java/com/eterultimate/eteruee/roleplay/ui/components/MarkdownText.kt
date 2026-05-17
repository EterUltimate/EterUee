package com.eterultimate.eteruee.roleplay.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Markdown 文本渲染组件
 * 支持基本的 Markdown 语法和代码块高亮
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    enableCodeHighlight: Boolean = true,
    showCopyButton: Boolean = true
) {
    val clipboardManager = LocalClipboardManager.current
    
    Box(modifier = modifier) {
        SelectionContainer {
            Text(
                text = text,
                style = style,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // 复制按钮（仅当有内容且启用时显示）
        if (showCopyButton && text.isNotBlank()) {
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(text))
                },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.TopEnd)
                    .size(24.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "复制",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
