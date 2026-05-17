package com.eterultimate.eteruee.roleplay.ui.pages.group

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
import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.ui.viewmodel.GroupEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.GroupProperty
import org.koin.androidx.compose.koinViewModel

/**
 * 群组编辑页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupEditPage(
    groupId: kotlin.uuid.Uuid? = null, // null表示创建新群组
    onSaveSuccess: () -> Unit,
    viewModel: GroupEditViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 初始化
    LaunchedEffect(groupId) {
        if (groupId != null) {
            // 从服务加载群组进行编辑
            viewModel.loadGroupForEdit(groupId)
        } else {
            viewModel.initializeForCreate()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "编辑群组" else "创建群组") },
                actions = {
                    // 保存按钮
                    IconButton(
                        onClick = { viewModel.saveGroup() },
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
                value = uiState.group.name,
                onValueChange = { viewModel.updateGroupProperty(GroupProperty.NAME, it) },
                label = { Text("群组名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.group.description,
                onValueChange = { viewModel.updateGroupProperty(GroupProperty.DESCRIPTION, it) },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            // 成员列表
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
                            text = "成员 (${uiState.group.members.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        // TODO: 添加成员功能（需要从角色列表中选择）
                        IconButton(onClick = { 
                            // 暂时禁用，需要实现角色选择器
                        }, enabled = false) {
                            Icon(Icons.Default.Add, contentDescription = "添加成员")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 成员列表
                    if (uiState.group.members.isEmpty()) {
                        Text(
                            text = "暂无成员",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        uiState.group.members.forEach { member ->
                            MemberCard(
                                member = member,
                                onRemove = {
                                    viewModel.removeMember(member.characterId)
                                }
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    
                    // 提示信息
                    Text(
                        text = "提示：成员管理功能需要在聊天页面中实现",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * 成员卡片
 */
@Composable
fun MemberCard(
    member: com.eterultimate.eteruee.roleplay.data.model.GroupMember,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = member.name.ifBlank { "成员 ID: ${member.characterId.toString().take(8)}..." },
                style = MaterialTheme.typography.bodyMedium
            )
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "移除成员",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
