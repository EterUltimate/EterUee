package com.eterultimate.eteruee.roleplay.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.eterultimate.eteruee.roleplay.data.model.Character
import com.eterultimate.eteruee.roleplay.domain.service.CharacterService
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
data class CharacterListUiState(
    val searchResults: List<Character> = emptyList(),
    val isSearching: Boolean = false,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
