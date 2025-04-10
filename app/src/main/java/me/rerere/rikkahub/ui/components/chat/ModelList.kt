package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelType
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.components.AutoAIIcon
import me.rerere.rikkahub.ui.components.Tag
import me.rerere.rikkahub.ui.components.TagType
import me.rerere.rikkahub.ui.theme.extendColors
import kotlin.uuid.Uuid

@Composable
fun ModelSelector(
    modelId: Uuid,
    providers: List<ProviderSetting>,
    type: ModelType,
    modifier: Modifier = Modifier,
    onSelect: (Model) -> Unit = {}
) {
    var popup by remember {
        mutableStateOf(false)
    }
    val model = providers.findModelById(modelId)

    TextButton(
        onClick = {
            popup = true
        }, modifier = modifier
    ) {
        model?.modelId?.let {
            AutoAIIcon(
                it, Modifier
                    .padding(end = 4.dp)
                    .size(24.dp)
            )
        }
        Text(
            text = model?.displayName ?: "Select Model",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )
    }

    if (popup) {
        ModalBottomSheet(
            onDismissRequest = {
                popup = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModelList(providers = providers, modelType = type) {
                    popup = false
                    onSelect(it)
                }
            }
        }
    }
}

@Composable
fun ModelList(
    providers: List<ProviderSetting>,
    modelType: ModelType,
    onSelect: (Model) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        providers
            .fastFilter { it.enabled && it.models.isNotEmpty() }
            .fastForEach { providerSetting ->
                stickyHeader {
                    Text(
                        text = providerSetting.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                    )
                }

                items(
                    providerSetting.models.fastFilter { it.type == modelType },
                    key = { it.id }) { model ->
                    ModelItem(onSelect, model)
                }
            }
    }
}

@Composable
private fun ModelItem(
    onSelect: (Model) -> Unit,
    model: Model
) {
    OutlinedCard(
        onClick = { onSelect(model) }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            AutoAIIcon(model.modelId, modifier = Modifier.size(32.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    model.modelId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.extendColors.gray4
                )
                Text(
                    text = model.displayName,
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Tag(type = TagType.INFO) {
                        Text(
                            when (model.type) {
                                ModelType.CHAT -> "Chat"
                                ModelType.EMBEDDING -> "Embedding"
                            }
                        )
                    }
                }
            }
        }
    }
}
