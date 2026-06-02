package com.eterultimate.eteruee.roleplay.ui.pages.worldinfo

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoListViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * 世界书列表页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldInfoListPage(
    onWorldInfoClick: (WorldInfo) -> Unit = {},
    onCreateWorldInfo: () -> Unit = {},
    viewModel: WorldInfoListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::importWorldInfo)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("世界书") },
                actions = {
                    IconButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "text/plain")) },
                        enabled = !uiState.isImporting
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = "导入世界书")
                    }
                    IconButton(onClick = onCreateWorldInfo) {
                        Icon(Icons.Default.Add, contentDescription = "创建世界书")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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

            if (uiState.worldInfos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无世界书", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onCreateWorldInfo) {
                                Text("创建第一个世界书")
                            }
                            OutlinedButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "text/json", "text/plain"))
                                },
                                enabled = !uiState.isImporting
                            ) {
                                Text(if (uiState.isImporting) "导入中..." else "导入世界书")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.worldInfos) { worldInfo ->
                        WorldInfoCard(
                            worldInfo = worldInfo,
                            onClick = { onWorldInfoClick(worldInfo) },
                            onDeleteClick = { viewModel.deleteWorldInfo(worldInfo.id) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 世界书卡片
 */
@Composable
fun WorldInfoCard(
    worldInfo: WorldInfo,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = worldInfo.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "条目数: ${worldInfo.entries.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (worldInfo.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = worldInfo.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
