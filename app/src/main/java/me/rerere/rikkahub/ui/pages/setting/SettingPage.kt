package me.rerere.rikkahub.ui.pages.setting

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.composables.icons.lucide.BadgeInfo
import com.composables.icons.lucide.Bot
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Earth
import com.composables.icons.lucide.HardDrive
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Monitor
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Share2
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.utils.countChatFiles
import me.rerere.rikkahub.utils.navigateToChatPage
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingPage(vm: SettingVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding
        ) {
            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_general_settings),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_display_setting)) },
                    description = { Text(stringResource(R.string.setting_page_display_setting_desc)) },
                    icon = { Icon(Lucide.Monitor, "Display Setting") },
                    link = "setting/display"
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_assistant)) },
                    description = { Text(stringResource(R.string.setting_page_assistant_desc)) },
                    icon = { Icon(Lucide.Bot, "Assistant") },
                    link = "assistant"
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(stringResource(R.string.setting_page_dynamic_color))
                    },
                    supportingContent = {
                        Text(stringResource(R.string.setting_page_dynamic_color_desc))
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = {
                                vm.updateSettings(settings.copy(dynamicColor = it))
                                navigateToChatPage(navController)
                            },
                        )
                    },
                    leadingContent = {
                        Icon(Lucide.Palette, null)
                    }
                )
            }

            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_model_and_services),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_default_model)) },
                    description = { Text(stringResource(R.string.setting_page_default_model_desc)) },
                    icon = { Icon(Lucide.Heart, "Default Model") },
                    link = "setting/models"
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_providers)) },
                    description = { Text(stringResource(R.string.setting_page_providers_desc)) },
                    icon = { Icon(Lucide.Boxes, "Models") },
                    link = "setting/provider"
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_search_service)) },
                    description = { Text(stringResource(R.string.setting_page_search_service_desc)) },
                    icon = { Icon(Lucide.Earth, "Search") },
                    link = "setting/search"
                )
            }

            stickyHeader {
                Text(
                    text = stringResource(R.string.setting_page_about),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_about)) },
                    description = { Text(stringResource(R.string.setting_page_about_desc)) },
                    icon = { Icon(Lucide.BadgeInfo, "About") },
                    link = "setting/about"
                )
            }

            item {
                val context = LocalContext.current
                val storageState by produceState(-1 to 0L) {
                    value = context.countChatFiles()
                }
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_chat_storage)) },
                    description = {
                        if (storageState.first == -1) {
                            Text(stringResource(R.string.calculating))
                        } else {
                            Text(
                                stringResource(
                                    R.string.setting_page_chat_storage_desc,
                                    storageState.first,
                                    storageState.second / 1024 / 1024.0
                                )
                            )
                        }
                    },
                    icon = {
                        Icon(Lucide.HardDrive, "Storage")
                    },
                )
            }

            item {
                val context = LocalContext.current
                val shareText = stringResource(R.string.setting_page_share_text)
                val share = stringResource(R.string.setting_page_share)
                val noShareApp = stringResource(R.string.setting_page_no_share_app)
                SettingItem(
                    navController = navController,
                    title = { Text(stringResource(R.string.setting_page_share)) },
                    description = {
                        Text(stringResource(R.string.setting_page_share_desc))
                    },
                    icon = {
                        Icon(Lucide.Share2, "Share")
                    },
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND)
                        intent.type = "text/plain"
                        intent.putExtra(Intent.EXTRA_TEXT, shareText)
                        try {
                            context.startActivity(Intent.createChooser(intent, share))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, noShareApp, Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SettingItem(
    navController: NavController,
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    link: String? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = {
            if (link != null) navController.navigate(link)
            onClick()
        }
    ) {
        ListItem(
            headlineContent = {
                title()
            },
            supportingContent = {
                description()
            },
            leadingContent = {
                icon()
            }
        )
    }
}