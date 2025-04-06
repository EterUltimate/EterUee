package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.ui.components.icons.ArrowUp
import me.rerere.rikkahub.ui.components.icons.Earth
import me.rerere.rikkahub.ui.components.icons.Plus
import me.rerere.rikkahub.ui.components.icons.Stop

class ChatInputState {
    var messageContent by mutableStateOf(listOf<UIMessagePart>())
    var useWebSearch by mutableStateOf(false)
    var loading by mutableStateOf(false)

    fun reset() {
        messageContent = emptyList()
        useWebSearch = false
        loading = false
    }

    fun setMessageText(text: String) {
        val newMessage = messageContent.toMutableList()
        if (newMessage.isEmpty()) {
            newMessage.add(UIMessagePart.Text(text))
        } else {
            messageContent = newMessage.map {
                if(it is UIMessagePart.Text) {
                    it.copy(text)
                } else {
                    it
                }
            }
            return
        }
        messageContent = newMessage
    }
}

@Composable
fun rememberChatInputState(
    message: List<UIMessagePart> = emptyList(),
    useWebSearch: Boolean = false,
    loading: Boolean = false,
): ChatInputState {
    return remember(message, useWebSearch, loading) {
        ChatInputState().apply {
            this.messageContent = message
            this.useWebSearch = useWebSearch
            this.loading = loading
        }
    }
}

@Composable
fun ChatInput(
    state: ChatInputState,
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onSendClick: () -> Unit,
) {
    val text =
        state.messageContent.filterIsInstance<UIMessagePart.Text>().firstOrNull() ?: UIMessagePart.Text("")

    Surface {
        Column(
            modifier = modifier
                .padding(8.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextField(
                value = text.text,
                onValueChange = { state.setMessageText(it) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                placeholder = {
                    Text("Type a message to chat with AI")
                },
                maxLines = 5,
                colors = TextFieldDefaults.colors().copy(
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedIconToggleButton(
                    checked = state.useWebSearch,
                    onCheckedChange = {
                        state.useWebSearch = it
                    },
                ) {
                    Icon(Earth, "Use Web Search")
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = {}
                ) {
                    Icon(Plus, "More options")
                }

                IconButton(
                    onClick = {
                        if (state.loading) onCancelClick() else onSendClick()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (state.loading) MaterialTheme.colorScheme.errorContainer else Color.Unspecified,
                        contentColor = if (state.loading) MaterialTheme.colorScheme.onErrorContainer else Color.Unspecified,
                    )
                ) {
                    if (state.loading) {
                        Icon(Stop, "Stop")
                    } else {
                        Icon(ArrowUp, "Send")
                    }
                }
            }
        }
    }
}