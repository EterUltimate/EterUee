package com.eterultimate.eteruee.roleplay.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * 聊天设置对话框
 */
@Composable
fun ChatSettingsDialog(
    onDismiss: () -> Unit,
    onSave: (ChatSettings) -> Unit,
    initialSettings: ChatSettings = ChatSettings()
) {
    var temperature by remember { mutableFloatStateOf(initialSettings.temperature) }
    var maxTokens by remember { mutableIntStateOf(initialSettings.maxTokens) }
    var topP by remember { mutableFloatStateOf(initialSettings.topP) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // 标题
                Text(
                    text = "聊天设置",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 温度设置
                Text(
                    text = "温度 (Temperature): ${String.format("%.1f", temperature)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0f..2f,
                    steps = 19,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "控制回复的随机性，值越高越有创意",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Top P 设置
                Text(
                    text = "Top P: ${String.format("%.2f", topP)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = topP,
                    onValueChange = { topP = it },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "核采样参数，控制词汇多样性",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 最大 Token 设置
                Text(
                    text = "最大 Token: $maxTokens",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { maxTokens = it.toInt() },
                    valueRange = 100f..4096f,
                    steps = 39,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "限制单次回复的最大长度",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(onClick = {
                        onSave(
                            ChatSettings(
                                temperature = temperature,
                                maxTokens = maxTokens,
                                topP = topP
                            )
                        )
                    }) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

/**
 * 聊天设置数据类
 */
data class ChatSettings(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 2048,
    val topP: Float = 0.95f
)
