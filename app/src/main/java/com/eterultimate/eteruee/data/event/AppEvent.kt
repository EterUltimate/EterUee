package com.eterultimate.eteruee.data.event

sealed class AppEvent {
    data class Speak(val text: String) : AppEvent()
}

