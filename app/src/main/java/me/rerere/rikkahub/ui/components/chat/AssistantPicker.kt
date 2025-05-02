package me.rerere.rikkahub.ui.components.chat

import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.composables.icons.lucide.Bot
import com.composables.icons.lucide.Lucide
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.hooks.rememberAssistantState

@Composable
fun AssistantPicker(
    settings: Settings,
    onUpdateSettings: (Settings) -> Unit,
    modifier: Modifier = Modifier,
    onClickSetting: () -> Unit,
) {
    val state = rememberAssistantState(settings, onUpdateSettings)
    Select(
        options = settings.assistants,
        selectedOption = state.currentAssistant,
        onOptionSelected = {
            state.setSelectAssistant(it)
        },
        optionToString = {
            it.name.ifEmpty { "默认助手" }
        },
        modifier = modifier,
        leading = {
            FilledTonalIconButton(
                onClick = onClickSetting,
            ) {
                Icon(Lucide.Bot, null)
            }
        }
    )
}