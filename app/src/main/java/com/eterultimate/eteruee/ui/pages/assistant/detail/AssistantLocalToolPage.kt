package com.eterultimate.eteruee.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sonner.ToastType
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.data.ai.tools.LocalToolOption
import com.eterultimate.eteruee.data.model.Assistant
import com.eterultimate.eteruee.ui.components.nav.BackButton
import com.eterultimate.eteruee.ui.components.ui.CardGroup
import com.eterultimate.eteruee.ui.context.LocalToaster
import com.eterultimate.eteruee.ui.theme.CustomColors
import com.eterultimate.eteruee.utils.hasUsageStatsPermission
import com.eterultimate.eteruee.utils.openUsageAccessSettings
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AssistantLocalToolPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(stringResource(R.string.assistant_page_tab_local_tools))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        AssistantLocalToolContent(
            innerPadding = innerPadding,
            assistant = assistant,
            onUpdate = { vm.update(it) }
        )
    }
}

@Composable
private fun AssistantLocalToolContent(
    innerPadding: PaddingValues,
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val permissionRequiredText =
        stringResource(R.string.assistant_page_local_tools_screen_time_permission_required)

    fun toggleLocalTool(option: LocalToolOption, enabled: Boolean) {
        if (enabled && option == LocalToolOption.ScreenTime && !context.hasUsageStatsPermission()) {
            toaster.show(message = permissionRequiredText, type = ToastType.Warning)
            context.openUsageAccessSettings()
        }
        val newLocalTools = if (enabled) {
            assistant.localTools + option
        } else {
            assistant.localTools - option
        }
        onUpdate(assistant.copy(localTools = newLocalTools))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(innerPadding)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardGroup {
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_javascript_engine_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_javascript_engine_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.JavascriptEngine),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.JavascriptEngine, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_time_info_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_time_info_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.TimeInfo),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.TimeInfo, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_screen_time_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_screen_time_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.ScreenTime),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.ScreenTime, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_clipboard_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_clipboard_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.Clipboard),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.Clipboard, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_tts_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_tts_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.Tts),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.Tts, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_video_generation_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_video_generation_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.VideoGeneration),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.VideoGeneration, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_ask_user_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_ask_user_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.AskUser),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.AskUser, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_ssh_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_ssh_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.Ssh),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.Ssh, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_shell_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_shell_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.Shell),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.Shell, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_linux_environment_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_linux_environment_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.LinuxEnvironment),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.LinuxEnvironment, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_traffic_control_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_traffic_control_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.TrafficControl),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.TrafficControl, it) }
                    )
                }
            )
            item(
                headlineContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_device_agent_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.assistant_page_local_tools_device_agent_desc))
                },
                trailingContent = {
                    Switch(
                        checked = assistant.localTools.contains(LocalToolOption.DeviceAgent),
                        onCheckedChange = { toggleLocalTool(LocalToolOption.DeviceAgent, it) }
                    )
                }
            )
        }
    }
}

