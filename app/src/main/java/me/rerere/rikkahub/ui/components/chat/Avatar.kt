package me.rerere.rikkahub.ui.components.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.ui.components.ui.EmojiPicker
import me.rerere.rikkahub.utils.createChatFilesByContents

@Composable
fun Avatar(
  name: String,
  value: Avatar,
  modifier: Modifier = Modifier,
  onUpdate: ((Avatar) -> Unit)? = null,
) {
  val context = LocalContext.current
  var showPickOption by remember { mutableStateOf(false) }
  var showEmojiPicker by remember { mutableStateOf(false) }

  // 图片选择launcher
  val imagePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let {
      val localUri = context.createChatFilesByContents(listOf(it))[0]
      onUpdate?.invoke(Avatar.Image(localUri.toString()))
    }
  }

  Surface(
    shape = CircleShape,
    modifier = modifier.size(32.dp),
    onClick = {
      if (onUpdate != null) showPickOption = true
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

        is Avatar.Dummy -> {
          Text(
            text = name.takeIf { it.isNotEmpty() }?.firstOrNull()?.toString()?.uppercase() ?: "A",
            fontSize = 20.sp,
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
        Text(text = stringResource(id = R.string.avatar_change_avatar))
      },
      text = {
        Column(
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {
              showPickOption = false
              imagePickerLauncher.launch("image/*")
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(text = stringResource(id = R.string.avatar_pick_image))
          }
          Button(
            onClick = {
              showPickOption = false
              showEmojiPicker = true
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(text = stringResource(id = R.string.avatar_pick_emoji))
          }
          Button(
            onClick = {
              showPickOption = false
              onUpdate?.invoke(Avatar.Dummy)
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(text = stringResource(id = R.string.avatar_reset))
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showPickOption = false
          }
        ) {
          Text(stringResource(id = R.string.avatar_cancel))
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
          onUpdate?.invoke(Avatar.Emoji(content = emoji.emoji))
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