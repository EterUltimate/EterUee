package me.rerere.rikkahub.ui.components.richtext

import android.content.Context
import me.rerere.rikkahub.utils.base64Encode

/**
 * Build HTML page for markdown preview with support for:
 * - Markdown rendering via marked.js
 * - LaTeX math via KaTeX
 * - Mermaid diagrams
 * - Syntax highlighting via highlight.js
 */
fun buildMarkdownPreviewHtml(context: Context, markdown: String): String {
    val htmlTemplate = context.assets.open("html/mark.html").bufferedReader().use { it.readText() }
    return htmlTemplate.replace("{{MARKDOWN_BASE64}}", markdown.base64Encode())
}
