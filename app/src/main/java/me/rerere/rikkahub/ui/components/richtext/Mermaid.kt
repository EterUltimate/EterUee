package me.rerere.rikkahub.ui.components.richtext

import android.webkit.JavascriptInterface
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.webview.WebView
import me.rerere.rikkahub.ui.components.webview.rememberWebViewState
import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.utils.escapeHtml
import me.rerere.rikkahub.utils.toCssHex

/**
 * A component that renders Mermaid diagrams.
 *
 * @param code The Mermaid diagram code
 * @param modifier The modifier to be applied to the component
 */
@Composable
fun Mermaid(
    code: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val darkMode = LocalDarkMode.current
    val density = LocalDensity.current
    val context = LocalContext.current

    var contentHeight by remember { mutableIntStateOf(50) }
    var height = with(density) {
        contentHeight.toDp()
    }
    val jsInterface = remember {
        MermaidHeightInterface { height ->
            // 需要乘以density
            // https://stackoverflow.com/questions/43394498/how-to-get-the-full-height-of-in-android-webview
            contentHeight = (height * context.resources.displayMetrics.density).toInt()
        }
    }

    val html = remember(code, colorScheme) {
        buildMermaidHtml(
            code = code,
            theme = if (darkMode) MermaidTheme.DARK else MermaidTheme.DEFAULT,
            colorScheme = colorScheme,
            darkMode = darkMode
        )
    }

    val webViewState = rememberWebViewState(
        data = html,
        mimeType = "text/html",
        encoding = "UTF-8",
        interfaces = mapOf(
            "AndroidInterface" to jsInterface
        ),
        settings = {
            builtInZoomControls = true
            displayZoomControls = false
        }
    )

    WebView(
        state = webViewState,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .animateContentSize()
            .height(height),
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

/**
 * Builds HTML with Mermaid JS to render the diagram
 */
private fun buildMermaidHtml(
    code: String,
    theme: MermaidTheme,
    colorScheme: ColorScheme,
    darkMode: Boolean
): String {
    // 将 ColorScheme 颜色转为 HEX 字符串
    val primaryColor = colorScheme.primaryContainer.toCssHex()
    val secondaryColor = colorScheme.secondaryContainer.toCssHex()
    val tertiaryColor = colorScheme.tertiaryContainer.toCssHex()
    val background = colorScheme.background.toCssHex()
    val surface = colorScheme.surface.toCssHex()
    val onPrimary = colorScheme.onPrimaryContainer.toCssHex()
    val onSecondary = colorScheme.onSecondaryContainer.toCssHex()
    val onTertiary = colorScheme.onTertiaryContainer.toCssHex()
    val onBackground = colorScheme.onBackground.toCssHex()
    val errorColor = colorScheme.error.toCssHex()
    val onErrorColor = colorScheme.onError.toCssHex()

    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes, maximum-scale=5.0">
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
                    background-color: ${background};
                }
                .mermaid {
                    width: 100%;
                    padding: 8px;
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
                    theme: '${theme.value}',
                    themeVariables: {                        
                        primaryColor: '${primaryColor}',
                        primaryTextColor: '${onPrimary}',
                        primaryBorderColor: '${primaryColor}',
                        
                        secondaryColor: '${secondaryColor}',
                        secondaryTextColor: '${onSecondary}',
                        secondaryBorderColor: '${secondaryColor}',
                        
                        tertiaryColor: '${tertiaryColor}',
                        tertiaryTextColor: '${onTertiary}',
                        tertiaryBorderColor: '${tertiaryColor}',
                        
                        background: '${background}',
                        mainBkg: '${primaryColor}',
                        secondBkg: '${secondaryColor}',
                        
                        lineColor: '${onBackground}',
                        textColor: '${onBackground}',
                        
                        nodeBkg: '${surface}',
                        nodeBorder: '${primaryColor}',
                        clusterBkg: '${surface}',
                        clusterBorder: '${primaryColor}',
                        
                        // 序列图变量
                        actorBorder: '${primaryColor}',
                        actorBkg: '${surface}',
                        actorTextColor: '${onBackground}',
                        actorLineColor: '${primaryColor}',
                        
                        // 甘特图变量
                        taskBorderColor: '${primaryColor}',
                        taskBkgColor: '${primaryColor}',
                        taskTextLightColor: '${onPrimary}',
                        taskTextDarkColor: '${onBackground}',
                        
                        // 状态图变量
                        labelColor: '${onBackground}',
                        errorBkgColor: '${errorColor}',
                        errorTextColor: '${onErrorColor}'
                    }
              });
              
              await mermaid.run({
                    querySelector: '.mermaid'
              });

              function calculateAndSendHeight() {
                    // 获取实际内容高度，考虑缩放因素
                    const contentElement = document.querySelector('.mermaid');
                    const contentBox = contentElement.getBoundingClientRect();
                    // 添加内边距和一点额外空间以确保完整显示
                    const height = Math.ceil(contentBox.height) + 20;
                    
                    // 处理移动设备的初始缩放
                    const visualViewportScale = window.visualViewport ? window.visualViewport.scale : 1;
                    console.warn('visualViewportScale', visualViewportScale)
                    const adjustedHeight = Math.ceil(height * visualViewportScale);
                    
                    AndroidInterface.updateHeight(adjustedHeight);
              }
              
              // 等待绘制完成后计算高度
              requestAnimationFrame(() => {
                  // 延迟一点时间以确保Mermaid渲染完成
                  setTimeout(calculateAndSendHeight, 100);
              });
              
              // 监听窗口大小变化以重新计算高度
              window.addEventListener('resize', calculateAndSendHeight);
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
    DARK("dark"),
}
