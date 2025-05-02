package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.History
import com.composables.icons.lucide.ListTree
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.MessageCirclePlus
import com.composables.icons.lucide.Settings
import com.dokar.sonner.ToastType
import kotlinx.coroutines.launch
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelType
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.BuildConfig
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.components.chat.AssistantPicker
import me.rerere.rikkahub.ui.components.chat.ChatInput
import me.rerere.rikkahub.ui.components.chat.ChatMessage
import me.rerere.rikkahub.ui.components.chat.ModelSelector
import me.rerere.rikkahub.ui.components.chat.rememberChatInputState
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.ui.WavyCircularProgressIndicator
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.hooks.EditStateContent
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.ui.hooks.useThrottle
import me.rerere.rikkahub.utils.UpdateDownload
import me.rerere.rikkahub.utils.Version
import me.rerere.rikkahub.utils.navigateToChatPage
import me.rerere.rikkahub.utils.onError
import me.rerere.rikkahub.utils.onSuccess
import me.rerere.rikkahub.utils.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.uuid.Uuid

@Composable
fun ChatPage(id: Uuid, vm: ChatVM = koinViewModel()) {
    val navController = LocalNavController.current
    val toaster = LocalToaster.current

    // Handle Error
    LaunchedEffect(Unit) {
        vm.errorFlow.collect { error ->
            toaster.show(error.message ?: "Error", type = ToastType.Error)
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
                vm = vm,
                settings = setting
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
                    },
                    onClickMenu = {
                        navController.navigate("menu")
                    },
                    onUpdateTitle = {
                        vm.updateTitle(it)
                    }
                )
            },
            bottomBar = {
                ChatInput(
                    state = inputState,
                    onCancelClick = {
                        loadingJob?.cancel()
                    },
                    enableSearch = vm.useWebSearch,
                    onToggleSearch = {
                        vm.useWebSearch = it
                    },
                    onSendClick = {
                        if (currentChatModel == null) {
                            toaster.show("请先选择模型", type = ToastType.Error)
                            return@ChatInput
                        }
                        if (inputState.isEditing()) {
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
                    Box(Modifier.weight(1f)) {
                        ModelSelector(
                            modelId = setting.chatModelId,
                            providers = setting.providers,
                            onSelect = {
                                vm.setChatModel(it)
                            },
                            type = ModelType.CHAT
                        )
                    }
                }
            }
        ) { innerPadding ->
            ChatList(
                innerPadding = innerPadding,
                conversation = conversation,
                loading = loadingJob != null,
                model = currentChatModel ?: Model(),
                settings = setting,
                onRegenerate = {
                    vm.regenerateAtMessage(it)
                },
                onEdit = {
                    inputState.editingMessage = it.id
                    inputState.messageContent = it.parts
                }
            )
        }
    }
}

private const val LoadingIndicatorKey = "LoadingIndicator"
private const val ScrollBottomKey = "ScrollBottomKey"
private const val TokenUsageItemKey = "TokenUsageItemKey"

@Composable
private fun ChatList(
    innerPadding: PaddingValues,
    conversation: Conversation,
    loading: Boolean,
    model: Model,
    settings: Settings,
    onRegenerate: (UIMessage) -> Unit = {},
    onEdit: (UIMessage) -> Unit = {},
) {
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val scrollToBottom = {
        state.requestScrollToItem(conversation.messages.lastIndex + 5)
    }
    val viewPortSize by remember { derivedStateOf { state.layoutInfo.viewportSize } }
    Box(
        modifier = Modifier.padding(innerPadding),
    ) {
        fun List<LazyListItemInfo>.isAtBottom(): Boolean {
            val lastItem = lastOrNull() ?: return false
            if (lastItem.key == LoadingIndicatorKey || lastItem.key == ScrollBottomKey || lastItem.key == TokenUsageItemKey) {
                return true
            }
            return (lastItem.offset + lastItem.size >= state.layoutInfo.viewportEndOffset - 4) && lastItem.key == conversation.messages.lastOrNull()?.id
        }

        // 自动滚动到底部
        LaunchedEffect(loading, conversation.messages, viewPortSize) {
            if (!state.isScrollInProgress && state.canScrollForward) {
                if (state.layoutInfo.visibleItemsInfo.isAtBottom()) {
                    state.requestScrollToItem(conversation.messages.lastIndex + 10)
                }
            }
        }

        LazyColumn(
            state = state,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(conversation.messages, key = { it.id }) { message ->
                ChatMessage(
                    message = message,
                    showIcon = settings.displaySetting.showModelIcon,
                    model = message.modelId?.let { settings.providers.findModelById(it) },
                    onRegenerate = {
                        onRegenerate(message)
                    },
                    onEdit = {
                        onEdit(message)
                    },
                )
            }

            if (loading) {
                item(LoadingIndicatorKey) {
                    WavyCircularProgressIndicator(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .size(24.dp),
                        strokeWidth = 2.dp,
                        waveCount = 8
                    )
                }
            }

            if (settings.displaySetting.showTokenUsage) {
                conversation.tokenUsage?.let { usage ->
                    item(TokenUsageItemKey) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(
                                8.dp,
                                Alignment.CenterHorizontally
                            ),
                        ) {
                            Text(
                                text = "Tokens: ${usage.totalTokens}  (${usage.promptTokens} -> ${usage.completionTokens})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }

            // 为了能正确滚动到这
            item(ScrollBottomKey) {
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                )
            }
        }

        AnimatedVisibility(
            state.canScrollForward,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                modifier = Modifier.padding(8.dp),
                onClick = {
                    scrollToBottom()
                },
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Lucide.ChevronDown,
                        contentDescription = "Scroll to bottom",
                        modifier = Modifier.size(16.dp)
                    )
                    Text("滚动到底部", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    conversation: Conversation,
    drawerState: DrawerState,
    onClickMenu: () -> Unit,
    onNewChat: () -> Unit,
    onUpdateTitle: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    val titleState = useEditState<String> {
        onUpdateTitle(it)
    }

    TopAppBar(
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
            Surface(
                onClick = {
                    if (conversation.messages.isNotEmpty()) {
                        titleState.open(conversation.title)
                    } else {
                        toaster.show("请先聊天再编辑标题吧", type = ToastType.Warning)
                    }
                }
            ) {
                Text(
                    text = conversation.title.ifBlank { "新聊天" },
                    maxLines = 1,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    onClickMenu()
                }
            ) {
                Icon(Lucide.Menu, "Menu")
            }

            IconButton(
                onClick = {
                    onNewChat()
                }
            ) {
                Icon(Lucide.MessageCirclePlus, "New Message")
            }
        },
    )
    titleState.EditStateContent { title, onUpdate ->
        AlertDialog(
            onDismissRequest = {
                titleState.dismiss()
            },
            title = {
                Text("编辑标题")
            },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = onUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        titleState.confirm()
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        titleState.dismiss()
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DrawerContent(
    navController: NavController,
    vm: ChatVM,
    settings: Settings,
    current: Conversation,
    conversations: List<Conversation>,
    loading: Boolean,
) {
    ModalDrawerSheet(
        modifier = Modifier.width(270.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            UpdateCard(vm)
            ConversationList(
                current = current,
                conversations = conversations,
                loadings = if (loading) listOf(current.id) else emptyList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onClick = {
                    navController.navigate("chat/${it.id}") {
                        popUpTo("chat/${current.id}") {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
                onRegenerateTitle = {
                    vm.generateTitle(it, true)
                },
                onDelete = {
                    vm.deleteConversation(it)
                    if (it.id == current.id) {
                        navigateToChatPage(navController)
                    }
                }
            )
            AssistantPicker(
                settings = settings,
                onUpdateSettings = { vm.updateSettings(it) },
                modifier = Modifier.fillMaxWidth(),
                onClickSetting = {
                    navController.navigate("assistant")
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        navController.navigate("history")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Lucide.History, "Chat History")
                    Text("聊天历史", modifier = Modifier.padding(start = 4.dp))
                }
                TextButton(
                    onClick = {
                        navController.navigate("setting")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Lucide.Settings, stringResource(R.string.settings))
                    Text(
                        stringResource(R.string.settings),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun UpdateCard(vm: ChatVM) {
    val state by vm.updateState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val toaster = LocalToaster.current
    state.onError {
        Card {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "检查更新失败",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = it.message ?: "未知错误",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
    state.onSuccess { info ->
        var showDetail by remember { mutableStateOf(false) }
        val current = remember { Version(BuildConfig.VERSION_NAME) }
        val latest = remember(info) { Version(info.version) }
        if (latest > current) {
            Card(
                onClick = {
                    showDetail = true
                }
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "发现新版本 ${info.version}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    MarkdownBlock(
                        content = info.changelog,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        if (showDetail) {
            val downloadHandler = useThrottle<UpdateDownload>(500) { item ->
                vm.updateChecker.downloadUpdate(context, item)
                showDetail = false
                toaster.show("已在下载，请在状态栏查看下载进度", type = ToastType.Info)
            }
            ModalBottomSheet(
                onDismissRequest = { showDetail = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = info.version,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = Instant.parse(info.publishedAt).toJavaInstant().toLocalDateTime()
                            .toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    MarkdownBlock(
                        content = info.changelog,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    info.downloads.fastForEach { downloadItem ->
                        Card(
                            onClick = {
                                downloadHandler(downloadItem)
                            }
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = downloadItem.name,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        text = downloadItem.size
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
