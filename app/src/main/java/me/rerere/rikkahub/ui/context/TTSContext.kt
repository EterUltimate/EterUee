package me.rerere.rikkahub.ui.context

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.compositionLocalOf

val LocalTTSService = compositionLocalOf<TextToSpeech?> {
    error("No TTS engine provided")
}