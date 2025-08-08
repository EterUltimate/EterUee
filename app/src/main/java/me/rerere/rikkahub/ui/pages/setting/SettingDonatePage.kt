package me.rerere.rikkahub.ui.pages.setting

import android.view.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import me.rerere.ai.util.await
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.api.SponsorAPI
import me.rerere.rikkahub.data.model.Sponsor
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.UiState
import me.rerere.rikkahub.utils.onError
import me.rerere.rikkahub.utils.onLoading
import me.rerere.rikkahub.utils.onSuccess
import me.rerere.rikkahub.utils.openUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.compose.koinInject

@Composable
fun SettingDonatePage() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Donate")
                },
                navigationIcon = {
                    BackButton()
                }
            )
        }
    ) { paddings ->
        Column(
            modifier = Modifier
                .padding(paddings)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "赞助方式",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Patreon()
            Afdian()

            Text(
                text = "赞助用户列表",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Sponsors(
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun Patreon() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = {
            context.openUrl("https://patreon.com/rikkahub")
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.patreon),
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Patreon",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "适用于全球用户, 赞助后可以获得Discord Role",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Afdian() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = {
            context.openUrl("https://afdian.com/a/reovo")
        }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.afdian),
                contentDescription = null
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "afdian",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "适用于🇨🇳用户",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun Sponsors(modifier: Modifier = Modifier) {
    val sponsorAPI = koinInject<SponsorAPI>()
    val sponsors by produceState<UiState<List<Sponsor>>>(UiState.Idle) {
        value = UiState.Loading
        runCatching {
            val sponsors = sponsorAPI.getSponsors()
            println(sponsors)
            value = UiState.Success(sponsors)
        }.onFailure {
            value = UiState.Error(it)
        }
    }
    Box(
        modifier = modifier
    ) {
        sponsors.onSuccess { value ->
            LazyVerticalGrid(
                columns = GridCells.FixedSize(48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(value) {
                    Surface {
                        AsyncImage(
                            model = it.avatar,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = it.userName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }.onLoading {
            CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }.onError {
            Text(
                text = it.message ?: it.javaClass.simpleName,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

