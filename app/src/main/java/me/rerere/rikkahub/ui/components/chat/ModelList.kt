package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.unit.dp
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelType
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.components.Tag
import me.rerere.rikkahub.ui.components.TagType
import me.rerere.rikkahub.ui.theme.extendColors
import kotlin.uuid.Uuid

@Composable
fun ModelSelector(
    modelId: Uuid,
    providers: List<ProviderSetting>,
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
        Text(model?.displayName ?: "Select Model")
    }

    if (popup) {
        ModalBottomSheet(
            onDismissRequest = {
                popup = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ModelList(providers) {
                    popup = false
                    onSelect(it)
                }
            }
        }
    }
}

@Composable
fun ModelList(providers: List<ProviderSetting>, onSelect: (Model) -> Unit) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        providers.filter { it.enabled && it.models.isNotEmpty() }.forEach { providerSetting ->
            stickyHeader {
                Text(
                    text = providerSetting.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                )
            }

            items(providerSetting.models) { model ->
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = model.displayName, style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    model.modelId,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.extendColors.gray4
                )
            }

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
