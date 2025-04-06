package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelType
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.ui.components.BackButton
import me.rerere.rikkahub.ui.components.Tag
import me.rerere.rikkahub.ui.components.TagType
import me.rerere.rikkahub.ui.components.TextAvatar
import me.rerere.rikkahub.ui.components.ToastVariant
import me.rerere.rikkahub.ui.components.icons.Boxes
import me.rerere.rikkahub.ui.components.icons.Settings
import me.rerere.rikkahub.ui.components.rememberDialogState
import me.rerere.rikkahub.ui.components.rememberToastState
import me.rerere.rikkahub.ui.utils.plus
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
    Card {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .animateContentSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextAvatar(provider.name, modifier = Modifier.size(32.dp))
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
                        expand = ProviderExpandState.Models
                    }
                ) {
                    Icon(Boxes, "Models")
                }
                IconButton(
                    onClick = {
                        expand = ProviderExpandState.Setting
                    }
                ) {
                    Icon(Settings, "Setting")
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

            AddModelButton {
                onUpdate(providerSetting.addModel(it))
            }
        } else {
            providerSetting.models.forEach { model ->
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

            AddModelButton {
                onUpdate(providerSetting.addModel(it))
            }
        }
    }
}

@Composable
private fun AddModelButton(onAdd: (Model) -> Unit) {
    val dialogState = rememberDialogState()
    var modelId by remember { mutableStateOf("") }
    var modelDisplayName by remember { mutableStateOf("") }
    var modelType by remember { mutableStateOf(ModelType.CHAT) }

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
                                modelId = it
                                modelDisplayName = it.uppercase()
                            },
                            label = { Text("模型ID") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("例如：gpt-3.5-turbo")
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

                        Text("模型类型", style = MaterialTheme.typography.titleSmall)
                        ModelTypeSelector(
                            selectedType = modelType,
                            onTypeSelected = { modelType = it }
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
                        onAdd(
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
private fun ModelCard(
    model: Model,
    onDelete: () -> Unit,
    onEdit: (Model) -> Unit
) {
    val dialogState = rememberDialogState()
    var editingModel by remember { mutableStateOf(model) }

    SwipeToDismissBox(
        state = rememberSwipeToDismissBoxState(),
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End)
            ) {
                IconButton(
                    onClick = {
                        onDelete()
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.displayName, style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Tag {
                            Text(
                                text = model.modelId,
                            )
                        }
                        Tag {
                            Text(
                                text = when (model.type) {
                                    ModelType.CHAT -> "聊天模型"
                                    ModelType.EMBEDDING -> "嵌入模型"
                                }
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

                                    Text(
                                        "模型类型",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    ModelTypeSelector(
                                        selectedType = editingModel.type,
                                        onTypeSelected = {
                                            editingModel = editingModel.copy(type = it)
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