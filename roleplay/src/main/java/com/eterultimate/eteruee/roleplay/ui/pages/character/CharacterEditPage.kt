package com.eterultimate.eteruee.roleplay.ui.pages.character

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.ui.viewmodel.CharacterEditViewModel
import com.eterultimate.eteruee.roleplay.ui.viewmodel.CharacterProperty
import org.koin.androidx.compose.koinViewModel

/**
 * 角色编辑页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditPage(
    characterId: kotlin.uuid.Uuid? = null, // null表示创建新角色
    onSaveSuccess: () -> Unit,
    viewModel: CharacterEditViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // 头像选择器
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateAvatar(uri)
        }
    }
    
    // PNG导出选择器
    val pngExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportPngCharacter(uri)
        }
    }
    
    // JSON导出选择器
    val jsonExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportJsonCharacter(uri)
        }
    }
    
    // 初始化
    LaunchedEffect(characterId) {
        if (characterId != null) {
            // 从服务加载角色进行编辑
            viewModel.loadCharacterForEdit(characterId)
        } else {
            viewModel.initializeForCreate()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "编辑角色" else "创建角色") },
                actions = {
                    // 导出按钮
                    if (uiState.isEditing) {
                        IconButton(onClick = { 
                            pngExporter.launch("${uiState.character.name}.png")
                        }) {
                            Icon(Icons.Default.Download, contentDescription = "导出PNG")
                        }
                        IconButton(onClick = { 
                            jsonExporter.launch("${uiState.character.name}.json")
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "导出JSON")
                        }
                    }
                    
                    // 保存按钮
                    IconButton(
                        onClick = { viewModel.saveCharacter() },
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
            
            // 头像
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onClick = { avatarPicker.launch("image/*") }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.character.avatarUrl != null) {
                        AsyncImage(
                            model = uiState.character.avatarUrl,
                            contentDescription = "角色头像",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddAPhoto,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("点击选择头像")
                        }
                    }
                }
            }
            
            // 基本信息
            OutlinedTextField(
                value = uiState.character.name,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.NAME, it) },
                label = { Text("角色名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.character.description,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.DESCRIPTION, it) },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            OutlinedTextField(
                value = uiState.character.personality,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.PERSONALITY, it) },
                label = { Text("性格") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            OutlinedTextField(
                value = uiState.character.scenario,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.SCENARIO, it) },
                label = { Text("场景") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            // 对话相关
            OutlinedTextField(
                value = uiState.character.firstMessage,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.FIRST_MESSAGE, it) },
                label = { Text("第一条消息") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            OutlinedTextField(
                value = uiState.character.messageExamples,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.MESSAGE_EXAMPLES, it) },
                label = { Text("对话示例") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5
            )
            
            // AI提示词
            OutlinedTextField(
                value = uiState.character.systemPrompt,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.SYSTEM_PROMPT, it) },
                label = { Text("系统提示词") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            OutlinedTextField(
                value = uiState.character.postHistoryInstructions,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.POST_HISTORY_INSTRUCTIONS, it) },
                label = { Text("后处理指令") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            // 元信息
            OutlinedTextField(
                value = uiState.character.creator,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.CREATOR, it) },
                label = { Text("创作者") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            OutlinedTextField(
                value = uiState.character.creatorNotes,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.CREATOR_NOTES, it) },
                label = { Text("创作者备注") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            OutlinedTextField(
                value = uiState.character.characterVersion,
                onValueChange = { viewModel.updateCharacterProperty(CharacterProperty.CHARACTER_VERSION, it) },
                label = { Text("角色版本") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            // 收藏开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("收藏此角色")
                Switch(
                    checked = uiState.character.favorite,
                    onCheckedChange = { viewModel.toggleFavorite() }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
