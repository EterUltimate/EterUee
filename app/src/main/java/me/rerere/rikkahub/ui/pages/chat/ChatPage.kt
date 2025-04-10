package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.composables.icons.lucide.History
import com.composables.icons.lucide.ListTree
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCirclePlus
import com.composables.icons.lucide.Settings
import kotlinx.coroutines.launch
import me.rerere.ai.provider.ModelType
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.ui.components.ToastVariant
import me.rerere.rikkahub.ui.components.chat.ChatInput
import me.rerere.rikkahub.ui.components.chat.ChatMessage
import me.rerere.rikkahub.ui.components.chat.ModelSelector
import me.rerere.rikkahub.ui.components.chat.rememberChatInputState
import me.rerere.rikkahub.ui.components.rememberToastState
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.utils.navigateToChatPage
import me.rerere.rikkahub.ui.utils.plus
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

@Composable
fun ChatPage(id: Uuid, vm: ChatVM = koinViewModel()) {
    val navController = LocalNavController.current
    val toastState = rememberToastState()

    // Handle Error
    LaunchedEffect(Unit) {
        vm.errorFlow.collect { error ->
            error?.let {
                toastState.show(it.message ?: "错误", ToastVariant.ERROR)
            }
        }
    }

    val setting by vm.settings.collectAsStateWithLifecycle()
    val conversations by vm.conversations.collectAsStateWithLifecycle()
    val conversation by vm.conversation.collectAsStateWithLifecycle()
    val loadingJob by vm.conversationJob.collectAsStateWithLifecycle()
    val currentChatModel by vm.currentChatModel.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                navController = navController,
                current = conversation,
                conversations = conversations,
                loading = loadingJob != null,
                vm = vm
            )
        }
    ) {
        val inputState = rememberChatInputState()
        LaunchedEffect(loadingJob) {
            inputState.loading = loadingJob != null
        }
        Scaffold(
            topBar = {
                TopBar(
                    conversation = conversation,
                    drawerState = drawerState,
                    onNewChat = {
                        navigateToChatPage(navController)
                    }
                )
            },
            bottomBar = {
                ChatInput(
                    state = inputState,
                    onCancelClick = {
                        loadingJob?.cancel()
                    },
                    onSendClick = {
                        if (currentChatModel == null) {
                            toastState.show("请先选择模型", ToastVariant.ERROR)
                            return@ChatInput
                        }
                        if(inputState.isEditing()) {
                            vm.handleMessageEdit(
                                inputState.messageContent,
                                inputState.editingMessage
                            )
                        } else {
                            vm.handleMessageSend(inputState.messageContent)
                        }
                        inputState.clearInput()
                    }
                ) {
                    ModelSelector(
                        modelId = setting.chatModelId,
                        providers = setting.providers,
                        onSelect = {
                            vm.setChatModel(it)
                        },
                        type = ModelType.CHAT,
                    )
                }
            }
        ) { innerPadding ->
            ChatList(
                innerPadding = innerPadding,
                conversation = conversation,
                loading = loadingJob != null,
                onRegenerate = { vm.regenerateAtMessage(it) },
                onEdit = {
                    inputState.editingMessage = it.id
                    inputState.messageContent = it.parts
                }
            )
        }
    }
}

@Composable
private fun ChatList(
    innerPadding: PaddingValues,
    conversation: Conversation,
    loading: Boolean,
    onRegenerate: (UIMessage) -> Unit = {},
    onEdit: (UIMessage) -> Unit = {},
) {
    LazyColumn(
        contentPadding = innerPadding + PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(conversation.messages, key = { it.id }) {
            ChatMessage(
                message = it,
                onRegenerate = {
                    onRegenerate(it)
                },
                onEdit = {
                    onEdit(it)
                },
            )
        }

        if (loading) {
            item {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            }
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
    vm: ChatVM,
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
            onRegenerateTitle = {
                vm.generateTitle(true)
            },
            onDelete = {
                vm.deleteConversation(it)
                if (it.id == current.id) {
                    navigateToChatPage(navController)
                }
            }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        NavigationDrawerItem(
            label = {
                Text("聊天历史")
            },
            icon = {
                Icon(Lucide.History, "Chat History")
            },
            onClick = {
                navController.navigate("history")
            },
            selected = false,
            modifier = Modifier.wrapContentWidth()
        )
        NavigationDrawerItem(
            label = {
                Text("设置")
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

