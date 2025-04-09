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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Code
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.Lucide
import me.rerere.rikkahub.ui.components.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingPage(vm: SettingVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
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
                    text = "模型设置",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                DefaultModelSetting(navController)
            }

            item {
                ProviderSetting(navController)
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
                DebugSetting(navController)
            }
        }
    }
}

@Composable
private fun DefaultModelSetting(navController: NavController) {
    Surface(
        onClick = {
            navController.navigate("setting/models")
        }
    ) {
        ListItem(
            headlineContent = {
                Text("默认模型")
            },
            supportingContent = {
                Text("设置各个功能的默认模型")
            },
            leadingContent = {
                Icon(Lucide.Heart, "Default Model")
            }
        )
    }
}

@Composable
private fun ProviderSetting(navController: NavController) {
    Surface(
        onClick = {
            navController.navigate("setting/provider")
        }
    ) {
        ListItem(
            headlineContent = {
                Text("提供商")
            },
            supportingContent = {
                Text("配置AI提供商")
            },
            leadingContent = {
                Icon(Lucide.Boxes, "Models")
            }
        )
    }
}

@Composable
fun DebugSetting(navController: NavController) {
    Surface(
        onClick = {
            navController.navigate("debug")
        }
    ) {
        ListItem(
            headlineContent = {
                Text("Debug")
            },
            supportingContent = {
                Text("开发者选项")
            },
            leadingContent = {
                Icon(Lucide.Code, "Developer Options")
            }
        )
    }
}