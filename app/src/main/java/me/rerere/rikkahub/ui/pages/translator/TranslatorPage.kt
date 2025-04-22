package me.rerere.rikkahub.ui.pages.translator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ClipboardPaste
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lucide
import me.rerere.ai.provider.ModelType
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.ui.components.chat.ModelSelector
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.ToastVariant
import me.rerere.rikkahub.ui.components.ui.WavyLinearProgressIndicator
import me.rerere.rikkahub.ui.components.ui.rememberToastState
import org.koin.androidx.compose.koinViewModel

@Composable
fun TranslatorPage(vm: TranslatorVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val inputText by vm.inputText.collectAsStateWithLifecycle()
    val translatedText by vm.translatedText.collectAsStateWithLifecycle()
    val targetLanguage by vm.targetLanguage.collectAsStateWithLifecycle()
    val translating by vm.translating.collectAsStateWithLifecycle()
    val toastState = rememberToastState()
    val clipboard = LocalClipboardManager.current

    // 处理错误
    LaunchedEffect(Unit) {
        vm.errorFlow.collect { error ->
            toastState.show(error.message ?: "错误", ToastVariant.ERROR)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.translator_page_title))
                },
                navigationIcon = {
                    BackButton()
                }
            )
        },
        bottomBar = {
            BottomBar(
                settings = settings,
                onUpdateSettings = {
                    vm.updateSettings(it)
                },
                translating = translating,
                onTranslate = {
                    vm.translate()
                },
                onCancelTranslation = {
                    vm.cancelTranslation()
                },
                onLanguageSelected = {
                    vm.updateTargetLanguage(it)
                },
                targetLanguage = targetLanguage
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 输入区域
            Column {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { vm.updateInputText(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.translator_page_input_placeholder)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    ),
                    maxLines = 10
                )

                FilledTonalButton(
                    onClick = {
                        clipboard.getText()?.text?.let {
                            vm.updateInputText(it)
                        }
                    }
                ) {
                    Icon(Lucide.ClipboardPaste, null)
                    Text("粘贴文本", modifier = Modifier.padding(start = 4.dp))
                }
            }

            // 翻译进度条
            if (translating) {
                WavyLinearProgressIndicator(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth()
                )
            }

            // 翻译结果
            SelectionContainer {
                Text(
                    text = translatedText.ifEmpty {
                        stringResource(R.string.translator_page_result_placeholder)
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguageSelector(
    targetLanguage: String,
    onLanguageSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val languages = listOf("中文", "英文", "日文", "韩文", "法文", "德文", "西班牙文", "俄文")

    Box(
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = targetLanguage,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = {
                            onLanguageSelected(language)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar(
    settings: Settings,
    onUpdateSettings: (Settings) -> Unit,
    targetLanguage: String,
    onLanguageSelected: (String) -> Unit,
    translating: Boolean,
    onTranslate: () -> Unit,
    onCancelTranslation: () -> Unit
) {
    BottomAppBar(
        actions = {
            ModelSelector(
                modelId = settings.translateModeId,
                onSelect = {
                    onUpdateSettings(settings.copy(translateModeId = it.id))
                },
                providers = settings.providers,
                type = ModelType.CHAT
            )

            // 目标语言选择
            LanguageSelector(
                targetLanguage = targetLanguage,
                onLanguageSelected = { onLanguageSelected(it) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (translating) {
                        onCancelTranslation()
                    } else {
                        onTranslate()
                    }
                },
                containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
            ) {
                if (!translating) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            Lucide.Languages,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            stringResource(R.string.translator_page_translate),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                } else {
                    Text(stringResource(R.string.translator_page_cancel))
                }
            }
        }
    )
}