package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings
import kotlinx.coroutines.launch
import me.rerere.mcp.McpServerConfig
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.hooks.EditState
import me.rerere.rikkahub.ui.hooks.EditStateContent
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingMcpPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val mcpConfigs = settings.mcpServers
    val creationState = useEditState<McpServerConfig> {

    }
    val editState = useEditState<McpServerConfig> { }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("MCP")
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    IconButton(
                        onClick = {
                            creationState.open(McpServerConfig.SseTransportServer())
                        }
                    ) {
                        Icon(Lucide.Plus, null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = innerPadding + PaddingValues(8.dp)
        ) {
            items(mcpConfigs) { mcpConfig ->
                McpServerItem(
                    item = mcpConfig
                )
            }
        }
    }
    McpServerConfigModal(creationState)
    McpServerConfigModal(editState)
}

@Composable
private fun McpServerItem(
    item: McpServerConfig
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.commonOptions.name,
                    style = MaterialTheme.typography.titleMedium,
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Tag(type = TagType.SUCCESS) {
                        when (item) {
                            is McpServerConfig.SseTransportServer -> "SSE"
                            is McpServerConfig.WebSocketServer -> "WebSocket"
                        }
                    }
                }
            }

            IconButton(
                onClick = {
                    // TODO
                }
            ) {
                Icon(Lucide.Settings, null)
            }
        }
    }
}

@Composable
fun McpServerConfigModal(state: EditState<McpServerConfig>) {
    state.EditStateContent { config, updateValue ->
        val pagerState = rememberPagerState { 2 }
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = {
                state.dismiss()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SecondaryTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        text = {
                            Text("基础设置")
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        text = {
                            Text("工具")
                        }
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    when(page) {
                        0 -> {

                        }

                        1 -> {

                        }
                    }
                }
            }
        }
    }
}