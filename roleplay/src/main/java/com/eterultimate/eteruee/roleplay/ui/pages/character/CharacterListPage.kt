package com.eterultimate.eteruee.roleplay.ui.pages.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.domain.service.CharacterSortOption
import com.eterultimate.eteruee.roleplay.ui.viewmodel.CharacterListViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * 角色列表页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListPage(
    onCharacterClick: (Character) -> Unit,
    onCreateCharacter: () -> Unit,
    viewModel: CharacterListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagingItems = viewModel.loadCharacters().collectAsLazyPagingItems()
    val selectedIds by viewModel.selectedCharacterIds.collectAsState()
    val isMultiSelectMode by viewModel.isMultiSelectMode.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val favoriteOnly by viewModel.favoriteOnly.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // 加载可用标签
    LaunchedEffect(Unit) {
        viewModel.loadAvailableTags()
    }
    
    Scaffold(
        topBar = {
            if (isMultiSelectMode) {
                // 多选模式顶栏
                TopAppBar(
                    title = { Text("已选择 ${selectedIds.size} 个") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.toggleMultiSelectMode() }) {
                            Icon(Icons.Default.Search, contentDescription = "退出多选")
                        }
                    },
                    actions = {
                        IconButton(onClick = { 
                            // 全选/取消全选
                            val allIds = if (isSearching && searchQuery.isNotBlank()) {
                                uiState.searchResults.map { it.id }
                            } else {
                                (0 until pagingItems.itemCount).mapNotNull { pagingItems[it]?.id }
                            }
                            viewModel.toggleSelectAll(allIds)
                        }) {
                            Icon(
                                if (selectedIds.size == (if (isSearching && searchQuery.isNotBlank()) uiState.searchResults.size else pagingItems.itemCount))
                                    Icons.Default.CheckCircle
                                else
                                    Icons.Default.RadioButtonUnchecked,
                                contentDescription = "全选"
                            )
                        }
                        IconButton(onClick = { viewModel.batchDeleteCharacters() }) {
                            Icon(Icons.Default.Delete, contentDescription = "批量删除")
                        }
                    }
                )
            } else {
                // 普通模式顶栏
                TopAppBar(
                    title = { Text("角色列表") },
                    actions = {
                        // 收藏过滤开关
                        IconButton(onClick = { viewModel.toggleFavoriteFilter() }) {
                            Icon(
                                imageVector = if (favoriteOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "收藏过滤",
                                tint = if (favoriteOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // 排序选项下拉菜单
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "排序")
                            }
                            
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                CharacterSortOption.entries.forEach { option ->
                                    val isSelected = option == sortOption
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(getSortOptionText(option))
                                                if (isSelected) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            viewModel.setSortOption(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        IconButton(onClick = { viewModel.toggleMultiSelectMode() }) {
                            Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "多选模式")
                        }
                        IconButton(onClick = {
                            isSearching = !isSearching
                            if (!isSearching) {
                                viewModel.clearSearch()
                                searchQuery = ""
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                        IconButton(onClick = onCreateCharacter) {
                            Icon(Icons.Default.Add, contentDescription = "创建角色")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 搜索栏
            if (isSearching) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        if (query.isNotBlank()) {
                            viewModel.searchCharacters(query)
                        } else {
                            viewModel.clearSearch()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    placeholder = { Text("搜索角色...") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                )
            }
            
            // 标签过滤器
            if (availableTags.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableTags) { tag ->
                        val isSelected = selectedTags.contains(tag)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.toggleTag(tag) },
                            label = { Text(tag) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                    
                    // 清除按钮
                    if (selectedTags.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.clearSelectedTags() },
                                label = { Text("清除") }
                            )
                        }
                    }
                }
            }
            
            // 错误/成功提示
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(error)
                }
            }
            
            uiState.successMessage?.let { success ->
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(success)
                }
            }
            
            // 角色列表
            if (isSearching && searchQuery.isNotBlank()) {
                // 显示搜索结果
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.searchResults) { character ->
                        CharacterCard(
                            character = character,
                            onClick = { 
                                if (isMultiSelectMode) {
                                    viewModel.toggleCharacterSelection(character.id)
                                } else {
                                    onCharacterClick(character) 
                                }
                            },
                            onFavoriteClick = { viewModel.toggleFavorite(character.id) },
                            onDeleteClick = { viewModel.deleteCharacter(character.id) },
                            isSelected = selectedIds.contains(character.id),
                            isMultiSelectMode = isMultiSelectMode
                        )
                    }
                }
            } else {
                // 显示所有角色(分页)
                val isInitialLoading = pagingItems.loadState.refresh is LoadState.Loading
                val isEmpty = pagingItems.loadState.refresh is LoadState.NotLoading && pagingItems.itemCount == 0

                if (isInitialLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (isEmpty) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("暂无角色", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onCreateCharacter) {
                                Text("创建第一个角色")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pagingItems.itemCount) { index ->
                            pagingItems[index]?.let { character ->
                                CharacterCard(
                                    character = character,
                                    onClick = {
                                        if (isMultiSelectMode) {
                                            viewModel.toggleCharacterSelection(character.id)
                                        } else {
                                            onCharacterClick(character)
                                        }
                                    },
                                    onFavoriteClick = { viewModel.toggleFavorite(character.id) },
                                    onDeleteClick = { viewModel.deleteCharacter(character.id) },
                                    isSelected = selectedIds.contains(character.id),
                                    isMultiSelectMode = isMultiSelectMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 角色卡片
 */
@Composable
fun CharacterCard(
    character: Character,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 多选复选框
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            
            // 头像
            AsyncImage(
                model = character.avatarUrl,
                contentDescription = character.name,
                modifier = Modifier
                    .size(64.dp)
                    .padding(end = 12.dp)
            )
            
            // 信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = character.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "对话数: ${character.chatCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (character.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = character.tags.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // 操作按钮
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (character.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (character.favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
            }
            
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * 获取排序选项的显示文本
 */
@Composable
fun getSortOptionText(option: CharacterSortOption): String {
    return when (option) {
        CharacterSortOption.NAME_ASC -> "名称升序"
        CharacterSortOption.NAME_DESC -> "名称降序"
        CharacterSortOption.LAST_CHAT_DESC -> "最后聊天时间（最近优先）"
        CharacterSortOption.LAST_CHAT_ASC -> "最后聊天时间（最早优先）"
        CharacterSortOption.CREATED_DESC -> "创建时间（最新优先）"
        CharacterSortOption.CREATED_ASC -> "创建时间（最早优先）"
        CharacterSortOption.CHAT_COUNT_DESC -> "聊天数量（最多优先）"
    }
}
