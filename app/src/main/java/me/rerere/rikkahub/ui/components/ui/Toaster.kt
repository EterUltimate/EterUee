package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.rerere.rikkahub.ui.theme.extendColors
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

private val DEFAULT_DURATION = 3.seconds

enum class ToastType {
    DEFAULT,
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

data class Toast(
    val id: String = UUID.randomUUID().toString(),
    val message: @Composable () -> Unit,
    val type: ToastType = ToastType.DEFAULT,
    val duration: Duration = DEFAULT_DURATION,
    val createdAt: Instant = Clock.System.now(),
    val action: Action? = null,
) {
    data class Action(
        val label: String,
        val onClick: () -> Unit
    )
}

class ToastState {
    private val _toasts = MutableStateFlow<List<Toast>>(emptyList())
    val toasts = _toasts.asStateFlow()

    fun show(
        message: String,
        type: ToastType = ToastType.DEFAULT,
        duration: Duration = DEFAULT_DURATION,
        action: Toast.Action? = null
    ) {
        return this.create(
            message = { Text(message) },
            type = type,
            duration = duration,
            action = action
        )
    }

    fun show(
        message: @Composable () -> Unit,
        type: ToastType = ToastType.DEFAULT,
        duration: Duration = DEFAULT_DURATION,
        action: Toast.Action? = null,
    ) {
        return this.create(
            message = message,
            type = type,
            duration = duration,
            action = action
        )
    }

    fun info(
        message: String,
        duration: Duration = DEFAULT_DURATION,
        action: Toast.Action? = null,
    ) {
        return this.show(
            message = message,
            type = ToastType.INFO,
            duration = duration,
            action = action
        )
    }

    fun warning(
        message: String,
        duration: Duration = DEFAULT_DURATION,
        action: Toast.Action? = null,
    ) {
        return this.show(
            message = message,
            type = ToastType.WARNING,
            duration = duration,
            action = action
        )
    }

    fun error(
        message: String,
        duration: Duration = DEFAULT_DURATION,
        action: Toast.Action? = null,
    ) {
        return this.show(
            message = message,
            type = ToastType.ERROR,
            duration = duration,
            action = action
        )
    }

    fun success(
        message: String,
        duration: Duration = DEFAULT_DURATION,
        action: Toast.Action? = null,
    ) {
        return this.show(
            message = message,
            type = ToastType.SUCCESS,
            duration = duration,
            action = action
        )
    }

    fun dismiss(id: String) {
        _toasts.update { currentToasts ->
            currentToasts.filter { it.id != id }
        }
    }

    private fun create(
        message: @Composable () -> Unit,
        type: ToastType,
        duration: Duration,
        action: Toast.Action? = null,
    ) {
        val toast = Toast(
            message = message,
            type = type,
            duration = duration,
            action = action,
        )
        _toasts.update { currentToasts ->
            currentToasts + toast
        }
    }
}

val toaster by lazy { ToastState() }

@Composable
fun Toaster(modifier: Modifier = Modifier, toastState: ToastState = toaster) {
    val toasts by toastState.toasts.collectAsState()

    LaunchedEffect(Unit) {
        while (true) {
            for (toast in toasts) {
                val duration = toast.duration
                val startTime = toast.createdAt
                val currentTime = Clock.System.now()
                val elapsedTime = currentTime - startTime
                val remainingTime = duration - elapsedTime
                if (remainingTime <= Duration.ZERO) {
                    toastState.dismiss(toast.id)
                }
            }
            delay(50)
        }
    }

    if (toasts.isNotEmpty()) {
        Popup(
            popupPositionProvider = object : PopupPositionProvider {
                override fun calculatePosition(
                    anchorBounds: IntRect,
                    windowSize: IntSize,
                    layoutDirection: LayoutDirection,
                    popupContentSize: IntSize
                ): IntOffset {
                    // 水平居中，垂直方向固定在顶部
                    val x = (windowSize.width - popupContentSize.width) / 2
                    val y = windowSize.height - popupContentSize.height - 4
                    return IntOffset(x, y)
                }
            },
            properties = PopupProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                focusable = false,
            )
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                toasts.forEachIndexed { index, toast ->
                    key(toast.id) {
                        ToastItem(
                            toast = toast,
                            onDismiss = { toastState.dismiss(toast.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToastItem(
    toast: Toast,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val bgColor = when (toast.type) {
        ToastType.SUCCESS -> MaterialTheme.extendColors.green1
        ToastType.INFO -> MaterialTheme.extendColors.blue1
        ToastType.WARNING -> MaterialTheme.extendColors.orange1
        ToastType.ERROR -> MaterialTheme.extendColors.red1
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when (toast.type) {
        ToastType.SUCCESS -> MaterialTheme.extendColors.green8
        ToastType.INFO -> MaterialTheme.extendColors.blue6
        ToastType.WARNING -> MaterialTheme.extendColors.orange6
        ToastType.ERROR -> MaterialTheme.extendColors.red6
        else -> MaterialTheme.colorScheme.onSurface
    }
    val borderColor = when (toast.type) {
        ToastType.SUCCESS -> MaterialTheme.extendColors.green2
        ToastType.INFO -> MaterialTheme.extendColors.blue2
        ToastType.WARNING -> MaterialTheme.extendColors.orange2
        ToastType.ERROR -> MaterialTheme.extendColors.red2
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    }
    Surface(
        modifier = modifier.padding(horizontal = 16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 8.dp
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    toast.message()
                }
                if (toast.action != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = {
                            toast.action.onClick()
                            onDismiss()
                        },
                        modifier = Modifier
                            .semantics {
                                role = Role.Button
                            },
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = toast.action.label,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun ToastPreviewer() {
    Column(
        modifier = Modifier
            .safeContentPadding()
            .padding(16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ToastItem(
            Toast(
                message = {
                    Text(
                        text = "This is a toast",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                type = ToastType.SUCCESS,
            )
        )
        ToastItem(
            Toast(
                message = {
                    Text(
                        text = "This is a toast",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                type = ToastType.INFO,
            )
        )
        ToastItem(
            Toast(
                message = {
                    Text(
                        text = "This is a toast",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                type = ToastType.ERROR,
            )
        )
        ToastItem(
            Toast(
                message = {
                    Text(
                        text = "This is a toast",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                type = ToastType.WARNING,
            )
        )
        ToastItem(
            Toast(
                message = {
                    Text(
                        text = "This is a toast",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                type = ToastType.DEFAULT,
            )
        )
        ToastItem(
            Toast(
                message = {
                    Text(
                        text = "This is a toast",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                type = ToastType.DEFAULT,
                action = Toast.Action(
                    label = "Action",
                    onClick = {}
                )
            ))
    }
}