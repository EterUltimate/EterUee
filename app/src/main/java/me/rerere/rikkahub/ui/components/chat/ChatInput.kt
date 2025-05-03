package me.rerere.rikkahub.ui.components.chat

import android.Manifest
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Earth
import com.composables.icons.lucide.Fullscreen
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import com.meticha.permissions_compose.AppPermission
import com.meticha.permissions_compose.rememberAppPermissionState
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.isEmptyInputMessage
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.ui.IconTextButton
import me.rerere.rikkahub.ui.theme.extendColors
import me.rerere.rikkahub.utils.createChatFiles
import me.rerere.rikkahub.utils.deleteChatFiles
import java.io.File
import kotlin.uuid.Uuid

class ChatInputState {
    var messageContent by mutableStateOf(listOf<UIMessagePart>())
    var editingMessage by mutableStateOf<Uuid?>(null)
    var loading by mutableStateOf(false)

    fun clearInput() {
        messageContent = emptyList()
        editingMessage = null
    }

    fun isEditing() = editingMessage != null

    fun setMessageText(text: String) {
        val newMessage = messageContent.toMutableList()
        if (newMessage.isEmpty()) {
            newMessage.add(UIMessagePart.Text(text))
            messageContent = newMessage
        } else {
            if (messageContent.filterIsInstance<UIMessagePart.Text>().isEmpty()) {
                newMessage.add(UIMessagePart.Text(text))
            }
            messageContent = newMessage.map {
                if (it is UIMessagePart.Text) {
                    it.copy(text)
                } else {
                    it
                }
            }
        }
    }

    fun addImages(uris: List<Uri>) {
        val newMessage = messageContent.toMutableList()
        uris.forEach { uri ->
            newMessage.add(UIMessagePart.Image(uri.toString()))
        }
        messageContent = newMessage
    }
}

@Composable
fun rememberChatInputState(
    message: List<UIMessagePart> = emptyList(),
    loading: Boolean = false,
): ChatInputState {
    return remember(message, loading) {
        ChatInputState().apply {
            this.messageContent = message
            this.loading = loading
        }
    }
}

@Composable
fun ChatInput(
    state: ChatInputState,
    enableSearch: Boolean,
    onToggleSearch: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onCancelClick: () -> Unit,
    onSendClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val text =
        state.messageContent.filterIsInstance<UIMessagePart.Text>().firstOrNull()
            ?: UIMessagePart.Text("")

    val context = LocalContext.current
    var expand by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun sendMessage() {
        keyboardController?.hide()
        if (state.loading) onCancelClick() else onSendClick()
    }

    Surface {
        Column(
            modifier = modifier
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Medias
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                state.messageContent.filterIsInstance<UIMessagePart.Image>().forEach { image ->
                    Box {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(8.dp),
                            tonalElevation = 4.dp
                        ) {
                            AsyncImage(
                                model = image.url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                        Icon(
                            imageVector = Lucide.X,
                            contentDescription = null,
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(20.dp)
                                .clickable {
                                    // Remove image
                                    state.messageContent =
                                        state.messageContent.filterNot { it == image }
                                    // Delete image
                                    context.deleteChatFiles(listOf(image.url.toUri()))
                                }
                                .align(Alignment.TopEnd)
                                .background(MaterialTheme.colorScheme.secondary),
                            tint = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }

            // TextField
            Surface(
                shape = RoundedCornerShape(32.dp),
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column {
                    if (state.isEditing()) {
                        Surface(
                            tonalElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.editing),
                                )
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    Lucide.X, stringResource(R.string.cancel_edit),
                                    modifier = Modifier
                                        .clickable {
                                            state.clearInput()
                                        }
                                )
                            }
                        }
                    }
                    var isFocused by remember { mutableStateOf(false) }
                    var isFullScreen by remember { mutableStateOf(false) }
                    TextField(
                        value = text.text,
                        onValueChange = { state.setMessageText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged {
                                isFocused = it.isFocused
                            },
                        shape = RoundedCornerShape(32.dp),
                        placeholder = {
                            Text(stringResource(R.string.chat_input_placeholder))
                        },
                        maxLines = 5,
                        colors = TextFieldDefaults.colors().copy(
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                        trailingIcon = {
                            if (isFocused) {
                                IconButton(
                                    onClick = {
                                        isFullScreen = !isFullScreen
                                    }
                                ) {
                                    Icon(Lucide.Fullscreen, null)
                                }
                            }
                        }
                    )
                    if (isFullScreen) {
                        FullScreenEditor(text, state) {
                            isFullScreen = false
                        }
                    }
                }
            }

            // Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                actions()

                IconButton(
                    onClick = {
                        expand = !expand
                    }
                ) {
                    Icon(
                        if (expand) Lucide.X else Lucide.Plus,
                        stringResource(R.string.more_options)
                    )
                }

                val badgeColor = MaterialTheme.extendColors.green6
                IconToggleButton(
                    checked = enableSearch,
                    onCheckedChange = {
                        onToggleSearch(it)
                    },
                    modifier = Modifier.drawWithContent {
                        drawContent()
                        if (enableSearch) {
                            drawCircle(
                                color = badgeColor,
                                radius = 4.dp.toPx(),
                                center = center.copy(
                                    x = size.width - 8.dp.toPx(),
                                    y = 8.dp.toPx()
                                )
                            )
                        }
                    }
                ) {
                    Icon(Lucide.Earth, stringResource(R.string.use_web_search))
                }

                Spacer(Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        sendMessage()
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (state.loading) MaterialTheme.colorScheme.errorContainer else Color.Unspecified,
                        contentColor = if (state.loading) MaterialTheme.colorScheme.onErrorContainer else Color.Unspecified,
                    ),
                    enabled = state.loading || !state.messageContent.isEmptyInputMessage()
                ) {
                    if (state.loading) {
                        Icon(Lucide.X, stringResource(R.string.stop))
                    } else {
                        Icon(Lucide.ArrowUp, stringResource(R.string.send))
                    }
                }
            }

            // Files
            AnimatedVisibility(expand) {
                Surface(
                    tonalElevation = 4.dp
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        TakePicButton {
                            state.addImages(it)
                            expand = false
                        }

                        ImagePickButton {
                            state.addImages(it)
                            expand = false
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FullScreenEditor(
    text: UIMessagePart.Text,
    state: ChatInputState,
    onDone: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = {
            onDone()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxHeight(0.9f),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row {
                        TextButton(
                            onClick = {
                                onDone()
                            }
                        ) {
                            Text(stringResource(R.string.chat_page_save))
                        }
                    }
                    TextField(
                        value = text.text,
                        onValueChange = { state.setMessageText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(32.dp),
                        placeholder = {
                            Text(stringResource(R.string.chat_input_placeholder))
                        },
                        colors = TextFieldDefaults.colors().copy(
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ImagePickButton(onAddImages: (List<Uri>) -> Unit = {}) {
    val context = LocalContext.current
    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
            if (uris.isNotEmpty()) {
                Log.d("PhotoPicker", "Selected URI: $uris")
                onAddImages(context.createChatFiles(uris))
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        }
    IconTextButton(
        icon = {
            Icon(Lucide.Image, null)
        },
        text = {
            Text(stringResource(R.string.photo))
        }
    ) {
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}

@Composable
fun TakePicButton(onAddImages: (List<Uri>) -> Unit = {}) {
    val permissionState = rememberAppPermissionState(
        permissions = listOf(
            AppPermission(
                permission = Manifest.permission.CAMERA,
                description = "需要权限才能使用相机功能",
                isRequired = true
            )
        )
    )
    val context = LocalContext.current
    var providerUri by remember { mutableStateOf<Uri?>(null) }
    var file by remember { mutableStateOf<File?>(null) }
    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                onAddImages(context.createChatFiles(listOf(providerUri!!)))
            }
            // delete the temp file
            file?.delete()
        }

    IconTextButton(
        icon = {
            Icon(Lucide.Camera, null)
        },
        text = {
            Text(stringResource(R.string.take_picture))
        }
    ) {
        permissionState.requestPermission()
        if (permissionState.allRequiredGranted()) {
            file = context.cacheDir.resolve(Uuid.random().toString())
            providerUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file!!
            )
            pickMedia.launch(providerUri!!)
        }
    }
}