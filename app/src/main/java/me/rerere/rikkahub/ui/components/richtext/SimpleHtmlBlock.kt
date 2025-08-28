package me.rerere.rikkahub.ui.components.richtext

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

@Composable
fun SimpleHtmlBlock(
    html: String,
    modifier: Modifier = Modifier
) {
    val document = remember(html) {
        Jsoup.parse(html)
    }

    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier) {
        document.body().childNodes().forEach { node ->
            RenderNode(
                node = node,
                onLinkClick = { url ->
                    try {
                        uriHandler.openUri(url)
                    } catch (e: Exception) {
                        // Handle link click error silently
                    }
                }
            )
        }
    }
}

@Composable
private fun RenderNode(
    node: Node,
    onLinkClick: (String) -> Unit
) {
    when (node) {
        is TextNode -> {
            if (node.text().isNotBlank()) {
                Text(
                    text = node.text(),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalContentColor.current
                    )
                )
            }
        }
        is Element -> {
            when (node.tagName().lowercase()) {
                "p" -> {
                    val annotatedString = buildAnnotatedStringFromElement(node, onLinkClick)
                    if (annotatedString.text.isNotBlank()) {
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = LocalContentColor.current
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val headingLevel = node.tagName().substring(1).toIntOrNull() ?: 1
                    val textStyle = when (headingLevel) {
                        1 -> MaterialTheme.typography.headlineLarge
                        2 -> MaterialTheme.typography.headlineMedium
                        3 -> MaterialTheme.typography.headlineSmall
                        4 -> MaterialTheme.typography.titleLarge
                        5 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }

                    val annotatedString = buildAnnotatedStringFromElement(node, onLinkClick)
                    if (annotatedString.text.isNotBlank()) {
                        Text(
                            text = annotatedString,
                            style = textStyle.copy(
                                color = LocalContentColor.current
                            ),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                "ul", "ol" -> {
                    RenderList(node, node.tagName() == "ol", onLinkClick)
                }
                "details" -> {
                    RenderDetails(node, onLinkClick)
                }
                "img" -> {
                    RenderImage(node)
                }
                "br" -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                "div" -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        node.childNodes().forEach { childNode ->
                            RenderNode(childNode, onLinkClick)
                        }
                    }
                }
                else -> {
                    // Render other elements as text
                    val annotatedString = buildAnnotatedStringFromElement(node, onLinkClick)
                    if (annotatedString.text.isNotBlank()) {
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = LocalContentColor.current
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderList(
    listElement: Element,
    isOrdered: Boolean,
    onLinkClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)) {
        listElement.children().forEachIndexed { index, item ->
            if (item.tagName().lowercase() == "li") {
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        text = if (isOrdered) "${index + 1}. " else "• ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalContentColor.current
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    val annotatedString = buildAnnotatedStringFromElement(item, onLinkClick)
                    if (annotatedString.text.isNotBlank()) {
                        Text(
                            text = annotatedString,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = LocalContentColor.current
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderDetails(
    detailsElement: Element,
    onLinkClick: (String) -> Unit
) {
    val isOpenByDefault = detailsElement.hasAttr("open")
    var isExpanded by remember { mutableStateOf(isOpenByDefault) }

    val summaryElement = detailsElement.children().find {
        it.tagName().lowercase() == "summary"
    }
    val summaryText = summaryElement?.text() ?: "Details"

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        // Summary (clickable header)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "▼ " else "▶ ",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LocalContentColor.current
                )
            )

            val summaryAnnotatedString = if (summaryElement != null) {
                buildAnnotatedStringFromElement(summaryElement, onLinkClick)
            } else {
                AnnotatedString(summaryText)
            }

            Text(
                text = summaryAnnotatedString,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LocalContentColor.current,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Details content (animated visibility)
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            ) {
                detailsElement.children().forEach { child ->
                    if (child.tagName().lowercase() != "summary") {
                        RenderNode(child, onLinkClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderImage(
    imgElement: Element
) {
    val src = imgElement.attr("src")
    val alt = imgElement.attr("alt")
    if (src.isNotEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = src,
                contentDescription = alt.takeIf { it.isNotEmpty() },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit,
                onState = { state ->
                    when (state) {
                        is AsyncImagePainter.State.Loading -> {
                            // Show loading indicator
                        }
                        is AsyncImagePainter.State.Error -> {
                            // Handle error silently or show placeholder
                        }
                        else -> Unit
                    }
                }
            )
        }
    }
}

private fun buildAnnotatedStringFromElement(
    element: Element,
    onLinkClick: (String) -> Unit
): AnnotatedString {
    return buildAnnotatedString {
        processElementNodes(element, this, onLinkClick)
    }
}

private fun processElementNodes(
    element: Element,
    builder: AnnotatedString.Builder,
    onLinkClick: (String) -> Unit
) {
    element.childNodes().forEach { node ->
        when (node) {
            is TextNode -> {
                builder.append(node.text())
            }
            is Element -> {
                when (node.tagName().lowercase()) {
                    "b", "strong" -> {
                        val start = builder.length
                        processElementNodes(node, builder, onLinkClick)
                        builder.addStyle(
                            SpanStyle(fontWeight = FontWeight.Bold),
                            start,
                            builder.length
                        )
                    }
                    "i", "em" -> {
                        val start = builder.length
                        processElementNodes(node, builder, onLinkClick)
                        builder.addStyle(
                            SpanStyle(fontStyle = FontStyle.Italic),
                            start,
                            builder.length
                        )
                    }
                    "u" -> {
                        val start = builder.length
                        processElementNodes(node, builder, onLinkClick)
                        builder.addStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline),
                            start,
                            builder.length
                        )
                    }
                    "a" -> {
                        val href = node.attr("href")
                        val start = builder.length
                        processElementNodes(node, builder, onLinkClick)
                        if (href.isNotEmpty()) {
                            builder.addStyle(
                                SpanStyle(
                                    color = Color.Blue,
                                    textDecoration = TextDecoration.Underline
                                ),
                                start,
                                builder.length
                            )
                            builder.addStringAnnotation(
                                tag = "URL",
                                annotation = href,
                                start = start,
                                end = builder.length
                            )
                        }
                    }
                    "code" -> {
                        val start = builder.length
                        processElementNodes(node, builder, onLinkClick)
                        builder.addStyle(
                            SpanStyle(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                background = Color.Gray.copy(alpha = 0.2f)
                            ),
                            start,
                            builder.length
                        )
                    }
                    "br" -> {
                        builder.append("\n")
                    }
                    else -> {
                        processElementNodes(node, builder, onLinkClick)
                    }
                }
            }
        }
    }
}
