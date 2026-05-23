package com.eterultimate.eteruee.roleplay.ui.pages.preset

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.roleplay.data.model.PresetType
import com.eterultimate.eteruee.roleplay.ui.viewmodel.PresetEditViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresetEditPage(
    presetId: Uuid? = null,
    onSaveSuccess: () -> Unit,
    viewModel: PresetEditViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(presetId) {
        if (presetId == null) {
            viewModel.initializeForCreate()
        } else {
            viewModel.loadPresetForEdit(presetId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditing) "编辑预设" else "创建预设") },
                actions = {
                    IconButton(
                        onClick = { viewModel.savePreset() },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.padding(12.dp))
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
                        TextButton(onClick = {
                            viewModel.clearMessage()
                            onSaveSuccess()
                        }) {
                            Text("完成")
                        }
                    }
                ) {
                    Text(success)
                }
            }

            PresetBasicCard(
                name = uiState.preset.name,
                description = uiState.preset.description,
                type = uiState.preset.type,
                onNameChange = viewModel::updateName,
                onDescriptionChange = viewModel::updateDescription,
                onTypeChange = viewModel::updateType
            )

            ParameterCard(title = "采样") {
                SliderParameter(
                    label = "Temperature",
                    value = uiState.preset.parameters.floatValue("temperature", 0.8f),
                    valueRange = 0f..2f,
                    onValueChange = { viewModel.updateFloatParameter("temperature", it) }
                )
                SliderParameter(
                    label = "Top P",
                    value = uiState.preset.parameters.floatValue("top_p", 0.95f),
                    valueRange = 0f..1f,
                    onValueChange = { viewModel.updateFloatParameter("top_p", it) }
                )
                SliderParameter(
                    label = "Presence Penalty",
                    value = uiState.preset.parameters.floatValue("presence_penalty", 0f),
                    valueRange = -2f..2f,
                    onValueChange = { viewModel.updateFloatParameter("presence_penalty", it) }
                )
                SliderParameter(
                    label = "Frequency Penalty",
                    value = uiState.preset.parameters.floatValue("frequency_penalty", 0f),
                    valueRange = -2f..2f,
                    onValueChange = { viewModel.updateFloatParameter("frequency_penalty", it) }
                )
            }

            ParameterCard(title = "长度与停止") {
                NumberParameter(
                    label = "Top K",
                    value = uiState.preset.parameters.intValue("top_k", 40),
                    onValueChange = { viewModel.updateIntParameter("top_k", it) }
                )
                NumberParameter(
                    label = "Max Tokens",
                    value = uiState.preset.parameters.intValue("max_tokens", 1024),
                    onValueChange = { viewModel.updateIntParameter("max_tokens", it) }
                )
                OutlinedTextField(
                    value = uiState.preset.parameters["stop_sequences"]?.toString().orEmpty(),
                    onValueChange = { viewModel.updateStringParameter("stop_sequences", it) },
                    label = { Text("停止序列，逗号分隔") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            ParameterCard(title = "行为") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("流式输出", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "生成时逐步显示内容",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.preset.parameters.booleanValue("stream", true),
                        onCheckedChange = { viewModel.updateBooleanParameter("stream", it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetBasicCard(
    name: String,
    description: String,
    type: PresetType,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (PresetType) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("基础信息", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("预设名称 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                PresetType.entries.forEach { option ->
                    item {
                        AssistChip(
                            onClick = { onTypeChange(option) },
                            label = { Text(option.displayName()) },
                            leadingIcon = if (option == type) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else {
                                null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
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
private fun SliderParameter(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label)
            Text(value.format2(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.coerceIn(valueRange),
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun NumberParameter(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { input ->
            input.toIntOrNull()?.let { onValueChange(it.coerceAtLeast(1)) }
        },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

private fun Map<String, Any>.floatValue(key: String, defaultValue: Float): Float {
    return when (val value = this[key]) {
        is Number -> value.toFloat()
        is String -> value.toFloatOrNull() ?: defaultValue
        else -> defaultValue
    }
}

private fun Map<String, Any>.intValue(key: String, defaultValue: Int): Int {
    return when (val value = this[key]) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: defaultValue
        else -> defaultValue
    }
}

private fun Map<String, Any>.booleanValue(key: String, defaultValue: Boolean): Boolean {
    return when (val value = this[key]) {
        is Boolean -> value
        is String -> value.toBooleanStrictOrNull() ?: defaultValue
        else -> defaultValue
    }
}

private fun Float.format2(): String {
    return ((this * 100).roundToInt() / 100f).toString()
}
