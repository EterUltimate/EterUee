package com.eterultimate.eteruee.tts.provider

import android.content.Context
import kotlinx.coroutines.flow.Flow
import com.eterultimate.eteruee.tts.model.AudioChunk
import com.eterultimate.eteruee.tts.model.TTSRequest

interface TTSProvider<T : TTSProviderSetting> {
    fun generateSpeech(
        context: Context,
        providerSetting: T,
        request: TTSRequest
    ): Flow<AudioChunk>
}

