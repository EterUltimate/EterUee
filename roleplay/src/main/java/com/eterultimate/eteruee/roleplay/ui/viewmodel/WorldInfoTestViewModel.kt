package com.eterultimate.eteruee.roleplay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.roleplay.data.model.ChatMessage
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.data.model.InsertionPosition
import com.eterultimate.eteruee.roleplay.domain.service.PromptBuilderService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * 世界书测试页面 ViewModel
 */
class WorldInfoTestViewModel(
    private val promptBuilderService: PromptBuilderService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorldInfoTestUiState())
    val uiState: StateFlow<WorldInfoTestUiState> = _uiState.asStateFlow()
    
    init {
        // 初始化示例世界书条目
        _uiState.value = _uiState.value.copy(
            sampleEntries = createSampleEntries()
        )
    }
    
    fun updateSystemPrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(systemPrompt = prompt)
    }
    
    fun updateUserMessage(message: String) {
        _uiState.value = _uiState.value.copy(userMessage = message)
    }
    
    fun generatePrompt() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // 构建消息列表
            val messages = listOf(
                ChatMessage(
                    id = kotlin.uuid.Uuid.random(),
                    role = MessageRole.USER,
                    content = state.userMessage,
                    timestamp = Instant.now()
                )
            )
            
            // 使用 PromptBuilderService 生成 Prompt
            val generatedPrompt = promptBuilderService.buildPrompt(
                systemPrompt = state.systemPrompt,
                worldInfoEntries = state.sampleEntries,
                messages = messages,
                maxContextLength = 4096
            )
            
            _uiState.value = state.copy(generatedPrompt = generatedPrompt)
        }
    }
    
    /**
     * 创建示例世界书条目
     */
    private fun createSampleEntries(): List<WorldInfoEntry> {
        return listOf(
            WorldInfoEntry(
                id = kotlin.uuid.Uuid.random(),
                key = "魔法",
                keys = listOf("magic", "法术"),
                content = "在这个世界中，魔法是一种普遍存在的力量。魔法师可以通过吟唱咒语来施展各种法术，包括火球术、冰霜新星和闪电链等。",
                constant = false,
                order = 1,
                position = InsertionPosition.AFTER_SYSTEM_PROMPT,
                enabled = true,
                probability = 1.0f
            ),
            WorldInfoEntry(
                id = kotlin.uuid.Uuid.random(),
                key = "王国",
                keys = listOf("kingdom", "首都"),
                content = "艾尔利亚王国是大陆上最强大的国家，首都是银月城。国王阿尔弗雷德三世已经统治了30年，以公正和智慧著称。",
                constant = false,
                order = 2,
                position = InsertionPosition.BEFORE_LAST_USER_MESSAGE,
                enabled = true,
                probability = 1.0f
            ),
            WorldInfoEntry(
                id = kotlin.uuid.Uuid.random(),
                key = "龙",
                keys = listOf("dragon", "巨龙"),
                content = "龙是传说中的生物，拥有强大的力量和智慧。它们能够喷吐火焰、冰冻或雷电。龙族通常居住在高山或火山中，守护着巨大的宝藏。",
                constant = false,
                order = 3,
                position = InsertionPosition.AT_END,
                enabled = true,
                probability = 1.0f
            )
        )
    }
}

data class WorldInfoTestUiState(
    val systemPrompt: String = "你是一个角色扮演助手，扮演一个奇幻世界中的冒险者。",
    val userMessage: String = "",
    val generatedPrompt: String = "",
    val sampleEntries: List<WorldInfoEntry> = emptyList()
)
