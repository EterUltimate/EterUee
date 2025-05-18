package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.components.nav.BackButton
import org.koin.androidx.compose.koinViewModel

@Composable
fun AssistantDetailPage(vm: AssistantDetailVM = koinViewModel()) {
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    fun onUpdate(assistant: Assistant) {
        vm.update(assistant)
    }
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        "基础设定",
        "提示词",
        "记忆",
        "自定义请求"
    )
    val pagerState = rememberPagerState { tabs.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) })
                },
                navigationIcon = {
                    BackButton()
                }
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {

            SecondaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 24.dp,
            ) {
                tabs.fastForEachIndexed { index, tab ->
                    Tab(
                        selected = index == pagerState.currentPage,
                        onClick = { scope.launch { pagerState.scrollToPage(index) } },
                        text = {
                            Text(tab)
                        }
                    )
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        AssistantBasicSettings(assistant) {
                            onUpdate(it)
                        }
                    }
                    1 -> {

                    }
                    2 -> {

                    }
                    3 -> {

                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantBasicSettings(
    assistant: Assistant,
    onUpdate: (Assistant) -> Unit
) {

}