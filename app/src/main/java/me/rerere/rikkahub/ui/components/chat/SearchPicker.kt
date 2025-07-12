package me.rerere.rikkahub.ui.components.chat

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import com.composables.icons.lucide.Earth
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Settings2
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.ui.ToggleSurface
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.push
import me.rerere.search.SearchService
import me.rerere.search.SearchServiceOptions

@Composable
fun SearchPickerButton(
  enableSearch: Boolean,
  settings: Settings,
  modifier: Modifier = Modifier,
  onToggleSearch: (Boolean) -> Unit,
  onUpdateSearchService: (Int) -> Unit,
) {
  var showSearchPicker by remember { mutableStateOf(false) }
  val currentService = settings.searchServices.getOrNull(settings.searchServiceSelected)

  ToggleSurface(
    modifier = modifier,
    checked = enableSearch,
    onClick = {
      showSearchPicker = true
    }
  ) {
    Row(
      modifier = Modifier
        .padding(vertical = 8.dp, horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
      ) {
        if (enableSearch && currentService != null) {
          Icon(
            imageVector = Lucide.Earth,
            contentDescription = stringResource(R.string.use_web_search),
          )
        } else {
          Icon(
            imageVector = Lucide.Earth,
            contentDescription = stringResource(R.string.use_web_search),
          )
        }
      }
    }
  }

  if (showSearchPicker) {
    ModalBottomSheet(
      onDismissRequest = { showSearchPicker = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
      Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.7f)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        Text(
          text = stringResource(R.string.search_picker_title),
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold
          )
        )

        SearchPicker(
          enableSearch = enableSearch,
          settings = settings,
          onToggleSearch = onToggleSearch,
          onUpdateSearchService = { index ->
            onUpdateSearchService(index)
          },
          modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
          onDismiss = {
            showSearchPicker = false
          }
        )
      }
    }
  }
}

@Composable
fun SearchPicker(
  enableSearch: Boolean,
  settings: Settings,
  modifier: Modifier = Modifier,
  onToggleSearch: (Boolean) -> Unit,
  onUpdateSearchService: (Int) -> Unit,
  onDismiss: () -> Unit
) {
  val navBackStack = LocalNavController.current

  Card {
    Row(
      modifier = Modifier
          .padding(horizontal = 16.dp, vertical = 12.dp)
          .fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(Lucide.Earth, null)
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = stringResource(R.string.use_web_search),
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          text = if (enableSearch) {
            stringResource(R.string.web_search_enabled)
          } else {
            stringResource(R.string.web_search_disabled)
          },
          style = MaterialTheme.typography.bodySmall,
          color = LocalContentColor.current.copy(alpha = 0.8f)
        )
      }
      IconButton(
        onClick = {
          onDismiss()
          navBackStack.push(Screen.SettingSearch)
        }
      ) {
        Icon(Lucide.Settings2, null)
      }
      Switch(
        checked = enableSearch,
        onCheckedChange = onToggleSearch
      )
    }
  }

  LazyVerticalGrid(
    modifier = modifier.fillMaxSize(),
    columns = GridCells.Adaptive(150.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    itemsIndexed(settings.searchServices) { index, service ->
      val containerColor = animateColorAsState(
        if (settings.searchServiceSelected == index) {
          MaterialTheme.colorScheme.primary
        } else {
          MaterialTheme.colorScheme.primaryContainer
        }
      )
      val textColor = animateColorAsState(
        if (settings.searchServiceSelected == index) {
          MaterialTheme.colorScheme.onPrimary
        } else {
          MaterialTheme.colorScheme.onPrimaryContainer
        }
      )
      Card(
        colors = CardDefaults.cardColors(
          containerColor = containerColor.value,
          contentColor = textColor.value,
        ),
        onClick = {
          onUpdateSearchService(index)
        },
        shape = RoundedCornerShape(50),
      ) {
        Row(
          modifier = Modifier
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AutoAIIcon(
            name = SearchServiceOptions.TYPES[service::class] ?: "Search",
            modifier = Modifier.size(32.dp)
          )
          Column(
            modifier = Modifier.weight(1f),
          ) {
            Text(
              text = SearchServiceOptions.TYPES[service::class] ?: "Unknown",
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text = SearchService.getService(service).name,
              style = MaterialTheme.typography.bodySmall,
              color = LocalContentColor.current.copy(alpha = 0.8f)
            )
          }
        }
      }
    }
  }
} 