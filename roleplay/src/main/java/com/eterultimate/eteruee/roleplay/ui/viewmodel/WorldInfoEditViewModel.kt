package com.eterultimate.eteruee.roleplay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.roleplay.data.model.WorldInfo
import com.eterultimate.eteruee.roleplay.data.model.WorldInfoEntry
import com.eterultimate.eteruee.roleplay.domain.service.WorldInfoService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/**
 * 世界书编辑页 ViewModel
 */
class WorldInfoEditViewModel(
    private val worldInfoService: WorldInfoService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorldInfoEditUiState())
    val uiState: StateFlow<WorldInfoEditUiState> = _uiState.asStateFlow()
    
    /**
     * 初始化编辑状态
     */
    fun initializeForEdit(worldInfo: WorldInfo) {
        _uiState.value = _uiState.value.copy(
            worldInfo = worldInfo,
            originalWorldInfo = worldInfo,
            isEditing = true,
            isLoading = false
        )
    }
    
    /**
     * 初始化创建状态
     */
    fun initializeForCreate() {
        _uiState.value = _uiState.value.copy(
            worldInfo = WorldInfo(),
            originalWorldInfo = null,
            isEditing = false,
            isLoading = false
        )
    }
    
    /**
     * 加载世界书进行编辑
     */
    fun loadWorldInfoForEdit(worldInfoId: Uuid) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = worldInfoService.getWorldInfoById(worldInfoId)
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    worldInfo = result,
                    originalWorldInfo = result,
                    isEditing = true,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "无法加载世界书"
                )
            }
        }
    }
    
    /**
     * 更新世界书属性
     */
    fun updateWorldInfoProperty(property: WorldInfoProperty, value: String) {
        val currentWorldInfo = _uiState.value.worldInfo
        val updatedWorldInfo = when (property) {
            WorldInfoProperty.NAME -> currentWorldInfo.copy(name = value)
            WorldInfoProperty.DESCRIPTION -> currentWorldInfo.copy(description = value)
        }
        
        _uiState.value = _uiState.value.copy(worldInfo = updatedWorldInfo)
    }
    
    /**
     * 添加条目
     */
    fun addEntry() {
        val currentWorldInfo = _uiState.value.worldInfo
        val newEntry = WorldInfoEntry()
        val updatedEntries = currentWorldInfo.entries + newEntry
        val updatedWorldInfo = currentWorldInfo.copy(entries = updatedEntries)
        
        _uiState.value = _uiState.value.copy(worldInfo = updatedWorldInfo)
    }
    
    /**
     * 删除条目
     */
    fun removeEntry(index: Int) {
        val currentWorldInfo = _uiState.value.worldInfo
        val updatedEntries = currentWorldInfo.entries.toMutableList().apply {
            if (index in indices) removeAt(index)
        }
        val updatedWorldInfo = currentWorldInfo.copy(entries = updatedEntries)
        
        _uiState.value = _uiState.value.copy(worldInfo = updatedWorldInfo)
    }
    
    /**
     * 更新条目
     */
    fun updateEntry(index: Int, entry: WorldInfoEntry) {
        val currentWorldInfo = _uiState.value.worldInfo
        val updatedEntries = currentWorldInfo.entries.toMutableList().apply {
            if (index in indices) set(index, entry)
        }
        val updatedWorldInfo = currentWorldInfo.copy(entries = updatedEntries)
        
        _uiState.value = _uiState.value.copy(worldInfo = updatedWorldInfo)
    }
    
    /**
     * 保存世界书
     */
    fun saveWorldInfo() {
        val worldInfo = _uiState.value.worldInfo
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = if (_uiState.value.isEditing) {
                // 编辑现有世界书
                worldInfoService.updateWorldInfo(worldInfo)
            } else {
                // 创建新世界书并保留条目/扫描配置
                worldInfoService.saveWorldInfo(worldInfo)
            }
            
            result
                .onSuccess { savedWorldInfo ->
                    _uiState.value = _uiState.value.copy(
                        worldInfo = savedWorldInfo,
                        originalWorldInfo = savedWorldInfo,
                        isEditing = true,
                        isLoading = false,
                        successMessage = if (_uiState.value.isEditing) "世界书更新成功" else "世界书创建成功"
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
data class WorldInfoEditUiState(
    val worldInfo: WorldInfo = WorldInfo(),
    val originalWorldInfo: WorldInfo? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * 世界书属性枚举
 */
enum class WorldInfoProperty {
    NAME, DESCRIPTION
}
