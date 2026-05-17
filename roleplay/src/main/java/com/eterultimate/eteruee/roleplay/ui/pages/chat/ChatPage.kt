package com.eterultimate.eteruee.roleplay.ui.pages.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.ui.viewmodel.ChatViewModel
import com.eterultimate.eteruee.roleplay.ui.components.MarkdownText
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

/**
 * 聊天页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatPage(
    chatId: Uuid,
    onBackClick: () -> Unit,
    onRegenerate: (Int) -> Unit = {},
    viewModel: ChatViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    
    // 输入框状态
    var inputText by remember { mutableStateOf("") }
    
    // 初始化
    LaunchedEffect(chatId) {
        viewModel.initialize(chatId)
    }
    
    // 自动滚动到底部
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(uiState.chat?.title ?: "聊天")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 设置按钮
                    IconButton(onClick = { 
                        // TODO: 打开聊天设置对话框
                        // 可以配置模型、温度、最大token等参数
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                    
                    // 清空对话按钮
                    IconButton(onClick = { 
                        viewModel.clearAllMessages()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "清空对话")
                    }
                }
            )
        },
        bottomBar = {
            // 输入框
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("输入消息...") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            }
                        ),
                        maxLines = 4,
                        enabled = !uiState.isGenerating
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 发送按钮
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !uiState.isGenerating
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Send, contentDescription = "发送")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    // 加载状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                uiState.errorMessage != null -> {
                    // 错误提示
                    Snackbar(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp),
                        action = {
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("关闭")
                            }
                        }
                    ) {
                        Text(uiState.errorMessage!!)
                    }
                }
            }
            
            // 消息列表
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                reverseLayout = true, // 反转布局,最新消息在底部
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uiState.messages) { index, message ->
                    MessageBubble(
                        message = message,
                        messageIndex = index,
                        isStreaming = message.id.toString().startsWith("streaming_"),
                        onDelete = { viewModel.deleteMessage(message.id) },
                        onRegenerate = { idx -> 
                            // 重新生成消息
                            viewModel.regenerateMessage(message.id)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 消息气泡
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    messageIndex: Int,
    isStreaming: Boolean,
    onDelete: () -> Unit,
    onRegenerate: (Int) -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = if (isUser) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            },
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // 消息内容 - 使用 Markdown 渲染
                MarkdownText(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    enableCodeHighlight = true,
                    showCopyButton = !isUser // 仅助手消息显示复制按钮
                )
                
                // 流式指示器
                if (isStreaming) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "生成中...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
                
                // 消息操作(仅助手消息)
                if (!isUser && !isStreaming) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(onClick = { onRegenerate(messageIndex) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "重新生成", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("重新生成", style = MaterialTheme.typography.labelSmall)
                        }
                        
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("删除", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
