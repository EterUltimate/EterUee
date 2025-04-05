package me.rerere.rikkahub.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope

sealed class DialogContent {
    data class Alert(
        val title: @Composable () -> Unit,
        val text: @Composable () -> Unit,
        val confirmText: @Composable () -> Unit,
        val dismissText: (@Composable () -> Unit)? = null,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit
    ) : DialogContent()

    data class Basic(
        val content: @Composable () -> Unit,
        val onDismissRequest: () -> Unit
    ) : DialogContent()

    data class BottomSheet(
        val content: @Composable () -> Unit,
        val onDismiss: () -> Unit,
        val skipPartiallyExpanded: Boolean
    ) : DialogContent()
}

class DialogState {
    private val _dialog = mutableStateOf<DialogContent?>(null)
    val dialog: State<DialogContent?> = _dialog
    fun openAlertDialog(
        title: @Composable () -> Unit,
        text: @Composable () -> Unit,
        confirmText: @Composable () -> Unit,
        dismissText: (@Composable () -> Unit)? = null,
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        _dialog.value =
            DialogContent.Alert(title, text, confirmText, dismissText, onConfirm, onDismiss)
    }

    fun openBasicDialog(
        content: @Composable () -> Unit,
        onDismissRequest: () -> Unit
    ) {
        _dialog.value = DialogContent.Basic(content, onDismissRequest)
    }

    fun openModalBottomSheet(
        content: @Composable () -> Unit,
        onDismiss: () -> Unit,
        skipPartiallyExpanded: Boolean = false
    ) {
        _dialog.value = DialogContent.BottomSheet(content, onDismiss, skipPartiallyExpanded)
    }

    fun close() {
        _dialog.value = null
    }
}

@Composable
fun rememberDialogState(): DialogState {
    val state = remember { DialogState() }
    DialogHost(state)
    return state
}

@Composable
private fun DialogHost(dialogState: DialogState) {
    val dialog = dialogState.dialog.value
    val scope = rememberCoroutineScope()
    when (dialog) {
        is DialogContent.Alert -> {
            AlertDialog(
                onDismissRequest = {
                    dialog.onDismiss()
                    dialogState.close()
                },
                title = dialog.title,
                text = dialog.text,
                confirmButton = {
                    TextButton(onClick = {
                        dialog.onConfirm()
                        dialogState.close()
                    }) {
                        dialog.confirmText()
                    }
                },
                dismissButton = dialog.dismissText?.let {
                    {
                        TextButton(onClick = {
                            dialog.onDismiss()
                            dialogState.close()
                        }) {
                            it()
                        }
                    }
                }
            )
        }

        is DialogContent.Basic -> {
            AlertDialog(
                onDismissRequest = {
                    dialog.onDismissRequest()
                    dialogState.close()
                },
                title = {},
                text = {
                    dialog.content()
                },
                confirmButton = {}
            )
        }

        is DialogContent.BottomSheet -> {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = dialog.skipPartiallyExpanded
            )
            ModalBottomSheet(
                onDismissRequest = {
                    dialog.onDismiss()
                    dialogState.close()
                },
                sheetState = sheetState
            ) {
                dialog.content()
            }
        }

        null -> Unit
    }
}