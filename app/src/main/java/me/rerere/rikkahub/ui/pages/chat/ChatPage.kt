package me.rerere.rikkahub.ui.pages.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.rerere.rikkahub.ui.components.HighlightCodeBlock
import me.rerere.rikkahub.ui.components.MarkdownBlock
import me.rerere.rikkahub.ui.components.chat.ChatInput
import me.rerere.rikkahub.ui.components.icons.ListTree
import me.rerere.rikkahub.ui.components.icons.MessageCirclePlus
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.heroAnimation
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChatPage(vm: ChatVM = koinViewModel()) {
    val navController = LocalNavController.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("DeepSeek R1")
                    Text("DeepSeek R1")
                    Text("DeepSeek R1")
                    Text("DeepSeek R1")
                    Text("DeepSeek R1")
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
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
                                navController.popBackStack()
                            }
                        ) {
                            Icon(MessageCirclePlus, "New Message")
                        }
                    },
                )
            },
            bottomBar = {
                ChatInput()
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                Card(
                    modifier = Modifier.heroAnimation("setting_card"),
                    onClick = {
                        navController.navigate("setting")
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
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        HighlightCodeBlock(
                            """
                        fun helloWorld() {
                            console.log("Hello, World!");
                        }
                    """.trimIndent(), "java"
                        )
                    }

                    item {
                        Card {
                            MarkdownBlock(content, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }
        }
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