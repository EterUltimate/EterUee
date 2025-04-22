package me.rerere.rikkahub.ui.pages.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.utils.plus
import java.util.Calendar

@Composable
fun MenuPage() {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton()
                },
                title = {},
            )
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = it + PaddingValues(16.dp)
        ) {
            item {
                Greeting()
            }

            item {
                FeaturesSection()
            }

            item {
                LeaderBoard()
            }
        }
    }
}

@Composable
private fun Greeting(modifier: Modifier = Modifier) {
    fun getGreetingMessage(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "早上好"
            in 12..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深了，注意休息"
        }
    }

    Column {
        Text(
            text = getGreetingMessage() + "\uD83D\uDC4B",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )
    }
}

@Composable
private fun FeaturesSection() {
    val navController = LocalNavController.current
    Column {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FeatureCard(
                    title = {
                        Text("AI翻译")
                    },
                    image = {
                        AsyncImage(
                            model = "file:///android_asset/banner/translator.jpeg",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    },
                    modifier = Modifier.width(160.dp)
                ) {
                    navController.navigate("translator")
                }
            }

            item {
                FeatureCard(
                    title = {
                        Text("知识库 (施工中)")
                    },
                    image = {
                        AsyncImage(
                            model = "file:///android_asset/banner/library.jpeg",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    },
                    modifier = Modifier.width(160.dp)
                ) {
                    // navController.navigate("library")
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: @Composable () -> Unit,
    image: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Column {
            Box(
                modifier = Modifier
                    .clip(CardDefaults.shape)
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                image()
            }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                title()
            }
        }
    }
}

@Composable
private fun LeaderBoard() {

}