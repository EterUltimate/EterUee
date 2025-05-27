package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Terminal
import me.rerere.rikkahub.data.mcp.McpServerConfig
import me.rerere.rikkahub.data.model.Assistant

@Composable
fun McpPickerButton(
    assistant: Assistant,
    servers: List<McpServerConfig>,
    onUpdateAssistant: (Assistant) -> Unit
) {
    var showMcpPicker by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            showMcpPicker = true
        }
    ) {
        Icon(Lucide.Terminal, null)
    }
    if (showMcpPicker) {
        ModalBottomSheet(
            onDismissRequest = { showMcpPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "MCP服务器",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                McpPicker(
                    assistant = assistant,
                    servers = servers,
                    onUpdateAssistant = {
                        onUpdateAssistant(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
fun McpPicker(
    assistant: Assistant,
    servers: List<McpServerConfig>,
    modifier: Modifier = Modifier,
    onUpdateAssistant: (Assistant) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(servers.fastFilter { it.commonOptions.enable }) { server ->
            Card {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = server.commonOptions.name,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Switch(
                        checked = server.id in assistant.mcpServers,
                        onCheckedChange = {
                            if (it) {
                                onUpdateAssistant(
                                    assistant.copy(
                                        mcpServers = assistant.mcpServers + server.id
                                    )
                                )
                            } else {
                                onUpdateAssistant(
                                    assistant.copy(
                                        mcpServers = assistant.mcpServers - server.id
                                    )
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}