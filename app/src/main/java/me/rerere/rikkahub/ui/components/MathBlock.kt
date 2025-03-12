package me.rerere.rikkahub.ui.components

import android.content.Context
import android.util.AttributeSet
import android.util.Base64
import android.view.ViewGroup.LayoutParams
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun MathBlock(expr: String, modifier: Modifier = Modifier, displayMode: Boolean = true) {
    Box(modifier) {
        AndroidView(
            factory = { context ->
                MathView(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.WRAP_CONTENT,
                        LayoutParams.WRAP_CONTENT
                    )
                    renderLatex(expr, displayMode)
                }
            },
            update = { webView ->
                webView.renderLatex(expr, displayMode)
            },
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .wrapContentSize(),
        )
    }
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
                    background-color: transparent;
                    height: fit-content;
                    width: fit-content;
                }
                .formula {
                    width: fit-content;
                    height: fit-content;
                    display: inline-block;
                }
            </style>
        </head>
        <body>
            <div class="formula" id="formula"></div>
            <script type="text/javascript">
                function renderLatex(latex, displayMode) {
                    const decoded = atob(latex);
                    const formulaElement = document.getElementById('formula');
                    katex.render(decoded, formulaElement, {
                        throwOnError: false,
                        displayMode: displayMode
                    });
                }
            </script>
        </body>
        </html>
    """.trimIndent()

    private var latex: String = ""
    private var displayMode = true

    init {
        with(settings) {
            javaScriptEnabled = true
            builtInZoomControls = false
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        }

        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        loadData(
            Base64.encodeToString(katexHeader.toByteArray(), Base64.NO_PADDING),
            "text/html",
            "base64"
        )

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                println(">>> $sourceID:$lineNumber $message")
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                refresh()
            }
        }
    }

    fun renderLatex(latex: String, displayMode: Boolean = true) {
        this.latex = latex
        this.displayMode = displayMode

        this.refresh()
    }

    fun refresh() {
        val encodedLatex = Base64.encodeToString(latex.toByteArray(), Base64.NO_PADDING).trim()
        val javascriptCommand = "javascript:renderLatex(\"$encodedLatex\", $displayMode)"
        evaluateJavascript(javascriptCommand, null)
    }
}