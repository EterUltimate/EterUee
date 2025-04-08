package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.composables.icons.lucide.ListTree
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCirclePlus
import com.composables.icons.lucide.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.handleMessageChunk
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.data.datastore.findProvider
import me.rerere.rikkahub.ui.components.ToastVariant
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val toastState = rememberToastState()
    val navController = LocalNavController.current

    val setting by vm.settings.collectAsStateWithLifecycle()
    val model = setting.providers.findModelById(setting.chatModelId)
    val providerSetting = model?.findProvider(setting.providers)

    val conversationFlow = remember(id) { vm.getConversationById(id) }
    val conversation by conversationFlow.collectAsStateWithLifecycle(
        initialValue = Conversation.ofId(id)
    )
    val conversations by vm.conversations.collectAsStateWithLifecycle()

    val inputState = rememberChatInputState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                navController = navController,
                current = conversation,
                conversations = conversations
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopBar(
                    vm = vm,
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
                ChatInput(
                    state = inputState,
                    onCancelClick = {

                    },
                    onSendClick = {

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
    vm: ChatVM,
    onNewChat: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
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
            Text("新聊天")
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
) {
    ModalDrawerSheet(
        modifier = Modifier.width(270.dp)
    ) {
        ConversationList(
            current = current,
            conversations = conversations,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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