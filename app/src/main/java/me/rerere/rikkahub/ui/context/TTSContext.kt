package me.rerere.rikkahub.ui.context

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

val LocalTTSService = compositionLocalOf<TextToSpeech?> {
    error("No TTS engine provided")
}