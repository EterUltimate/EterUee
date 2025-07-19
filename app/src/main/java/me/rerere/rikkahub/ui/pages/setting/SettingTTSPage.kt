package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.GripHorizontal
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.Trash2
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.ui.Tag
import me.rerere.rikkahub.ui.components.ui.TagType
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.pages.setting.components.TTSProviderConfigure
import me.rerere.rikkahub.utils.plus
import me.rerere.tts.provider.TTSProviderSetting
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SettingTTSPage(vm: SettingVM = koinViewModel()) {
  val settings by vm.settings.collectAsStateWithLifecycle()
  val navController = LocalNavController.current
  var editingProvider by remember { mutableStateOf<TTSProviderSetting?>(null) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(text = stringResource(R.string.setting_tts_page_title))
        },
        navigationIcon = {
          BackButton()
        },
        actions = {
          AddTTSProviderButton {
            vm.updateSettings(
              settings.copy(
                ttsProviders = listOf(it) + settings.ttsProviders
              )
            )
          }
        }
      )
    },
  ) { innerPadding ->
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
      val newProviders = settings.ttsProviders.toMutableList().apply {
        add(to.index, removeAt(from.index))
      }
      vm.updateSettings(settings.copy(ttsProviders = newProviders))
    }

    LazyColumn(
      modifier = Modifier
          .fillMaxSize()
          .imePadding(),
      contentPadding = innerPadding + PaddingValues(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      state = lazyListState
    ) {
      items(settings.ttsProviders, key = { it.id }) { provider ->
        ReorderableItem(
          state = reorderableState,
          key = provider.id
        ) { isDragging ->
          TTSProviderItem(
            modifier = Modifier
                .scale(if (isDragging) 0.95f else 1f)
                .fillMaxWidth(),
            provider = provider,
            dragHandle = {
              val haptic = LocalHapticFeedback.current
              IconButton(
                onClick = {},
                modifier = Modifier
                  .longPressDraggableHandle(
                    onDragStarted = {
                      haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    },
                    onDragStopped = {
                      haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    }
                  )
              ) {
                Icon(
                  imageVector = Lucide.GripHorizontal,
                  contentDescription = null
                )
              }
            },
            isSelected = settings.selectedTTSProviderId == provider.id,
            onSelect = {
              vm.updateSettings(settings.copy(selectedTTSProviderId = provider.id))
            },
            onEdit = {
              editingProvider = provider
            },
            onDelete = {
              val newProviders = settings.ttsProviders - provider
              val newSelectedId =
                if (settings.selectedTTSProviderId == provider.id) null else settings.selectedTTSProviderId
              vm.updateSettings(
                settings.copy(
                  ttsProviders = newProviders,
                  selectedTTSProviderId = newSelectedId
                )
              )
            }
          )
        }
      }
    }
  }

  // Edit TTS Provider Bottom Sheet
  editingProvider?.let { provider ->
    val bottomSheetState = rememberModalBottomSheetState()
    var currentProvider by remember(provider) { mutableStateOf(provider) }

    ModalBottomSheet(
      onDismissRequest = {
        editingProvider = null
      },
      sheetState = bottomSheetState,
      dragHandle = {
        BottomSheetDefaults.DragHandle()
      }
    ) {
      Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .fillMaxHeight(0.8f),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = stringResource(R.string.setting_tts_page_edit_provider),
          style = MaterialTheme.typography.headlineSmall
        )

        TTSProviderConfigure(
          setting = currentProvider,
          onValueChange = { newState ->
            currentProvider = newState
          },
          modifier = Modifier.weight(1f)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TextButton(
            onClick = {
              editingProvider = null
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(stringResource(R.string.cancel))
          }

          TextButton(
            onClick = {
              val newProviders = settings.ttsProviders.map {
                if (it.id == provider.id) currentProvider else it
              }
              vm.updateSettings(settings.copy(ttsProviders = newProviders))
              editingProvider = null
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(stringResource(R.string.chat_page_save))
          }
        }
      }
    }
  }
}

@Composable
private fun AddTTSProviderButton(onAdd: (TTSProviderSetting) -> Unit) {
  var showBottomSheet by remember { mutableStateOf(false) }
  var currentProvider: TTSProviderSetting by remember { mutableStateOf(TTSProviderSetting.SystemTTS()) }

  IconButton(
    onClick = {
      currentProvider = TTSProviderSetting.SystemTTS()
      showBottomSheet = true
    }
  ) {
    Icon(Lucide.Plus, "Add TTS Provider")
  }

  if (showBottomSheet) {
    val bottomSheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
      onDismissRequest = {
        showBottomSheet = false
      },
      sheetState = bottomSheetState,
      dragHandle = {
        BottomSheetDefaults.DragHandle()
      }
    ) {
      Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .fillMaxHeight(0.8f),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = stringResource(R.string.setting_tts_page_add_provider),
          style = MaterialTheme.typography.headlineSmall
        )

        TTSProviderConfigure(
          setting = currentProvider,
          onValueChange = { newState ->
            currentProvider = newState
          },
          modifier = Modifier.weight(1f)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          TextButton(
            onClick = {
              showBottomSheet = false
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(stringResource(R.string.cancel))
          }

          TextButton(
            onClick = {
              onAdd(currentProvider)
              showBottomSheet = false
            },
            modifier = Modifier.weight(1f)
          ) {
            Text(stringResource(R.string.setting_tts_page_add))
          }
        }
      }
    }
  }
}

@Composable
private fun TTSProviderItem(
  provider: TTSProviderSetting,
  modifier: Modifier = Modifier,
  isSelected: Boolean = false,
  dragHandle: @Composable () -> Unit,
  onSelect: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  var showDropdownMenu by remember { mutableStateOf(false) }
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(
      containerColor = if (provider.enabled) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
      } else MaterialTheme.colorScheme.errorContainer,
    )
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AutoAIIcon(
          name = provider.name,
          modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        dragHandle()
      }
      Row {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Text(
            text = provider.name,
            style = MaterialTheme.typography.titleLarge
          )
          Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            if (isSelected) {
              Tag(type = TagType.SUCCESS) {
                Text(stringResource(R.string.setting_tts_page_selected))
              }
            }
            Tag(type = if (provider.enabled) TagType.SUCCESS else TagType.WARNING) {
              Text(stringResource(if (provider.enabled) R.string.setting_tts_page_enabled else R.string.setting_tts_page_disabled))
            }
            // Display provider type
            Tag(type = TagType.INFO) {
              Text(
                when (provider) {
                  is TTSProviderSetting.OpenAI -> "OpenAI"
                  is TTSProviderSetting.Gemini -> "Gemini"
                  is TTSProviderSetting.SystemTTS -> "System TTS"
                }
              )
            }
          }
        }
        IconButton(
          onClick = { showDropdownMenu = true }
        ) {
          Icon(
            imageVector = Lucide.Settings2,
            contentDescription = "More options"
          )
          DropdownMenu(
            expanded = showDropdownMenu,
            onDismissRequest = { showDropdownMenu = false }
          ) {
            if (!isSelected && provider.enabled) {
              DropdownMenuItem(
                text = { Text(stringResource(R.string.setting_tts_page_select)) },
                onClick = {
                  showDropdownMenu = false
                  onSelect()
                },
                leadingIcon = {
                  Icon(Lucide.Check, contentDescription = null)
                }
              )
            }
            DropdownMenuItem(
              text = { Text(stringResource(R.string.edit)) },
              onClick = {
                showDropdownMenu = false
                onEdit()
              },
              leadingIcon = {
                Icon(Lucide.Pencil, contentDescription = null)
              }
            )
            DropdownMenuItem(
              text = { Text(stringResource(R.string.delete)) },
              onClick = {
                showDropdownMenu = false
                onDelete()
              },
              leadingIcon = {
                Icon(Lucide.Trash2, contentDescription = null)
              }
            )
          }
        }
      }
    }
  }
}