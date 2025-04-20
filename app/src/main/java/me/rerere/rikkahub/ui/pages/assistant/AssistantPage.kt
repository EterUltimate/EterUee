package me.rerere.rikkahub.ui.pages.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import me.rerere.ai.ui.transformers.PlaceholderTransformer
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.NumberInput
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.hooks.EditState
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.utils.toFixed
import org.koin.androidx.compose.koinViewModel

@Composable
fun AssistantPage(vm: AssistantVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val createState = useEditState<Assistant> {
        vm.addAssistant(it)
    }
    val editState = useEditState<Assistant> {
        vm.updateAssistant(it)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("助手设置")
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    IconButton(
                        onClick = {
                            createState.open(Assistant())
                        }
                    ) {
                        Icon(Lucide.Plus, null)
                    }
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = it + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(settings.assistants, key = { it.id }) { assistant ->
                AssistantItem(
                    assistant = assistant,
                    onEdit = {
                        editState.open(assistant)
                    },
                    onDelete = {
                        vm.removeAssistant(assistant)
                    }
                )
            }
        }
    }
    if (createState.isEditing) {
        AssistantDialog(createState)
    }
    if (editState.isEditing) {
        AssistantDialog(editState)
    }
}

@Composable
private fun AssistantDialog(state: EditState<Assistant>) {
    AlertDialog(
        onDismissRequest = {
            state.dismiss()
        },
        title = {
            Text("新增助手")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = state.currentState?.name ?: "",
                    onValueChange = {
                        state.currentState = state.currentState?.copy(
                            name = it
                        )
                    },
                    label = {
                        Text("助手名称")
                    }
                )

                OutlinedTextField(
                    value = state.currentState?.systemPrompt ?: "",
                    onValueChange = {
                        state.currentState = state.currentState?.copy(
                            systemPrompt = it
                        )
                    },
                    label = {
                        Text("系统提示词")
                    },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "可用变量: " + PlaceholderTransformer.Placeholders.entries.joinToString(", ") { "${it.key}: ${it.value}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                NumberInput(
                    value = state.currentState?.temperature ?: 0.6f,
                    onValueChange = {
                        state.currentState = state.currentState?.copy(
                            temperature = it
                        )
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    state.confirm()
                }
            ) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    state.dismiss()
                }
            ) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun AssistantItem(
    assistant: Assistant,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = assistant.name.ifBlank { "默认助手" },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = assistant.systemPrompt.ifBlank { "无系统提示词" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Tag(type = TagType.INFO) {
                    Text("温度: ${assistant.temperature.toFixed(1)}")
                }
            }
            // Right
            IconButton(
                onClick = {
                    onEdit()
                }
            ) {
                Icon(Lucide.Pencil, stringResource(R.string.edit))
            }
            IconButton(
                onClick = {
                    onDelete()
                },
            ) {
                Icon(Lucide.Delete, "delete")
            }
        }
    }
}