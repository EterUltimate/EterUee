package com.eterultimate.eteruee.roleplay.ui.pages.worldinfo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoProperty
import org.koin.androidx.compose.koinViewModel

/**
 * 世界书编辑页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldInfoEditPage(
    worldInfoId: kotlin.uuid.Uuid? = null, // null表示创建新世界书
    onSaveSuccess: () -> Unit,
    viewModel: WorldInfoEditViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 初始化
    LaunchedEffect(worldInfoId) {
        if (worldInfoId != null) {
            // 从服务加载世界书进行编辑
            viewModel.loadWorldInfoForEdit(worldInfoId)
        } else {
            viewModel.initializeForCreate()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "编辑世界书" else "创建世界书") },
                actions = {
                    // 保存按钮
                    IconButton(
                        onClick = { viewModel.saveWorldInfo() },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "保存")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 错误/成功提示
            uiState.errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
                    action = {
                        TextButton(onClick = { viewModel.clearMessage() }) {
                            Text("关闭")
                        }
                    }
                ) {
                    Text(success)
                }
            }
            
            // 基本信息
            OutlinedTextField(
                value = uiState.worldInfo.name,
                onValueChange = { viewModel.updateWorldInfoProperty(WorldInfoProperty.NAME, it) },
                label = { Text("世界书名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.worldInfo.description,
                onValueChange = { viewModel.updateWorldInfoProperty(WorldInfoProperty.DESCRIPTION, it) },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            // 条目列表
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "条目 (${uiState.worldInfo.entries.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        IconButton(onClick = { viewModel.addEntry() }) {
                            Icon(Icons.Default.Add, contentDescription = "添加条目")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 条目列表
                    uiState.worldInfo.entries.forEachIndexed { index, entry ->
                        EntryCard(
                            entry = entry,
                            index = index,
                            onUpdate = { updatedEntry ->
                                viewModel.updateEntry(index, updatedEntry)
                            },
                            onDelete = {
                                viewModel.removeEntry(index)
                            }
                        )
                        
                        if (index < uiState.worldInfo.entries.size - 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    if (uiState.worldInfo.entries.isEmpty()) {
                        Text(
                            text = "暂无条目，点击右上角 + 添加",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 条目卡片
 */
@Composable
fun EntryCard(
    entry: WorldInfoEntry,
    index: Int,
    onUpdate: (WorldInfoEntry) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "条目 ${index + 1}",
                    style = MaterialTheme.typography.titleSmall
                )
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除条目",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = entry.keys.joinToString(", "),
                onValueChange = { 
                    val keysList = it.split(",").map { key -> key.trim() }.filter { key -> key.isNotBlank() }
                    onUpdate(entry.copy(keys = keysList)) 
                },
                label = { Text("关键词（逗号分隔）") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = entry.content,
                onValueChange = { onUpdate(entry.copy(content = it)) },
                label = { Text("内容") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}
