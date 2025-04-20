package me.rerere.rikkahub.ui.pages.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.search.SearchService
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.uuid.Uuid

@Composable
fun DebugPage(vm: DebugVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Debug Mode")
                },
                navigationIcon = {
                    BackButton()
                }
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    vm.updateSettings(
                        settings.copy(
                            chatModelId = Uuid.random()
                        )
                    )
                }
            ) {
                Text("重置Chat模型")
            }

            Button(
                onClick = {
                    error("测试崩溃 ${Random.nextInt(0..1000)}")
                }
            ) {
                Text("崩溃")
            }

            val scope = rememberCoroutineScope()
            Button(
                onClick = {
                    scope.launch {
                        val service = SearchService.getService(settings.searchServiceOptions)
                        val result = service.search(
                            query = "mc 1.21.5更新内容",
                            commonOptions = settings.searchCommonOptions,
                            serviceOptions = settings.searchServiceOptions
                        )
                        result.onSuccess {
                            println(it)
                        }.onFailure {
                            it.printStackTrace()
                        }
                    }
                }
            ) {
                Text("测试搜索")
            }

            Button(
                onClick = {
                    vm.vectorDatabase.test()
                }
            ) {
                Text("vector db test")
            }
        }
    }
}