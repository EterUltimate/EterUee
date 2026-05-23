package com.eterultimate.eteruee.roleplay.ui.pages.bookmark

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.roleplay.data.model.Bookmark
import com.eterultimate.eteruee.roleplay.ui.viewmodel.ChatViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.Uuid

/**
 * 书签管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkPage(
    chatId: Uuid,
    onBackClick: () -> Unit,
    onBookmarkClick: (Int) -> Unit = {},  // 新增：点击书签回调，传递消息索引
    viewModel: ChatViewModel = koinViewModel()
) {
    val bookmarks by viewModel.bookmarks.collectAsState()
    
    // 搜索状态
    var searchQuery by remember { mutableStateOf("") }
    
    // 多选模式状态
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedBookmarkIds = remember { mutableStateListOf<Uuid>() }
    
    // 过滤后的书签列表
    val filteredBookmarks = remember(bookmarks, searchQuery) {
        if (searchQuery.isBlank()) {
            bookmarks
        } else {
            val query = searchQuery.lowercase()
            bookmarks.filter { bookmark ->
                bookmark.title.lowercase().contains(query) ||
                bookmark.note.lowercase().contains(query)
            }
        }
    }
    
    // 编辑书签对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingBookmark by remember { mutableStateOf<Bookmark?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editNote by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        if (isMultiSelectMode) {
                            Text("已选择 ${selectedBookmarkIds.size} 项")
                        } else {
                            Text("书签管理")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        // 多选模式切换按钮
                        IconButton(onClick = {
                            isMultiSelectMode = !isMultiSelectMode
                            if (!isMultiSelectMode) {
                                selectedBookmarkIds.clear()
                            }
                        }) {
                            Icon(
                                if (isMultiSelectMode) Icons.Default.Close else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (isMultiSelectMode) "退出多选" else "多选"
                            )
                        }
                        
                        // 批量删除按钮（仅多选模式显示）
                        if (isMultiSelectMode && selectedBookmarkIds.isNotEmpty()) {
                            IconButton(onClick = {
                                selectedBookmarkIds.forEach { id ->
                                    viewModel.deleteBookmark(id)
                                }
                                selectedBookmarkIds.clear()
                                isMultiSelectMode = false
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "批量删除",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                )
                
                // 搜索栏
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("搜索标题或备注...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }
    ) { paddingValues ->
        if (filteredBookmarks.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "未找到匹配的书签" else "暂无书签",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty()) "尝试其他关键词" else "长按消息可添加书签",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // 书签列表
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredBookmarks) { bookmark ->
                    BookmarkItem(
                        bookmark = bookmark,
                        onDelete = { viewModel.deleteBookmark(bookmark.id) },
                        onEdit = {
                            editingBookmark = bookmark
                            editTitle = bookmark.title
                            editNote = bookmark.note
                            showEditDialog = true
                        },
                        onClick = {
                            if (isMultiSelectMode) {
                                // 多选模式：切换选中状态
                                if (selectedBookmarkIds.contains(bookmark.id)) {
                                    selectedBookmarkIds.remove(bookmark.id)
                                } else {
                                    selectedBookmarkIds.add(bookmark.id)
                                }
                            } else {
                                // 普通模式：跳转到对应消息
                                onBookmarkClick(bookmark.messageIndex)
                            }
                        },
                        isSelected = selectedBookmarkIds.contains(bookmark.id),
                        isMultiSelectMode = isMultiSelectMode
                    )
                }
            }
        }
    }
    
    // 编辑书签对话框
    if (showEditDialog && editingBookmark != null) {
        AlertDialog(
            onDismissRequest = {
                showEditDialog = false
                editingBookmark = null
            },
            title = { Text("编辑书签") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("标题（可选）") },
                        placeholder = { Text("输入书签标题") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("备注（可选）") },
                        placeholder = { Text("输入备注说明") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    editingBookmark?.let { bm ->
                        viewModel.updateBookmark(bm.id, editTitle, editNote)
                    }
                    showEditDialog = false
                    editingBookmark = null
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditDialog = false
                    editingBookmark = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 书签列表项
 */
@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit = {},
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            },
        colors = if (isSelected) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        } else {
            CardDefaults.cardColors()
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 多选模式下的复选框
                if (isMultiSelectMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookmark.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (bookmark.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bookmark.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "消息索引: ${bookmark.messageIndex}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 操作菜单按钮
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                
                // 下拉菜单
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    
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
            
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "创建于: ${bookmark.createdAt.toString().take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
