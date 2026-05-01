package com.eterultimate.eteruee.ui.pages.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sonner.ToastType
import kotlinx.coroutines.launch
import me.rerere.common.android.Logging
import com.eterultimate.eteruee.data.model.Avatar
import com.eterultimate.eteruee.ui.components.ui.UIAvatar
import com.eterultimate.eteruee.ui.components.nav.BackButton
import com.eterultimate.eteruee.ui.components.richtext.MarkdownBlock
import com.eterultimate.eteruee.ui.components.richtext.MathBlock
import com.eterultimate.eteruee.ui.components.richtext.Mermaid
import com.eterultimate.eteruee.ui.context.LocalSettings
import com.eterultimate.eteruee.ui.context.LocalToaster
import com.eterultimate.eteruee.ui.theme.JetbrainsMono
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.uuid.Uuid

@Composable
fun DebugPage(vm: DebugVM = koinViewModel()) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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
        val state = rememberPagerState { 2 }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            SecondaryTabRow(
                selectedTabIndex = state.currentPage,
            ) {
                Tab(
                    selected = state.currentPage == 0,
                    onClick = {
                        scope.launch {
                            state.animateScrollToPage(0)
                        }
                    },
                    text = {
                        Text("Main")
                    }
                )
                Tab(
                    selected = state.currentPage == 1,
                    onClick = {
                        scope.launch {
                            state.animateScrollToPage(1)
                        }
                    },
                    text = {
                        Text("Logging")
                    }
                )
            }
            HorizontalPager(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> MainPage(vm)
                    1 -> Box {}
                }
            }
        }
    }
}

@Composable
private fun MainPage(vm: DebugVM) {
    val settings = LocalSettings.current
    Column(
        modifier = Modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        var avatar: Avatar by remember { mutableStateOf(Avatar.Emoji("馃槑")) }
        UIAvatar(
            value = avatar,
            onUpdate = {
                println("Avatar updated: $it")
                avatar = it
            },
            name = "A"
        )
        Mermaid(
            code = """
                mindmap
                  root((mindmap))
                    Origins
                      Long history
                      ::icon(fa fa-book)
                      Popularisation
                        British popular psychology author Tony Buzan
                    Research
                      On effectiveness<br/>and features
                      On Automatic creation
                        Uses
                            Creative techniques
                            Strategic planning
                            Argument mapping
                    Tools
                      Pen and paper
                      Mermaid
                """.trimIndent(),
            modifier = Modifier.fillMaxWidth(),
        )

        var counter by remember {
            mutableIntStateOf(0)
        }
        val toaster = LocalToaster.current
        Button(
            onClick = {
                toaster.show("娴嬭瘯 ${counter++}")
                toaster.show("娴嬭瘯 ${counter++}", type = ToastType.Info)
                toaster.show("娴嬭瘯 ${counter++}", type = ToastType.Error)
            }
        ) {
            Text("toast")
        }
        Button(
            onClick = {
                vm.updateSettings(
                    settings.copy(
                        chatModelId = Uuid.random()
                    )
                )
            }
        ) {
            Text("閲嶇疆Chat妯″瀷")
        }

        Button(
            onClick = {
                error("娴嬭瘯宕╂簝 ${Random.nextInt(0..1000)}")
            }
        ) {
            Text("宕╂簝")
        }

        Button(
            onClick = {
                vm.createOversizedConversation(30)
                toaster.show("姝ｅ湪鍒涘缓 30MB 瓒呭ぇ瀵硅瘽...")
            }
        ) {
            Text("鍒涘缓瓒呭ぇ瀵硅瘽 (30MB)")
        }

        Button(
            onClick = {
                vm.createConversationWithMessages(1024)
                toaster.show("姝ｅ湪鍒涘缓 1024 鏉℃秷鎭璇?..")
            }
        ) {
            Text("鍒涘缓 1024 涓秷鎭殑鑱婂ぉ")
        }

        HorizontalDivider()

        Text("Launch Stats", style = MaterialTheme.typography.labelMedium)

        var launchCountInput by remember(settings.launchCount) {
            mutableStateOf(settings.launchCount.toString())
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = launchCountInput,
                onValueChange = { launchCountInput = it },
                label = { Text("launchCount (current: ${settings.launchCount})") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(onClick = {
                launchCountInput.toIntOrNull()?.let {
                    vm.updateSettings(settings.copy(launchCount = it))
                }
            }) {
                Text("Set")
            }
        }

        var dismissedAtInput by remember(settings.sponsorAlertDismissedAt) {
            mutableStateOf(settings.sponsorAlertDismissedAt.toString())
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = dismissedAtInput,
                onValueChange = { dismissedAtInput = it },
                label = { Text("sponsorAlertDismissedAt (current: ${settings.sponsorAlertDismissedAt})") },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Button(onClick = {
                dismissedAtInput.toIntOrNull()?.let {
                    vm.updateSettings(settings.copy(sponsorAlertDismissedAt = it))
                }
            }) {
                Text("Set")
            }
        }

        var markdown by remember { mutableStateOf("") }
        MarkdownBlock(markdown, modifier = Modifier.fillMaxWidth())
        MathBlock(markdown)
        OutlinedTextField(
            value = markdown,
            onValueChange = { markdown = it },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

