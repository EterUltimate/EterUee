package com.eterultimate.eteruee.roleplay.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eterultimate.eteruee.roleplay.data.model.Preset
import com.eterultimate.eteruee.roleplay.data.model.PresetType
import com.eterultimate.eteruee.roleplay.domain.service.PresetService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.uuid.Uuid

class PresetEditViewModel(
    private val presetService: PresetService
) : ViewModel() {
    private val _uiState = MutableStateFlow(PresetEditUiState())
    val uiState: StateFlow<PresetEditUiState> = _uiState.asStateFlow()

    fun initializeForCreate() {
        _uiState.value = PresetEditUiState(
            preset = Preset(
                name = "",
                description = "",
                type = PresetType.OPENAI,
                parameters = defaultParameters()
            )
        )
    }

    fun loadPresetForEdit(presetId: Uuid) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val preset = presetService.getPresetById(presetId)
            _uiState.value = if (preset == null) {
                _uiState.value.copy(isLoading = false, errorMessage = "无法加载预设")
            } else {
                _uiState.value.copy(
                    preset = preset.withDefaultParameters(),
                    originalPreset = preset,
                    isEditing = true,
                    isLoading = false
                )
            }
        }
    }

    fun updateName(value: String) {
        updatePreset { copy(name = value) }
    }

    fun updateDescription(value: String) {
        updatePreset { copy(description = value) }
    }

    fun updateType(type: PresetType) {
        updatePreset { copy(type = type) }
    }

    fun updateFloatParameter(key: String, value: Float) {
        updateParameter(key, value.roundTo(2))
    }

    fun updateIntParameter(key: String, value: Int) {
        updateParameter(key, value)
    }

    fun updateBooleanParameter(key: String, value: Boolean) {
        updateParameter(key, value)
    }

    fun updateStringParameter(key: String, value: String) {
        updateParameter(key, value)
    }

    fun savePreset() {
        val preset = _uiState.value.preset
        if (preset.name.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "预设名称不能为空")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val now = Instant.now()
            val saved = preset.copy(
                updatedAt = now,
                createdAt = _uiState.value.originalPreset?.createdAt ?: preset.createdAt
            )
            presetService.savePreset(saved)
                .onSuccess { result ->
                    _uiState.value = _uiState.value.copy(
                        preset = result,
                        originalPreset = result,
                        isEditing = true,
                        isLoading = false,
                        successMessage = "预设已保存"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
                }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    private fun updateParameter(key: String, value: Any) {
        updatePreset {
            copy(parameters = parameters.toMutableMap().apply { put(key, value) })
        }
    }

    private fun updatePreset(block: Preset.() -> Preset) {
        _uiState.value = _uiState.value.copy(preset = _uiState.value.preset.block())
    }

    private fun Preset.withDefaultParameters(): Preset {
        return copy(parameters = defaultParameters() + parameters)
    }

    private fun Float.roundTo(decimals: Int): Float {
        val factor = 10f.pow(decimals)
        return (this * factor).roundToInt() / factor
    }

    private fun Float.pow(exponent: Int): Float {
        var result = 1f
        repeat(exponent) { result *= this }
        return result
    }

    companion object {
        fun defaultParameters(): Map<String, Any> = mapOf(
            "temperature" to 0.8f,
            "top_p" to 0.95f,
            "top_k" to 40,
            "max_tokens" to 1024,
            "presence_penalty" to 0f,
            "frequency_penalty" to 0f,
            "stop_sequences" to "",
            "stream" to true
        )
    }
}

data class PresetEditUiState(
    val preset: Preset = Preset(parameters = PresetEditViewModel.defaultParameters()),
    val originalPreset: Preset? = null,
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
