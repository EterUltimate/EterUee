package com.eterultimate.eteruee.ui.pages.setting

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.AlertCircle
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.ArrowUp01
import me.rerere.hugeicons.stroke.FileImport
import me.rerere.hugeicons.stroke.MessageBlocked
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Settings03
import me.rerere.hugeicons.stroke.Console
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Upload02
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Search01
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowOverflow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import com.eterultimate.eteruee.ui.components.ui.Switch
import com.eterultimate.eteruee.ui.components.ui.SwitchSize
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import com.eterultimate.eteruee.ai.core.InputSchema
import me.rerere.hugeicons.stroke.McpServer
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.data.ai.mcp.LanScanner
import com.eterultimate.eteruee.data.ai.mcp.McpManager
import com.eterultimate.eteruee.data.ai.mcp.McpServerConfig
import com.eterultimate.eteruee.data.ai.mcp.McpCommonOptions
import com.eterultimate.eteruee.data.ai.mcp.McpStatus
import com.eterultimate.eteruee.data.ai.mcp.McpTool
import com.eterultimate.eteruee.data.ai.mcp.ScanResult
import com.eterultimate.eteruee.ui.components.nav.BackButton
import com.eterultimate.eteruee.ui.components.ui.FormItem
import com.eterultimate.eteruee.ui.components.ui.Tag
import com.eterultimate.eteruee.ui.components.ui.TagType
import com.eterultimate.eteruee.ui.hooks.EditState
import com.eterultimate.eteruee.ui.hooks.EditStateContent
import com.eterultimate.eteruee.ui.hooks.useEditState
import com.eterultimate.eteruee.ui.theme.CustomColors
import com.eterultimate.eteruee.ui.theme.extendColors
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingMcpPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val mcpConfigs = settings.mcpServers
    val creationState = useEditState<McpServerConfig> {
        vm.updateSettings(
            settings.copy(
                mcpServers = mcpConfigs + it
            )
        )
    }
    val editState = useEditState<McpServerConfig> { newConfig ->
        vm.updateSettings(
            settings.copy(
                mcpServers = mcpConfigs.map {
                    if (it.id == newConfig.id) {
                        newConfig
                    } else {
                        it
                    }
                }
            ))
    }
    var showImportDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.setting_mcp_page_title))
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    IconButton(
                        onClick = {
                            showImportDialog = true
                        }
                    ) {
                        Icon(HugeIcons.FileImport, null)
                    }
                    IconButton(
                        onClick = {
                            creationState.open(McpServerConfig.StreamableHTTPServer())
                        }
                    ) {
                        Icon(HugeIcons.Add01, null)
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) { innerPadding ->
        val mcpManager = koinInject<McpManager>()
        val status by mcpManager.syncingStatus.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()
        val state = rememberPullToRefreshState()
        val loading = status.values.any { it == McpStatus.Connecting || it is McpStatus.Reconnecting }
        PullToRefreshBox(
            isRefreshing = loading,
            onRefresh = {
                scope.launch {
                    mcpManager.syncAll()
                }
            },
            state = state,
            modifier = Modifier.padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(mcpConfigs, key = { it.id }) { mcpConfig ->
                    McpServerItem(
                        item = mcpConfig,
                        onEdit = {
                            editState.open(mcpConfig)
                        },
                        onDelete = {
                            vm.updateSettings(
                                settings.copy(
                                    mcpServers = mcpConfigs.filter { it.id != mcpConfig.id }
                                )
                            )
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }

            if (mcpConfigs.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(text = stringResource(R.string.setting_mcp_page_no_mcp_servers_found))
                    Text(
                        text = stringResource(R.string.setting_mcp_page_add_one_to_get_started),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
    McpServerConfigModal(creationState)
    McpServerConfigModal(editState)
    if (showImportDialog) {
        McpImportModal(
            onDismiss = { showImportDialog = false },
            onImport = { newConfigs ->
                val existingIds = mcpConfigs.map { it.commonOptions.name }.toSet()
                val toAdd = newConfigs.filter { it.commonOptions.name !in existingIds }
                vm.updateSettings(settings.copy(mcpServers = mcpConfigs + toAdd))
                showImportDialog = false
            }
        )
    }
}

@Composable
private fun McpServerItem(
    item: McpServerConfig,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onEdit: (McpServerConfig) -> Unit,
) {
    val mcpManager = koinInject<McpManager>()
    val status by mcpManager.getStatus(item).collectAsStateWithLifecycle(McpStatus.Idle)
    val dismissBoxState = rememberSwipeToDismissBoxState()
    val scope = rememberCoroutineScope()
    SwipeToDismissBox(
        state = dismissBoxState,
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                FilledTonalIconButton(
                    onClick = {
                        scope.launch { dismissBoxState.reset() }
                    }
                ) {
                    Icon(HugeIcons.Cancel01, null)
                }
                FilledTonalIconButton(
                    onClick = {
                        onDelete()
                    }
                ) {
                    Icon(HugeIcons.Delete01, null)
                }
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = CustomColors.listItemColors.containerColor
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when (status) {
                    McpStatus.Idle -> Icon(HugeIcons.MessageBlocked, null)
                    McpStatus.Connecting -> CircularProgressIndicator(
                        modifier = Modifier.size(
                            24.dp
                        )
                    )

                    McpStatus.Connected -> Icon(HugeIcons.McpServer, null)
                    is McpStatus.Reconnecting -> CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                    is McpStatus.Error -> Icon(HugeIcons.AlertCircle, null)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.commonOptions.name,
                            style = MaterialTheme.typography.titleLarge,
                        )
                        val dotColor =
                            if (item.commonOptions.enable) MaterialTheme.extendColors.blue6 else MaterialTheme.extendColors.red6
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .drawWithContent {
                                    drawCircle(
                                        color = dotColor
                                    )
                                }
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Tag(type = TagType.SUCCESS) {
                            when (item) {
                                is McpServerConfig.SseTransportServer -> Text("SSE")
                                is McpServerConfig.StreamableHTTPServer -> Text("Streamable HTTP")
                                is McpServerConfig.StdioTransportServer -> Text("STDIO")
                            }
                        }
                    }
                }

                IconButton(
                    onClick = {
                        onEdit(item)
                    }
                ) {
                    Icon(HugeIcons.Settings03, null)
                }
            }
        }
    }
}

@Composable
private fun McpServerConfigModal(state: EditState<McpServerConfig>) {
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
                    .fillMaxHeight(0.9f)
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
                            Text(stringResource(R.string.setting_mcp_page_basic_settings))
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
                            Text(stringResource(R.string.setting_mcp_page_tools))
                        }
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    when (page) {
                        0 -> {
                            McpCommonOptionsConfigure(
                                config = config,
                                update = updateValue
                            )
                        }

                        1 -> {
                            McpToolsConfigure(
                                config = config,
                                update = updateValue,
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = {
                            if (config.commonOptions.name.isNotBlank()) {
                                state.confirm()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.setting_mcp_page_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun McpCommonOptionsConfigure(
    config: McpServerConfig,
    update: (McpServerConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 启用/禁用开关
        FormItem(
            label = {
                Text(stringResource(R.string.setting_mcp_page_enable))
            },
            description = {
                Text(stringResource(R.string.setting_mcp_page_enable_desc))
            }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.setting_mcp_page_enable))
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = config.commonOptions.enable,
                    onCheckedChange = { enabled ->
                        update(
                            when (config) {
                                is McpServerConfig.SseTransportServer -> config.copy(
                                    commonOptions = config.commonOptions.copy(enable = enabled)
                                )

                                is McpServerConfig.StreamableHTTPServer -> config.copy(
                                    commonOptions = config.commonOptions.copy(enable = enabled)
                                )

                                is McpServerConfig.StdioTransportServer -> config.copy(
                                    commonOptions = config.commonOptions.copy(enable = enabled)
                                )
                            }
                        )
                    }
                )
            }
        }

        HorizontalDivider()

        // 名称输入框
        FormItem(
            label = {
                Text(stringResource(R.string.setting_mcp_page_name))
            },
            description = {
                Text(stringResource(R.string.setting_mcp_page_name_desc))
            }
        ) {
            OutlinedTextField(
                value = config.commonOptions.name,
                onValueChange = { name ->
                    update(
                        when (config) {
                            is McpServerConfig.SseTransportServer -> config.copy(
                                commonOptions = config.commonOptions.copy(name = name)
                            )

                            is McpServerConfig.StreamableHTTPServer -> config.copy(
                                commonOptions = config.commonOptions.copy(name = name)
                            )

                            is McpServerConfig.StdioTransportServer -> config.copy(
                                commonOptions = config.commonOptions.copy(name = name)
                            )
                        }
                    )
                },
                label = { Text(stringResource(R.string.setting_mcp_page_name)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.setting_mcp_page_name_placeholder)) }
            )
        }

        HorizontalDivider()

        // 传输类型选择
        FormItem(
            label = {
                Text(stringResource(R.string.setting_mcp_page_transport_type))
            },
            description = {
                Text(stringResource(R.string.setting_mcp_page_transport_type_desc))
            }
        ) {
            val transportTypes = listOf(
                "Streamable HTTP",
                "SSE",
                "STDIO"
            )
            val currentTypeIndex = when (config) {
                is McpServerConfig.StreamableHTTPServer -> 0
                is McpServerConfig.SseTransportServer -> 1
                is McpServerConfig.StdioTransportServer -> 2
            }

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                transportTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, transportTypes.size),
                        onClick = {
                            if (index != currentTypeIndex) {
                                val newConfig = when (index) {
                                    0 -> McpServerConfig.StreamableHTTPServer(
                                        id = config.id,
                                        commonOptions = config.commonOptions,
                                        url = when (config) {
                                            is McpServerConfig.SseTransportServer -> config.url
                                            is McpServerConfig.StreamableHTTPServer -> config.url
                                            is McpServerConfig.StdioTransportServer -> ""
                                        }
                                    )

                                    1 -> McpServerConfig.SseTransportServer(
                                        id = config.id,
                                        commonOptions = config.commonOptions,
                                        url = when (config) {
                                            is McpServerConfig.SseTransportServer -> config.url
                                            is McpServerConfig.StreamableHTTPServer -> config.url
                                            is McpServerConfig.StdioTransportServer -> ""
                                        }
                                    )

                                    2 -> McpServerConfig.StdioTransportServer(
                                        id = config.id,
                                        commonOptions = config.commonOptions
                                    )

                                    else -> config
                                }
                                update(newConfig)
                            }
                        },
                        selected = index == currentTypeIndex
                    ) {
                        Text(type)
                    }
                }
            }
        }

        HorizontalDivider()

        // 服务器配置（URL 或 STDIO）
        if (config is McpServerConfig.StdioTransportServer) {
            // Command
            FormItem(
                label = {
                    Text(stringResource(R.string.setting_mcp_page_command))
                },
                description = {
                    Text(stringResource(R.string.setting_mcp_page_command_desc))
                }
            ) {
                OutlinedTextField(
                    value = config.command,
                    onValueChange = { cmd ->
                        update(config.copy(command = cmd))
                    },
                    label = { Text(stringResource(R.string.setting_mcp_page_command)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("npx") }
                )
            }

            HorizontalDivider()

            // Args
            FormItem(
                label = {
                    Text(stringResource(R.string.setting_mcp_page_args))
                },
                description = {
                    Text(stringResource(R.string.setting_mcp_page_args_desc))
                }
            ) {
                var argsText by remember(config.args) {
                    mutableStateOf(config.args.joinToString(" ")
                    ) }
                OutlinedTextField(
                    value = argsText,
                    onValueChange = { text ->
                        argsText = text
                        update(
                            config.copy(
                                args = text.split(" ").filter { it.isNotBlank() }
                            )
                        )
                    },
                    label = { Text(stringResource(R.string.setting_mcp_page_args)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("-y @modelcontextprotocol/server-memory") }
                )
            }

            HorizontalDivider()

            // Env vars
            FormItem(
                label = {
                    Text(stringResource(R.string.setting_mcp_page_env_vars))
                },
                description = {
                    Text(stringResource(R.string.setting_mcp_page_env_vars_desc))
                }
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    config.env.forEachIndexed { index, envVar ->
                        var envKey by remember(envVar.first) { mutableStateOf(envVar.first) }
                        var envValue by remember(envVar.second) { mutableStateOf(envVar.second) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = envKey,
                                    onValueChange = {
                                        envKey = it
                                        val updatedEnv = config.env.toMutableList()
                                        updatedEnv[index] = it.trim() to updatedEnv[index].second
                                        update(config.copy(env = updatedEnv))
                                    },
                                    label = { Text("KEY") },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("API_KEY") }
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = envValue,
                                    onValueChange = {
                                        envValue = it
                                        val updatedEnv = config.env.toMutableList()
                                        updatedEnv[index] = updatedEnv[index].first to it
                                        update(config.copy(env = updatedEnv))
                                    },
                                    label = { Text("VALUE") },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("sk-...") }
                                )
                            }
                            IconButton(onClick = {
                                val updatedEnv = config.env.toMutableList()
                                updatedEnv.removeAt(index)
                                update(config.copy(env = updatedEnv))
                            }) {
                                Icon(
                                    HugeIcons.Delete01,
                                    contentDescription = stringResource(R.string.setting_mcp_page_delete_header)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val updatedEnv = config.env.toMutableList()
                            updatedEnv.add("" to "")
                            update(config.copy(env = updatedEnv))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            HugeIcons.Add01,
                            contentDescription = stringResource(R.string.setting_mcp_page_add_env_var)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.setting_mcp_page_add_env_var))
                    }
                }
            }
        } else {
            FormItem(
                label = {
                    Text(stringResource(R.string.setting_mcp_page_server_url))
                },
                description = {
                    Text(
                        when (config) {
                            is McpServerConfig.SseTransportServer -> stringResource(R.string.setting_mcp_page_sse_url_desc)
                            is McpServerConfig.StreamableHTTPServer -> stringResource(R.string.setting_mcp_page_streamable_http_url_desc)
                            is McpServerConfig.StdioTransportServer -> ""
                        }
                    )
                }
            ) {
                OutlinedTextField(
                    value = when (config) {
                        is McpServerConfig.SseTransportServer -> config.url
                        is McpServerConfig.StreamableHTTPServer -> config.url
                        is McpServerConfig.StdioTransportServer -> ""
                    },
                    onValueChange = { url ->
                        update(
                            when (config) {
                                is McpServerConfig.SseTransportServer -> config.copy(url = url)
                                is McpServerConfig.StreamableHTTPServer -> config.copy(url = url)
                                is McpServerConfig.StdioTransportServer -> config
                            }
                        )
                    },
                    label = { Text(stringResource(R.string.setting_mcp_page_url_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            when (config) {
                                is McpServerConfig.SseTransportServer -> stringResource(R.string.setting_mcp_page_sse_url_placeholder)
                                is McpServerConfig.StreamableHTTPServer -> stringResource(R.string.setting_mcp_page_streamable_http_url_placeholder)
                                is McpServerConfig.StdioTransportServer -> ""
                            }
                        )
                    }
                )
            }
        }

        // 局域网发现 & 连接测试（仅 SSE / Streamable HTTP）
        if (config is McpServerConfig.SseTransportServer || config is McpServerConfig.StreamableHTTPServer) {
            LanDiscoverySection(
                config = config,
                update = update
            )
        }

        HorizontalDivider()

        // 请求头配置
        FormItem(
            label = {
                Text(stringResource(R.string.setting_mcp_page_custom_headers))
            },
            description = {
                Text(stringResource(R.string.setting_mcp_page_custom_headers_desc))
            }
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                config.commonOptions.headers.forEachIndexed { index, header ->
                    var headerName by remember(header.first) { mutableStateOf(header.first) }
                    var headerValue by remember(header.second) { mutableStateOf(header.second) }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = headerName,
                                onValueChange = {
                                    headerName = it
                                    val updatedHeaders =
                                        config.commonOptions.headers.toMutableList()
                                    updatedHeaders[index] =
                                        it.trim() to updatedHeaders[index].second
                                    update(
                                        when (config) {
                                            is McpServerConfig.SseTransportServer -> config.copy(
                                                commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                            )

                                            is McpServerConfig.StreamableHTTPServer -> config.copy(
                                                commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                            )

                                            is McpServerConfig.StdioTransportServer -> config.copy(
                                                commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                            )
                                        }
                                    )
                                },
                                label = { Text(stringResource(R.string.setting_mcp_page_header_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.setting_mcp_page_header_name_placeholder)) }
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = headerValue,
                                onValueChange = {
                                    headerValue = it
                                    val updatedHeaders =
                                        config.commonOptions.headers.toMutableList()
                                    updatedHeaders[index] = updatedHeaders[index].first to it.trim()
                                    update(
                                        when (config) {
                                            is McpServerConfig.SseTransportServer -> config.copy(
                                                commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                            )

                                            is McpServerConfig.StreamableHTTPServer -> config.copy(
                                                commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                            )

                                            is McpServerConfig.StdioTransportServer -> config.copy(
                                                commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                            )
                                        }
                                    )
                                },
                                label = { Text(stringResource(R.string.setting_mcp_page_header_value)) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text(stringResource(R.string.setting_mcp_page_header_value_placeholder)) }
                            )
                        }
                        IconButton(onClick = {
                            val updatedHeaders = config.commonOptions.headers.toMutableList()
                            updatedHeaders.removeAt(index)
                            update(
                                when (config) {
                                    is McpServerConfig.SseTransportServer -> config.copy(
                                        commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                    )

                                    is McpServerConfig.StreamableHTTPServer -> config.copy(
                                        commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                    )

                                    is McpServerConfig.StdioTransportServer -> config.copy(
                                        commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                    )
                                }
                            )
                        }) {
                            Icon(
                                HugeIcons.Delete01,
                                contentDescription = stringResource(R.string.setting_mcp_page_delete_header)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val updatedHeaders = config.commonOptions.headers.toMutableList()
                        updatedHeaders.add("" to "")
                        update(
                            when (config) {
                                is McpServerConfig.SseTransportServer -> config.copy(
                                    commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                )

                                is McpServerConfig.StreamableHTTPServer -> config.copy(
                                    commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                )

                                is McpServerConfig.StdioTransportServer -> config.copy(
                                    commonOptions = config.commonOptions.copy(headers = updatedHeaders)
                                )
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        HugeIcons.Add01,
                        contentDescription = stringResource(R.string.setting_mcp_page_add_header)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.setting_mcp_page_add_header))
                }
            }
        }
    }
}

@Composable
private fun McpToolsConfigure(
    config: McpServerConfig,
    update: (McpServerConfig) -> Unit,
) {
    val mcpManager = koinInject<McpManager>()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (mcpManager.getClient(config) == null) {
            item {
                Text(stringResource(R.string.setting_mcp_page_tools_unavailable_message))
            }
        }
        items(config.commonOptions.tools) { tool ->
            McpToolCard(
                tool = tool,
                onEnableChange = { newVal ->
                    update(
                        config.clone(
                            commonOptions = config.commonOptions.copy(
                                tools = config.commonOptions.tools.map {
                                    if (tool.name == it.name) {
                                        it.copy(enable = newVal)
                                    } else {
                                        it
                                    }
                                }
                            )
                        )
                    )
                },
                onNeedsApprovalChange = { newVal ->
                    update(
                        config.clone(
                            commonOptions = config.commonOptions.copy(
                                tools = config.commonOptions.tools.map {
                                    if (tool.name == it.name) {
                                        it.copy(needsApproval = newVal)
                                    } else {
                                        it
                                    }
                                }
                            )
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun McpToolCard(
    tool: McpTool,
    onEnableChange: (Boolean) -> Unit,
    onNeedsApprovalChange: (Boolean) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = CustomColors.listItemColors.containerColor
        )
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // 第一行：工具名字和3个按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // 需要审批开关
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.setting_mcp_page_needs_approval),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Switch(
                        checked = tool.needsApproval,
                        onCheckedChange = onNeedsApprovalChange,
                        size = SwitchSize.Small
                    )
                }
                // 启用开关
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "启用",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Switch(
                        checked = tool.enable,
                        onCheckedChange = onEnableChange,
                        size = SwitchSize.Small
                    )
                }
                // 展开/收起按钮
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (expanded) HugeIcons.ArrowUp01 else HugeIcons.ArrowDown01,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            // 展开后显示描述和参数
            if (expanded) {
                // 描述
                if (!tool.description.isNullOrBlank()) {
                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    )
                }
                // 参数标签
                tool.inputSchema?.let { it as? InputSchema.Obj }?.let { schema ->
                    if (schema.properties.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            schema.properties.forEach { (key, _) ->
                                Tag(
                                    type = if (schema.required?.contains(key) == true) TagType.INFO else TagType.DEFAULT
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun parseMcpServersFromJson(json: String): List<McpServerConfig> {
    val root = Json.parseToJsonElement(json).jsonObject
    val mcpServers = root["mcpServers"]?.jsonObject ?: return emptyList()
    return mcpServers.entries.mapNotNull { (name, element) ->
        val obj = element.jsonObject
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "streamable_http"
        val headers = obj["headers"]?.jsonObject?.entries?.map { (k, v) ->
            k to (v.jsonPrimitive.contentOrNull ?: "")
        } ?: emptyList()
        val commonOptions = McpCommonOptions(name = name, headers = headers)
        when (type) {
            "sse" -> {
                val url = obj["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                McpServerConfig.SseTransportServer(commonOptions = commonOptions, url = url)
            }
            "stdio" -> {
                val command = obj["command"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val args = obj["args"]?.jsonArray?.map { it.jsonPrimitive.contentOrNull ?: "" }?.filter { it.isNotBlank() } ?: emptyList()
                val env = obj["env"]?.jsonObject?.entries?.map { (k, v) ->
                    k to (v.jsonPrimitive.contentOrNull ?: "")
                }?.filter { it.first.isNotBlank() } ?: emptyList()
                McpServerConfig.StdioTransportServer(
                    commonOptions = commonOptions,
                    command = command,
                    args = args,
                    env = env
                )
            }
            else -> {
                val url = obj["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                McpServerConfig.StreamableHTTPServer(commonOptions = commonOptions, url = url)
            }
        }
    }
}

@Composable
private fun McpImportModal(
    onDismiss: () -> Unit,
    onImport: (List<McpServerConfig>) -> Unit,
) {
    var jsonText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val noValidConfigMsg = stringResource(R.string.setting_mcp_page_import_no_valid_config)
    val parseErrorMsg = stringResource(R.string.setting_mcp_page_import_parse_error)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.setting_mcp_page_import_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.setting_mcp_page_import_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = jsonText,
                onValueChange = {
                    jsonText = it
                    errorMessage = null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("{ \"mcpServers\": { ... } }") },
                isError = errorMessage != null,
                supportingText = errorMessage?.let { msg -> { Text(msg, color = MaterialTheme.colorScheme.error) } }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = {
                        try {
                            val configs = parseMcpServersFromJson(jsonText.trim())
                            if (configs.isEmpty()) {
                                errorMessage = noValidConfigMsg
                            } else {
                                onImport(configs)
                            }
                        } catch (e: Exception) {
                            errorMessage = parseErrorMsg.format(e.message ?: "")
                        }
                    }
                ) {
                    Text(stringResource(R.string.setting_mcp_page_import_confirm))
                }
            }
        }
    }
}

@Composable
private fun LanDiscoverySection(
    config: McpServerConfig,
    update: (McpServerConfig) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    val scanner = remember { LanScanner(context) }
    var wifiIp by remember { mutableStateOf<String?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<ScanResult?>(null) }
    var scanResults by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var showResultsDialog by remember { mutableStateOf(false) }

    var scanPort by remember { mutableStateOf("9000") }
    var scanPath by remember { mutableStateOf("/mcp") }
    var testUrl by remember {
        mutableStateOf(
            when (config) {
                is McpServerConfig.SseTransportServer -> config.url
                is McpServerConfig.StreamableHTTPServer -> config.url
                else -> ""
            }
        )
    }

    LaunchedEffect(Unit) {
        wifiIp = scanner.getWifiIpAddress()
    }

    FormItem(
        label = { Text(stringResource(R.string.setting_mcp_page_lan_discovery)) },
        description = { Text(stringResource(R.string.setting_mcp_page_lan_discovery_desc)) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 本机 IP 显示
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.setting_mcp_page_your_ip),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = wifiIp ?: stringResource(R.string.setting_mcp_page_no_wifi_ip),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (wifiIp != null) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.error
                )
            }

            // 测试连接
            if (wifiIp != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = testUrl,
                        onValueChange = { testUrl = it },
                        label = { Text(stringResource(R.string.setting_mcp_page_url_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    FilledTonalIconButton(
                        onClick = {
                            isTesting = true
                            testResult = null
                            scope.launch {
                                testResult = scanner.testConnection(testUrl)
                                isTesting = false
                            }
                        },
                        enabled = !isTesting && testUrl.isNotBlank()
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                HugeIcons.Search01,
                                contentDescription = null
                            )
                        }
                    }
                }

                // 测试结果
                testResult?.let { result ->
                    Text(
                        text = stringResource(
                            if (result.isMcpServer) R.string.setting_mcp_page_test_success
                            else R.string.setting_mcp_page_test_fail,
                            result.responseTimeMs
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.isMcpServer) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error
                    )
                }

                HorizontalDivider()

                // 扫描子网
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = scanPort,
                        onValueChange = { scanPort = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text(stringResource(R.string.setting_mcp_page_scan_port)) },
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = scanPath,
                        onValueChange = { scanPath = it },
                        label = { Text(stringResource(R.string.setting_mcp_page_scan_path)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            isScanning = true
                            scanResults = emptyList()
                            scope.launch {
                                scanResults = scanner.scanSubnet(
                                    port = scanPort.toIntOrNull() ?: 9000,
                                    path = scanPath
                                )
                                isScanning = false
                                if (scanResults.isNotEmpty()) {
                                    showResultsDialog = true
                                }
                            }
                        },
                        enabled = !isScanning
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(stringResource(R.string.setting_mcp_page_scanning))
                            } else {
                                Icon(
                                    HugeIcons.Search01,
                                    contentDescription = null
                                )
                                Text(stringResource(R.string.setting_mcp_page_scan_subnet))
                            }
                        }
                    }
                }
            }
        }
    }

    // 扫描结果对话框
    if (showResultsDialog) {
        AlertDialog(
            onDismissRequest = { showResultsDialog = false },
            title = { Text(stringResource(R.string.setting_mcp_page_scan_results)) },
            text = {
                LazyColumn {
                    items(scanResults) { result ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            onClick = {
                                val newUrl = result.url
                                update(
                                    when (config) {
                                        is McpServerConfig.SseTransportServer ->
                                            config.copy(url = newUrl)
                                        is McpServerConfig.StreamableHTTPServer ->
                                            config.copy(url = newUrl)
                                        else -> config
                                    }
                                )
                                testUrl = newUrl
                                showResultsDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = result.serverName ?: result.host,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = result.url,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = stringResource(
                                        R.string.setting_mcp_page_response_time,
                                        result.responseTimeMs
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResultsDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

