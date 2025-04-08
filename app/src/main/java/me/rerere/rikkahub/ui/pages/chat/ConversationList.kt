package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.ai.ui.Conversation

@Composable
fun ConversationList(
    current: Conversation,
    conversations: List<Conversation>,
    modifier: Modifier = Modifier,
    onClick: (Conversation) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(conversations) { conversation ->
            ConversationItem(
                conversation = conversation,
                onClick = onClick,
                selected = conversation.id == current.id,
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (Conversation) -> Unit
) {
    Surface(
        selected = selected,
        onClick = {
            onClick(conversation)
        },
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(conversation.title.ifBlank { "新消息" })
        }
    }
}
