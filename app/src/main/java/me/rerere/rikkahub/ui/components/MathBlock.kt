package me.rerere.rikkahub.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.Base64
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MathBlock(expr: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            MathView(context).apply {
                renderLatex(expr, true)
            }
        },
        update = { webView ->
            webView.renderLatex(expr, true)
        },
        modifier = modifier,
    )
}

class MathView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : WebView(context, attrs, defStyleAttr) {
    private val katexHeader = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.4/dist/katex.min.css">
            <script src="https://cdn.jsdelivr.net/npm/katex@0.16.4/dist/katex.min.js"></script>
            <style>
                body {
                    margin: 0;
                    padding: 0;
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    background-color: transparent;
                }
            </style>
        </head>
        <body>
            <div class="formula" id="formula"></div>
            <script>
                function renderLatex(latex, displayMode) {
                    const formulaElement = document.getElementById('formula');
                    katex.render(latex, formulaElement, {
                        throwOnError: false,
                        displayMode: displayMode
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    init {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        with(settings) {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false
            domStorageEnabled = true
            allowFileAccess = true
        }

        setBackgroundColor(android.graphics.Color.BLUE)
        loadData(Base64.encodeToString(katexHeader.toByteArray(), Base64.NO_PADDING), "text/html", "base64")

        webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                println(error)
                super.onReceivedError(view, request, error)
            }
        }
    }

    fun renderLatex(latex: String, displayMode: Boolean = true) {
        val escapedLatex = latex.replace("\\", "\\\\").replace("\"", "\\\"")
        val javascriptCommand = "renderLatex(\"$escapedLatex\", $displayMode)"

        evaluateJavascript(javascriptCommand, null)
    }
}