package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import coil3.compose.AsyncImage
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Composable
fun Favicon(url: String, modifier: Modifier = Modifier) {
    val faviconUrl = remember {
        url.toHttpUrlOrNull()?.let { httpUrl ->
            val scheme = httpUrl.scheme
            val host = httpUrl.host
            HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegment("favicon.ico")
                .build()
                .toString()
        }
    }
    AsyncImage(
        model = faviconUrl,
        modifier = modifier.clip(CircleShape),
        contentDescription = null
    )
}