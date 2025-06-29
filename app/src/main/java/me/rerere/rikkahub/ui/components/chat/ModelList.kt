package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFilter
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.launch
import com.composables.icons.lucide.Boxes
import com.composables.icons.lucide.Hammer
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.HeartOff
import com.composables.icons.lucide.Lightbulb
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.X
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelAbility
import me.rerere.ai.provider.ModelType
import me.rerere.ai.provider.ProviderSetting
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.components.ui.icons.HeartIcon
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.theme.extendColors
import kotlin.uuid.Uuid

@Composable
fun ModelSelector(
  modelId: Uuid?,
  providers: List<ProviderSetting>,
  type: ModelType,
  modifier: Modifier = Modifier,
  onlyIcon: Boolean = false,
  allowClear: Boolean = false,
  onUpdate: ((List<ProviderSetting>) -> Unit)? = null,
  onSelect: (Model) -> Unit
) {
  var popup by remember {
    mutableStateOf(false)
  }
  val model = providers.findModelById(modelId ?: Uuid.random())

  if (!onlyIcon) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      TextButton(
        onClick = {
          popup = true
        },
        modifier = modifier
      ) {
        model?.modelId?.let {
          AutoAIIcon(
            it, Modifier
                  .padding(end = 4.dp)
                  .size(24.dp)
          )
        }
        Text(
          text = model?.displayName ?: stringResource(R.string.model_list_select_model),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MaterialTheme.typography.bodySmall
        )
      }
      if (allowClear && model != null) {
        IconButton(
          onClick = {
            onSelect(Model())
          }
        ) {
          Icon(
            Lucide.X,
            contentDescription = "Clear"
          )
        }
      }
    }
  } else {
    FilledTonalButton(
      onClick = {
        popup = true
      }
    ) {
      if (model != null) {
        AutoAIIcon(
          modifier = Modifier.size(20.dp),
          name = model.modelId
        )
      } else {
        Icon(
          Lucide.Boxes,
          contentDescription = stringResource(R.string.setting_model_page_chat_model),
          modifier = Modifier.size(20.dp)
        )
      }
    }
  }

  if (popup) {
    ModalBottomSheet(
      onDismissRequest = {
        popup = false
      },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
      Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxHeight(0.8f)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        val filteredProviderSettings = providers.fastFilter {
          it.enabled && it.models.fastAny { model -> model.type == type }
        }
        ModelList(
          currentModel = modelId,
          providers = filteredProviderSettings,
          modelType = type,
          onSelect = {
            popup = false
            onSelect(it)
          },
          onUpdate = { newModel ->
            onUpdate?.invoke(providers.map { provider ->
              provider.copyProvider(
                models = provider.models.map {
                  if (it.id == newModel.id) {
                    newModel
                  } else {
                    it
                  }
                }
              )
            })
          },
          allowFavorite = onUpdate != null,
          onDismiss = {
            popup = false
          }
        )
      }
    }
  }
}

@OptIn(FlowPreview::class)
@Composable
private fun ColumnScope.ModelList(
  currentModel: Uuid? = null,
  providers: List<ProviderSetting>,
  modelType: ModelType,
  allowFavorite: Boolean = true,
  onUpdate: (Model) -> Unit,
  onSelect: (Model) -> Unit,
  onDismiss: () -> Unit
) {
  val favoriteModels = providers
    .flatMap { provider ->
      provider.models.map { model -> model to provider }
    }
    .fastFilter { (model, _) ->
      model.favorite && model.type == modelType
    }
  var searchKeywords by remember { mutableStateOf("") }
  val lazyListState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val providerPositions = remember(providers, favoriteModels, allowFavorite) {
    var currentIndex = 0
    if (providers.isEmpty()) {
      currentIndex = 1 // no providers item
    }
    if (favoriteModels.isNotEmpty() && allowFavorite) {
      currentIndex += 1 // favorite header
      currentIndex += favoriteModels.size // favorite models
    }

    providers.mapIndexed { index, provider ->
      val position = currentIndex
      currentIndex += 1 // provider header
      currentIndex += provider.models.fastFilter {
        it.type == modelType && it.displayName.contains(searchKeywords, true)
      }.size
      provider.id to position
    }.toMap()
  }

  LazyColumn(
    state = lazyListState,
    verticalArrangement = Arrangement.spacedBy(4.dp),
    contentPadding = PaddingValues(8.dp),
    modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (providers.isEmpty()) {
      item {
        Text(
          text = stringResource(R.string.model_list_no_providers),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.extendColors.gray6,
          modifier = Modifier.padding(8.dp)
        )
      }
    }

    if (favoriteModels.isNotEmpty() && allowFavorite) {
      stickyHeader {
        Text(
          text = stringResource(R.string.model_list_favorite),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
              .padding(bottom = 4.dp, top = 8.dp)
              .fillMaxWidth(),
          textAlign = TextAlign.Center
        )
      }

      items(
        items = favoriteModels,
        key = { "favorite:" + it.first.id.toString() }
      ) { (model, provider) ->
        ModelItem(
          model = model,
          onSelect = onSelect,
          modifier = Modifier.animateItem(),
          providerSetting = provider,
          select = model.id == currentModel,
          onDismiss = {
            onDismiss()
          },
        ) {
          IconButton(
            onClick = {
              onUpdate(
                model.copy(
                  favorite = !model.favorite
                )
              )
            }
          ) {
            if (model.favorite) {
              Icon(
                HeartIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.extendColors.red6
              )
            } else {
              Icon(
                Lucide.HeartOff,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }

    providers.fastForEach { providerSetting ->
      stickyHeader {
        Text(
          text = providerSetting.name,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
              .padding(bottom = 4.dp, top = 8.dp)
              .fillMaxWidth(),
          textAlign = TextAlign.Center
        )
      }

      items(
        items = providerSetting.models.fastFilter {
          it.type == modelType && it.displayName.contains(
            searchKeywords,
            true
          )
        },
        key = { it.id }
      ) { model ->
        ModelItem(
          model = model,
          onSelect = onSelect,
          modifier = Modifier.animateItem(),
          providerSetting = providerSetting,
          select = currentModel == model.id,
          onDismiss = {
            onDismiss()
          },
          tail = {
            if (allowFavorite) {
              IconButton(
                onClick = {
                  onUpdate(
                    model.copy(
                      favorite = !model.favorite
                    )
                  )
                }
              ) {
                if (model.favorite) {
                  Icon(
                    HeartIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.extendColors.red6
                  )
                } else {
                  Icon(
                    Lucide.Heart,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        )
      }
    }
  }

  // 供应商Badge行
  val providerBadgeListState = rememberLazyListState()
  LaunchedEffect(lazyListState) {
    // 当LazyColumn滚动时，LazyRow也跟随滚动
    snapshotFlow { lazyListState.firstVisibleItemIndex }
      .distinctUntilChanged()
      .debounce(100) // 防抖处理
      .collect { index ->
        if (index > 0) {
          val currentProvider = providerPositions.entries.findLast {
            index > it.value
          }
          val index = providers.indexOfFirst { it.id == currentProvider?.key }
          if (index >= 0) {
            providerBadgeListState.animateScrollToItem(index)
          } else {
            providerBadgeListState.requestScrollToItem(0)
          }
        } else {
          providerBadgeListState.requestScrollToItem(0)
        }
      }
  }
  if (providers.isNotEmpty()) {
    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 8.dp),
      state = providerBadgeListState
    ) {
      items(providers) { provider ->
        AssistChip(
          onClick = {
            val position = providerPositions[provider.id] ?: 0
            coroutineScope.launch {
              lazyListState.animateScrollToItem(position)
            }
          },
          label = {
            Text(provider.name)
          },
          leadingIcon = {
            AutoAIIcon(name = provider.name, modifier = Modifier.size(16.dp))
          },
        )
      }
    }
  }

  OutlinedTextField(
    value = searchKeywords,
    onValueChange = { searchKeywords = it },
    modifier = Modifier.fillMaxWidth(),
    placeholder = {
      Text(
        text = stringResource(R.string.model_list_search_placeholder),
      )
    },
    shape = RoundedCornerShape(50)
  )
}

@Composable
private fun ModelItem(
  model: Model,
  providerSetting: ProviderSetting,
  select: Boolean,
  onSelect: (Model) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
  tail: @Composable RowScope.() -> Unit = {}
) {
  val navController = LocalNavController.current
  val interactionSource = remember { MutableInteractionSource() }
  Card(
    modifier = modifier.combinedClickable(
      enabled = true,
      onLongClick = {
        onDismiss()
        navController.navigate(
          "setting/provider/${providerSetting.id}"
        )
      },
      onClick = { onSelect(model) },
      interactionSource = interactionSource,
      indication = LocalIndication.current
    ),
    colors = CardDefaults.cardColors(
      containerColor = if (select) {
        MaterialTheme.colorScheme.tertiaryContainer
      } else {
        MaterialTheme.colorScheme.primaryContainer
      }
    ),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
      AutoAIIcon(
        name = model.modelId,
        modifier = Modifier.size(32.dp)
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = providerSetting.name,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.extendColors.gray4
        )

        Text(
          text = model.displayName,
          style = MaterialTheme.typography.labelMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Row(
          modifier = Modifier
              .fillMaxWidth()
              .height(IntrinsicSize.Min),
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Tag(type = TagType.INFO) {
            Text(
              when (model.type) {
                ModelType.CHAT -> stringResource(R.string.model_list_chat)
                ModelType.EMBEDDING -> stringResource(R.string.model_list_embedding)
              }
            )
          }

          Tag(type = TagType.SUCCESS) {
            Text(
              text = buildString {
                append(model.inputModalities.joinToString(",") { it.name.lowercase() })
                append("->")
                append(model.outputModalities.joinToString(",") { it.name.lowercase() })
              },
              maxLines = 1,
            )
          }

          val iconHeight = with(LocalDensity.current) {
            LocalTextStyle.current.fontSize.toDp() * 0.9f
          }
          model.abilities.fastForEach { ability ->
            when (ability) {
              ModelAbility.TOOL -> {
                Tag(
                  type = TagType.WARNING,
                  modifier = Modifier.fillMaxHeight()
                ) {
                  Icon(
                    imageVector = Lucide.Hammer,
                    contentDescription = null,
                    modifier = Modifier
                        .height(iconHeight)
                        .aspectRatio(1f)
                  )
                }
              }

              ModelAbility.REASONING -> {
                Tag(
                  type = TagType.INFO,
                  modifier = Modifier.fillMaxHeight()
                ) {
                  Icon(
                    imageVector = Lucide.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier
                        .height(iconHeight)
                        .aspectRatio(1f)
                  )
                }
              }
            }
          }
        }
      }
      tail()
    }
  }
}
