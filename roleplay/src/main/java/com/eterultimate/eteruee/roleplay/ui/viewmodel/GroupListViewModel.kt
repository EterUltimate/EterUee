package com.eterultimate.eteruee.roleplay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.domain.service.GroupService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 群组列表页 ViewModel
 */
class GroupListViewModel(
    private val groupService: GroupService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupListUiState())
    val uiState: StateFlow<GroupListUiState> = _uiState.asStateFlow()
    
    init {
        loadGroups()
    }
    
    /**
     * 加载群组列表
     */
    private fun loadGroups() {
        viewModelScope.launch {
            groupService.getAllGroups().collect { groups ->
                _uiState.value = _uiState.value.copy(
                    groups = groups,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * 创建新群组
     */
    fun createGroup(name: String, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = groupService.createGroup(name, description)
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
     * 删除群组
     */
    fun deleteGroup(groupId: kotlin.uuid.Uuid) {
        viewModelScope.launch {
            val result = groupService.deleteGroup(groupId)
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
data class GroupListUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)
