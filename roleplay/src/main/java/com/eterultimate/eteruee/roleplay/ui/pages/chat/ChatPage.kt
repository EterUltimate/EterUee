package com.eterultimate.eteruee.roleplay.ui.pages.chat

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.ui.viewmodel.ChatViewModel
import com.eterultimate.eteruee.roleplay.ui.components.MarkdownText
import com.eterultimate.eteruee.roleplay.ui.components.ChatSettingsDialog
import com.eterultimate.eteruee.roleplay.ui.components.ChatSettings
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
    
    // 设置对话框状态
    var showSettingsDialog by remember { mutableStateOf(false) }
    
    // 分支选择器状态
    var showBranchMenu by remember { mutableStateOf(false) }
    
    // 消息编辑对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    
    // 消息操作菜单状态
    var showActionMenu by remember { mutableStateOf(false) }
    var actionMenuMessageIndex by remember { mutableStateOf(-1) }
    var actionMenuMessage by remember { mutableStateOf<ChatMessage?>(null) }
    
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
                    // 分支选择器
                    if (uiState.branches.isNotEmpty()) {
                        Box {
                            IconButton(onClick = { showBranchMenu = true }) {
                                Icon(Icons.Default.AccountTree, contentDescription = "分支")
                            }
                            
                            DropdownMenu(
                                expanded = showBranchMenu,
                                onDismissRequest = { showBranchMenu = false }
                            ) {
                                uiState.branches.forEachIndexed { index, branch ->
                                    val isActive = branch.id == uiState.activeBranchId
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("分支 ${index + 1}")
                                                if (isActive) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = "当前分支",
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.switchBranch(branch.id)
                                            showBranchMenu = false
                                        }
                                    )
                                }
                                
                                HorizontalDivider()
                                
                                DropdownMenuItem(
                                    text = { Text("新建分支") },
                                    onClick = {
                                        // 从最后一条消息创建新分支
                                        if (uiState.messages.isNotEmpty()) {
                                            viewModel.createBranch(uiState.messages.size - 1)
                                        }
                                        showBranchMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // 设置按钮
                    IconButton(onClick = { 
                        showSettingsDialog = true
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
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Token 统计显示
                if (uiState.totalTokens > 0 || uiState.currentMessageTokens > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "总 Tokens: ${uiState.totalTokens}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (uiState.currentMessageTokens > 0) {
                            Text(
                                text = "当前: ${uiState.currentMessageTokens}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
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
                        
                        // 发送/停止按钮
                        IconButton(
                            onClick = {
                                if (uiState.isGenerating) {
                                    // 停止生成
                                    viewModel.stopGeneration()
                                } else if (inputText.isNotBlank()) {
                                    viewModel.sendMessage(inputText)
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank() || uiState.isGenerating
                        ) {
                            if (uiState.isGenerating) {
                                Icon(Icons.Default.Stop, contentDescription = "停止")
                            } else {
                                Icon(Icons.Default.Send, contentDescription = "发送")
                            }
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
                        },
                        onEdit = { msgId, content ->
                            viewModel.startEditMessage(msgId, content)
                            showEditDialog = true
                        },
                        onCreateBranch = { fromIndex ->
                            viewModel.createBranch(fromIndex)
                        }
                    )
                }
            }
        }
    }
    
    // 设置对话框
    if (showSettingsDialog) {
        ChatSettingsDialog(
            onDismiss = { showSettingsDialog = false },
            onSave = { settings ->
                // TODO: 保存设置到 ViewModel 或 DataStore
                showSettingsDialog = false
            }
        )
    }
    
    // 消息编辑对话框
    if (showEditDialog && uiState.editingMessageId != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.cancelEdit()
                showEditDialog = false
            },
            title = { Text("编辑消息") },
            text = {
                var editContent by remember(uiState.editContent) {
                    mutableStateOf(uiState.editContent)
                }
                
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 10,
                    placeholder = { Text("输入消息内容...") }
                )
                
                LaunchedEffect(editContent) {
                    // 实时更新编辑内容
                    // 注意：这里不直接调用 ViewModel，只在保存时更新
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveEditedMessage()
                    showEditDialog = false
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.cancelEdit()
                    showEditDialog = false
                }) {
                    Text("取消")
                }
            }
        )
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
    onRegenerate: (Int) -> Unit = {},
    onEdit: (kotlin.uuid.Uuid, String) -> Unit = { _, _ -> },
    onCreateBranch: (Int) -> Unit = {}
) {
    val isUser = message.role == MessageRole.USER
    var showMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Card(
            colors = if (isUser) {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            } else {
                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            },
            modifier = Modifier
                .widthIn(max = 300.dp)
                .pointerInput(Unit) {
                    // 长按显示菜单
                    detectTapGestures(
                        onLongPress = { showMenu = true }
                    )
                }
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
        
        // 消息操作菜单（长按触发）
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            // 编辑消息（所有消息都可编辑）
            DropdownMenuItem(
                text = { Text("编辑") },
                onClick = {
                    showMenu = false
                    onEdit(message.id, message.content)
                },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null)
                }
            )
            
            // 从此处新建分支
            DropdownMenuItem(
                text = { Text("从此处新建分支") },
                onClick = {
                    showMenu = false
                    onCreateBranch(messageIndex)
                },
                leadingIcon = {
                    Icon(Icons.Default.AccountTree, contentDescription = null)
                }
            )
            
            // 重新生成（仅助手消息）
            if (!isUser && !isStreaming) {
                DropdownMenuItem(
                    text = { Text("重新生成") },
                    onClick = {
                        showMenu = false
                        onRegenerate(messageIndex)
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                    }
                )
            }
            
            HorizontalDivider()
            
            // 删除消息
            DropdownMenuItem(
                text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDelete()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            )
        }
    }
}
