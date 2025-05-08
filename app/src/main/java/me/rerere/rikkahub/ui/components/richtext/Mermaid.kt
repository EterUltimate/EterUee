package me.rerere.rikkahub.ui.components.richtext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import me.rerere.rikkahub.ui.components.webview.WebView
import me.rerere.rikkahub.ui.components.webview.rememberWebViewState

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
    val html = remember(code, theme) {
        buildMermaidHtml(code, theme)
    }
    
    val webViewState = rememberWebViewState(
        data = html,
        mimeType = "text/html",
        encoding = "UTF-8"
    )
    
    WebView(
        state = webViewState,
        modifier = modifier
    )
}

/**
 * Builds HTML with Mermaid JS to render the diagram
 */
private fun buildMermaidHtml(code: String, theme: MermaidTheme): String {
    val escapedCode = code.replace("\"", "\\\"")
    
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
                }
                .mermaid {
                    width: 100%;
                }
            </style>
        </head>
        <body>
            <pre class="mermaid">
                $escapedCode
            </pre>
            <script>
                mermaid.initialize({
                    startOnLoad: true,
                    theme: '${theme.value}'
                });
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
