package me.rerere.rikkahub.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.hooks.LocalNavController
import me.rerere.rikkahub.ui.hooks.heroAnimation

@Composable
fun ChatPage() {
    val navController = LocalNavController.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "设置")
                },
                actions = {

                },
                navigationIcon = {

                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
        ) {
            Card(
                modifier = Modifier.heroAnimation("setting_card"),
                onClick = {
                    navController.navigate("setting")
                }
            ) {
                Box(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("设置")
                }
            }
        }
    }
}