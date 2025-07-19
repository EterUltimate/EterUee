package me.rerere.tts.provider

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
sealed class TTSProviderSetting {
  abstract val id: Uuid
  abstract val enabled: Boolean
  abstract val name: String

  abstract fun copyProvider(
    id: Uuid = this.id,
    enabled: Boolean = this.enabled,
    name: String = this.name,
  ): TTSProviderSetting

  @Serializable
  @SerialName("openai")
  data class OpenAI(
    override var id: Uuid = Uuid.random(),
    override var enabled: Boolean = true,
    override var name: String = "OpenAI TTS",
    val apiKey: String = "",
    val baseUrl: String = "https://api.openai.com/v1",
    val model: String = "gpt-4o-mini-tts",
    val speed: Float = 1.0f,
    val voice: String = "alloy"
  ) : TTSProviderSetting() {
    override fun copyProvider(
      id: Uuid,
      enabled: Boolean,
      name: String,
    ): TTSProviderSetting {
      return this.copy(
        id = id,
        enabled = enabled,
        name = name,
      )
    }
  }

  @Serializable
  @SerialName("gemini")
  data class Gemini(
    override var id: Uuid = Uuid.random(),
    override var enabled: Boolean = true,
    override var name: String = "Gemini TTS",
    val apiKey: String = "",
    val model: String = "gemini-2.5-flash-preview-tts",
    val voiceName: String = "Kore"
  ) : TTSProviderSetting() {
    override fun copyProvider(
      id: Uuid,
      enabled: Boolean,
      name: String,
    ): TTSProviderSetting {
      return this.copy(
        id = id,
        enabled = enabled,
        name = name,
      )
    }
  }

  @Serializable
  @SerialName("system")
  data class SystemTTS(
    override var id: Uuid = Uuid.random(),
    override var enabled: Boolean = true,
    override var name: String = "System TTS",
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val language: String = "en-US"
  ) : TTSProviderSetting() {
    override fun copyProvider(
      id: Uuid,
      enabled: Boolean,
      name: String,
    ): TTSProviderSetting {
      return this.copy(
        id = id,
        enabled = enabled,
        name = name,
      )
    }
  }

  companion object {
    val Types by lazy {
      listOf(
        OpenAI::class,
        Gemini::class,
        SystemTTS::class,
      )
    }
  }
}