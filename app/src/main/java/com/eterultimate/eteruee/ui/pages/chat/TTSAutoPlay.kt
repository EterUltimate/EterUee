package com.eterultimate.eteruee.ui.pages.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import me.rerere.ai.core.MessageRole
import com.eterultimate.eteruee.data.datastore.Settings
import com.eterultimate.eteruee.data.model.Conversation
import com.eterultimate.eteruee.ui.context.LocalTTSState
import com.eterultimate.eteruee.utils.extractQuotedContentAsText

@Composable
fun TTSAutoPlay(vm: ChatVM, setting: Settings, conversation: Conversation) {
    // Auto-play TTS after generation completes
    val tts = LocalTTSState.current
    val currentConversation by rememberUpdatedState(conversation)
    val updatedSetting by rememberUpdatedState(setting)
    LaunchedEffect(Unit) {
        vm.generationDoneFlow.collect { conversationId ->
            if (updatedSetting.displaySetting.autoPlayTTSAfterGeneration) {
                val lastMessage = currentConversation.currentMessages.lastOrNull()
                if (lastMessage != null && lastMessage.role == MessageRole.ASSISTANT) {
                    val text = lastMessage.toText()
                    val textToSpeak = if (updatedSetting.displaySetting.ttsOnlyReadQuoted) {
                        text.extractQuotedContentAsText() ?: text
                    } else {
                        text
                    }
                    if (textToSpeak.isNotBlank()) {
                        tts.speak(textToSpeak)
                    }
                }
            }
        }
    }
}

