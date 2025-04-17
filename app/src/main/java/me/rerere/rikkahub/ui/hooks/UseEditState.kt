package me.rerere.rikkahub.ui.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun <T> useEditState(
    onUpdate: (T) -> Unit
): EditState<T> {
    return remember {
        EditStateImpl(onUpdate)
    }
}

sealed interface EditState<T> {
    var isEditing: Boolean
    var currentState : T?

    fun open(initialState: T)

    fun confirm()

    fun dismiss()
}

class EditStateImpl<T>(private val onUpdate: (T) -> Unit) : EditState<T> {
    override var isEditing: Boolean by mutableStateOf(false)
    override var currentState: T? by mutableStateOf(null)

    override fun open(initialState: T) {
        this.isEditing = true
        this.currentState = initialState
    }

    override fun confirm() {
        if (currentState != null) {
            onUpdate(currentState!!)
            isEditing = false
        }
    }

    override fun dismiss() {
        isEditing = false
    }
}
