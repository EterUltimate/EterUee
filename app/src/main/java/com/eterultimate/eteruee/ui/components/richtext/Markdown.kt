package com.eterultimate.eteruee.ui.components.richtext

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tick01
import com.eterultimate.eteruee.ui.components.table.DataTable
import com.eterultimate.eteruee.ui.context.LocalSettings
import com.eterultimate.eteruee.ui.theme.JetbrainsMono
import com.eterultimate.eteruee.utils.toDp
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser
import androidx.compose.ui.graphics.RectangleShape

private val flavour by lazy {
    GFMFlavourDescriptor(
        makeHttpsAutoLinks = true, useSafeLinks = true
    )
}

private val parser by lazy {
    MarkdownParser(flavour)
}

private val INLINE_LATEX_REGEX = Regex("\\\\\\((.+?)\\\\\\)")
private val BLOCK_LATEX_REGEX = Regex("\\\\\\[(.+?)\\\\\\]", RegexOption.DOT_MATCHES_ALL)
val THINKING_REGEX = Regex("<think>([\\s\\S]*?)(?:</think>|$)", RegexOption.DOT_MATCHES_ALL)
private val CODE_BLOCK_REGEX = Regex("```[\\s\\S]*?```|`[^`\n]*`", RegexOption.DOT_MATCHES_ALL)
private val BREAK_LINE_REGEX = Regex("(?i)<br\\s*/?>")

// 棰勫鐞唌arkdown鍐呭
private fun preProcess(content: String): String {
    // 鍏堟壘鍑烘墍鏈変唬鐮佸潡鐨勪綅缃?
    val codeBlocks = mutableListOf<IntRange>()
    CODE_BLOCK_REGEX.findAll(content).forEach { match ->
        codeBlocks.add(match.range)
    }

    // 妫€鏌ヤ綅缃槸鍚﹀湪浠ｇ爜鍧楀唴
    fun isInCodeBlock(position: Int): Boolean {
        return codeBlocks.any { range -> position in range }
    }

    // 鏇挎崲琛屽唴鍏紡 \( ... \) 鍒?$ ... $锛屼絾璺宠繃浠ｇ爜鍧楀唴鐨勫唴瀹?
    var result = INLINE_LATEX_REGEX.replace(content) { matchResult ->
        if (isInCodeBlock(matchResult.range.first)) {
            matchResult.value // 淇濇寔鍘熸牱
        } else {
            "$" + matchResult.groupValues[1] + "$"
        }
    }

    // 鏇挎崲鍧楃骇鍏紡 \[ ... \] 鍒?$$ ... $$锛屼絾璺宠繃浠ｇ爜鍧楀唴鐨勫唴瀹?
    result = BLOCK_LATEX_REGEX.replace(result) { matchResult ->
        if (isInCodeBlock(matchResult.range.first)) {
            matchResult.value // 淇濇寔鍘熸牱
        } else {
            "$$" + matchResult.groupValues[1] + "$$"
        }
    }

    return result
}

@Preview(showBackground = true)
@Composable
private fun MarkdownPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MarkdownBlock(
                content = "Hi there!", modifier = Modifier.background(Color.Red)
            )
            MarkdownBlock(
                content = """
                    ### 馃實 This is Markdown Test This Markdown Test
                    1. How many roads must a man walk down
                        * the slings and arrows of outrageous fortune, Or to take arms against a sea of troubles,
                        * by opposing end them.
                            * How many times must a man look up, Before he can see the sky?
                            * How many times $ f(x) = \sum_{n=0}^{\infty} \frac{f^{(n)}(a)}{n!}(x-a)^n$
                    2. How many times must a man look up, Before he can see the sky?

                    * [ ] Before they're allowed to be free? Yes, 'n' how many times can a man turn his head
                    * [x] Before they're allowed to be free? Yes, 'n' how many times can a man turn his head

                    4. For in that sleep of death what dreams may come [citation](1)

                    This is Markdown Test, This <br/> is Markdown Test.
                    ha<br/>ha

                    ***
                    This is Markdown Test, This is Markdown Test.

                    | Name | Age | Address | Email | Job | Homepage |
                    | ---- | --- | ------- | ----- | --- | -------- |
                    | John | 25  | New York | john@example.com | Software Engineer | john.com |
                    | Jane | 26  | London   | jane@example.com | Data Scientist | jane.com |

                    ## HTML Escaping
                    This is a &gt;  test

                """.trimIndent()
            )
        }
    }
}

private data class MarkdownParseResult(
    val preprocessed: String,
    val astTree: ASTNode,
    val hasHtmlBlocks: Boolean,
)

private fun ASTNode.containsHtmlBlocks(): Boolean {
    if (type == MarkdownElementTypes.HTML_BLOCK) return true
    return children.any { it.containsHtmlBlocks() }
}

private fun parseMarkdown(content: String): MarkdownParseResult {
    val preprocessed = preProcess(content)
    val astTree = parser.buildMarkdownTreeFromString(preprocessed)
    return MarkdownParseResult(preprocessed, astTree, astTree.containsHtmlBlocks())
}

@Composable
fun MarkdownBlock(
    content: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    onClickCitation: (String) -> Unit = {}
) {
    var (data, setData) = remember { mutableStateOf(parseMarkdown(content)) }

    // 鐩戝惉鍐呭鍙樺寲锛岄噸鏂拌В鏋怉ST鏍?
    // 杩欓噷鍦ㄥ悗鍙扮嚎绋嬭В鏋怉ST鏍? 闃叉棰戠箒鏇存柊鐨勬椂鍊欐帀甯?
    val updatedContent by rememberUpdatedState(content)
    LaunchedEffect(Unit) {
        snapshotFlow { updatedContent }
            .distinctUntilChanged()
            .mapLatest { parseMarkdown(it) }
            .catch { exception -> exception.printStackTrace() }
            .flowOn(Dispatchers.Default)
            .collect { setData(it) }
    }

    if (data.hasHtmlBlocks) {
        MarkdownNew(
            content = content,
            modifier = modifier,
            style = style,
            onClickCitation = onClickCitation,
        )
    } else {
        ProvideTextStyle(style) {
            Column(
                modifier = modifier.padding(start = 4.dp)
            ) {
                data.astTree.children.fastForEach { child ->
                    MarkdownNode(
                        node = child, content = data.preprocessed, onClickCitation = onClickCitation
                    )
                }
            }
        }
    }
}

// for debug
private fun dumpAst(node: ASTNode, text: String, indent: String = "") {
    println("$indent${node.type} ${if (node.children.isEmpty()) node.getTextInNode(text) else ""} | ${node.javaClass.simpleName}")
    node.children.fastForEach {
        dumpAst(it, text, "$indent  ")
    }
}

object HeaderStyle {
    val H1 = TextStyle(
        fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 24.sp
    )

    val H2 = TextStyle(
        fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 22.sp
    )

    val H3 = TextStyle(
        fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 20.sp
    )

    val H4 = TextStyle(
        fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 18.sp
    )

    val H5 = TextStyle(
        fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 16.sp
    )

    val H6 = TextStyle(
        fontStyle = FontStyle.Normal, fontWeight = FontWeight.Bold, fontSize = 14.sp
    )
}

@Composable
private fun MarkdownNode(
    node: ASTNode,
    content: String,
    modifier: Modifier = Modifier,
    onClickCitation: (String) -> Unit = {},
    listLevel: Int = 0
) {
    when (node.type) {
        // 鏂囦欢鏍硅妭鐐?
        MarkdownElementTypes.MARKDOWN_FILE -> {
            node.children.fastForEach { child ->
                MarkdownNode(
                    node = child, content = content, modifier = modifier, onClickCitation = onClickCitation
                )
            }
        }

        // 娈佃惤
        MarkdownElementTypes.PARAGRAPH -> {
            Paragraph(
                node = node, content = content, modifier = modifier, onClickCitation = onClickCitation
            )
        }

        // 鏍囬
        MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3, MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> {
            val style = when (node.type) {
                MarkdownElementTypes.ATX_1 -> HeaderStyle.H1
                MarkdownElementTypes.ATX_2 -> HeaderStyle.H2
                MarkdownElementTypes.ATX_3 -> HeaderStyle.H3
                MarkdownElementTypes.ATX_4 -> HeaderStyle.H4
                MarkdownElementTypes.ATX_5 -> HeaderStyle.H5
                MarkdownElementTypes.ATX_6 -> HeaderStyle.H6
                else -> throw IllegalArgumentException("Unknown header type")
            }
            val headingPadding = when (node.type) {
                MarkdownElementTypes.ATX_1 -> 16.dp
                MarkdownElementTypes.ATX_2 -> 14.dp
                MarkdownElementTypes.ATX_3 -> 12.dp
                MarkdownElementTypes.ATX_4 -> 10.dp
                MarkdownElementTypes.ATX_5 -> 8.dp
                MarkdownElementTypes.ATX_6 -> 6.dp
                else -> 8.dp
            }
            ProvideTextStyle(value = style) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    node.children.fastForEach { node ->
                        if (node.type == MarkdownTokenTypes.ATX_CONTENT) {
                            Paragraph(
                                node = node,
                                content = content,
                                onClickCitation = onClickCitation,
                                modifier = modifier.padding(vertical = headingPadding),
                                trim = true,
                            )
                        }
                    }
                }
            }
        }

        // 鍒楄〃
        MarkdownElementTypes.UNORDERED_LIST -> {
            UnorderedListNode(
                node = node,
                content = content,
                modifier = modifier.padding(vertical = 4.dp),
                onClickCitation = onClickCitation,
                level = listLevel
            )
        }

        MarkdownElementTypes.ORDERED_LIST -> {
            OrderedListNode(
                node = node,
                content = content,
                modifier = modifier.padding(vertical = 4.dp),
                onClickCitation = onClickCitation,
                level = listLevel
            )
        }

        // Checkbox
        GFMTokenTypes.CHECK_BOX -> {
            val isChecked = node.getTextInNode(content).trim() == "[x]"
            Surface(
                shape = RectangleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = modifier,
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(LocalTextStyle.current.fontSize.toDp() * 0.8f),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isChecked) {
                        Icon(
                            imageVector = HugeIcons.Tick01,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // 寮曠敤鍧?
        MarkdownElementTypes.BLOCK_QUOTE -> {
            ProvideTextStyle(LocalTextStyle.current.copy(fontStyle = FontStyle.Italic)) {
                val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                Column(
                    modifier = Modifier
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                color = bgColor, size = size
                            )
                            drawRect(
                                color = borderColor, size = Size(10f, size.height)
                            )
                        }
                        .padding(8.dp)) {
                    node.children.fastForEach { child ->
                        MarkdownNode(
                            node = child, content = content, onClickCitation = onClickCitation
                        )
                    }
                }
            }
        }

        // 閾炬帴
        MarkdownElementTypes.INLINE_LINK -> {
            val linkText = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)
                ?.findChildOfTypeRecursive(GFMTokenTypes.GFM_AUTOLINK, MarkdownTokenTypes.TEXT)?.getTextInNode(content)
                ?: ""
            val linkDest =
                node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content) ?: ""
            val context = LocalContext.current
            Text(
                text = linkText,
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = modifier.clickable {
                    val intent = Intent(Intent.ACTION_VIEW, linkDest.toUri())
                    context.startActivity(intent)
                })
        }

        // 鍔犵矖鍜屾枩浣?
        MarkdownElementTypes.EMPH -> {
            ProvideTextStyle(TextStyle(fontStyle = FontStyle.Italic)) {
                node.children.fastForEach { child ->
                    MarkdownNode(
                        node = child, content = content, modifier = modifier, onClickCitation = onClickCitation
                    )
                }
            }
        }

        MarkdownElementTypes.STRONG -> {
            ProvideTextStyle(TextStyle(fontWeight = FontWeight.SemiBold)) {
                node.children.fastForEach { child ->
                    MarkdownNode(
                        node = child, content = content, modifier = modifier, onClickCitation = onClickCitation
                    )
                }
            }
        }

        // GFM 鐗规畩鍏冪礌
        GFMElementTypes.STRIKETHROUGH -> {
            Text(
                text = node.getTextInNode(content), textDecoration = TextDecoration.LineThrough, modifier = modifier
            )
        }

        GFMElementTypes.TABLE -> {
            TableNode(node = node, content = content, modifier = modifier)
        }

        MarkdownTokenTypes.HORIZONTAL_RULE -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        }

        // 鍥剧墖
        MarkdownElementTypes.IMAGE -> {
            val altText = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content) ?: ""
            val imageUrl =
                node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content) ?: ""
            Column(
                modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 杩欓噷鍙互浣跨敤Coil绛夊浘鐗囧姞杞藉簱鍔犺浇鍥剧墖
                ZoomableAsyncImage(
                    model = imageUrl,
                    contentDescription = altText,
                    modifier = Modifier
                        .clip(RectangleShape)
                        .widthIn(min = 120.dp)
                        .heightIn(min = 120.dp),
                )
            }
        }

        GFMElementTypes.INLINE_MATH -> {
            val formula = node.getTextInNode(content)
            val enableLatexRendering = LocalSettings.current.displaySetting.enableLatexRendering
            if (enableLatexRendering) {
                MathInline(
                    formula, modifier = modifier.padding(horizontal = 1.dp)
                )
            } else {
                Text(
                    text = formula,
                    fontFamily = FontFamily.Monospace,
                    modifier = modifier.padding(horizontal = 1.dp)
                )
            }
        }

        GFMElementTypes.BLOCK_MATH -> {
            val formula = node.getTextInNode(content)
            val enableLatexRendering = LocalSettings.current.displaySetting.enableLatexRendering
            if (enableLatexRendering) {
                MathBlock(
                    formula, modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            } else {
                Text(
                    text = formula,
                    fontFamily = FontFamily.Monospace,
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }

        MarkdownElementTypes.CODE_SPAN -> {
            val code = node.getTextInNode(content).trim('`')
            Text(
                text = code, fontFamily = FontFamily.Monospace, modifier = modifier
            )
        }

        MarkdownElementTypes.CODE_BLOCK -> {
            val code = node.getTextInNode(content)
            Text(
                text = code,
                modifier = modifier,
            )
        }

        // 浠ｇ爜鍧?
        MarkdownElementTypes.CODE_FENCE -> {
            // 杩欓噷涓嶈兘鐩存帴鍙朇ODE_FENCE_CONTENT鐨勫唴瀹癸紝鍥犱负棣栬indent娌℃湁鍖呭惈鍦ㄥ唴
            // 鍥犳锛岄渶瑕佸線涓婃壘鍒版渶鍚庝竴涓狤OL鍏冪礌锛岀敤瀹冩潵浣滀负浠ｇ爜鍧楃殑璧峰offset
            val contentStartIndex = node.children.indexOfFirst { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }
            if (contentStartIndex == -1) return
            val eolElement =
                node.children.subList(0, contentStartIndex).findLast { it.type == MarkdownTokenTypes.EOL } ?: return
            val codeContentStartOffset = eolElement.endOffset
            val codeContentEndOffset =
                node.children.findLast { it.type == MarkdownTokenTypes.CODE_FENCE_CONTENT }?.endOffset ?: return
            val code = content.substring(
                codeContentStartOffset, codeContentEndOffset
            ).trimIndent()

            val language =
                node.findChildOfTypeRecursive(MarkdownTokenTypes.FENCE_LANG)?.getTextInNode(content) ?: "plaintext"
            val hasEnd = node.findChildOfTypeRecursive(MarkdownTokenTypes.CODE_FENCE_END) != null

            HighlightCodeBlock(
                code = code,
                language = language,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .fillMaxWidth(),
                completeCodeBlock = hasEnd
            )
        }

        MarkdownTokenTypes.TEXT -> {
            val text = node.getTextInNode(content)
            Text(
                text = text,
                modifier = modifier,
            )
        }

        MarkdownElementTypes.HTML_BLOCK -> {
            val text = node.getTextInNode(content)
            SimpleHtmlBlock(
                html = text, modifier = modifier
            )
        }

        // 鍏朵粬绫诲瀷鐨勮妭鐐癸紝閫掑綊澶勭悊瀛愯妭鐐?
        else -> {
            // 閫掑綊澶勭悊鍏朵粬鑺傜偣鐨勫瓙鑺傜偣
            node.children.fastForEach { child ->
                MarkdownNode(
                    node = child, content = content, modifier = modifier, onClickCitation = onClickCitation
                )
            }
        }
    }
}

@Composable
private fun UnorderedListNode(
    node: ASTNode,
    content: String,
    modifier: Modifier = Modifier,
    onClickCitation: (String) -> Unit = {},
    level: Int = 0
) {
    val bulletStyle = when (level % 3) {
        0 -> "鈥?"
        1 -> "鈼?"
        else -> "鈻?"
    }

    Column(
        modifier = modifier.padding(start = (level * 8).dp)
    ) {
        node.children.fastForEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                ListItemNode(
                    node = child,
                    content = content,
                    bulletText = bulletStyle,
                    onClickCitation = onClickCitation,
                    level = level
                )
            }
        }
    }
}

@Composable
private fun OrderedListNode(
    node: ASTNode,
    content: String,
    modifier: Modifier = Modifier,
    onClickCitation: (String) -> Unit = {},
    level: Int = 0
) {
    Column(modifier.padding(start = (level * 8).dp)) {
        var index = 1
        node.children.fastForEach { child ->
            if (child.type == MarkdownElementTypes.LIST_ITEM) {
                val numberText =
                    child.findChildOfTypeRecursive(MarkdownTokenTypes.LIST_NUMBER)?.getTextInNode(content) ?: "$index. "
                ListItemNode(
                    node = child,
                    content = content,
                    bulletText = numberText,
                    onClickCitation = onClickCitation,
                    level = level
                )
                index++
            }
        }
    }
}

@Composable
private fun ListItemNode(
    node: ASTNode, content: String, bulletText: String, onClickCitation: (String) -> Unit = {}, level: Int
) {
    Column {
        // 鍒嗙鍒楄〃椤圭殑鐩存帴鍐呭鍜屽祵濂楀垪琛?
        val (directContent, nestedLists) = separateContentAndLists(node)
        // directContent 娓叉煋澶勭悊
        if (directContent.isNotEmpty()) {
            Row {
                Text(
                    text = bulletText,
                    modifier = Modifier.alignByBaseline(),
                    color = MaterialTheme.colorScheme.primary,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    directContent.fastForEach { contentChild ->
                        MarkdownNode(
                            node = contentChild,
                            content = content,
                            onClickCitation = onClickCitation,
                            listLevel = level,
                        )
                    }
                }
            }
        }
        // nestedLists 娓叉煋澶勭悊
        nestedLists.fastForEach { nestedList ->
            MarkdownNode(
                node = nestedList, content = content, onClickCitation = onClickCitation, listLevel = level + 1 // 澧炲姞灞傜骇
            )
        }
    }
}

// 鍒嗙鍒楄〃椤圭殑鐩存帴鍐呭鍜屽祵濂楀垪琛?
private fun separateContentAndLists(listItemNode: ASTNode): Pair<List<ASTNode>, List<ASTNode>> {
    val directContent = mutableListOf<ASTNode>()
    val nestedLists = mutableListOf<ASTNode>()
    listItemNode.children.fastForEach { child ->
        when (child.type) {
            MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> {
                nestedLists.add(child)
            }

            else -> {
                directContent.add(child)
            }
        }
    }
    return directContent to nestedLists
}

@Composable
private fun Paragraph(
    node: ASTNode,
    content: String,
    trim: Boolean = false,
    onClickCitation: (String) -> Unit = {},
    modifier: Modifier,
) {
    // dumpAst(node, content)
    if (node.findChildOfTypeRecursive(MarkdownElementTypes.IMAGE, GFMElementTypes.BLOCK_MATH) != null) {
        FlowRow(modifier = modifier) {
            node.children.fastForEach { child ->
                MarkdownNode(
                    node = child, content = content, onClickCitation = onClickCitation
                )
            }
        }
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val inlineContents = remember {
        mutableStateMapOf<String, InlineTextContent>()
    }
    val hasInlineMath = remember(node) {
        node.findChildOfTypeRecursive(GFMElementTypes.INLINE_MATH) != null
    }
    val enableLatexRendering = LocalSettings.current.displaySetting.enableLatexRendering

    val textStyle = LocalTextStyle.current
    val density = LocalDensity.current
    FlowRow(
        modifier = modifier.then(
            if (node.nextSibling() != null) Modifier.padding(bottom = LocalTextStyle.current.fontSize.toDp())
            else Modifier
        )
    ) {
        val annotatedString = remember(content, enableLatexRendering) {
            buildAnnotatedString {
                node.children.fastForEach { child ->
                    appendMarkdownNodeContent(
                        node = child,
                        content = content,
                        inlineContents = inlineContents,
                        colorScheme = colorScheme,
                        onClickCitation = onClickCitation,
                        style = textStyle,
                        density = density,
                        trim = trim,
                        enableLatexRendering = enableLatexRendering,
                    )
                }
            }
        }
        Text(
            text = annotatedString,
            modifier = Modifier,
            inlineContent = inlineContents,
            softWrap = true,
            overflow = TextOverflow.Visible,
            style = LocalTextStyle.current.copy(
                lineHeight = if (hasInlineMath && enableLatexRendering) TextUnit.Unspecified else LocalTextStyle.current.lineHeight
            )
        )
    }
}

@Composable
private fun TableNode(node: ASTNode, content: String, modifier: Modifier = Modifier) {
    // 鎻愬彇琛ㄦ牸鐨勬爣棰樿鍜屾暟鎹
    val headerNode = node.children.find { it.type == GFMElementTypes.HEADER }
    val rowNodes = node.children.filter { it.type == GFMElementTypes.ROW }

    // 璁＄畻鍒楁暟锛堜粠鏍囬琛岃幏鍙栵級
    val columnCount = headerNode?.children?.count { it.type == GFMTokenTypes.CELL } ?: 0

    // 妫€鏌ユ槸鍚︽湁瓒冲鐨勫垪鏉ユ樉绀鸿〃鏍?
    if (columnCount == 0) return

    // 鎻愬彇琛ㄥご鍗曞厓鏍兼枃鏈?
    val headerCells =
        headerNode?.children?.filter { it.type == GFMTokenTypes.CELL }?.map { it.getTextInNode(content).trim() }
            ?: emptyList()

    // 鎻愬彇鎵€鏈夎鐨勬暟鎹?
    val rows = rowNodes.map { rowNode ->
        rowNode.children.filter { it.type == GFMTokenTypes.CELL }.map { it.getTextInNode(content).trim() }
    }

    // 鍒涘缓琛ㄥごcomposable鍒楄〃
    val headers = List(columnCount) { columnIndex ->
        @Composable {
            MarkdownBlock(
                content = if (columnIndex < headerCells.size) headerCells[columnIndex] else "",
            )
        }
    }

    // 鍒涘缓琛屾暟鎹甤omposable鍒楄〃
    val rowComposables = rows.map { rowData ->
        List(columnCount) { columnIndex ->
            @Composable {
                MarkdownBlock(
                    content = if (columnIndex < rowData.size) rowData[columnIndex] else "",
                )
            }
        }
    }

    // 娓叉煋琛ㄦ牸
    DataTable(
        headers = headers,
        rows = rowComposables,
        modifier = modifier.padding(vertical = 8.dp),
        columnMinWidths = List(columnCount) { 80.dp },
        columnMaxWidths = List(columnCount) { 200.dp },
    )
}

private fun AnnotatedString.Builder.appendMarkdownNodeContent(
    node: ASTNode,
    content: String,
    trim: Boolean = false,
    inlineContents: MutableMap<String, InlineTextContent>,
    colorScheme: ColorScheme,
    density: Density,
    style: TextStyle,
    enableLatexRendering: Boolean = true,
    onClickCitation: (String) -> Unit = {},
) {
    when {
        node.type == MarkdownTokenTypes.BLOCK_QUOTE -> {}

        node.type == GFMTokenTypes.GFM_AUTOLINK -> {
            val link = node.getTextInNode(content)
            withLink(LinkAnnotation.Url(link)) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(link)
                }
            }
        }

        node is LeafASTNode -> {
            val text = node.getTextInNode(content).let {
                if (trim) {
                    it.trim()
                } else {
                    it
                }.replace(BREAK_LINE_REGEX, "\n")
            }
            append(
                text = text,
            )
        }

        node.type == MarkdownElementTypes.EMPH -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children.trim(MarkdownTokenTypes.EMPH, 1).fastForEach {
                    appendMarkdownNodeContent(
                        node = it,
                        content = content,
                        inlineContents = inlineContents,
                        colorScheme = colorScheme,
                        density = density,
                        style = style,
                        enableLatexRendering = enableLatexRendering,
                        onClickCitation = onClickCitation
                    )
                }
            }
        }

        node.type == MarkdownElementTypes.STRONG -> {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                node.children.trim(MarkdownTokenTypes.EMPH, 2).fastForEach {
                    appendMarkdownNodeContent(
                        node = it,
                        content = content,
                        inlineContents = inlineContents,
                        colorScheme = colorScheme,
                        density = density,
                        style = style,
                        enableLatexRendering = enableLatexRendering,
                        onClickCitation = onClickCitation
                    )
                }
            }
        }

        node.type == GFMElementTypes.STRIKETHROUGH -> {
            withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                node.children.trim(GFMTokenTypes.TILDE, 2).fastForEach {
                    appendMarkdownNodeContent(
                        node = it,
                        content = content,
                        inlineContents = inlineContents,
                        colorScheme = colorScheme,
                        density = density,
                        style = style,
                        enableLatexRendering = enableLatexRendering,
                        onClickCitation = onClickCitation
                    )
                }
            }
        }

        node.type == MarkdownElementTypes.INLINE_LINK -> {
            val linkDest =
                node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)?.getTextInNode(content) ?: ""
            val linkText = node.findChildOfTypeRecursive(MarkdownElementTypes.LINK_TEXT)?.getTextInNode(content)
                ?.trim { it == '[' || it == ']' } ?: linkDest
            if (linkText.startsWith("citation,")) {
                // 濡傛灉鏄紩鐢紝鍒欑壒娈婂鐞?
                val domain = linkText.substringAfter("citation,")
                val id = linkDest
                if (id.length == 6) {
                    inlineContents.putIfAbsent(
                        "citation:$linkDest", InlineTextContent(
                            placeholder = Placeholder(
                                width = (domain.length * 7).sp,
                                height = 1.em,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                            ), children = {
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            onClickCitation(id.trim())
                                        }
                                        .fillMaxSize()
                                        .clip(RectangleShape)
                                        .background(colorScheme.tertiaryContainer.copy(0.2f)),
                                    contentAlignment = Alignment.Center) {
                                    Text(
                                        text = domain,
                                        modifier = Modifier.wrapContentSize(),
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            lineHeight = 10.sp,
                                            fontFamily = JetbrainsMono,
                                            color = colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Thin
                                        ),
                                    )
                                }
                            })
                    )
                    appendInlineContent("citation:$linkDest")
                }
            } else {
                withLink(LinkAnnotation.Url(linkDest)) {
                    withStyle(
                        SpanStyle(
                            color = colorScheme.primary, textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(linkText)
                    }
                }
            }
        }

        node.type == MarkdownElementTypes.AUTOLINK -> {
            val links = node.children.trim(MarkdownTokenTypes.LT, 1).trim(MarkdownTokenTypes.GT, 1)
            links.fastForEach { link ->
                withLink(LinkAnnotation.Url(link.getTextInNode(content))) {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(link.getTextInNode(content))
                    }
                }
            }
        }

        node.type == MarkdownElementTypes.CODE_SPAN -> {
            val code = node.getTextInNode(content).trim('`')
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 0.95.em,
                    background = colorScheme.secondaryContainer.copy(alpha = 0.2f),
                )
            ) {
                append(code)
            }
        }

        node.type == GFMElementTypes.INLINE_MATH -> {
            if (enableLatexRendering) {
                // formula as id
                val formula = node.getTextInNode(content)
                appendInlineContent(formula, "[Latex]")
                val (width, height) = with(density) {
                    assumeLatexSize(
                        latex = formula, fontSize = style.fontSize.toPx()
                    ).let {
                        it.width().toSp() to it.height().toSp()
                    }
                }
                inlineContents.putIfAbsent(/* key = */ formula,/* value = */ InlineTextContent(
                    placeholder = Placeholder(
                        width = width, height = height, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    ), children = {
                        MathInline(
                            latex = formula, modifier = Modifier
                        )
                    })
                )
            } else {
                // 绂佺敤 LaTeX 娓叉煋鏃讹紝浠ョ瓑瀹藉瓧浣撴樉绀哄師濮嬪叕寮?
                val formula = node.getTextInNode(content)
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 0.95.em,
                    )
                ) {
                    append(formula)
                }
            }
        }

        // 鍏朵粬绫诲瀷缁х画閫掑綊澶勭悊
        else -> {
            node.children.fastForEach {
                appendMarkdownNodeContent(
                    node = it,
                    content = content,
                    inlineContents = inlineContents,
                    colorScheme = colorScheme,
                    density = density,
                    style = style,
                    enableLatexRendering = enableLatexRendering,
                    onClickCitation = onClickCitation
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

private fun ASTNode.nextSibling(): ASTNode? {
    val brother = this.parent?.children ?: return null
    for (i in brother.indices) {
        if (brother[i] == this) {
            if (i + 1 < brother.size) {
                return brother[i + 1]
            }
        }
    }
    return null
}

private fun ASTNode.findChildOfTypeRecursive(vararg types: IElementType): ASTNode? {
    if (this.type in types) return this
    for (child in children) {
        val result = child.findChildOfTypeRecursive(*types)
        if (result != null) return result
    }
    return null
}

private fun ASTNode.traverseChildren(
    action: (ASTNode) -> Unit
) {
    children.fastForEach { child ->
        action(child)
        child.traverseChildren(action)
    }
}

private fun List<ASTNode>.trim(type: IElementType, size: Int): List<ASTNode> {
    if (this.isEmpty() || size <= 0) return this
    var start = 0
    var end = this.size
    // 浠庡ご瑁佸壀
    var trimmed = 0
    while (start < end && trimmed < size && this[start].type == type) {
        start++
        trimmed++
    }
    // 浠庡熬瑁佸壀
    trimmed = 0
    while (end > start && trimmed < size && this[end - 1].type == type) {
        end--
        trimmed++
    }
    return this.subList(start, end)
}

