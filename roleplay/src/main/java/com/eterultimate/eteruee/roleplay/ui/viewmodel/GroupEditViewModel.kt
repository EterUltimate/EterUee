package com.eterultimate.eteruee.roleplay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.roleplay.data.model.Group
import com.eterultimate.eteruee.roleplay.data.model.GroupMember
import com.eterultimate.eteruee.roleplay.domain.service.GroupService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

/**
 * 群组编辑页 ViewModel
 */
class GroupEditViewModel(
    private val groupService: GroupService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(GroupEditUiState())
    val uiState: StateFlow<GroupEditUiState> = _uiState.asStateFlow()
    
    /**
     * 初始化编辑状态
     */
    fun initializeForEdit(group: Group) {
        _uiState.value = _uiState.value.copy(
            group = group,
            originalGroup = group,
            isEditing = true,
            isLoading = false
        )
    }
    
    /**
     * 初始化创建状态
     */
    fun initializeForCreate() {
        _uiState.value = _uiState.value.copy(
            group = Group(),
            originalGroup = null,
            isEditing = false,
            isLoading = false
        )
    }
    
    /**
     * 加载群组进行编辑
     */
    fun loadGroupForEdit(groupId: Uuid) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = groupService.getGroupById(groupId)
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    group = result,
                    originalGroup = result,
                    isEditing = true,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "无法加载群组"
                )
            }
        }
    }
    
    /**
     * 更新群组属性
     */
    fun updateGroupProperty(property: GroupProperty, value: String) {
        val currentGroup = _uiState.value.group
        val updatedGroup = when (property) {
            GroupProperty.NAME -> currentGroup.copy(name = value)
            GroupProperty.DESCRIPTION -> currentGroup.copy(description = value)
        }
        
        _uiState.value = _uiState.value.copy(group = updatedGroup)
    }
    
    /**
     * 添加成员
     */
    fun addMember(characterId: Uuid) {
        val currentGroup = _uiState.value.group
        val alreadyExists = currentGroup.members.any { it.characterId == characterId }
        if (!alreadyExists) {
            val newMember = GroupMember(characterId = characterId)
            val updatedMembers = currentGroup.members + newMember
            val updatedGroup = currentGroup.copy(members = updatedMembers)
            _uiState.value = _uiState.value.copy(group = updatedGroup)
        }
    }
    
    /**
     * 移除成员
     */
    fun removeMember(characterId: Uuid) {
        val currentGroup = _uiState.value.group
        val updatedMembers = currentGroup.members.filter { it.characterId != characterId }
        val updatedGroup = currentGroup.copy(members = updatedMembers)
        _uiState.value = _uiState.value.copy(group = updatedGroup)
    }
    
    /**
     * 保存群组
     */
    fun saveGroup() {
        val group = _uiState.value.group
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        
        viewModelScope.launch {
            val result = if (_uiState.value.isEditing) {
                // 编辑现有群组
                groupService.updateGroup(group)
            } else {
                // 创建新群组
                groupService.createGroup(group.name, group.description)
            }
            
            result
                .onSuccess { savedGroup ->
                    _uiState.value = _uiState.value.copy(
                        group = savedGroup,
                        originalGroup = savedGroup,
                        isEditing = true,
                        isLoading = false,
                        successMessage = if (_uiState.value.isEditing) "群组更新成功" else "群组创建成功"
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
data class GroupEditUiState(
    val group: Group = Group(),
    val originalGroup: Group? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

/**
 * 群组属性枚举
 */
enum class GroupProperty {
    NAME, DESCRIPTION
}
