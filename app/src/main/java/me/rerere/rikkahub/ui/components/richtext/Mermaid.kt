package me.rerere.rikkahub.ui.components.richtext

import android.webkit.JavascriptInterface
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import me.rerere.rikkahub.ui.components.webview.WebView
import me.rerere.rikkahub.ui.components.webview.rememberWebViewState
import me.rerere.rikkahub.ui.context.LocalToaster

/**
 * A component that renders Mermaid diagrams.
 *
 * @param code The Mermaid diagram code
 * @param modifier The modifier to be applied to the component
 * @param theme The theme for the Mermaid diagram (default, forest, dark, neutral)
 */
@Composable
fun Mermaid(
    code: String,
    modifier: Modifier = Modifier,
    theme: MermaidTheme = MermaidTheme.DEFAULT,
) {
    var contentHeight by remember { mutableIntStateOf(330) }
    var height = with(LocalDensity.current) {
        contentHeight.toDp()
    }
    val toaster = LocalToaster.current
    val jsInterface = remember {
        MermaidHeightInterface { height ->
            contentHeight = height
            toaster.show("height = $height")
        }
    }

    val html = remember(code, theme) {
        buildMermaidHtml(code, theme)
    }

    val webViewState = rememberWebViewState(
        data = html,
        mimeType = "text/html",
        encoding = "UTF-8",
        interfaces = mapOf(
            "AndroidInterface" to jsInterface
        )
    )

    WebView(
        state = webViewState,
        modifier = modifier
            .height(height)
    )
}

/**
 * JavaScript interface to receive height updates from the WebView
 */
private class MermaidHeightInterface(private val onHeightChanged: (Int) -> Unit) {
    @JavascriptInterface
    fun updateHeight(height: Int) {
        onHeightChanged(height)
    }
}

fun String.escapeHtml(): String {
    if (this.isEmpty()) {
        return ""
    }
    val sb = StringBuilder(this.length + (this.length / 10)) // 预估容量，避免多次扩容
    for (char in this) {
        when (char) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\'' -> sb.append("&apos;")
            // 可选：处理其他一些不常见的字符，但以上5个是最核心的
            // '/' -> sb.append("&#x2F;") // OWASP 推荐，但并非所有场景都需要
            else -> sb.append(char)
        }
    }
    return sb.toString()
}

/**
 * Builds HTML with Mermaid JS to render the diagram
 */
private fun buildMermaidHtml(code: String, theme: MermaidTheme): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Mermaid Diagram</title>
            <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    background-color: transparent;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    height: auto;
                }
                .mermaid {
                    width: 100%;
                }
            </style>
        </head>
        <body>
            <pre class="mermaid">
                ${code.escapeHtml()}
            </pre>
            <script type="module">
              import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.esm.min.mjs';
              
              mermaid.initialize({
                    startOnLoad: false,
                    theme: '${theme.value}'
              });
              
              await mermaid.run({
                    querySelector: '.mermaid'
              });

              function calculateAndSendHeight() {
                    const height = document.body.scrollHeight;
                    AndroidInterface.updateHeight(height);
              }
              
              // Schedule the height update after the browser's next paint
              requestAnimationFrame(calculateAndSendHeight);
            </script>
        </body>
        </html>
    """.trimIndent()
}

/**
 * Enum class for Mermaid diagram themes
 */
enum class MermaidTheme(val value: String) {
    DEFAULT("default"),
    FOREST("forest"),
    DARK("dark"),
    NEUTRAL("neutral")
}
