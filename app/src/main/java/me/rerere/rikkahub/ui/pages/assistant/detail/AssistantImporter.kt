package me.rerere.rikkahub.ui.pages.assistant.detail

import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.utils.ImageUtils
import me.rerere.rikkahub.utils.jsonPrimitiveOrNull

@Composable
fun AssistantImporter(
    assistant: Assistant,
    modifier: Modifier = Modifier,
    onUpdate: (Assistant) -> Unit,
) {
//    Row(
//        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.spacedBy(8.dp),
//        modifier = modifier,
//    ) {
//        SillyTavernImporter(assistant = assistant, onImport = onUpdate)
//    }
}

@Composable
private fun SillyTavernImporter(
    assistant: Assistant,
    onImport: (Assistant) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    var isLoading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isLoading = true
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        ImageUtils.getTavernCharacterMeta(context, uri)
                    }

                    result.mapCatching { base64Data ->
                        // Decode base64 to JSON string
                        val jsonString = String(Base64.decode(base64Data, Base64.DEFAULT))
                        val json = Json.parseToJsonElement(jsonString).jsonObject

                        // Extract character data
                        val spec = json["spec"]?.jsonPrimitive?.content

                        when (spec) {
                            "chara_card_v2" -> {
                                // Handle chara_card_v2
                                val data = json["data"]?.jsonObject ?: error("Missing data field")
                                val name =
                                    data["name"]?.jsonPrimitiveOrNull?.contentOrNull ?: error("Missing name field")
                                val firstMessagfe = data["first_mes"]?.jsonPrimitiveOrNull?.contentOrNull

                                onImport(assistant.copy(
                                    name = name,
                                    presetMessages = if (firstMessagfe != null) {
                                        listOf(UIMessage.assistant( firstMessagfe))
                                    } else {
                                        emptyList()
                                    },
                                ))
                            }
                            // "chara_card_v3" -> {}
                            else -> error("Unsupported spec: $spec")
                        }
                    }.onFailure { exception ->
                        // Handle error - you can add proper error handling here
                        exception.printStackTrace()
                        toaster.show(exception.message ?: "导入失败")
                    }
                } finally {
                    isLoading = false
                }
            }
        }
    }

    OutlinedButton(
        onClick = {
            imagePickerLauncher.launch("image/png")
        },
        enabled = !isLoading
    ) {
        Text(if (isLoading) "导入中..." else "导入酒馆角色卡")
    }
}
