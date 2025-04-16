package me.rerere.rikkahub.ui.pages.setting

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.composables.icons.lucide.BadgeInfo
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Earth
import com.composables.icons.lucide.HardDrive
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
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
                    Text(text = "设置")
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
                    text = "界面设置",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text("动态颜色")
                    },
                    supportingContent = {
                        Text("是否使用动态颜色")
                    },
                    trailingContent = {
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = {
                                vm.updateSettings(settings.copy(dynamicColor = it))
                                navigateToChatPage(navController)
                            },
                        )
                    }
                )
            }

            stickyHeader {
                Text(
                    text = "模型与服务",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text("默认模型") },
                    description = { Text("设置各个功能的默认模型") },
                    icon = { Icon(Lucide.Heart, "Default Model") },
                    link = "setting/models"
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text("提供商") },
                    description = { Text("配置AI提供商") },
                    icon = { Icon(Lucide.Boxes, "Models") },
                    link = "setting/provider"
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text("搜索服务") },
                    description = { Text("设置搜索服务") },
                    icon = { Icon(Lucide.Earth, "Search") },
                    link = "setting/search"
                )
            }

            stickyHeader {
                Text(
                    text = "关于",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingItem(
                    navController = navController,
                    title = { Text("关于") },
                    description = { Text("关于本APP") },
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
                    title = { Text("聊天记录存储") },
                    description = {
                        if(storageState.first == -1) {
                            Text("计算中...")
                        } else {
                            Text("${storageState.first} 个文件，${"%.2f".format(storageState.second / 1024 / 1024.0)} MB")
                        }
                    },
                    icon = {
                        Icon(Lucide.HardDrive, "Storage")
                    },
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
) {
    Surface(
        onClick = {
            if(link != null) navController.navigate(link)
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