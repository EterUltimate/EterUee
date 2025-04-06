package me.rerere.rikkahub.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import me.rerere.rikkahub.ui.theme.extendColors

/**
 * Toast 变体类型
 */
enum class ToastVariant {
    SUCCESS,
    ERROR,
    WARNING,
    INFO,
    DEFAULT
}

/**
 * Toast 配置数据类
 */
data class ToastConfig(
    val message: String,
    val variant: ToastVariant = ToastVariant.DEFAULT,
    val duration: Long = 3000L // 默认3秒
)

/**
 * Toast 图标资源
 */
private val toastIcons = mapOf(
    ToastVariant.SUCCESS to android.R.drawable.ic_menu_upload,
    ToastVariant.ERROR to android.R.drawable.ic_delete,
    ToastVariant.WARNING to android.R.drawable.ic_dialog_alert,
    ToastVariant.INFO to android.R.drawable.ic_dialog_info,
    ToastVariant.DEFAULT to android.R.drawable.ic_dialog_email
)

/**
 * Toast 可组合函数
 */
@Composable
fun Toast(
    config: ToastConfig,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showToast by remember { mutableStateOf(true) }

    LaunchedEffect(key1 = showToast) {
        if (showToast) {
            delay(config.duration)
            showToast = false
            onDismiss()
        }
    }

    if (showToast) {
        Popup(
            alignment = Alignment.BottomCenter,
            properties = PopupProperties(
                excludeFromSystemGesture = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
            onDismissRequest = {
                showToast = false
                onDismiss()
            }
        ) {
            Surface(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                shape = MaterialTheme.shapes.medium,
                color = when (config.variant) {
                    ToastVariant.INFO -> MaterialTheme.extendColors.blue6
                    ToastVariant.SUCCESS -> MaterialTheme.extendColors.green6
                    ToastVariant.WARNING -> MaterialTheme.extendColors.orange6
                    ToastVariant.ERROR -> MaterialTheme.extendColors.red6
                    ToastVariant.DEFAULT -> MaterialTheme.colorScheme.primary
                },
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            id = toastIcons[config.variant] ?: android.R.drawable.ic_dialog_email
                        ),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = config.message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

/**
 * 状态管理器和 Toast 控制器
 */
@Composable
fun rememberToastState(host: Boolean = true): ToastState {
    val state = remember { ToastState() }
    if (host) {
        state.Show()
    }
    return state
}

class ToastState {
    private var currentConfig: ToastConfig? by mutableStateOf(null)

    @Composable
    fun Show() {
        currentConfig?.let { config ->
            Toast(config = config) {
                currentConfig = null
            }
        }
    }

    fun show(config: ToastConfig) {
        currentConfig = config
    }

    fun show(
        message: String,
        variant: ToastVariant = ToastVariant.DEFAULT,
        duration: Long = 3000L
    ) {
        currentConfig = ToastConfig(message, variant, duration)
    }
}

// 使用示例:
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ToastExample() {
    val toastState = rememberToastState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            toastState.show("操作成功!", ToastVariant.SUCCESS)
        }) {
            Text("显示成功Toast")
        }

        Button(onClick = {
            toastState.show("发生错误!", ToastVariant.ERROR)
        }) {
            Text("显示错误Toast")
        }
    }

    // 必须在UI树的某个地方调用Show()
    toastState.Show()
}
