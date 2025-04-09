package me.rerere.rikkahub.ui.components

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
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

// 预处理markdown内容，将行内公式和块级公式转换为LaTeX格式
// 替换行内公式 \( ... \) 到 $ ... $
// 替换块级公式 \[ ... \] 到 $$ ... $$
private fun preProcess(content: String): String {
    // 替换行内公式 \( ... \) 到 $ ... $
    var result = content.replace(Regex("\\\\\\((.+?)\\\\\\)")) { matchResult ->
        "$" + matchResult.groupValues[1] + "$"
    }

    // 替换块级公式 \[ ... \] 到 $$ ... $$
    result =
        result.replace(Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL)) { matchResult ->
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
        parser.buildMarkdownTreeFromString(preprocessed)
//            .also {
//                dumpAst(it, preprocessed) // for debugging ast tree
//            }
    }

    MarkdownAst(astTree, preprocessed, modifier)
}

// for debug
private fun dumpAst(node: ASTNode, text: String, indent: String = "") {
    println("$indent${node.type} ${if (node.children.isEmpty()) node.getTextInNode(text) else ""}")
    node.children.forEach {
        dumpAst(it, text, "$indent  ")
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

object HeaderStyle {
    val H1 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    )

    val H2 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    )

    val H3 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    val H4 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    )

    val H5 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )

    val H6 = TextStyle(
        fontStyle = FontStyle.Normal,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkdownNode(node: ASTNode, content: String, modifier: Modifier = Modifier) {
    when (node.type) {
        // 文件根节点
        MarkdownElementTypes.MARKDOWN_FILE -> {
            node.children.forEach { child ->
                MarkdownNode(child, content, modifier)
            }
        }

        // 段落
        MarkdownElementTypes.PARAGRAPH -> {
            Paragraph(
                node = node,
                content = content,
                modifier = modifier
            )
        }

        // 标题
        MarkdownElementTypes.ATX_1,
        MarkdownElementTypes.ATX_2,
        MarkdownElementTypes.ATX_3,
        MarkdownElementTypes.ATX_4,
        MarkdownElementTypes.ATX_5,
        MarkdownElementTypes.ATX_6 -> {
            val style = when (node.type) {
                MarkdownElementTypes.ATX_1 -> HeaderStyle.H1
                MarkdownElementTypes.ATX_2 -> HeaderStyle.H2
                MarkdownElementTypes.ATX_3 -> HeaderStyle.H3
                MarkdownElementTypes.ATX_4 -> HeaderStyle.H4
                MarkdownElementTypes.ATX_5 -> HeaderStyle.H5
                MarkdownElementTypes.ATX_6 -> HeaderStyle.H6
                else -> throw IllegalArgumentException("Unknown header type")
            }
            ProvideTextStyle(style) {
                FlowRow(modifier = modifier.padding(vertical = 8.dp)) {
                    node.children.forEach { child ->
                        MarkdownNode(child, content, Modifier.align(Alignment.CenterVertically))
                    }
                }
            }
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
                node.findChildOfType(MarkdownTokenTypes.TEXT)?.getTextInNode(content) ?: ""
            val linkDest =
                node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                    ?: ""
            val context = LocalContext.current
            Text(
                text = linkText,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, linkDest.toUri())
                    context.startActivity(intent)
                }
            )
        }

        // 加粗和斜体
        MarkdownElementTypes.EMPH -> {
            ProvideTextStyle(TextStyle(fontStyle = FontStyle.Italic)) {
                node.children.forEach { child ->
                    MarkdownNode(child, content, modifier)
                }
            }
        }

        MarkdownElementTypes.STRONG -> {
            ProvideTextStyle(TextStyle(fontWeight = FontWeight.Bold)) {
                node.children.forEach { child ->
                    MarkdownNode(child, content, modifier)
                }
            }
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
            Table(modifier = modifier) {
                node.children.forEach {
                    MarkdownNode(it, content)
                }
            }
        }

        GFMElementTypes.HEADER -> {
            TableHeader(modifier = modifier) {
                node.children.forEach {
                    if (it.type == GFMTokenTypes.CELL) {
                        TableCell {
                            MarkdownNode(it, content)
                        }
                    }
                }
            }
        }

        GFMElementTypes.ROW -> {
            TableRow(modifier = modifier) {
                node.children.forEach {
                    if (it.type == GFMTokenTypes.CELL) {
                        TableCell {
                            MarkdownNode(it, content)
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
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 这里可以使用Coil等图片加载库加载图片
                AsyncImage(model = imageUrl, contentDescription = altText)
            }
        }

        GFMElementTypes.INLINE_MATH -> {
            val formula = node.getTextInNode(content)
            MathInline(
                formula,
                modifier = modifier
                    .padding(horizontal = 1.dp)
            )
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
                MarkdownNode(child, content, modifier)
            }
        }
    }
}

@Composable
private fun Paragraph(node: ASTNode, content: String, modifier: Modifier) {
    // 如果段落中包含块级数学公式，则直接渲染所有子节点，不使用AnnotatedString
    if (node.findChildOfType(GFMElementTypes.BLOCK_MATH) != null) {
        node.children.forEach {
            MarkdownNode(it, content, modifier)
        }
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val inlineContents = remember {
        mutableStateMapOf<String, InlineTextContent>()
    }
    val annotatedString = remember(content) {
        buildAnnotatedString {
            node.children.forEach { child ->
                appendMarkdownNodeContent(child, content, inlineContents, colorScheme)
            }
        }
    }

    Text(
        text = annotatedString,
        modifier = modifier
            .padding(start = 4.dp),
        style = LocalTextStyle.current,
        inlineContent = inlineContents
    )
}

private fun AnnotatedString.Builder.appendMarkdownNodeContent(
    node: ASTNode,
    content: String,
    inlineContents: MutableMap<String, InlineTextContent>,
    colorScheme: ColorScheme
) {
    when (node.type) {
        MarkdownTokenTypes.TEXT,
        MarkdownTokenTypes.LPAREN,
        MarkdownTokenTypes.RPAREN,
        MarkdownTokenTypes.WHITE_SPACE,
        MarkdownTokenTypes.COLON -> {
            append(node.getTextInNode(content))
        }

        MarkdownElementTypes.EMPH -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children.forEach {
                    appendMarkdownNodeContent(
                        it,
                        content,
                        inlineContents,
                        colorScheme
                    )
                }
            }
        }

        MarkdownElementTypes.STRONG -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                node.children.forEach {
                    appendMarkdownNodeContent(
                        it,
                        content,
                        inlineContents,
                        colorScheme
                    )
                }
            }
        }

        MarkdownElementTypes.INLINE_LINK -> {
            val linkText =
                node.findChildOfType(MarkdownTokenTypes.TEXT)?.getTextInNode(content) ?: ""
            val linkDest =
                node.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content)
                    ?: ""
            withLink(LinkAnnotation.Url(linkDest)) {
                withStyle(
                    SpanStyle(
                        color = colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(linkText)
                }
            }
        }

        MarkdownElementTypes.CODE_SPAN -> {
            val code = node.getTextInNode(content).trim('`')
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 0.95.em,
                )
            ) {
                append(code)
            }
        }

        GFMElementTypes.INLINE_MATH -> {
            // formula as id
            val formula = node.getTextInNode(content)
            appendInlineContent(formula, "[${node.getTextInNode(content)}]")
            inlineContents.putIfAbsent(
                formula, InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.em,
                        height = 1.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    ),
                    children = {
                        val density = LocalDensity.current
                        MathInline(
                            formula,
                            modifier = Modifier
                                .onGloballyPositioned { coord ->
                                    val width = coord.size.width
                                    val height = coord.size.height
                                    with(density) {
                                        val widthInSp = width.toDp().toSp()
                                        val heightInSp = height.toDp().toSp()
                                        val inlineContent = inlineContents[formula]
                                        if (inlineContent != null && inlineContent.placeholder.width != widthInSp) {
                                            inlineContents[formula] = InlineTextContent(
                                                placeholder = Placeholder(
                                                    width = widthInSp,
                                                    height = heightInSp,
                                                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                                                ),
                                                children = inlineContent.children
                                            )
                                        }
                                    }
                                }
                        )
                    }
                ))
        }

        // 其他类型继续递归处理
        else -> {
            node.children.forEach {
                appendMarkdownNodeContent(
                    it,
                    content,
                    inlineContents,
                    colorScheme
                )
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
            if (startOffset == -1) {
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

private fun ASTNode.findChildOfType(vararg types: IElementType): ASTNode? {
    if (this.type in types) return this
    for (child in children) {
        val result = child.findChildOfType(*types)
        if (result != null) return result
    }
    return null
}

private fun ASTNode.traverseChildren(
    action: (ASTNode) -> Unit
) {
    children.forEach { child ->
        action(child)
        child.traverseChildren(action)
    }
}

private fun estimateLatexLength(latex: String): Int {
    // 移除所有空白字符
    var cleanedLatex = latex.replace("\\s".toRegex(), "")

    // 移除常见的LaTeX命令前缀，这些通常不会增加显示宽度
    cleanedLatex = cleanedLatex.replace("\\\\[a-zA-Z]+".toRegex(), "")

    // 处理分数，分数通常会取分子和分母中较长的一个
    val fractionPattern = "\\\\frac\\{([^{}]*)\\}\\{([^{}]*)\\}".toRegex()
    while (fractionPattern.containsMatchIn(cleanedLatex)) {
        cleanedLatex = fractionPattern.replace(cleanedLatex) { matchResult ->
            val numerator = matchResult.groupValues[1]
            val denominator = matchResult.groupValues[2]
            if (numerator.length > denominator.length) numerator else denominator
        }
    }

    // 处理上标和下标，它们通常比普通字符小
    cleanedLatex = cleanedLatex.replace("_\\{[^{}]*\\}|\\^\\{[^{}]*\\}".toRegex(), "x")
    cleanedLatex = cleanedLatex.replace("_[^{]|\\^[^{]".toRegex(), "")

    // 处理根号，根号下的内容加上根号符号的宽度
    val sqrtPattern = "\\\\sqrt\\{([^{}]*)\\}".toRegex()
    cleanedLatex = sqrtPattern.replace(cleanedLatex) { matchResult ->
        "√" + matchResult.groupValues[1]
    }

    // 移除括号，保留内容
    cleanedLatex = cleanedLatex.replace("[{}]".toRegex(), "")

    // 最后计算剩余字符的长度，可以根据经验为某些特殊字符赋予不同的权重
    var length = 0
    for (char in cleanedLatex) {
        length += when (char) {
            '∑', '∏', '∫', '√' -> 2  // 大型数学符号
            'm', 'w' -> 2           // 宽字母
            'i', 'l', '.' -> 1      // 窄字母和标点
            else -> 1               // 默认宽度
        }
    }

    return length
}