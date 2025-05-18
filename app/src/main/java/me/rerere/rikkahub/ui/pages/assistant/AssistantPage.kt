package me.rerere.rikkahub.ui.pages.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.composables.icons.lucide.X
import me.rerere.ai.ui.transformers.PlaceholderTransformer
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.DEFAULT_ASSISTANTS_IDS
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.EditState
import me.rerere.rikkahub.ui.hooks.EditStateContent
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.ui.theme.extendColors
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.utils.toFixed
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun AssistantPage(vm: AssistantVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val createState = useEditState<Assistant> {
        vm.addAssistant(it)
    }
    val editState = useEditState<Assistant> {
        vm.updateAssistant(it)
    }
    val memoryState = useEditState<Assistant> {
        vm.updateAssistant(it)
    }
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.assistant_page_title))
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
                        Icon(Lucide.Plus, stringResource(R.string.assistant_page_add))
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
                val memories by vm.getMemories(assistant).collectAsStateWithLifecycle(
                    initialValue = emptyList(),
                )
                AssistantItem(
                    assistant = assistant,
                    memories = memories,
                    onEdit = {
                        // editState.open(assistant)
                        navController.navigate("assistant/${assistant.id}")
                    },
                    onDelete = {
                        vm.removeAssistant(assistant)
                    },
                )
            }
        }
    }
    AssistantEditSheet(vm, createState, memoryState)
    AssistantEditSheet(vm, editState, memoryState)
    MemorySheet(vm, memoryState)
}

@Composable
private fun AssistantEditSheet(
    vm: AssistantVM,
    state: EditState<Assistant>,
    memoryState: EditState<Assistant>
) {
    state.EditStateContent { assistant, update ->
        val memories by vm.getMemories(assistant).collectAsStateWithLifecycle(
            initialValue = emptyList(),
        )
        ModalBottomSheet(
            onDismissRequest = {
                state.dismiss()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = {},
            sheetGesturesEnabled = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(horizontal = 16.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(
                            state = rememberScrollState(),
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_name))
                        },
                    ) {
                        OutlinedTextField(
                            value = assistant.name,
                            onValueChange = {
                                update(
                                    assistant.copy(
                                        name = it
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HorizontalDivider()

                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_system_prompt))
                        },
                    ) {
                        OutlinedTextField(
                            value = assistant.systemPrompt,
                            onValueChange = {
                                update(
                                    assistant.copy(
                                        systemPrompt = it
                                    )
                                )
                            },
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = stringResource(
                                R.string.assistant_page_available_variables,
                                PlaceholderTransformer.Placeholders.entries.joinToString(", ") { "${it.key}: ${it.value}" }
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                        )
                    }

                    HorizontalDivider()

                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_temperature))
                        },
                    ) {
                        Slider(
                            value = assistant.temperature,
                            onValueChange = {
                                update(
                                    assistant.copy(
                                        temperature = it.toFixed(2).toFloatOrNull() ?: 0.6f
                                    )
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
                                        in 0.0f..0.3f -> stringResource(R.string.assistant_page_strict)
                                        in 0.3f..1.0f -> stringResource(R.string.assistant_page_balanced)
                                        in 1.0f..1.5f -> stringResource(R.string.assistant_page_creative)
                                        in 1.5f..2.0f -> stringResource(R.string.assistant_page_chaotic)
                                        else -> "?"
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_top_p))
                        },
                        description = {
                            Text(
                                text = buildAnnotatedString {
                                    append(stringResource(R.string.assistant_page_top_p_warning))
                                }
                            )
                        }
                    ) {
                        Slider(
                            value = assistant.topP,
                            onValueChange = {
                                update(
                                    assistant.copy(
                                        topP = it.toFixed(2).toFloatOrNull() ?: 1.0f
                                    )
                                )
                            },
                            valueRange = 0f..1f,
                            steps = 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(
                                R.string.assistant_page_top_p_value,
                                assistant.topP.toString()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                        )
                    }

                    HorizontalDivider()

                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_context_message_size))
                        },
                        description = {
                            Text(
                                text = stringResource(R.string.assistant_page_context_message_desc),
                            )
                        }
                    ) {
                        Slider(
                            value = assistant.contextMessageSize.toFloat(),
                            onValueChange = {
                                update(
                                    assistant.copy(
                                        contextMessageSize = it.roundToInt()
                                    )
                                )
                            },
                            valueRange = 4f..512f,
                            steps = 0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = stringResource(
                                R.string.assistant_page_context_message_count,
                                assistant.contextMessageSize
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.75f),
                        )
                    }

                    HorizontalDivider()

                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_memory))
                        },
                        description = {
                            Text(
                                text = stringResource(R.string.assistant_page_memory_desc),
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    state.dismiss()
                                    memoryState.open(assistant)
                                }
                            ) {
                                Text(
                                    stringResource(
                                        R.string.assistant_page_manage_memory,
                                        memories.size
                                    )
                                )
                            }

                            Spacer(Modifier.weight(1f))

                            Switch(
                                checked = assistant.enableMemory,
                                onCheckedChange = {
                                    update(
                                        assistant.copy(
                                            enableMemory = it
                                        )
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_inject_message_time))
                        },
                        description = {
                            Text(
                                text = stringResource(R.string.assistant_page_inject_message_time_desc),
                            )
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = assistant.enableMessageTime,
                                onCheckedChange = {
                                    update(
                                        assistant.copy(
                                            enableMessageTime = it
                                        )
                                    )
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    FormItem(
                        label = {
                            Text(stringResource(R.string.assistant_page_thinking_budget))
                        },
                        description = {
                            Text(stringResource(R.string.assistant_page_thinking_budget_desc))
                            Text(
                                text = stringResource(R.string.assistant_page_thinking_budget_warning),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    ) {
                        var input by remember(assistant) {
                            mutableStateOf(assistant.thinkingBudget?.toString() ?: "")
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = {
                                    input = it
                                    update(
                                        assistant.copy(
                                            thinkingBudget = if (it.isBlank()) null else it.toIntOrNull()
                                        )
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            input = ""
                                            update(
                                                assistant.copy(
                                                    thinkingBudget = null
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Lucide.X,
                                            contentDescription = null
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = stringResource(
                                    R.string.assistant_page_thinking_budget_tokens,
                                    assistant.thinkingBudget?.toString()
                                        ?: stringResource(R.string.assistant_page_thinking_budget_default)
                                ),
                            )
                        }
                    }

                    HorizontalDivider()

                    AssistantCustomHeaders(assistant = assistant, onUpdate = update)

                    HorizontalDivider()

                    AssistantCustomBodies(assistant = assistant, onUpdate = update)
                }

                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = {
                            state.dismiss()
                        }
                    ) {
                        Text(stringResource(R.string.assistant_page_cancel))
                    }
                    TextButton(
                        onClick = {
                            state.confirm()
                        }
                    ) {
                        Text(stringResource(R.string.assistant_page_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemorySheet(vm: AssistantVM, memoryState: EditState<Assistant>) {
    val memoryItemDialogState = useEditState<AssistantMemory> {
        if (it.id == 0) {
            vm.addMemory(memoryState.currentState!!, it)
        } else {
            vm.updateMemory(it)
        }
    }
    memoryState.EditStateContent { assistant, update ->
        val memories by vm.getMemories(assistant).collectAsStateWithLifecycle(
            initialValue = emptyList(),
        )
        ModalBottomSheet(
            onDismissRequest = {
                memoryState.dismiss()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.assistant_page_manage_memory_title),
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.Center)
                    )

                    IconButton(
                        onClick = {
                            memoryItemDialogState.open(AssistantMemory(0, ""))
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Lucide.Plus,
                            contentDescription = null
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(memories, key = { it.id }) { memory ->
                        Card(
                            modifier = Modifier.animateItem()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = memory.content,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        memoryItemDialogState.open(memory)
                                    }
                                ) {
                                    Icon(Lucide.Pencil, "编辑")
                                }
                                IconButton(
                                    onClick = {
                                        vm.deleteMemory(memory)
                                    }
                                ) {
                                    Icon(
                                        Lucide.Trash2,
                                        stringResource(R.string.assistant_page_delete)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    memoryItemDialogState.EditStateContent { memory, update ->
        AlertDialog(
            onDismissRequest = {
                memoryItemDialogState.dismiss()
            },
            title = {
                Text(stringResource(R.string.assistant_page_manage_memory_title))
            },
            text = {
                TextField(
                    value = memory.content,
                    onValueChange = {
                        update(memory.copy(content = it))
                    },
                    label = {
                        Text(stringResource(R.string.assistant_page_manage_memory_title))
                    },
                    minLines = 1,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        memoryItemDialogState.confirm()
                    }
                ) {
                    Text(stringResource(R.string.assistant_page_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        memoryItemDialogState.dismiss()
                    }
                ) {
                    Text(stringResource(R.string.assistant_page_cancel))
                }
            }
        )
    }
}

@Composable
private fun AssistantItem(
    assistant: Assistant,
    memories: List<AssistantMemory>,
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
                    text = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.weight(1f))

                Tag(
                    type = TagType.INFO
                ) {
                    Text(
                        stringResource(
                            R.string.assistant_page_temperature_value,
                            assistant.temperature.toFixed(1)
                        )
                    )
                }

                if (assistant.enableMemory) {
                    Tag(
                        type = TagType.SUCCESS
                    ) {
                        Text(stringResource(R.string.assistant_page_memory_count, memories.size))
                    }
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
                            append(stringResource(R.string.assistant_page_no_system_prompt))
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
                    enabled = assistant.id !in DEFAULT_ASSISTANTS_IDS
                ) {
                    Icon(
                        Lucide.Trash2,
                        stringResource(R.string.assistant_page_delete),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(18.dp)
                    )
                    Text(stringResource(R.string.assistant_page_delete))
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
                    Text(stringResource(R.string.edit))
                }
            }
        }
    }
}