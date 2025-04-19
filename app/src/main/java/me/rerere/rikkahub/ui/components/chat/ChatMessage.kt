package me.rerere.rikkahub.ui.components.chat

import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.ChevronUp
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.Volume2
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageAnnotation
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.ui.Favicon
import me.rerere.rikkahub.ui.components.ui.ImagePreviewDialog
import me.rerere.rikkahub.ui.context.LocalTTSService
import me.rerere.rikkahub.ui.theme.extendColors
import me.rerere.rikkahub.utils.copyMessageToClipboard
import me.rerere.rikkahub.utils.urlDecode

@Composable
fun ChatMessage(
    message: UIMessage,
    modifier: Modifier = Modifier,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.role == MessageRole.USER) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MessagePartsBlock(message.role, message.parts, message.annotations)
        Actions(
            message = message,
            onRegenerate = onRegenerate,
            onEdit = onEdit
        )
    }
}

@Composable
private fun Actions(
    message: UIMessage,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Lucide.Copy, stringResource(R.string.copy), modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = {
                        context.copyMessageToClipboard(message)
                    }
                )
                .padding(8.dp)
                .size(16.dp)
        )

        Icon(
            Lucide.RefreshCw, stringResource(R.string.regenerate), modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = LocalIndication.current,
                    onClick = {
                        onRegenerate()
                    }
                )
                .padding(8.dp)
                .size(16.dp)
        )

        if (message.role == MessageRole.USER) {
            Icon(
                Lucide.Pencil, stringResource(R.string.edit), modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = {
                            onEdit()
                        }
                    )
                    .padding(8.dp)
                    .size(16.dp)
            )
        }
        if (message.role == MessageRole.ASSISTANT) {
            val tts = LocalTTSService.current
            Icon(
                Lucide.Volume2, stringResource(R.string.tts), modifier = Modifier
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        onClick = {
                            tts?.speak(message.text(), TextToSpeech.QUEUE_FLUSH, null, null)
                        }
                    )
                    .padding(8.dp)
                    .size(16.dp)
            )
        }
    }
}

@Composable
fun MessagePartsBlock(
    role: MessageRole,
    parts: List<UIMessagePart>,
    annotations: List<UIMessageAnnotation>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    var expandReasoning by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    // Search
    parts.filterIsInstance<UIMessagePart.Search>().fastForEach { search ->
        ProvideTextStyle(MaterialTheme.typography.labelSmall) {
            OutlinedCard {
                Column(
                    modifier = Modifier.padding(4.dp)
                ) {
                    search.search.items.fastForEach {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Favicon(it.url, Modifier.size(24.dp))
                            Text(it.title)
                        }
                    }
                }
            }
        }
    }

    // Reasoning
    parts.filterIsInstance<UIMessagePart.Reasoning>().fastForEach {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        indication = LocalIndication.current,
                        interactionSource = interactionSource
                    ) {
                        expandReasoning = !expandReasoning
                    }
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Lucide.Lightbulb, null, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.deep_thinking))
                Icon(
                    if (expandReasoning) Lucide.ChevronUp else Lucide.ChevronDown, null
                )
            }
        }
        AnimatedVisibility(
            expandReasoning,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Box(
                modifier = Modifier
                    .drawWithContent {
                        drawContent()
                        drawRoundRect(
                            color = contentColor.copy(alpha = 0.2f),
                            size = Size(width = 10f, height = size.height),
                        )
                    }
                    .padding(start = 4.dp)
                    .padding(4.dp)
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodySmall) {
                    MarkdownBlock(it.reasoning)
                }
            }
        }
    }

    // Text
    parts.filterIsInstance<UIMessagePart.Text>().fastForEach { part ->
        SelectionContainer {
            if (role == MessageRole.USER) {
                Card(
                    modifier = Modifier
                        .animateContentSize(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(Modifier.padding(8.dp)) {
                        MarkdownBlock(part.text)
                    }
                }
            } else {
                MarkdownBlock(part.text)
            }
        }
    }

    // Annotations
    if (annotations.isNotEmpty()) {
        Column(
            modifier = Modifier.animateContentSize(),
        ) {
            var expand by remember { mutableStateOf(false) }
            if (expand) {
                ProvideTextStyle(
                    MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.extendColors.gray8.copy(alpha = 0.65f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    color = contentColor.copy(alpha = 0.2f),
                                    size = Size(width = 10f, height = size.height),
                                )
                            }
                            .padding(start = 16.dp)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        annotations.fastForEachIndexed { index, annotation ->
                            when (annotation) {
                                is UIMessageAnnotation.UrlCitation -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Favicon(annotation.url, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = buildAnnotatedString {
                                                append("${index + 1}. ")
                                                withLink(LinkAnnotation.Url(annotation.url)) {
                                                    append(annotation.title.urlDecode())
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = {
                    expand = !expand
                }
            ) {
                Text(stringResource(R.string.citations_count, annotations.size))
            }
        }
    }

    // Images
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var showImageViewer by remember { mutableStateOf(false) }
        val images = parts.filterIsInstance<UIMessagePart.Image>()
        images.fastForEach {
            AsyncImage(
                model = it.url,
                contentDescription = null,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .width(72.dp)
                    .clickable { 
                        showImageViewer = true
                    }
            )
        }
        if(showImageViewer) {
            ImagePreviewDialog(images.map { it.url }) {
                showImageViewer = false
            }
        }
    }
}