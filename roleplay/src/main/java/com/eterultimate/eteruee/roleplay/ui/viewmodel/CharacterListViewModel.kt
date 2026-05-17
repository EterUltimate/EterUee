package com.eterultimate.eteruee.roleplay.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.domain.service.CharacterService
import com.eterultimate.eteruee.roleplay.domain.service.CharacterSortOption
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 角色列表页 ViewModel
 */
class CharacterListViewModel(
    private val characterService: CharacterService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CharacterListUiState())
    val uiState: StateFlow<CharacterListUiState> = _uiState.asStateFlow()
    
    /**
     * 选中的角色ID列表（用于批量操作）
     */
    private val _selectedCharacterIds = MutableStateFlow<Set<kotlin.uuid.Uuid>>(emptySet())
    val selectedCharacterIds: StateFlow<Set<kotlin.uuid.Uuid>> = _selectedCharacterIds.asStateFlow()
    
    /**
     * 是否处于多选模式
     */
    private val _isMultiSelectMode = MutableStateFlow(false)
    val isMultiSelectMode: StateFlow<Boolean> = _isMultiSelectMode.asStateFlow()
    
    /**
     * 当前选中的标签列表
     */
    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags.asStateFlow()
    
    /**
     * 所有可用标签
     */
    private val _availableTags = MutableStateFlow<List<String>>(emptyList())
    val availableTags: StateFlow<List<String>> = _availableTags.asStateFlow()
    
    /**
     * 当前排序选项
     */
    private val _sortOption = MutableStateFlow(CharacterSortOption.LAST_CHAT_DESC)
    val sortOption: StateFlow<CharacterSortOption> = _sortOption.asStateFlow()
    
    /**
     * 是否只显示收藏
     */
    private val _favoriteOnly = MutableStateFlow(false)
    val favoriteOnly: StateFlow<Boolean> = _favoriteOnly.asStateFlow()
    
    /**
     * 获取所有角色(分页)
     */
    fun loadCharacters(): Flow<PagingData<Character>> {
        return characterService.getAllCharacters().cachedIn(viewModelScope)
    }
    
    /**
     * 搜索角色
     */
    fun searchCharacters(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            
            val result = runCatching {
                characterService.searchCharacters(query)
            }
            
            result.onSuccess { characters ->
                _uiState.value = _uiState.value.copy(
                    searchResults = characters,
                    isSearching = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message,
                    isSearching = false
                )
            }
        }
    }
    
    /**
     * 清除搜索结果
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchResults = emptyList(),
            isSearching = false
        )
    }
    
    /**
     * 切换收藏状态
     */
    fun toggleFavorite(characterId: kotlin.uuid.Uuid) {
        viewModelScope.launch {
            val result = characterService.toggleFavorite(characterId)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        }
    }
    
    /**
     * 删除角色
     */
    fun deleteCharacter(characterId: kotlin.uuid.Uuid) {
        viewModelScope.launch {
            val result = characterService.deleteCharacter(characterId)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        }
    }
    
    /**
     * 导入PNG角色卡
     */
    fun importPngCharacter(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            
            val result = characterService.importPngCharacter(uri)
            result.onSuccess {
                _uiState.value = _uiState.value.copy(
                    successMessage = "角色导入成功",
                    isImporting = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = error.message,
                    isImporting = false
                )
            }
        }
    }
    
    /**
     * 切换多选模式
     */
    fun toggleMultiSelectMode() {
        _isMultiSelectMode.value = !_isMultiSelectMode.value
        if (!_isMultiSelectMode.value) {
            // 退出多选模式时清空选择
            _selectedCharacterIds.value = emptySet()
        }
    }
    
    /**
     * 切换角色选中状态
     */
    fun toggleCharacterSelection(characterId: kotlin.uuid.Uuid) {
        val currentSet = _selectedCharacterIds.value.toMutableSet()
        if (currentSet.contains(characterId)) {
            currentSet.remove(characterId)
        } else {
            currentSet.add(characterId)
        }
        _selectedCharacterIds.value = currentSet
    }
    
    /**
     * 全选/取消全选
     */
    fun toggleSelectAll(characterIds: List<kotlin.uuid.Uuid>) {
        if (_selectedCharacterIds.value.size == characterIds.size) {
            // 已全选，取消全选
            _selectedCharacterIds.value = emptySet()
        } else {
            // 全选
            _selectedCharacterIds.value = characterIds.toSet()
        }
    }
    
    /**
     * 批量删除角色
     */
    fun batchDeleteCharacters() {
        viewModelScope.launch {
            val idsToDelete = _selectedCharacterIds.value.toList()
            var successCount = 0
            var failCount = 0
            
            idsToDelete.forEach { characterId ->
                val result = characterService.deleteCharacter(characterId)
                if (result.isSuccess) {
                    successCount++
                } else {
                    failCount++
                }
            }
            
            // 清空选择并退出多选模式
            _selectedCharacterIds.value = emptySet()
            _isMultiSelectMode.value = false
            
            // 显示结果
            if (failCount == 0) {
                _uiState.value = _uiState.value.copy(
                    successMessage = "成功删除 $successCount 个角色"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除完成：成功 $successCount 个，失败 $failCount 个"
                )
            }
        }
    }
    
    /**
     * 批量导出角色为 JSON
     */
    fun batchExportCharacters(context: android.content.Context) {
        viewModelScope.launch {
            // TODO: 实现批量导出功能
            _uiState.value = _uiState.value.copy(
                successMessage = "批量导出功能开发中"
            )
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
    
    // ==================== 标签过滤 ====================
    
    /**
     * 加载所有可用标签
     */
    fun loadAvailableTags() {
        viewModelScope.launch {
            try {
                val tags = characterService.getAllTags()
                _availableTags.value = tags
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "加载标签失败: ${e.message}")
            }
        }
    }
    
    /**
     * 切换标签选中状态
     */
    fun toggleTag(tag: String) {
        val currentTags = _selectedTags.value.toMutableList()
        if (currentTags.contains(tag)) {
            currentTags.remove(tag)
        } else {
            currentTags.add(tag)
        }
        _selectedTags.value = currentTags
    }
    
    /**
     * 清除所有选中标签
     */
    fun clearSelectedTags() {
        _selectedTags.value = emptyList()
    }
    
    // ==================== 排序 ====================
    
    /**
     * 设置排序选项
     */
    fun setSortOption(option: CharacterSortOption) {
        _sortOption.value = option
    }
    
    /**
     * 切换收藏过滤
     */
    fun toggleFavoriteFilter() {
        _favoriteOnly.value = !_favoriteOnly.value
    }
}

/**
 * UI 状态
 */
data class CharacterListUiState(
    val searchResults: List<Character> = emptyList(),
    val isSearching: Boolean = false,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // 过滤和排序状态
    val selectedTags: List<String> = emptyList(),
    val availableTags: List<String> = emptyList(),
    val sortOption: CharacterSortOption = CharacterSortOption.LAST_CHAT_DESC,
    val favoriteOnly: Boolean = false
)
