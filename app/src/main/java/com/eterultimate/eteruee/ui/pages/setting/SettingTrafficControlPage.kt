package com.eterultimate.eteruee.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.network.HiddifyCoreManager
import com.eterultimate.eteruee.ui.components.nav.BackButton
import com.eterultimate.eteruee.ui.components.ui.CardGroup
import com.eterultimate.eteruee.ui.context.LocalToaster
import com.eterultimate.eteruee.ui.theme.CustomColors
import com.eterultimate.eteruee.utils.plus
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Connect
import me.rerere.hugeicons.stroke.Play
import me.rerere.hugeicons.stroke.Stop
import org.koin.compose.koinInject

@Composable
fun SettingTrafficControlPage(
    manager: HiddifyCoreManager = koinInject(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state by manager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    var configPath by remember { mutableStateOf(manager.defaultConfigPath()) }
    var configContent by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val testSuccess = stringResource(R.string.setting_traffic_control_test_success)

    fun runCoreAction(block: suspend () -> Unit) {
        scope.launch {
            localError = null
            runCatching { block() }
                .onFailure { localError = it.message ?: it.javaClass.simpleName }
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.setting_page_traffic_control)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CardGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.setting_traffic_control_core)) },
                ) {
                    item(
                        leadingContent = { Icon(HugeIcons.Connect, contentDescription = null) },
                        headlineContent = {
                            Text(
                                if (state.isRunning) {
                                    stringResource(R.string.setting_traffic_control_status_running)
                                } else if (state.isAvailable) {
                                    stringResource(R.string.setting_traffic_control_status_ready)
                                } else {
                                    stringResource(R.string.setting_traffic_control_status_missing)
                                }
                            )
                        },
                        supportingContent = {
                            Text(
                                state.bindingClassName
                                    ?: stringResource(R.string.setting_traffic_control_missing_aar)
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_traffic_control_core_message)) },
                        supportingContent = {
                            SelectionContainer {
                                Text(
                                    text = localError ?: state.error ?: state.message ?: "",
                                    color = if (localError != null || state.error != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        },
                    )
                }
            }

            item {
                CardGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.setting_traffic_control_config)) },
                ) {
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_traffic_control_config_path)) },
                        supportingContent = {
                            OutlinedTextField(
                                value = configPath,
                                onValueChange = { configPath = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_traffic_control_config_content)) },
                        supportingContent = {
                            OutlinedTextField(
                                value = configContent,
                                onValueChange = { configContent = it },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 8,
                                maxLines = 18,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                        },
                    )
                }
            }

            item {
                CardGroup(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.setting_traffic_control_actions)) },
                ) {
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_traffic_control_test)) },
                        trailingContent = {
                            FilledTonalButton(
                                enabled = state.isAvailable && !state.isLoading,
                                onClick = {
                                    runCoreAction {
                                        toaster.show(manager.test().ifBlank { testSuccess })
                                    }
                                },
                            ) {
                                Text(stringResource(R.string.setting_traffic_control_test))
                            }
                        },
                    )
                    item(
                        headlineContent = {
                            Text(
                                if (state.isRunning) {
                                    stringResource(R.string.setting_traffic_control_stop)
                                } else {
                                    stringResource(R.string.setting_traffic_control_start)
                                }
                            )
                        },
                        trailingContent = {
                            Button(
                                enabled = state.isAvailable && !state.isLoading &&
                                    (state.isRunning || configContent.isNotBlank()),
                                onClick = {
                                    if (state.isRunning) {
                                        runCoreAction { manager.stop() }
                                    } else {
                                        runCoreAction {
                                            manager.start(
                                                configPath = configPath,
                                                configContent = configContent,
                                            )
                                        }
                                    }
                                },
                            ) {
                                if (state.isLoading) {
                                    CircularProgressIndicator()
                                } else {
                                    Icon(
                                        imageVector = if (state.isRunning) HugeIcons.Stop else HugeIcons.Play,
                                        contentDescription = null,
                                    )
                                }
                                Text(
                                    if (state.isRunning) {
                                        stringResource(R.string.setting_traffic_control_stop)
                                    } else {
                                        stringResource(R.string.setting_traffic_control_start)
                                    }
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
