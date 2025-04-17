package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun <T : Number> NumberInput(
    value: T,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
) {
    var textFieldValue by remember { mutableStateOf(value.toString()) }
    var hasError by remember { mutableStateOf(false) }
    val errorText = remember { mutableStateOf("") }
    OutlinedTextField(
        modifier = modifier,
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            try {
                @Suppress("UNCHECKED_CAST")
                when (value) {
                    is Int -> onValueChange((newValue.toIntOrNull() ?: 0) as T)
                    is Float -> onValueChange((newValue.toFloatOrNull() ?: 0f) as T)
                    is Double -> onValueChange((newValue.toDoubleOrNull() ?: 0.0) as T)
                }
                hasError = false
            } catch (e: Exception) {
                hasError = true
                errorText.value = "Invalid number format"
            }
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = textFieldValue.toDoubleOrNull() == null,
    )
}
