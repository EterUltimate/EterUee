package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
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
import me.rerere.rikkahub.ui.components.MarkdownBlock
import me.rerere.rikkahub.ui.components.ToastVariant
import me.rerere.rikkahub.ui.components.chat.ChatInput
import me.rerere.rikkahub.ui.components.chat.ChatMessage
import me.rerere.rikkahub.ui.components.chat.ModelSelector
import me.rerere.rikkahub.ui.components.chat.rememberChatInputState
import me.rerere.rikkahub.ui.components.icons.ListTree
import me.rerere.rikkahub.ui.components.icons.MessageCirclePlus
import me.rerere.rikkahub.ui.components.icons.Settings
import me.rerere.rikkahub.ui.components.rememberToastState
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChatPage(vm: ChatVM = koinViewModel()) {
    val navController = LocalNavController.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val toastState = rememberToastState()

    val setting by vm.settings.collectAsStateWithLifecycle()
    val model = setting.providers.findModelById(setting.chatModelId)
    val providerSetting = model?.findProvider(setting.providers)

    var conversation by remember {
        mutableStateOf(
            Conversation.empty(),
        )
    }

    val inputState = rememberChatInputState()

    fun handleSend() {
        conversation = conversation.copy(
            messages = conversation.messages + UIMessage(
                role = MessageRole.USER,
                parts = inputState.messageContent
            )
        )
        inputState.reset()
        scope.launch {
            inputState.loading = true
            providerSetting?.let {
                val providerImpl =
                    ProviderManager.getProviderByType(it)
                providerImpl?.let {
                    providerImpl.streamText(
                        providerSetting, conversation, TextGenerationParams(
                            model = model,
                        )
                    ).onEach {
                        val messages = conversation.messages.handleMessageChunk(it).toList()
                        conversation = conversation.copy(
                            messages = messages
                        )
                        println(conversation.messages)
                    }.catch {
                        it.printStackTrace()
                    }.collect()
                }
            } ?: run {
                toastState.show("请配置模型", ToastVariant.ERROR)
            }
        }.invokeOnCompletion {
            inputState.loading = false
        }
    }

    val chatListState = rememberLazyListState()
    LaunchedEffect(conversation) {
        if (!chatListState.isScrollInProgress && conversation.messages.size > 1) {
            chatListState.scrollToItem(conversation.messages.size)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(navController)
        }
    ) {
        Scaffold(
            topBar = {
                TopBar(scope, drawerState, vm) {
                    conversation = Conversation.empty()
                }
            },
            bottomBar = {
                ChatInput(
                    state = inputState,
                    onCancelClick = {},
                    onSendClick = {
                        handleSend()
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
            LazyColumn(
                contentPadding = innerPadding + PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                state = chatListState
            ) {
                item {
                    MarkdownBlock("""
                        这是一个Paragraph，您可以在这里输入您的消息。各种元素的 $ f(x)=2x+3y+z+12+2z+5+12+erfe $ 并且可以内联 `代码`，或者使用代码块
                        ```java
                        这是一个代码块
                        ```
                        好吧，还不错吧，也可以 $ f(x)=x^2 $ 这样的数学公式
                        
                        ![图片](https://plus.unsplash.com/premium_photo-1676923902105-19d5c90d585c?w=500&auto=format&fit=crop&q=60&ixlib=rb-4.0.3&ixid=M3wxMjA3fDB8MHxmZWF0dXJlZC1waG90b3MtZmVlZHwxfHx8ZW58MHx8fHx8)
                        
                        这是一段代码
                        ```js
                        const a = 1
                        const b = 2
                        ```
                        
                        [GitHub](https://github.com) 违法沃尔夫wfewrf3erferf4二`分日狗`太贵违法二哥他和人不过去的柔肤乳3搞挺尴尬太高太高
                        
                        #### 标题测试 **哈哈**
                        1. 第一项
                        2. 第二项
                        
                        
                    """.trimIndent())
                }

                items(conversation.messages, key = { it.id }) {
                    ChatMessage(it)
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    scope: CoroutineScope,
    drawerState: DrawerState,
    vm: ChatVM,
    onNewChat: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(
                onClick = {
                    scope.launch { drawerState.open() }
                }
            ) {
                Icon(ListTree, "Messages")
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
                Icon(MessageCirclePlus, "New Message")
            }
        },
    )
}

@Composable
private fun DrawerContent(navController: NavController) {
    ModalDrawerSheet(
        modifier = Modifier.width(270.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .padding(8.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(5) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .combinedClickable(
                            onClick = {

                            },
                            onLongClick = {

                            }
                        )
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("???")
                }
            }
        }
        HorizontalDivider()
        NavigationDrawerItem(
            label = {
                Text("Setting")
            },
            icon = {
                Icon(Settings, "Setting")
            },
            onClick = {
                navController.navigate("setting")
            },
            selected = false,
            modifier = Modifier.wrapContentWidth()
        )
    }
}