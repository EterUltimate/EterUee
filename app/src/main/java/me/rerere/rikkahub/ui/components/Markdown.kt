package me.rerere.rikkahub.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
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

    MarkdownAst(astTree, modifier)
}

@Composable
private fun MarkdownAst(astNode: ASTNode, modifier: Modifier = Modifier) {
    // TODO: 实现渲染器
}