package com.eterultimate.eteruee.roleplay.ui.pages.character

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.data.tavern.TavernCharacterCardFormat
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
    var exportFormat by remember { mutableStateOf(TavernCharacterCardFormat.V2) }
    var showPngExportMenu by remember { mutableStateOf(false) }
    var showJsonExportMenu by remember { mutableStateOf(false) }
    
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
            viewModel.exportPngCharacter(uri, exportFormat)
        }
    }
    
    // JSON导出选择器
    val jsonExporter = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportJsonCharacter(uri, exportFormat)
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
                        Box {
                            IconButton(onClick = { showPngExportMenu = true }) {
                                Icon(Icons.Default.Download, contentDescription = "导出PNG")
                            }
                            ExportFormatMenu(
                                expanded = showPngExportMenu,
                                onDismiss = { showPngExportMenu = false },
                                onFormatSelected = { format ->
                                    exportFormat = format
                                    showPngExportMenu = false
                                    pngExporter.launch("${uiState.character.name}_${format.name.lowercase()}.png")
                                }
                            )
                        }
                        Box {
                            IconButton(onClick = { showJsonExportMenu = true }) {
                                Icon(Icons.Default.Save, contentDescription = "导出JSON")
                            }
                            ExportFormatMenu(
                                expanded = showJsonExportMenu,
                                onDismiss = { showJsonExportMenu = false },
                                onFormatSelected = { format ->
                                    exportFormat = format
                                    showJsonExportMenu = false
                                    jsonExporter.launch("${uiState.character.name}_${format.name.lowercase()}.json")
                                }
                            )
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
            
            CharacterVisualEditor(
                character = uiState.character,
                onAvatarClick = { avatarPicker.launch("image/*") },
                onPropertyChange = viewModel::updateCharacterProperty,
                onToggleFavorite = viewModel::toggleFavorite
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CharacterVisualEditor(
    character: Character,
    onAvatarClick: () -> Unit,
    onPropertyChange: (CharacterProperty, String) -> Unit,
    onToggleFavorite: () -> Unit
) {
    CharacterSection(title = "角色卡") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onAvatarClick),
            contentAlignment = Alignment.Center
        ) {
            if (character.avatarUrl != null) {
                AsyncImage(
                    model = character.avatarUrl,
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

        OutlinedTextField(
            value = character.name,
            onValueChange = { onPropertyChange(CharacterProperty.NAME, it) },
            label = { Text("角色名称 *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = character.description,
            onValueChange = { onPropertyChange(CharacterProperty.DESCRIPTION, it) },
            label = { Text("描述") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("收藏此角色", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "收藏后会在角色列表中优先筛选",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = character.favorite,
                onCheckedChange = { onToggleFavorite() }
            )
        }
    }

    CharacterSection(title = "设定") {
        OutlinedTextField(
            value = character.personality,
            onValueChange = { onPropertyChange(CharacterProperty.PERSONALITY, it) },
            label = { Text("性格") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = character.scenario,
            onValueChange = { onPropertyChange(CharacterProperty.SCENARIO, it) },
            label = { Text("场景") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }

    CharacterSection(title = "对话") {
        OutlinedTextField(
            value = character.firstMessage,
            onValueChange = { onPropertyChange(CharacterProperty.FIRST_MESSAGE, it) },
            label = { Text("第一条消息") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = character.messageExamples,
            onValueChange = { onPropertyChange(CharacterProperty.MESSAGE_EXAMPLES, it) },
            label = { Text("对话示例") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5
        )
    }

    CharacterSection(title = "提示词") {
        OutlinedTextField(
            value = character.systemPrompt,
            onValueChange = { onPropertyChange(CharacterProperty.SYSTEM_PROMPT, it) },
            label = { Text("系统提示词") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        OutlinedTextField(
            value = character.postHistoryInstructions,
            onValueChange = { onPropertyChange(CharacterProperty.POST_HISTORY_INSTRUCTIONS, it) },
            label = { Text("后处理指令") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
    }

    CharacterSection(title = "元信息") {
        OutlinedTextField(
            value = character.creator,
            onValueChange = { onPropertyChange(CharacterProperty.CREATOR, it) },
            label = { Text("创作者") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = character.characterVersion,
            onValueChange = { onPropertyChange(CharacterProperty.CHARACTER_VERSION, it) },
            label = { Text("角色版本") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = character.creatorNotes,
            onValueChange = { onPropertyChange(CharacterProperty.CREATOR_NOTES, it) },
            label = { Text("创作者备注") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )
    }
}

@Composable
private fun CharacterSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun ExportFormatMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onFormatSelected: (TavernCharacterCardFormat) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        TavernCharacterCardFormat.entries.forEach { format ->
            DropdownMenuItem(
                text = { Text("SillyTavern ${format.name}") },
                onClick = { onFormatSelected(format) }
            )
        }
    }
}
