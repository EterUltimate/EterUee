package me.rerere.rikkahub.ui.pages.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import me.rerere.ai.ui.transformers.PlaceholderTransformer
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.hooks.EditState
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.ui.theme.extendColors
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
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FormItem(
                    label = {
                        Text("助手名称")
                    },
                ) {
                    OutlinedTextField(
                        value = state.currentState?.name ?: "",
                        onValueChange = {
                            state.currentState = state.currentState?.copy(
                                name = it
                            )
                        },
                    )
                }

                FormItem(
                    label = {
                        Text("系统提示词")
                    },
                ) {
                    OutlinedTextField(
                        value = state.currentState?.systemPrompt ?: "",
                        onValueChange = {
                            state.currentState = state.currentState?.copy(
                                systemPrompt = it
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "可用变量: " + PlaceholderTransformer.Placeholders.entries.joinToString(
                            ", "
                        ) { "${it.key}: ${it.value}" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                FormItem(
                    label = {
                        Text("温度")
                    },
                ) {
                    Slider(
                        value = state.currentState?.temperature ?: 0.6f,
                        onValueChange = {
                            state.currentState = state.currentState?.copy(
                                temperature = it.toFixed(2).toFloatOrNull() ?: 0.6f
                            )
                        },
                        valueRange = 0f..2f,
                        steps = 19,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val currentTemperature = state.currentState?.temperature ?: 0.6f
                        val tagType = when (currentTemperature) {
                            in 0.0f..0.3f -> TagType.INFO
                            in 0.3f..1.0f -> TagType.SUCCESS
                            in 1.0f..1.5f -> TagType.WARNING
                            in 1.5f..2.0f -> TagType.ERROR
                            else -> TagType.ERROR
                        }
                        Tag(
                            type = TagType.INFO
                        ) {
                            Text(
                                text = "$currentTemperature"
                            )
                        }

                        Tag(
                            type = tagType
                        ) {
                            Text(
                                text = when (currentTemperature) {
                                    in 0.0f..0.3f -> "严谨"
                                    in 0.3f..1.0f -> "平衡"
                                    in 1.0f..1.5f -> "创造"
                                    in 1.5f..2.0f -> "混乱 (危险)"
                                    else -> "?"
                                }
                            )
                        }
                    }
                }
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
        // Left
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = assistant.name.ifBlank { "默认助手" },
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.weight(1f))

                Tag(type = TagType.INFO) {
                    Text("温度: ${assistant.temperature.toFixed(1)}")
                }
            }

            Text(
                text = buildAnnotatedString {
                    if (assistant.systemPrompt.isNotBlank()) {
                        // 变量替换为蓝色
                        // 正则匹配 {xxx}
                        val regex = "\\{[^}]+\\}".toRegex()
                        var lastIndex = 0
                        val input = assistant.systemPrompt
                        regex.findAll(input).forEach { matchResult ->
                            val start = matchResult.range.first
                            val end = matchResult.range.last + 1
                            // 普通文本
                            if (lastIndex < start) {
                                append(input.substring(lastIndex, start))
                            }
                            // 蓝色变量
                            withStyle(SpanStyle(color = MaterialTheme.extendColors.blue6)) { // 你可以自定义颜色
                                append(input.substring(start, end))
                            }
                            lastIndex = end
                        }
                        // 末尾剩余文本
                        if (lastIndex < input.length) {
                            append(input.substring(lastIndex))
                        }
                    } else {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("无系统提示词")
                        }
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )



            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))

                // Right
                TextButton(
                    onClick = {
                        onDelete()
                    },
                ) {
                    Icon(
                        Lucide.Trash2,
                        "delete",
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(18.dp)
                    )
                    Text("删除")
                }

                Button(
                    onClick = {
                        onEdit()
                    }
                ) {
                    Icon(
                        Lucide.Pencil,
                        stringResource(R.string.edit),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(18.dp)
                    )
                    Text("编辑")
                }
            }
        }
    }
}