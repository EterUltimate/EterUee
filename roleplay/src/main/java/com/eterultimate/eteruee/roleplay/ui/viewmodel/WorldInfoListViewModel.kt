package com.eterultimate.eteruee.roleplay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.domain.service.WorldInfoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 世界书列表页 ViewModel
 */
class WorldInfoListViewModel(
    private val worldInfoService: WorldInfoService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorldInfoListUiState())
    val uiState: StateFlow<WorldInfoListUiState> = _uiState.asStateFlow()
    
    init {
        loadWorldInfos()
    }
    
    /**
     * 加载世界书列表
     */
    private fun loadWorldInfos() {
        viewModelScope.launch {
            worldInfoService.getAllWorldInfos().collect { worldInfos ->
                _uiState.value = _uiState.value.copy(
                    worldInfos = worldInfos,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 创建新世界书
     */
    fun createWorldInfo(name: String, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = worldInfoService.createWorldInfo(name, description)
            result.onSuccess {
                // 自动重新加载
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "创建失败: ${error.message}"
                )
            }
        }
    }
    
    /**
     * 删除世界书
     */
    fun deleteWorldInfo(worldInfoId: kotlin.uuid.Uuid) {
        viewModelScope.launch {
            val result = worldInfoService.deleteWorldInfo(worldInfoId)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    errorMessage = "删除失败: ${error.message}"
                )
            }
        }
    }
    
    /**
     * 清除错误消息
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

/**
 * UI 状态
 */
data class WorldInfoListUiState(
    val worldInfos: List<WorldInfo> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
