package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.icons.ArrowUp
import me.rerere.rikkahub.ui.components.icons.Brain
import me.rerere.rikkahub.ui.components.icons.Earth
import me.rerere.rikkahub.ui.components.icons.Plus

@Preview(showBackground = true)
@Composable
fun ChatInput(
    modifier: Modifier = Modifier,
    onSendClick: (String) -> Unit = {},
) {
    var text by remember { mutableStateOf("") }
    Surface {
        Column(
            modifier = modifier
                .padding(8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TextField(
                value = text,
                onValueChange = { text = it },
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
                IconButton(
                    onClick = {}
                ) {
                    Icon(Brain, "Thinking Mode")
                }

                IconButton(
                    onClick = {}
                ) {
                    Icon(Earth, "Allow Search")
                }

                Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = {}
                ) {
                    Icon(Plus, "More options")
                }

                IconButton(
                    onClick = {
                        onSendClick(text)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors()
                ) {
                    Icon(ArrowUp, "Send")
                }
            }
        }
    }
}