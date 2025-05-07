package me.rerere.rikkahub.ui.pages.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import me.rerere.ai.provider.CustomBody
import me.rerere.ai.provider.CustomHeader
import me.rerere.rikkahub.data.model.Assistant

private val jsonLenient  = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
}

@Composable
fun AssistantCustomHeaders(assistant: Assistant, onUpdate: (Assistant) -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text("自定义 Headers")
        Spacer(Modifier.height(8.dp))

        assistant.customHeaders.forEachIndexed { index, header ->
            var headerName by remember(header.name) { mutableStateOf(header.name) }
            var headerValue by remember(header.value) { mutableStateOf(header.value) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = headerName,
                        onValueChange = {
                            headerName = it
                            val updatedHeaders = assistant.customHeaders.toMutableList()
                            updatedHeaders[index] = updatedHeaders[index].copy(name = it.trim())
                            onUpdate(assistant.copy(customHeaders = updatedHeaders))
                        },
                        label = { Text("Header 名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = headerValue,
                        onValueChange = {
                            headerValue = it
                            val updatedHeaders = assistant.customHeaders.toMutableList()
                            updatedHeaders[index] = updatedHeaders[index].copy(value = it.trim())
                            onUpdate(assistant.copy(customHeaders = updatedHeaders))
                        },
                        label = { Text("Header 值") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                IconButton(onClick = {
                    val updatedHeaders = assistant.customHeaders.toMutableList()
                    updatedHeaders.removeAt(index)
                    onUpdate(assistant.copy(customHeaders = updatedHeaders))
                }) {
                    Icon(Lucide.Trash, contentDescription = "删除 Header")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val updatedHeaders = assistant.customHeaders.toMutableList()
                updatedHeaders.add(CustomHeader("", ""))
                onUpdate(assistant.copy(customHeaders = updatedHeaders))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Lucide.Plus, contentDescription = "添加 Header")
            Spacer(Modifier.width(4.dp))
            Text("添加 Header")
        }
    }
}

@Composable
fun AssistantCustomBodies(assistant: Assistant, onUpdate: (Assistant) -> Unit) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text("自定义 Body")
        Spacer(Modifier.height(8.dp))

        assistant.customBodies.forEachIndexed { index, body ->
            var bodyKey by remember(body.key) { mutableStateOf(body.key) }
            var bodyValueString by remember(body.value) {
                mutableStateOf(jsonLenient.encodeToString(JsonElement.serializer(), body.value))
            }
            var jsonParseError by remember { mutableStateOf<String?>(null) }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = bodyKey,
                        onValueChange = {
                            bodyKey = it
                            val updatedBodies = assistant.customBodies.toMutableList()
                            updatedBodies[index] = updatedBodies[index].copy(key = it.trim())
                            onUpdate(assistant.copy(customBodies = updatedBodies))
                        },
                        label = { Text("Body Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bodyValueString,
                        onValueChange = { newString ->
                            bodyValueString = newString
                            try {
                                val newJsonValue = jsonLenient.parseToJsonElement(newString)
                                val updatedBodies = assistant.customBodies.toMutableList()
                                updatedBodies[index] =
                                    updatedBodies[index].copy(value = newJsonValue)
                                onUpdate(assistant.copy(customBodies = updatedBodies))
                                jsonParseError = null // Clear error on successful parse
                            } catch (e: Exception) { // Catching general Exception, JsonException is common here
                                jsonParseError =
                                    "无效的 JSON: ${e.message?.take(100)}" // Truncate for very long messages
                            }
                        },
                        label = { Text("Body Value (JSON)") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = jsonParseError != null,
                        supportingText = {
                            if (jsonParseError != null) {
                                Text(jsonParseError!!)
                            }
                        },
                        minLines = 3,
                        maxLines = 5
                    )
                }
                IconButton(onClick = {
                    val updatedBodies = assistant.customBodies.toMutableList()
                    updatedBodies.removeAt(index)
                    onUpdate(assistant.copy(customBodies = updatedBodies))
                }) {
                    Icon(Lucide.Trash, contentDescription = "删除 Body")
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                val updatedBodies = assistant.customBodies.toMutableList()
                updatedBodies.add(CustomBody("", JsonPrimitive("")))
                onUpdate(assistant.copy(customBodies = updatedBodies))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Lucide.Plus, contentDescription = "添加 Body")
            Spacer(Modifier.width(4.dp))
            Text("添加 Body")
        }
    }
}