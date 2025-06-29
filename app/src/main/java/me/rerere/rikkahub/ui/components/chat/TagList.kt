package me.rerere.rikkahub.ui.components.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.X
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Tag
import me.rerere.rikkahub.ui.components.ui.Tag as TagComponent
import me.rerere.rikkahub.ui.components.ui.TagType
import kotlin.uuid.Uuid

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TagsPreview() {
    var tags by remember {
        mutableStateOf(
            listOf(
                Tag(id = Uuid.random(), name = "标签1"),
                Tag(id = Uuid.random(), name = "标签2"),
                Tag(id = Uuid.random(), name = "标签3")
            )
        )
    }
    var value by remember { mutableStateOf(tags.map { it.id }) }
    Box(Modifier.safeContentPadding()) {
        TagsInput(
            value = tags.map { it.id },
            tags = tags,
            onValueChange = {},
            onUpdateTags = {
                tags = it
            }
        )
    }
}

@Composable
fun TagsInput(
    value: List<Uuid>,
    tags: List<Tag>,
    modifier: Modifier = Modifier,
    onValueChange: (List<Uuid>) -> Unit,
    onUpdateTags: (List<Tag>) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }

    // 根据value获取对应的tags
    val selectedTags = tags.filter { tag -> value.contains(tag.id) }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        // 显示已选择的tags
        selectedTags.forEach { tag ->
            InputChip(
                onClick = {},
                label = {
                    Text(tag.name)
                },
                selected = false,
                trailingIcon = {
                    Icon(
                        imageVector = Lucide.X,
                        contentDescription = null,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable {
                                onValueChange(value.filter { it != tag.id })
                            },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        // 添加按钮
        Surface(
            shape = CircleShape,
            tonalElevation = 2.dp,
            modifier = Modifier
                .clip(CircleShape)
                .clickable { showAddDialog = true }
        ) {
            Icon(
                Lucide.Plus,
                contentDescription = stringResource(R.string.add),
                modifier = Modifier
                    .padding(6.dp)
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    // 添加tag对话框
    if (showAddDialog) {
        var tagName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                tagName = ""
            },
            title = {
                Text(stringResource(R.string.tag_input_dialog_title))
            },
            text = {
                OutlinedTextField(
                    value = tagName,
                    onValueChange = { tagName = it },
                    label = { Text(stringResource(R.string.tag_input_dialog_label)) },
                    placeholder = { Text(stringResource(R.string.tag_input_dialog_placeholder)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tagName.isNotBlank()) {
                            val newTag = Tag(id = Uuid.random(), name = tagName.trim())
                            // 添加新tag到tags列表
                            onUpdateTags(tags + newTag)
                            // 将新tag添加到选中列表
                            onValueChange(value + newTag.id)
                            showAddDialog = false
                            tagName = ""
                        }
                    },
                    enabled = tagName.isNotBlank()
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        tagName = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}