package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.ai.ui.Conversation

@Composable
fun ConversationList(
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
                onClick = onClick
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    modifier: Modifier = Modifier,
    onClick: (Conversation) -> Unit
) {

}
