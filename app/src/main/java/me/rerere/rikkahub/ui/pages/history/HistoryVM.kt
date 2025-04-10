package me.rerere.rikkahub.ui.pages.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.ai.ui.Conversation
import me.rerere.rikkahub.data.repository.ConversationRepository

class HistoryVM(
    private val conversationRepo: ConversationRepository,
) : ViewModel() {
    val conversations = conversationRepo.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun searchConversations(query: String) = conversationRepo.searchConversations(query)

    fun deleteConversation(conversation: Conversation) {
        viewModelScope.launch {
            conversationRepo.deleteConversation(conversation)
        }
    }
}