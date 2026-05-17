package com.eterultimate.eteruee.roleplay.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.domain.service.CharacterService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/**
 * 角色编辑页 ViewModel
 */
class CharacterEditViewModel(
    private val characterService: CharacterService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CharacterEditUiState())
    val uiState: StateFlow<CharacterEditUiState> = _uiState.asStateFlow()
    
    /**
     * 初始化编辑状态
     */
    fun initializeForEdit(character: Character) {
        _uiState.value = _uiState.value.copy(
            character = character,
            originalCharacter = character,
            isEditing = true,
            isLoading = false
        )
    }
    
    /**
     * 初始化创建状态
     */
    fun initializeForCreate() {
        _uiState.value = _uiState.value.copy(
            character = Character(),
            originalCharacter = null,
            isEditing = false,
            isLoading = false
        )
    }
    
    /**
     * 加载角色进行编辑
     */
    fun loadCharacterForEdit(characterId: Uuid) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = characterService.getCharacterById(characterId)
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    character = result,
                    originalCharacter = result,
                    isEditing = true,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "无法加载角色"
                )
            }
        }
    }
    
    /**
     * 更新角色属性
     */
    fun updateCharacterProperty(property: CharacterProperty, value: String) {
        val currentCharacter = _uiState.value.character
        val updatedCharacter = when (property) {
            CharacterProperty.NAME -> currentCharacter.copy(name = value)
            CharacterProperty.DESCRIPTION -> currentCharacter.copy(description = value)
            CharacterProperty.PERSONALITY -> currentCharacter.copy(personality = value)
            CharacterProperty.SCENARIO -> currentCharacter.copy(scenario = value)
            CharacterProperty.FIRST_MESSAGE -> currentCharacter.copy(firstMessage = value)
            CharacterProperty.MESSAGE_EXAMPLES -> currentCharacter.copy(messageExamples = value)
            CharacterProperty.SYSTEM_PROMPT -> currentCharacter.copy(systemPrompt = value)
            CharacterProperty.POST_HISTORY_INSTRUCTIONS -> currentCharacter.copy(postHistoryInstructions = value)
            CharacterProperty.CREATOR -> currentCharacter.copy(creator = value)
            CharacterProperty.CREATOR_NOTES -> currentCharacter.copy(creatorNotes = value)
            CharacterProperty.CHARACTER_VERSION -> currentCharacter.copy(characterVersion = value)
        }
        
        _uiState.value = _uiState.value.copy(character = updatedCharacter)
    }
    
    /**
     * 更新头像
     */
    fun updateAvatar(avatarUri: Uri?) {
        val currentCharacter = _uiState.value.character
        val updatedCharacter = currentCharacter.copy(avatarUrl = avatarUri?.toString())
        
        _uiState.value = _uiState.value.copy(character = updatedCharacter)
    }
    
    /**
     * 更新标签
     */
    fun updateTags(tags: List<String>) {
        val currentCharacter = _uiState.value.character
        val updatedCharacter = currentCharacter.copy(tags = tags)
        
        _uiState.value = _uiState.value.copy(character = updatedCharacter)
    }
    
    /**
     * 切换收藏状态
     */
    fun toggleFavorite() {
        val currentCharacter = _uiState.value.character
        val updatedCharacter = currentCharacter.copy(favorite = !currentCharacter.favorite)
        
        _uiState.value = _uiState.value.copy(character = updatedCharacter)
    }
    
    /**
     * 保存角色
     */
    fun saveCharacter(avatarUri: Uri? = null) {
        val character = _uiState.value.character
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = if (_uiState.value.isEditing) {
                // 编辑现有角色
                characterService.updateCharacter(character, avatarUri)
            } else {
                // 创建新角色
                characterService.createCharacter(character, avatarUri)
            }
            
            result
                .onSuccess { savedCharacter ->
                    _uiState.value = _uiState.value.copy(
                        character = savedCharacter,
                        originalCharacter = savedCharacter,
                        isEditing = true, // 保存后变为编辑模式
                        isLoading = false,
                        successMessage = if (_uiState.value.isEditing) "角色更新成功" else "角色创建成功"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    /**
     * 导出PNG角色卡
     */
    fun exportPngCharacter(outputUri: Uri) {
        val characterId = _uiState.value.character.id
        _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = characterService.exportPngCharacter(characterId, outputUri)
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        successMessage = "PNG角色卡导出成功"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    /**
     * 导出JSON角色卡
     */
    fun exportJsonCharacter(outputUri: Uri) {
        val characterId = _uiState.value.character.id
        _uiState.value = _uiState.value.copy(isExporting = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = characterService.exportJsonCharacter(characterId, outputUri)
            result
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        successMessage = "JSON角色卡导出成功"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isExporting = false,
                        errorMessage = error.message
                    )
                }
        }
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

/**
 * UI 状态
 */
data class CharacterEditUiState(
    val character: Character = Character(),
    val originalCharacter: Character? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * 角色属性枚举
 */
enum class CharacterProperty {
    NAME, DESCRIPTION, PERSONALITY, SCENARIO, FIRST_MESSAGE, 
    MESSAGE_EXAMPLES, SYSTEM_PROMPT, POST_HISTORY_INSTRUCTIONS,
    CREATOR, CREATOR_NOTES, CHARACTER_VERSION
}
