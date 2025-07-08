package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
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
import com.composables.icons.lucide.Earth
import com.composables.icons.lucide.Lucide
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.ui.ToggleSurface
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
              .weight(1f)
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
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    // 搜索开关
    item {
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
          Switch(
            checked = enableSearch,
            onCheckedChange = onToggleSearch
          )
        }
      }
    }

    itemsIndexed(settings.searchServices) { index, service ->
      Card(
        colors = CardDefaults.cardColors(
          containerColor = if (settings.searchServiceSelected == index) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.primaryContainer
          }
        ),
        onClick = {
          onUpdateSearchService(index)
        }
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
            modifier = Modifier.size(24.dp)
          )
          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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