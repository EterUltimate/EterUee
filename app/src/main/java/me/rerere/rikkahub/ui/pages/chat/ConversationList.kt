package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Recycle
import com.composables.icons.lucide.RefreshCw
import me.rerere.ai.ui.Conversation
import me.rerere.rikkahub.ui.theme.extendColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.uuid.Uuid

@Composable
fun ConversationList(
    current: Conversation,
    conversations: List<Conversation>,
    loadings: Collection<Uuid>,
    modifier: Modifier = Modifier,
    onClick: (Conversation) -> Unit = {},
    onDelete: (Conversation) -> Unit = {},
    onRegenerateTitle: (Conversation) -> Unit = {}
) {
    // 按日期分组对话
    val groupedConversations = conversations.groupBy { conversation ->
        val instant = conversation.createAt
        instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSortedMap(compareByDescending { it })

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        groupedConversations.forEach { (date, conversationsInGroup) ->
            // 添加日期标题
            stickyHeader {
                DateHeader(date = date)
            }

            // 每组内的对话列表
            items(conversationsInGroup, key = { it.id }) { conversation ->
                ConversationItem(
                    conversation = conversation,
                    selected = conversation.id == current.id,
                    loading = conversation.id in loadings,
                    onClick = onClick,
                    onDelete = onDelete,
                    onRegenerateTitle = onRegenerateTitle
                )
            }
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    val displayText = when {
        date.isEqual(today) -> "今天"
        date.isEqual(yesterday) -> "昨天"
        date.year == today.year -> "${date.monthValue}月${date.dayOfMonth}日"
        else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    selected: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onDelete: (Conversation) -> Unit = {},
    onRegenerateTitle: (Conversation) -> Unit = {},
    onClick: (Conversation) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    } else {
        Color.Transparent
    }
    var showDropdownMenu by remember {
        mutableStateOf(false)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50f))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onClick(conversation) },
                onLongClick = {
                    showDropdownMenu = true
                }
            )
            .background(backgroundColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = conversation.title.ifBlank { "新消息" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(loading) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.extendColors.green6)
                        .size(4.dp)
                        .semantics {
                            contentDescription = "Loading"
                        }
                )
            }
            DropdownMenu(
                expanded = showDropdownMenu,
                onDismissRequest = { showDropdownMenu = false },
            ) {
                DropdownMenuItem(
                    text = {
                        Text("重新生成标题")
                    },
                    onClick = {
                        onRegenerateTitle(conversation)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(Lucide.RefreshCw, null)
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text("删除")
                    },
                    onClick = {
                        onDelete(conversation)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(Lucide.Delete, null)
                    }
                )
            }
        }
    }
}
