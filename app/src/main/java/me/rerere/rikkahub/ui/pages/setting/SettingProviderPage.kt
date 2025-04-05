package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.ui.components.BackButton
import me.rerere.rikkahub.ui.components.TextAvatar
import me.rerere.rikkahub.ui.components.icons.Boxes
import me.rerere.rikkahub.ui.components.icons.Settings
import me.rerere.rikkahub.ui.components.rememberDialogState
import me.rerere.rikkahub.ui.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingProviderPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "提供商")
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    AddButton {
                        vm.updateSettings(
                            settings.copy(
                                providers = settings.providers + it
                            )
                        )
                    }
                }
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .imePadding(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(settings.providers) { provider ->
                ProviderItem(
                    provider = provider,
                    onDelete = {
                        val newSettings = settings.copy(
                            providers = settings.providers - provider
                        )
                        vm.updateSettings(newSettings)
                    },
                    onEdit = { newProvider ->
                        val newSettings = settings.copy(
                            providers = settings.providers.map {
                                if (newProvider.id == provider.id) {
                                    newProvider
                                } else {
                                    it
                                }
                            }
                        )
                        vm.updateSettings(newSettings)
                    }
                )
            }
        }
    }
}

@Composable
fun AddButton(onAdd: (ProviderSetting) -> Unit) {
    val dialogState = rememberDialogState()

    var providerSetting: ProviderSetting by remember {
        mutableStateOf(ProviderSetting.OpenAI())
    }

    IconButton(
        onClick = {
            dialogState.openAlertDialog(
                title = {
                    Text("添加提供商")
                },
                text = {
                    ProviderConfigure(providerSetting) {
                        providerSetting = it
                    }
                },
                confirmText = {
                    Text("添加")
                },
                dismissText = {
                    Text("取消")
                },
                onConfirm = {
                    onAdd(providerSetting)
                    providerSetting = ProviderSetting.OpenAI()
                },
                onDismiss = {
                    dialogState.close()
                }
            )
        }
    ) {
        Icon(Icons.Outlined.Add, "Add")
    }
}

@Composable
private fun ProviderItem(
    provider: ProviderSetting,
    onEdit: (provider: ProviderSetting) -> Unit,
    onDelete: () -> Unit
) {
    val dialogState = rememberDialogState()
    var internalProvider by remember { mutableStateOf(provider) }
    var expand by remember { mutableStateOf(false) }
    Card {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .animateContentSize()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextAvatar(provider.name, modifier = Modifier.size(32.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(provider.name, style = MaterialTheme.typography.titleMedium)
                    ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                if (provider.enabled) "启用" else "禁用",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Text(
                                "${provider.models.size}个模型",
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        expand = !expand
                    }
                ) {
                    Icon(Settings, "Setting")
                }
            }
            if (expand) {
                ProviderConfigure(
                    provider = internalProvider,
                    modifier = Modifier.padding(8.dp),
                    onEdit = {
                        internalProvider = it
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = {
                            onDelete()
                        }
                    ) {
                        Icon(Icons.Outlined.Delete, "Delete")
                    }

                    Button(
                        onClick = {
                            onEdit(internalProvider)
                        }
                    ) {
                        Icon(Icons.Outlined.Edit, "Delete")
                        Text("保存")
                    }
                }
            }
        }
    }
}