package com.eterultimate.eteruee.roleplay.ui.pages.worldinfo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.data.model.InsertionPosition
import com.eterultimate.eteruee.roleplay.domain.service.PromptBuilderService
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoTestViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.WorldInfoTestUiState
import org.koin.androidx.compose.koinViewModel
import java.time.Instant

/**
 * 世界书测试页面
 * 用于测试 PromptBuilderService 的 Prompt 组装功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldInfoTestPage(
    onNavigateBack: () -> Unit,
    viewModel: WorldInfoTestViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("世界书测试") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 测试配置区域
            TestConfigSection(viewModel = viewModel, uiState = uiState)
            
            // 生成的 Prompt 预览
            PromptPreviewSection(prompt = uiState.generatedPrompt)
        }
    }
}

@Composable
private fun TestConfigSection(
    viewModel: WorldInfoTestViewModel,
    uiState: WorldInfoTestUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "测试配置",
                style = MaterialTheme.typography.titleMedium
            )
            
            // 系统提示词输入
            OutlinedTextField(
                value = uiState.systemPrompt,
                onValueChange = { viewModel.updateSystemPrompt(it) },
                label = { Text("系统提示词") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            // 用户消息输入
            OutlinedTextField(
                value = uiState.userMessage,
                onValueChange = { viewModel.updateUserMessage(it) },
                label = { Text("用户消息") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("输入测试消息...") }
            )
            
            // 世界书条目示例
            Text(
                text = "世界书条目示例（已内置）:",
                style = MaterialTheme.typography.bodySmall
            )
            
            uiState.sampleEntries.forEach { entry ->
                EntryChip(entry = entry)
            }
            
            // 生成按钮
            Button(
                onClick = { viewModel.generatePrompt() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.systemPrompt.isNotBlank() && uiState.userMessage.isNotBlank()
            ) {
                Text("生成 Prompt")
            }
        }
    }
}

@Composable
private fun EntryChip(entry: WorldInfoEntry) {
    AssistChip(
        onClick = { },
        label = { 
            Text(
                text = "[${entry.getAllKeys().joinToString(", ")}]: ${entry.content.take(30)}...",
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun PromptPreviewSection(prompt: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "生成的 Prompt",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (prompt.isNotBlank()) {
                    FilledTonalButton(onClick = { /* TODO: 复制功能 */ }) {
                        Text("复制")
                    }
                }
            }
            
            if (prompt.isBlank()) {
                Text(
                    text = "点击“生成 Prompt”按钮查看结果...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp, max = 400.dp),
                    color = MaterialTheme.colorScheme.background,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        }
    }
}
