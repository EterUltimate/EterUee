package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.ui.components.ui.EmojiPicker

@Composable
fun Avatar(
  value: Avatar,
  modifier: Modifier = Modifier,
  onUpdate: (Avatar) -> Unit = {},
) {
  var showPickOption by remember { mutableStateOf(false) }
  var showEmojiPicker by remember { mutableStateOf(false) }
  Surface(
    shape = CircleShape,
    modifier = modifier.size(32.dp),
    onClick = {
      showPickOption = true
    },
    tonalElevation = 2.dp,
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.fillMaxSize()
    ) {
      when (value) {
        is Avatar.Image -> {
          AsyncImage(
            model = value.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
          )
        }

        is Avatar.Emoji -> {
          Text(
            text = value.content,
            autoSize = TextAutoSize.StepBased(
              minFontSize = 15.sp,
              maxFontSize = 30.sp,
            ),
            modifier = Modifier.padding(4.dp)
          )
        }
      }
    }
  }

  if (showPickOption) {
    AlertDialog(
      onDismissRequest = {
        showPickOption = false
      },
      title = {
        Text(text = "Change Avatar")
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              showPickOption = false
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(text = "Pick Image")
          }
          Button(
            onClick = {
              showPickOption = false
              showEmojiPicker = true
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(text = "Pick Emoji")
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showPickOption = false
          }
        ) {
          Text("完成")
        }
      }
    )
  }

  if (showEmojiPicker) {
    ModalBottomSheet(
      onDismissRequest = {
        showEmojiPicker = false
      },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
      EmojiPicker(
        onEmojiSelected = { emoji ->
          onUpdate(Avatar.Emoji(content = emoji.emoji))
          showEmojiPicker = false
        },
        modifier = Modifier
          .fillMaxWidth()
          .wrapContentHeight()
          .padding(16.dp)
      )
    }
  }
}