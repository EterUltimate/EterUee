package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.composables.icons.lucide.ListTree
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCirclePlus
import com.composables.icons.lucide.Settings
import kotlinx.coroutines.launch
import me.rerere.ai.ui.Conversation
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.ui.components.chat.ChatInput
import me.rerere.rikkahub.ui.components.chat.ChatMessage
import me.rerere.rikkahub.ui.components.chat.ModelSelector
import me.rerere.rikkahub.ui.components.chat.rememberChatInputState
import me.rerere.rikkahub.ui.components.rememberToastState
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.utils.plus
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun ChatPage(id: Uuid, vm: ChatVM = koinViewModel()) {
    val navController = LocalNavController.current

    val setting by vm.settings.collectAsStateWithLifecycle()
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val conversation by vm.conversation.collectAsStateWithLifecycle()
    val loadingJob by vm.conversationJob.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                navController = navController,
                current = conversation,
                conversations = conversations,
                loading = loadingJob != null
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    conversation = conversation,
                    drawerState = drawerState,
                    onNewChat = {
                        navController.navigate("chat/${Uuid.random()}") {
                            popUpTo("chat/${conversation.id}") {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            },
            bottomBar = {
                val inputState = rememberChatInputState()
                ChatInput(
                    state = inputState,
                    onCancelClick = {

                    },
                    onSendClick = {
                        vm.handleMessageSend(inputState.messageContent)
                        inputState.clearInput()
                    }
                ) {
                    ModelSelector(
                        modelId = setting.chatModelId,
                        providers = setting.providers,
                        onSelect = {
                            vm.setChatModel(it)
                        }
                    )
                }
            }
        ) { innerPadding ->
            ChatList(innerPadding, conversation)
        }
    }
}

@Composable
private fun ChatList(
    innerPadding: PaddingValues,
    conversation: Conversation
) {
    LazyColumn(
        contentPadding = innerPadding + PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(conversation.messages, key = { it.id }) {
            ChatMessage(it)
        }
    }
}

@Composable
private fun TopBar(
    conversation: Conversation,
    drawerState: DrawerState,
    onNewChat: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    scope.launch { drawerState.open() }
                }
            ) {
                Icon(Lucide.ListTree, "Messages")
            }
        },
        title = {
            Text(
                text = conversation.title.ifBlank { "新聊天" },
                maxLines = 1,
                fontSize = MaterialTheme.typography.bodyMedium.fontSize
            )
        },
        actions = {
            IconButton(
                onClick = {
                    onNewChat()
                }
            ) {
                Icon(Lucide.MessageCirclePlus, "New Message")
            }
        },
    )
}

@Composable
private fun DrawerContent(
    navController: NavController,
    current: Conversation,
    conversations: List<Conversation>,
    loading: Boolean,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(270.dp)
    ) {
        ConversationList(
            current = current,
            conversations = conversations,
            loadings = if (loading) listOf(current.id) else emptyList(),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            onClick = {
                navController.navigate("chat/${it.id}") {
                    popUpTo("chat/${current.id}") {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
        )
        HorizontalDivider()
        NavigationDrawerItem(
            label = {
                Text("Setting")
            },
            icon = {
                Icon(Lucide.Settings, "Setting")
            },
            onClick = {
                navController.navigate("setting")
            },
            selected = false,
            modifier = Modifier.wrapContentWidth()
        )
    }
}