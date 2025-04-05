package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.material3.TextButton
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
import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.TextGenerationParams
import me.rerere.ai.provider.providers.OpenAIProvider
import me.rerere.ai.ui.Conversation
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.handleMessageChunk
import me.rerere.rikkahub.ui.components.chat.ChatInput
import me.rerere.rikkahub.ui.components.chat.ChatMessage
import me.rerere.rikkahub.ui.components.icons.ListTree
import me.rerere.rikkahub.ui.components.icons.MessageCirclePlus
import me.rerere.rikkahub.ui.components.icons.Settings
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.heroAnimation
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChatPage(vm: ChatVM = koinViewModel()) {
    val navController = LocalNavController.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val setting = ProviderSetting.OpenAI(
        enabled = true,
        name = "CloseAI",
        apiKey = "sk-8Jf80gTBWPL5mqPtuWpbNwfCHo7n8TcNUXCVx98i8cpIW1hf",
        baseUrl = "https://api.openai-proxy.org/v1",
        models = emptyList(),
    )
    var conversation by remember {
        mutableStateOf(
            Conversation.empty(),
        )
    }

    fun handleSend(message: String) {
        conversation = conversation.copy(
            messages = conversation.messages + UIMessage.ofText(MessageRole.USER, message)
        )
        scope.launch {
            flow {
                emit(
                    withContext(Dispatchers.IO) {

                        OpenAIProvider.generateText(
                            providerSetting = setting,
                            conversation = conversation,
                            params = TextGenerationParams(
                                model = Model("gpt-4o")
                            )

                        )
                    }
                )
            }.onEach {
                println(it)
                val messages = conversation.messages.handleMessageChunk(it).toList()
                conversation = conversation.copy(
                    messages = messages
                )
                println(conversation.messages)
            }.catch {
                println(it)
            }.collect()
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
                TopBar(scope, drawerState, navController) {
                    conversation = Conversation.empty()
                }
            },
            bottomBar = {
                ChatInput {
                    handleSend(message = it)
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
            ) {

                Card(
                    modifier = Modifier.heroAnimation("setting_card"),
                    onClick = {
                        
                    }
                ) {
                    Box(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text("设置")
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    state = chatListState
                ) {
                    items(conversation.messages, key = { it.id }) {
                        ChatMessage(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(
    scope: CoroutineScope,
    drawerState: DrawerState,
    navController: NavController,
    onNewChat: () -> Unit,
) {
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
            TextButton(onClick = {}) {
                Text("DeepSeek R1")
            }
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
            items(100) {
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

val content = """
    ## Text
    This is a text. **This is a bold text.** *This is a italic text.* ~~This is a strikethrough text.~~
    
    ### Heading 3
    This is a heading 3.
    
    ### Heading 4
    This is a heading 4.
    
    ## Code
    ```java
    public class Main {
        public static void main(String[] args) {
            System.out.println("Hello, World!");
        }
    }
    ```
    
    ## LaTex Math
    $$
    a^2 + b^2 = c^2
    $$
    
    and $ f(x) = x^2 $
    
    ## Image
    ![Image](https://picsum.photos/200/300)
    
    ## Link
    [Link](https://www.google.com)
    
    ## List
    - List item 1
    - List item 2
    - List item 3
    
    ## Table
    | Column 1 | Column 2 | Column 3 |
    | -------- | -------- | -------- |
    | Row 1    | Row 1    | Row 1    |
    | Row 2    | Row 2    | Row 2    |
    | Row 3    | Row 3    | Row 3    |
    
    ## Blockquote
    > This is a blockquote.
    
    ## Horizontal Rule
    ---
    
    ## Empty Line
    
    This is a paragraph.
    
    This is another paragraph.
    
    ## Paragraph
    This is a paragraph.
""".trimIndent()