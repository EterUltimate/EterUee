package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter
import com.jvziyaoyao.scale.image.pager.ImagePager
import com.jvziyaoyao.scale.zoomable.pager.rememberZoomablePagerState

@Composable
fun ImagePreviewDialog(
    images: List<Any>,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        ImagePager(
            modifier = Modifier.fillMaxSize(),
            pagerState = rememberZoomablePagerState { images.size },
            imageLoader = { index ->
                val painter = rememberAsyncImagePainter(images[index])
                return@ImagePager Pair(painter, painter.intrinsicSize)
            },
        )
    }
}