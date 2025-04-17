package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingSearchPage(vm: SettingVM = koinViewModel()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("搜索服务")
                },
                navigationIcon = {
                    BackButton()
                }
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = it + PaddingValues(8.dp)
        ) {
            item {
                OutlinedCard {
                    FormItem(
                        label = {
                            Text("搜索提供商")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Select(
                            options = listOf("Tavily", "Bing", "Google"),
                            selectedOption = "Tavily",
                            optionToString = { it },
                            onOptionSelected = { selected ->

                            },
                        )
                    }
                }
            }
        }
    }
}