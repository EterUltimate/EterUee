package me.rerere.rikkahub.ui.pages.setting

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.util.fastFilter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Import
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Share
import com.composables.icons.lucide.Trash2
import com.composables.icons.lucide.X
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanQRCode
import kotlinx.coroutines.launch
import me.rerere.ai.provider.Modality
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelType
import me.rerere.ai.provider.ProviderManager
import me.rerere.ai.provider.ProviderSetting
import me.rerere.ai.provider.guessModalityFromModelId
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.ui.ShareSheet
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.components.ui.ToastState
import me.rerere.rikkahub.ui.components.ui.ToastVariant
import me.rerere.rikkahub.ui.components.ui.decodeProviderSetting
import me.rerere.rikkahub.ui.components.ui.rememberDialogState
import me.rerere.rikkahub.ui.components.ui.rememberShareSheetState
import me.rerere.rikkahub.ui.components.ui.rememberToastState
import me.rerere.rikkahub.ui.hooks.EditState
import me.rerere.rikkahub.ui.hooks.useEditState
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
                    ImportProviderButton {
                        vm.updateSettings(
                            settings.copy(
                                providers = settings.providers + it
                            )
                        )
                    }
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
            contentPadding = innerPadding + PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(settings.providers, key = { it.id }) { provider ->
                ProviderItem(
                    modifier = Modifier.animateItem(),
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
private fun ImportProviderButton(
    onAdd: (ProviderSetting) -> Unit
) {
    val toastState = rememberToastState()
    val scanQrCodeLauncher = rememberLauncherForActivityResult(ScanQRCode()) { result ->
        // handle QRResult
        runCatching {
            when (result) {
                is QRResult.QRError -> {
                    toastState.show("错误: $result", ToastVariant.ERROR)
                }

                QRResult.QRMissingPermission -> {
                    toastState.show("没有权限", ToastVariant.ERROR)
                }

                is QRResult.QRSuccess -> {
                    val setting = decodeProviderSetting(result.content.rawValue ?: "")
                    onAdd(setting)
                    toastState.show("导入成功", ToastVariant.SUCCESS)
                }

                QRResult.QRUserCanceled -> {}
            }
        }
    }
    IconButton(
        onClick = {
            scanQrCodeLauncher.launch(null)
        }
    ) {
        Icon(Lucide.Import, null)
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
    modifier: Modifier = Modifier,
    onEdit: (provider: ProviderSetting) -> Unit,
    onDelete: () -> Unit
) {
    val toastState = rememberToastState()
    // 临时复制一份用于编辑
    // 因为data store是异步操作的，会导致UI编辑不同步
    var internalProvider by remember(provider) { mutableStateOf(provider) }
    var expand by remember { mutableStateOf(ProviderExpandState.None) }
    fun setExpand(state: ProviderExpandState) {
        expand = if (expand == state) {
            ProviderExpandState.None
        } else {
            state
        }
    }
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (provider.enabled) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            } else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AutoAIIcon(
                    name = provider.name,
                    modifier = Modifier.size(32.dp)
                )
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = provider.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Tag(type = if (provider.enabled) TagType.SUCCESS else TagType.WARNING) {
                            Text(if (provider.enabled) "启用" else "禁用")
                        }
                        Tag(type = TagType.INFO) {
                            Text("${provider.models.size}个模型")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                val shareSheetState = rememberShareSheetState()
                ShareSheet(shareSheetState)
                TextButton(
                    onClick = {
                        shareSheetState.show(provider)
                    },
                ) {
                    Icon(
                        imageVector = Lucide.Share,
                        contentDescription = "Share",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp)
                    )
                    Text("分享")
                }
                TextButton(
                    onClick = {
                        setExpand(ProviderExpandState.Models)
                    },
                ) {
                    Icon(
                        imageVector = Lucide.Boxes,
                        contentDescription = "Models",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp)
                    )
                    Text("模型")
                }
                TextButton(
                    onClick = {
                        setExpand(ProviderExpandState.Setting)
                    }
                ) {
                    Icon(
                        imageVector = Lucide.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(16.dp)
                    )
                    Text("配置")
                }
            }

            if (expand == ProviderExpandState.Models) {
                ModelList(provider, toastState) {
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
private fun ModelList(
    providerSetting: ProviderSetting,
    toastState: ToastState,
    onUpdate: (ProviderSetting) -> Unit
) {
    val modelList by produceState(emptyList(), providerSetting) {
        runCatching {
            value = ProviderManager.getProviderByType(providerSetting)
                .listModels(providerSetting)
                .sortedBy { it.modelId }
        }.onFailure {
            it.printStackTrace()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        },
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

    val modelListState = useEditState<Model?> { model ->
        model?.let {
            setModelId(it.modelId)
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
                                ModelPicker(modelListState, models)
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
private fun ModelPicker(
    modelListState: EditState<Model?>,
    models: List<Model>
) {
    if (modelListState.isEditing) {
        ModalBottomSheet(
            onDismissRequest = { modelListState.dismiss() },
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            )
        ) {
            var filterText by remember { mutableStateOf("") }
            val filterKeywords = filterText.split(" ").filter { it.isNotBlank() }
            val filteredModels = models.fastFilter {
                if (filterKeywords.isEmpty()) {
                    true
                } else {
                    filterKeywords.all { keyword ->
                        it.modelId.contains(keyword, ignoreCase = true) ||
                                it.displayName.contains(keyword, ignoreCase = true)
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .padding(8.dp)
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredModels) {
                        Card(
                            onClick = {
                                modelListState.currentState = it.copy()
                                modelListState.confirm()
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(
                                    8.dp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                            ) {
                                AutoAIIcon(
                                    it.modelId,
                                    Modifier.size(32.dp)
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(
                                        4.dp
                                    ),
                                ) {
                                    Text(
                                        text = it.modelId,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                }
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = filterText,
                    onValueChange = {
                        filterText = it
                    },
                    label = { Text("输入模型名称筛选") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("例如：GPT-3.5")
                    },
                )
            }
        }
    }
    BadgedBox(
        badge = {
            if (models.isNotEmpty()) {
                Badge {
                    Text(models.size.toString())
                }
            }
        }
    ) {
        IconButton(
            onClick = {
                modelListState.open(null)
            }
        ) {
            Icon(Lucide.Boxes, null)
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
    modifier: Modifier = Modifier,
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            swipeToDismissBoxState.reset()
                        }
                    }
                ) {
                    Icon(Lucide.X, null)
                }
                FilledIconButton(
                    onClick = {
                        scope.launch {
                            onDelete()
                            swipeToDismissBoxState.reset()
                        }
                    }
                ) {
                    Icon(Lucide.Trash2, contentDescription = "删除")
                }
            }
        },
        enableDismissFromStartToEnd = false,
        gesturesEnabled = true,
        modifier = modifier
    ) {
        OutlinedCard {
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