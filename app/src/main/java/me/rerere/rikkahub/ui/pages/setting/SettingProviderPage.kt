package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import kotlinx.coroutines.launch
import me.rerere.ai.provider.Modality
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelType
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.guessModalityFromModelId
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.components.ui.ToastVariant
import me.rerere.rikkahub.ui.components.ui.rememberDialogState
import me.rerere.rikkahub.ui.components.ui.rememberToastState
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingProviderPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "提供商")
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    AddButton {
                        vm.updateSettings(
                            settings.copy(
                                providers = settings.providers + it
                            )
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(settings.providers) { provider ->
                ProviderItem(
                    provider = provider,
                    onDelete = {
                        val newSettings = settings.copy(
                            providers = settings.providers - provider
                        )
                        vm.updateSettings(newSettings)
                    },
                    onEdit = { newProvider ->
                        val newSettings = settings.copy(
                            providers = settings.providers.map {
                                if (newProvider.id == it.id) {
                                    newProvider
                                } else {
                                    it
                                }
                            }
                        )
                        vm.updateSettings(newSettings)
                    }
                )
            }
        }
    }
}

@Composable
private fun AddButton(onAdd: (ProviderSetting) -> Unit) {
    val dialogState = rememberDialogState()

    var providerSetting: ProviderSetting by remember {
        mutableStateOf(ProviderSetting.OpenAI())
    }

    IconButton(
        onClick = {
            dialogState.openAlertDialog(
                title = {
                    Text("添加提供商")
                },
                text = {
                    ProviderConfigure(providerSetting) {
                        providerSetting = it
                    }
                },
                confirmText = {
                    Text("添加")
                },
                dismissText = {
                    Text("取消")
                },
                onConfirm = {
                    onAdd(providerSetting)
                    providerSetting = ProviderSetting.OpenAI()
                },
                onDismiss = {
                    dialogState.close()
                }
            )
        }
    ) {
        Icon(Icons.Outlined.Add, "Add")
    }
}


private enum class ProviderExpandState {
    Setting,
    Models,
    None
}

@Composable
private fun ProviderItem(
    provider: ProviderSetting,
    onEdit: (provider: ProviderSetting) -> Unit,
    onDelete: () -> Unit
) {
    val toastState = rememberToastState()
    // 临时复制一份用于编辑
    // 因为data store是异步操作的，会导致UI编辑不同步
    var internalProvider by remember { mutableStateOf(provider) }
    var expand by remember { mutableStateOf(ProviderExpandState.None) }
    fun setExpand(state: ProviderExpandState) {
        expand = if (expand == state) {
            ProviderExpandState.None
        } else {
            state
        }
    }
    OutlinedCard {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .animateContentSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoAIIcon(provider.name, modifier = Modifier.size(32.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(provider.name, style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Tag(type = if (provider.enabled) TagType.SUCCESS else TagType.WARNING) {
                            Text(if (provider.enabled) "启用" else "禁用")
                        }
                        Tag(type = TagType.INFO) {
                            Text("${provider.models.size}个模型")
                        }
                    }
                }
                IconButton(
                    onClick = {
                        setExpand(ProviderExpandState.Models)
                    }
                ) {
                    Icon(Lucide.Boxes, "Models")
                }
                IconButton(
                    onClick = {
                        setExpand(ProviderExpandState.Setting)
                    }
                ) {
                    Icon(Lucide.Settings, "Setting")
                }
            }

            if (expand == ProviderExpandState.Models) {
                ModelList(provider) {
                    onEdit(it)
                }
            }

            if (expand == ProviderExpandState.Setting) {
                ProviderConfigure(
                    provider = internalProvider,
                    modifier = Modifier.padding(8.dp),
                    onEdit = {
                        internalProvider = it
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(Modifier.weight(1f))

                    IconButton(
                        onClick = {
                            onDelete()
                        }
                    ) {
                        Icon(Icons.Outlined.Delete, "Delete")
                    }

                    Button(
                        onClick = {
                            onEdit(internalProvider)
                            toastState.show("保存成功", ToastVariant.SUCCESS)
                            expand = ProviderExpandState.None
                        }
                    ) {
                        Icon(Icons.Outlined.Edit, "Delete")
                        Text("保存")
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelList(providerSetting: ProviderSetting, onUpdate: (ProviderSetting) -> Unit) {
    val toastState = rememberToastState()
    val modelList by produceState(emptyList()) {
        runCatching {
            value = ProviderManager.getProviderByType(providerSetting).listModels(providerSetting)
        }.onFailure {
            toastState.show(it.message ?: "获取模型列表失败", ToastVariant.ERROR)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 模型列表
        if (providerSetting.models.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "暂无模型",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "点击下方按钮添加模型",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            AddModelButton(modelList) {
                onUpdate(providerSetting.addModel(it))
            }
        } else {
            providerSetting.models.forEach { model ->
                key(model.id) {
                    ModelCard(
                        model = model,
                        onDelete = {
                            onUpdate(providerSetting.delModel(model))
                        },
                        onEdit = { editedModel ->
                            onUpdate(providerSetting.editModel(editedModel))
                        }
                    )
                }
            }

            AddModelButton(modelList) {
                onUpdate(providerSetting.addModel(it))
            }
        }
    }
}

@Composable
private fun AddModelButton(
    models: List<Model>,
    onAddModel: (Model) -> Unit
) {
    val dialogState = rememberDialogState()
    var modelId by remember { mutableStateOf("") }
    var modelDisplayName by remember { mutableStateOf("") }
    var modelType by remember { mutableStateOf(ModelType.CHAT) }
    var inputModalities by remember { mutableStateOf(listOf(Modality.TEXT)) }
    var outputModalities by remember { mutableStateOf(listOf(Modality.TEXT)) }

    fun setModelId(id: String) {
        modelId = id
        modelDisplayName = id.uppercase()
        guessModalityFromModelId(modelId).let { (input, output) ->
            inputModalities = input
            outputModalities = output
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            dialogState.openAlertDialog(
                title = {
                    Text("添加模型")
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = modelId,
                            onValueChange = {
                                setModelId(it)
                            },
                            label = { Text("模型ID") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("例如：gpt-3.5-turbo")
                            },
                            trailingIcon = {
                                var expandModelList by remember { mutableStateOf(false) }
                                IconButton(
                                    onClick = {
                                        expandModelList = !expandModelList
                                    }
                                ) {
                                    Icon(Lucide.Boxes, null)
                                    DropdownMenu(
                                        expanded = expandModelList,
                                        onDismissRequest = {
                                            expandModelList = false
                                        },
                                    ) {
                                        models.fastForEach {
                                            DropdownMenuItem(
                                                text = { Text(it.modelId) },
                                                onClick = {
                                                    setModelId(it.modelId)
                                                    expandModelList = false
                                                },
                                                leadingIcon = {
                                                    AutoAIIcon(it.modelId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        OutlinedTextField(
                            value = modelDisplayName,
                            onValueChange = { modelDisplayName = it },
                            label = { Text("模型显示名称") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("例如：GPT-3.5, 用于UI显示")
                            }
                        )


                        ModelTypeSelector(
                            selectedType = modelType,
                            onTypeSelected = { modelType = it }
                        )

                        ModelModalitySelector(
                            inputModalities = inputModalities,
                            onUpdateInputModalities = { inputModalities = it },
                            outputModalities = outputModalities,
                            onUpdateOutputModalities = { outputModalities = it }
                        )
                    }
                },
                confirmText = {
                    Text("添加")
                },
                dismissText = {
                    Text("取消")
                },
                onConfirm = {
                    if (modelId.isNotBlank() && modelDisplayName.isNotBlank()) {
                        onAddModel(
                            Model(
                                modelId = modelId,
                                displayName = modelDisplayName,
                                type = modelType
                            )
                        )
                        modelId = ""
                        modelDisplayName = ""
                        modelType = ModelType.CHAT
                    }
                },
                onDismiss = {
                    dialogState.close()
                }
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "添加模型")
            Spacer(modifier = Modifier.size(8.dp))
            Text("添加新模型", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun ModelTypeSelector(
    selectedType: ModelType,
    onTypeSelected: (ModelType) -> Unit
) {
    Text("模型类型", style = MaterialTheme.typography.titleSmall)
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        ModelType.entries.forEachIndexed { index, type ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index, ModelType.entries.size),
                label = {
                    Text(
                        text = when (type) {
                            ModelType.CHAT -> "聊天模型"
                            ModelType.EMBEDDING -> "嵌入模型"
                        }
                    )
                },
                selected = selectedType == type,
                onClick = { onTypeSelected(type) }
            )
        }
    }
}

@Composable
private fun ModelModalitySelector(
    inputModalities: List<Modality>,
    onUpdateInputModalities: (List<Modality>) -> Unit,
    outputModalities: List<Modality>,
    onUpdateOutputModalities: (List<Modality>) -> Unit
) {
    Text("输入模态", style = MaterialTheme.typography.titleSmall)
    MultiChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Modality.entries.forEachIndexed { index, modality ->
            SegmentedButton(
                checked = modality in inputModalities,
                shape = SegmentedButtonDefaults.itemShape(index, Modality.entries.size),
                onCheckedChange = {
                    if (it) {
                        onUpdateInputModalities(inputModalities + modality)
                    } else {
                        onUpdateInputModalities(inputModalities - modality)
                    }
                }
            ) {
                Text(
                    text = when (modality) {
                        Modality.TEXT -> "文本"
                        Modality.IMAGE -> "图片"
                    }
                )
            }
        }
    }
    Text("输出模态", style = MaterialTheme.typography.titleSmall)
    MultiChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Modality.entries.forEachIndexed { index, modality ->
            SegmentedButton(
                checked = modality in outputModalities,
                shape = SegmentedButtonDefaults.itemShape(index, Modality.entries.size),
                onCheckedChange = {
                    if (it) {
                        onUpdateOutputModalities(outputModalities + modality)
                    } else {
                        onUpdateOutputModalities(outputModalities - modality)
                    }
                }
            ) {
                Text(
                    text = when (modality) {
                        Modality.TEXT -> "文本"
                        Modality.IMAGE -> "图片"
                    }
                )
            }
        }
    }
}

@Composable
private fun ModelCard(
    model: Model,
    onDelete: () -> Unit,
    onEdit: (Model) -> Unit
) {
    val dialogState = rememberDialogState()
    var editingModel by remember { mutableStateOf(model) }
    val swipeToDismissBoxState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = swipeToDismissBoxState,
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            onDelete()
                            swipeToDismissBoxState.dismiss(SwipeToDismissBoxValue.EndToStart)
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除")
                }
            }
        },
        enableDismissFromStartToEnd = false,
        gesturesEnabled = true
    ) {
        ElevatedCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AutoAIIcon(
                    name = model.modelId,
                    modifier = Modifier.size(28.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.modelId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = model.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Tag(type = TagType.INFO) {
                            Text(
                                text = when (model.type) {
                                    ModelType.CHAT -> "聊天模型"
                                    ModelType.EMBEDDING -> "嵌入模型"
                                }
                            )
                        }
                        Tag(type = TagType.SUCCESS) {
                            Text(
                                text = buildString {
                                    append(model.inputModalities.joinToString(",") { it.name.lowercase() })
                                    append("->")
                                    append(model.outputModalities.joinToString(",") { it.name.lowercase() })
                                },
                                maxLines = 1,
                            )
                        }
                    }
                }

                // Edit button
                IconButton(
                    onClick = {
                        editingModel = model
                        dialogState.openAlertDialog(
                            title = {
                                Text("编辑模型")
                            },
                            text = {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editingModel.modelId,
                                        onValueChange = {},
                                        label = { Text("模型ID") },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = false
                                    )

                                    OutlinedTextField(
                                        value = editingModel.displayName,
                                        onValueChange = {
                                            editingModel = editingModel.copy(displayName = it)
                                        },
                                        label = { Text("模型名称") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    ModelTypeSelector(
                                        selectedType = editingModel.type,
                                        onTypeSelected = {
                                            editingModel = editingModel.copy(type = it)
                                        }
                                    )
                                    ModelModalitySelector(
                                        inputModalities = editingModel.inputModalities,
                                        onUpdateInputModalities = {
                                            editingModel = editingModel.copy(inputModalities = it)
                                        },
                                        outputModalities = editingModel.outputModalities,
                                        onUpdateOutputModalities = {
                                            editingModel = editingModel.copy(outputModalities = it)
                                        }
                                    )
                                }
                            },
                            confirmText = {
                                Text("保存")
                            },
                            dismissText = {
                                Text("取消")
                            },
                            onConfirm = {
                                if (editingModel.displayName.isNotBlank()) {
                                    onEdit(editingModel)
                                }
                            },
                            onDismiss = {
                                dialogState.close()
                            }
                        )
                    }
                ) {
                    Icon(Icons.Outlined.Edit, "Edit")
                }
            }
        }
    }
}