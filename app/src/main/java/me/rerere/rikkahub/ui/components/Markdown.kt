package me.rerere.rikkahub.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import coil3.compose.AsyncImage
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

private val flavour by lazy {
    GFMFlavourDescriptor()
}

private val parser by lazy {
    MarkdownParser(flavour)
}

// 预处理markdown内容，将行内公式和块级公式转换为LaTeX格式
// 替换行内公式 \( ... \) 到 $ ... $
// 替换块级公式 \[ ... \] 到 $$ ... $$
private fun preProcess(content: String): String {
    // 替换行内公式 \( ... \) 到 $ ... $
    var result = content.replace(Regex("\\\\\\((.+?)\\\\\\)")) { matchResult ->
        "$" + matchResult.groupValues[1] + "$"
    }
    
    // 替换块级公式 \[ ... \] 到 $$ ... $$
    result = result.replace(Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL)) { matchResult ->
        "$$" + matchResult.groupValues[1] + "$$"
    }
    
    return result
}

@Composable
fun MarkdownBlock(
    content: String,
    modifier: Modifier = Modifier,
) {
    val preprocessed = remember(content) { preProcess(content) }
    val astTree = remember(preprocessed) {
        parser.buildMarkdownTreeFromString(preprocessed).also {
            // dumpAst(it)
        }
    }

    MarkdownAst(astTree, preprocessed, modifier)
}

// for debug
private fun dumpAst(node: ASTNode, indent: String = "") {
    println("$indent${node.type}")
    node.children.forEach {
        dumpAst(it, "$indent  ")
    }
}

@Composable
private fun MarkdownAst(astNode: ASTNode, content: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
    ) {
        astNode.children.forEach { child ->
            MarkdownNode(child, content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkdownNode(node: ASTNode, content: String, modifier: Modifier = Modifier) {
    when (node.type) {
        // 文件根节点
        MarkdownElementTypes.MARKDOWN_FILE -> {
            node.children.forEach { child ->
                MarkdownNode(child, content)
            }
        }

        // 段落
        MarkdownElementTypes.PARAGRAPH -> {
            FlowRow(
                modifier = modifier.padding(start = 4.dp)
            ) {
                node.children.forEach { child ->
                    MarkdownNode(child, content)
                }
            }
        }

        // 标题
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6 -> {
            val headerContent =
                node.findChildOfType(MarkdownTokenTypes.ATX_CONTENT)?.getTextInNode(content) ?: ""
            val style = when (node.type) {
                MarkdownElementTypes.ATX_1 -> MaterialTheme.typography.headlineLarge
                MarkdownElementTypes.ATX_2 -> MaterialTheme.typography.headlineMedium
                MarkdownElementTypes.ATX_3 -> MaterialTheme.typography.headlineSmall
                MarkdownElementTypes.ATX_4 -> MaterialTheme.typography.titleLarge
                MarkdownElementTypes.ATX_5 -> MaterialTheme.typography.titleMedium
                MarkdownElementTypes.ATX_6 -> MaterialTheme.typography.titleSmall
                else -> throw IllegalArgumentException("Unknown header type")
            }
            Text(
                text = headerContent.trim(),
                style = style,
                modifier = modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }

        // 列表
        MarkdownElementTypes.UNORDERED_LIST -> {
            Column(modifier = modifier) {
                node.children.forEach { child ->
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        Row {
                            Text(
                                text = "• ",
                                modifier = Modifier.alignByBaseline()
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                child.children.forEach { listItemChild ->
                                    MarkdownNode(listItemChild, content)
                                }
                            }
                        }
                    }
                }
            }
        }

        MarkdownElementTypes.ORDERED_LIST -> {
            Column(modifier = modifier) {
                var index = 1
                node.children.forEach { child ->
                    if (child.type == MarkdownElementTypes.LIST_ITEM) {
                        Row {
                            Text(
                                text = "$index. ",
                                modifier = Modifier.alignByBaseline()
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                child.children.forEach { listItemChild ->
                                    MarkdownNode(listItemChild, content)
                                }
                            }
                        }
                        index++
                    }
                }
            }
        }

        // 引用块
        MarkdownElementTypes.BLOCK_QUOTE -> {
            Row(
                modifier = modifier
                    .height(IntrinsicSize.Min)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)),
            ) {
                val color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                Canvas(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight(),
                    onDraw = {
                        drawRect(
                            color = color,
                            size = size
                        )
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ProvideTextStyle(LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)) {
                    FlowRow(modifier = Modifier.weight(1f)) {
                        node.children.forEach { child ->
                            MarkdownNode(child, content)
                        }
                    }
                }
            }
        }

        // 链接
        MarkdownElementTypes.INLINE_LINK -> {
            val linkText =
                node.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content) ?: ""
            val linkDest =
                node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                    ?: ""
            Text(
                text = linkText,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = modifier
            )
        }

        // 加粗和斜体
        MarkdownElementTypes.EMPH -> {
            Text(
                text = node.getTextInNode(content),
                fontStyle = FontStyle.Italic,
                modifier = modifier
            )
        }

        MarkdownElementTypes.STRONG -> {
            Text(
                text = node.getTextInNode(content),
                fontWeight = FontWeight.Bold,
                modifier = modifier
            )
        }

        // GFM 特殊元素
        GFMElementTypes.STRIKETHROUGH -> {
            Text(
                text = node.getTextInNode(content),
                textDecoration = TextDecoration.LineThrough,
                modifier = modifier
            )
        }

        GFMElementTypes.TABLE -> {
            // 简单表格实现
            Column(modifier = modifier) {
                node.children.forEachIndexed { index, row ->
                    if (row.type == GFMElementTypes.ROW) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.children.forEach { cell ->
                                Text(
                                    text = cell.getTextInNode(content).trim(),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                )
                            }
                        }

                        // 表头下方添加分隔线
                        if (index == 0) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.2f
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }

        // 图片
        MarkdownElementTypes.IMAGE -> {
            val altText =
                node.findChildOfType(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content) ?: ""
            val imageUrl =
                node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                    ?: ""
            Column(
                modifier = modifier,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                // 这里可以使用Coil等图片加载库加载图片
                AsyncImage(model = imageUrl, contentDescription = altText)
            }
        }

        GFMElementTypes.INLINE_MATH -> {
            val formula = node.getTextInNode(content)
            MathInline(formula, modifier = modifier.padding(horizontal = 1.dp))
        }

        GFMElementTypes.BLOCK_MATH -> {
            val formula = node.getTextInNode(content)
            MathBlock(
                formula, modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        MarkdownElementTypes.CODE_SPAN -> {
            val code = node.getTextInNode(content).trim('`')
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                modifier = modifier
            )
        }

        // 代码块
        MarkdownElementTypes.CODE_FENCE -> {
            val code = node.getTextInNode(content, MarkdownTokenTypes.CODE_FENCE_CONTENT)
            val language = node.findChildOfType(MarkdownTokenTypes.FENCE_LANG)
                ?.getTextInNode(content)
                ?: "plaintext"

            HighlightCodeBlock(
                code = code,
                language = language,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .fillMaxWidth()
            )
        }

        MarkdownTokenTypes.TEXT, MarkdownTokenTypes.WHITE_SPACE -> {
            val text = node.getTextInNode(content)
            Text(
                text = text,
                modifier = modifier
            )
        }

        // 其他类型的节点，递归处理子节点
        else -> {
            // 递归处理其他节点的子节点
            node.children.forEach { child ->
                MarkdownNode(child, content)
            }
        }
    }
}

private fun ASTNode.getTextInNode(text: String): String {
    return text.substring(startOffset, endOffset)
}

private fun ASTNode.getTextInNode(text: String, type: IElementType): String {
    var startOffset = -1
    var endOffset = -1
    children.fastForEach {
        if (it.type == type) {
            if(startOffset == -1) {
                startOffset = it.startOffset
            }
            endOffset = it.endOffset
        }
    }
    if (startOffset == -1 || endOffset == -1) {
        return ""
    }
    return text.substring(startOffset, endOffset)
}

private fun ASTNode.findChildOfType(type: IElementType): ASTNode? {
    return children.find { it.type == type }
}