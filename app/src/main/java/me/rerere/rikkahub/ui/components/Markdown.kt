package me.rerere.rikkahub.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

private val flavour by lazy {
    GFMFlavourDescriptor()
}

private val parser by lazy {
    MarkdownParser(flavour)
}

@Composable
fun MarkdownBlock(
    content: String,
    modifier: Modifier = Modifier,
) {
    val astTree = remember(content) {
        parser.buildMarkdownTreeFromString(content).also {
            println(it)
        }
    }

    MarkdownAst(astTree, content, modifier)
}

@Composable
private fun MarkdownAst(astNode: ASTNode, content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        RenderMarkdownNode(astNode, content)
    }
}

@Composable
private fun RenderMarkdownNode(node: ASTNode, content: String) {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE -> {
            // 根节点，渲染所有子节点
            Column {
                node.children.forEach { child ->
                    RenderMarkdownNode(child, content)
                }
            }
        }

        // 标题
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6,
        MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 -> {
            // 获取标题级别
            val level = when (node.type) {
                MarkdownElementTypes.ATX_1, MarkdownElementTypes.SETEXT_1 -> 1
                MarkdownElementTypes.ATX_2, MarkdownElementTypes.SETEXT_2 -> 2
                MarkdownElementTypes.ATX_3 -> 3
                MarkdownElementTypes.ATX_4 -> 4
                MarkdownElementTypes.ATX_5 -> 5
                MarkdownElementTypes.ATX_6 -> 6
                else -> 1 // 默认值
            }

            val text = buildAnnotatedString {
                node.children.filter { it.type != MarkdownTokenTypes.ATX_HEADER && it.type != MarkdownTokenTypes.EOL }
                    .forEach { child ->
                        AppendMarkdownChildren(child, content)
                    }
            }

            when (level) {
                1 -> Text(
                    text = text,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                2 -> Text(
                    text = text,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                else -> Text(
                    text = text,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // 段落
        MarkdownElementTypes.PARAGRAPH -> {
            val text = buildAnnotatedString {
                node.children.forEach { child ->
                    AppendMarkdownChildren(child, content)
                }
            }

            if (text.isNotEmpty()) {
                val uriHandler = LocalUriHandler.current
                ClickableText(
                    text = text,
                    onClick = { offset ->
                        text.getStringAnnotations("URL", offset, offset).firstOrNull()
                            ?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // 水平规则
        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        // 代码块
        MarkdownElementTypes.CODE_FENCE -> {
            val language = node.children
                .find { it.type == MarkdownTokenTypes.FENCE_LANG }
                ?.getTextInNode(content) ?: ""

            val codeText = node.children
                .filter { it.type != MarkdownTokenTypes.FENCE_LANG && it.type != MarkdownTokenTypes.CODE_FENCE_START && it.type != MarkdownTokenTypes.CODE_FENCE_END }
                .joinToString("") { it.getTextInNode(content) }
                .trim()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                if (language.isNotEmpty()) {
                    Text(
                        text = language.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }

                Text(
                    text = codeText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }

        MarkdownElementTypes.CODE_BLOCK -> {
            val codeText = node.children
                .joinToString("") { it.getTextInNode(content) }
                .trim()

            Text(
                text = codeText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            )
        }

        // 无序列表
        MarkdownElementTypes.UNORDERED_LIST -> {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                node.children.forEach { child ->
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        Column(modifier = Modifier.padding(vertical = 2.dp)) {
                            Row {
                                // 检查是否有复选框
                                val hasCheckbox = child.children.any {
                                    it.type == GFMTokenTypes.CHECK_BOX
                                }

                                if (hasCheckbox) {
                                    val checkboxNode =
                                        child.children.find { it.type == GFMTokenTypes.CHECK_BOX }
                                    val isChecked =
                                        checkboxNode?.getTextInNode(content)?.contains("x") ?: false

                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null // 只读模式
                                    )

                                    // 渲染列表项内容（排除复选框）
                                    val itemText = buildAnnotatedString {
                                        child.children.filter { it.type != GFMTokenTypes.CHECK_BOX }
                                            .forEach { itemChild ->
                                                AppendMarkdownChildren(itemChild, content)
                                            }
                                    }
                                    Text(text = itemText)
                                } else {
                                    // 正常列表项
                                    Text(text = "• ", fontWeight = FontWeight.Bold)

                                    val itemText = buildAnnotatedString {
                                        child.children.forEach { itemChild ->
                                            AppendMarkdownChildren(itemChild, content)
                                        }
                                    }
                                    Text(text = itemText)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 有序列表
        MarkdownElementTypes.ORDERED_LIST -> {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                node.children
                    .filter { it.type == MarkdownElementTypes.LIST_ITEM }
                    .forEachIndexed { index, child ->
                        if (child.type == MarkdownElementTypes.LIST_ITEM) {
                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                Row {
                                    // 检查是否有复选框
                                    val hasCheckbox = child.children.any {
                                        it.type == GFMTokenTypes.CHECK_BOX
                                    }

                                    if (hasCheckbox) {
                                        val checkboxNode =
                                            child.children.find { it.type == GFMTokenTypes.CHECK_BOX }
                                        val isChecked =
                                            checkboxNode?.getTextInNode(content)?.contains("x")
                                                ?: false

                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = null // 只读模式
                                        )

                                        // 渲染列表项内容（排除复选框）
                                        val itemText = buildAnnotatedString {
                                            child.children.filter { it.type != GFMTokenTypes.CHECK_BOX }
                                                .forEach { itemChild ->
                                                    AppendMarkdownChildren(itemChild, content)
                                                }
                                        }
                                        Text(text = itemText)
                                    } else {
                                        // 正常列表项
                                        Text(text = "${index + 1}. ")

                                        val itemText = buildAnnotatedString {
                                            child.children.forEach { itemChild ->
                                                AppendMarkdownChildren(itemChild, content)
                                            }
                                        }
                                        Text(text = itemText)
                                    }
                                }
                            }
                        }
                    }
            }
        }

        // 块引用
        MarkdownElementTypes.BLOCK_QUOTE -> {
            val quoteText = buildAnnotatedString {
                node.children.forEach { child ->
                    AppendMarkdownChildren(child, content)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                        .width(4.dp)
                        .fillMaxHeight()
                )

                Text(
                    text = quoteText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic
                    ),
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }

        // GFM表格
        GFMElementTypes.TABLE -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // 表格头部
                val headerRow = node.children.find { it.type == GFMElementTypes.HEADER }
                headerRow?.let {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        it.children.filter { cell -> cell.type == GFMTokenTypes.CELL }
                            .forEachIndexed { _, cell ->
                                val cellText = buildAnnotatedString {
                                    cell.children.forEach { cellChild ->
                                        AppendMarkdownChildren(cellChild, content)
                                    }
                                }

                                Text(
                                    text = cellText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                )
                            }
                    }

                    Divider()
                }

                // 表格行
                node.children.filter { it.type == GFMElementTypes.ROW }.forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.children.filter { cell -> cell.type == GFMTokenTypes.CELL }
                            .forEach { cell ->
                                val cellText = buildAnnotatedString {
                                    cell.children.forEach { cellChild ->
                                        AppendMarkdownChildren(cellChild, content)
                                    }
                                }

                                Text(
                                    text = cellText,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                )
                            }
                    }
                    Divider(thickness = 0.5.dp)
                }
            }
        }

        // 数学公式
        GFMElementTypes.INLINE_MATH -> {
            val mathText = node.children
                .filter { it.type != GFMTokenTypes.DOLLAR }
                .joinToString("") { it.getTextInNode(content) }

            Text(
                text = "[$mathText]",
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 2.dp)
            )
        }

        GFMElementTypes.BLOCK_MATH -> {
            val mathText = node.children
                .filter { it.type != GFMTokenTypes.DOLLAR }
                .joinToString("") { it.getTextInNode(content) }
                .trim()

            Text(
                text = mathText,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(8.dp)
            )
        }

        // HTML块
        MarkdownElementTypes.HTML_BLOCK -> {
            Text(
                text = "HTML CONTENT (NOT RENDERED)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        // 递归处理子节点
        else -> {
            node.children.forEach { child ->
                RenderMarkdownNode(child, content)
            }
        }
    }
}

// 辅助函数，用于构建带样式的文本
@Composable
private fun AnnotatedString.Builder.AppendMarkdownChildren(node: ASTNode, content: String) {
    when (node.type) {
        // 内联元素
        MarkdownTokenTypes.TEXT -> append(node.getTextInNode(content))

        MarkdownElementTypes.EMPH -> {
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            node.children.forEach { child -> AppendMarkdownChildren(child, content) }
            pop()
        }

        MarkdownElementTypes.STRONG -> {
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            node.children.forEach { child -> AppendMarkdownChildren(child, content) }
            pop()
        }

        // 删除线
        GFMElementTypes.STRIKETHROUGH -> {
            pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
            node.children.forEach { child -> AppendMarkdownChildren(child, content) }
            pop()
        }

        // 内联数学公式
        GFMElementTypes.INLINE_MATH -> {
            pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
            append(" ")
            node.children
                .filter { it.type != GFMTokenTypes.DOLLAR }
                .forEach { append(it.getTextInNode(content)) }
            append(" ")
            pop()
        }

        MarkdownElementTypes.CODE_SPAN -> {
            pushStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            node.children
                .filter { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
                .forEach { append(it.getTextInNode(content)) }
            pop()
        }

        MarkdownElementTypes.LINK_DEFINITION, MarkdownElementTypes.INLINE_LINK -> {
            // 提取链接文本和URL
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                ?.let { linkTextNode ->
                    linkTextNode.children.filter { it.type == MarkdownTokenTypes.TEXT }
                        .joinToString("") { it.getTextInNode(content) }
                } ?: "链接"

            val linkDestination =
                node.children.find { it.type == MarkdownElementTypes.LINK_DESTINATION }
                    ?.getTextInNode(content)?.trim('(', ')', '<', '>') ?: "#"

            pushStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            )
            append(linkText)
            addStringAnnotation(
                "URL",
                linkDestination.toString(),
                start = length - linkText.length,
                end = length
            )
            pop()
        }

        MarkdownElementTypes.FULL_REFERENCE_LINK, MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                ?.let { linkTextNode ->
                    linkTextNode.children.filter { it.type == MarkdownTokenTypes.TEXT }
                        .joinToString("") { it.getTextInNode(content) }
                } ?: "链接"

            pushStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            )
            append(linkText)
            // 这里应该查找引用链接的实际URL，但简化处理
            addStringAnnotation("URL", "#ref", start = length - linkText.length, end = length)
            pop()
        }

        MarkdownElementTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK -> {
            val url = node.getTextInNode(content).trim('<', '>')
            pushStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            )
            append(url)
            addStringAnnotation("URL", url.toString(), start = length - url.length, end = length)
            pop()
        }

        MarkdownElementTypes.IMAGE -> {
            val altText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }
                ?.let { linkTextNode ->
                    linkTextNode.children.filter { it.type == MarkdownTokenTypes.TEXT }
                        .joinToString("") { it.getTextInNode(content) }
                } ?: "图片"

            append("[图片: $altText]")
        }

        MarkdownTokenTypes.HARD_LINE_BREAK -> {
            append("\n")
        }

        // 递归处理其他内联元素
        else -> {
            node.children.forEach { child -> AppendMarkdownChildren(child, content) }
        }
    }
}