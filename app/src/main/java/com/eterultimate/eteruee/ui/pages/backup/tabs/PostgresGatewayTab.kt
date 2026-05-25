package com.eterultimate.eteruee.ui.pages.backup.tabs

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Upload02
import me.rerere.hugeicons.stroke.View
import me.rerere.hugeicons.stroke.ViewOff
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sonner.ToastType
import kotlinx.coroutines.launch
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.data.sync.postgres.PostgresGatewayConfig
import com.eterultimate.eteruee.ui.components.ui.CardGroup
import com.eterultimate.eteruee.ui.context.LocalToaster
import com.eterultimate.eteruee.ui.pages.backup.BackupVM
import com.eterultimate.eteruee.utils.toLocalDateTime
import java.time.Instant

@Composable
fun PostgresGatewayTab(vm: BackupVM) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val config = settings.postgresGatewayConfig
    val toaster = LocalToaster.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBackingUp by remember { mutableStateOf(false) }

    fun updateConfig(newConfig: PostgresGatewayConfig) {
        vm.updateSettings(settings.copy(postgresGatewayConfig = newConfig))
    }

    val lastBackupText = if (settings.backupReminderConfig.lastBackupTime == 0L) {
        stringResource(R.string.backup_page_reminder_no_record)
    } else {
        stringResource(
            R.string.backup_page_reminder_last_time,
            Instant.ofEpochMilli(settings.backupReminderConfig.lastBackupTime).toLocalDateTime()
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CardGroup {
                item(
                    headlineContent = { Text(stringResource(R.string.backup_page_postgres_gateway)) },
                    supportingContent = { Text(lastBackupText) },
                )
            }

            CardGroup {
                item(
                    headlineContent = { Text(stringResource(R.string.backup_page_postgres_gateway_url)) },
                    supportingContent = {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = config.baseUrl,
                            onValueChange = { updateConfig(config.copy(baseUrl = it.trim())) },
                            placeholder = { Text("https://example.com/eteruee-pg") },
                            singleLine = true
                        )
                    },
                )
                item(
                    headlineContent = { Text(stringResource(R.string.backup_page_postgres_gateway_token)) },
                    supportingContent = {
                        var tokenVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = config.accessToken,
                            onValueChange = { updateConfig(config.copy(accessToken = it)) },
                            visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                    Icon(
                                        imageVector = if (tokenVisible) HugeIcons.ViewOff else HugeIcons.View,
                                        contentDescription = null
                                    )
                                }
                            },
                            singleLine = true
                        )
                    },
                )
                item(
                    headlineContent = { Text(stringResource(R.string.backup_page_postgres_gateway_namespace)) },
                    supportingContent = {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = config.namespace,
                            onValueChange = { updateConfig(config.copy(namespace = it.trim())) },
                            singleLine = true
                        )
                    },
                )
            }

            CardGroup {
                item(
                    headlineContent = { Text(stringResource(R.string.backup_page_backup_items)) },
                    supportingContent = {
                        MultiChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            PostgresGatewayConfig.BackupItem.entries.forEachIndexed { index, item ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = PostgresGatewayConfig.BackupItem.entries.size
                                    ),
                                    onCheckedChange = { checked ->
                                        val newItems = if (checked) {
                                            config.items + item
                                        } else {
                                            config.items - item
                                        }
                                        updateConfig(config.copy(items = newItems))
                                    },
                                    checked = item in config.items
                                ) {
                                    Text(
                                        when (item) {
                                            PostgresGatewayConfig.BackupItem.DATABASE ->
                                                stringResource(R.string.backup_page_chat_records)

                                            PostgresGatewayConfig.BackupItem.FILES ->
                                                stringResource(R.string.backup_page_files)
                                        }
                                    )
                                }
                            }
                        }
                    },
                )
            }
        }

        HorizontalDivider()
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        try {
                            vm.testPostgresGateway()
                            toaster.show(
                                context.getString(R.string.backup_page_connection_success),
                                type = ToastType.Success
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            toaster.show(
                                context.getString(
                                    R.string.backup_page_connection_failed,
                                    e.message ?: ""
                                ),
                                type = ToastType.Error
                            )
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.backup_page_test_connection))
            }

            Button(
                onClick = {
                    scope.launch {
                        isBackingUp = true
                        runCatching {
                            vm.backupToPostgresGateway()
                            toaster.show(
                                context.getString(R.string.backup_page_backup_success),
                                type = ToastType.Success
                            )
                        }.onFailure {
                            it.printStackTrace()
                            toaster.show(
                                it.message ?: context.getString(R.string.backup_page_unknown_error),
                                type = ToastType.Error
                            )
                        }
                        isBackingUp = false
                    }
                },
                enabled = !isBackingUp
            ) {
                if (isBackingUp) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(HugeIcons.Upload02, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (isBackingUp) {
                        stringResource(R.string.backup_page_backing_up)
                    } else {
                        stringResource(R.string.backup_page_backup_now)
                    }
                )
            }
        }
    }
}
