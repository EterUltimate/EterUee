package com.eterultimate.eteruee.roleplay.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.roleplay.data.model.Preset
import com.eterultimate.eteruee.roleplay.data.model.PresetType
import com.eterultimate.eteruee.roleplay.domain.service.PresetService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class PresetListViewModel(
    private val presetService: PresetService
) : ViewModel() {
    private val _uiState = MutableStateFlow(PresetListUiState())
    val uiState: StateFlow<PresetListUiState> = _uiState.asStateFlow()

    init {
        observePresets()
    }

    private fun observePresets() {
        viewModelScope.launch {
            presetService.getAllPresets().collect { presets ->
                _uiState.value = _uiState.value.copy(presets = presets, isLoading = false)
            }
        }
    }

    fun setTypeFilter(type: PresetType?) {
        _uiState.value = _uiState.value.copy(selectedType = type)
    }

    fun deletePreset(id: Uuid) {
        viewModelScope.launch {
            presetService.deletePreset(id).onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
        }
    }

    fun importPreset(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)

            presetService.importPreset(uri).onSuccess { preset ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    successMessage = "已导入预设：${preset.name.ifBlank { "未命名" }}"
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    errorMessage = "导入失败: ${error.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

data class PresetListUiState(
    val presets: List<Preset> = emptyList(),
    val selectedType: PresetType? = null,
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val filteredPresets: List<Preset>
        get() = selectedType?.let { type -> presets.filter { it.type == type } } ?: presets
}
