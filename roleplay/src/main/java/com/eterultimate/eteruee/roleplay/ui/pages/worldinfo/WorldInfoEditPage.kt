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
import com.eterultimate.eteruee.roleplay.data.model.InsertionPosition
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoProperty
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

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
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("基础信息", style = MaterialTheme.typography.titleMedium)
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
                }
            }
            
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
                            text = "触发条目 (${uiState.worldInfo.entries.size})",
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
                            text = "暂无条目，点击右上角 + 添加触发规则",
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
                value = entry.comment,
                onValueChange = { onUpdate(entry.copy(comment = it)) },
                label = { Text("条目名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

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
                value = entry.secondaryKeys.joinToString(", "),
                onValueChange = {
                    val keysList = it.split(",").map { key -> key.trim() }.filter { key -> key.isNotBlank() }
                    onUpdate(entry.copy(secondaryKeys = keysList))
                },
                label = { Text("次级关键词（逗号分隔）") },
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

            Spacer(modifier = Modifier.height(12.dp))

            WorldInfoEntryControls(
                entry = entry,
                onUpdate = onUpdate
            )
        }
    }
}

@Composable
private fun WorldInfoEntryControls(
    entry: WorldInfoEntry,
    onUpdate: (WorldInfoEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("启用条目")
            Switch(
                checked = entry.enabled,
                onCheckedChange = { onUpdate(entry.copy(enabled = it)) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("始终注入")
            Switch(
                checked = entry.constant,
                onCheckedChange = { onUpdate(entry.copy(constant = it)) }
            )
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("触发概率")
                Text("${(entry.probability.coerceIn(0f, 1f) * 100).roundToInt()}%")
            }
            Slider(
                value = entry.probability.coerceIn(0f, 1f),
                onValueChange = { onUpdate(entry.copy(probability = it, useProbability = it < 1f)) },
                valueRange = 0f..1f
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = entry.order.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { onUpdate(entry.copy(order = it)) }
                },
                label = { Text("顺序") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = entry.depth.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let { onUpdate(entry.copy(depth = it.coerceAtLeast(0))) }
                },
                label = { Text("扫描深度") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Text("插入位置", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InsertionPosition.entries.forEach { position ->
                AssistChip(
                    onClick = { onUpdate(entry.copy(position = position)) },
                    label = { Text(position.displayName()) },
                    leadingIcon = if (entry.position == position) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    }
                )
            }
        }
    }
}

private fun InsertionPosition.displayName(): String {
    return when (this) {
        InsertionPosition.AFTER_SYSTEM_PROMPT -> "系统后"
        InsertionPosition.BEFORE_LAST_USER_MESSAGE -> "末条前"
        InsertionPosition.AT_END -> "末尾"
    }
}
