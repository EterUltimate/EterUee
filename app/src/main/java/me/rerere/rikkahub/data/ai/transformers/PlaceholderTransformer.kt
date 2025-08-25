package me.rerere.rikkahub.data.ai.transformers

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.InputMessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.Temporal
import java.util.Locale
import java.util.TimeZone

object PlaceholderTransformer : InputMessageTransformer, KoinComponent {
    val Placeholders = mapOf(
        "cur_date" to "日期",
        "cur_time" to "时间",
        "cur_datetime" to "日期和时间",
        "model_id" to "模型ID",
        "model_name" to "模型名称",
        "locale" to "语言环境",
        "timezone" to "时区",
        "system_version" to "系统版本",
        "device_info" to "设备信息",
        "battery_level" to "电池电量",
        "nickname" to "用户昵称"
    )

    private val placeholderResolvers: Map<String, (Context, SettingsStore, Model) -> String> = mapOf(
        "cur_date" to { _, _, _ -> LocalDate.now().toDateString() },
        "cur_time" to { _, _, _ -> LocalTime.now().toTimeString() },
        "cur_datetime" to { _, _, _ -> LocalDateTime.now().toDateTimeString() },
        "model_id" to { _, _, model -> model.modelId },
        "model_name" to { _, _, model -> model.displayName },
        "locale" to { _, _, _ -> Locale.getDefault().displayName },
        "timezone" to { _, _, _ -> TimeZone.getDefault().displayName },
        "system_version" to { _, _, _ -> "Android SDK v${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})" },
        "device_info" to { _, _, _ -> "${Build.BRAND} ${Build.MODEL}" },
        "battery_level" to { context, _, _ -> context.batteryLevel().toString() },
        "nickname" to { _, settingsStore, _ -> settingsStore.settingsFlow.value.displaySetting.userNickname.ifBlank { "user" } },
        "char" to { _, settingsStore, _ -> settingsStore.settingsFlow.value.getCurrentAssistant().name.ifBlank { "assistant" } },
        "user" to { _, settingsStore, _ -> settingsStore.settingsFlow.value.displaySetting.userNickname.ifBlank { "user" } },
    )

    override suspend fun transform(
        context: Context,
        messages: List<UIMessage>,
        model: Model
    ): List<UIMessage> {
        val settingsStore = get<SettingsStore>()
        return messages.map {
            it.copy(
                parts = it.parts.map { part ->
                    if (part is UIMessagePart.Text) {
                        part.copy(
                            text = replacePlaceholders(part.text, context, settingsStore, model)
                        )
                    } else {
                        part
                    }
                }
            )
        }
    }

    private fun replacePlaceholders(
        text: String,
        context: Context,
        settingsStore: SettingsStore,
        model: Model
    ): String {
        var result = text

        placeholderResolvers.forEach { (key, resolver) ->
            val value = resolver(context, settingsStore, model)
            result = result
                .replace("{{$key}}", value)
                .replace("{$key}", value)
        }

        return result
    }

    private fun Temporal.toDateString() = DateTimeFormatter
        .ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(this)

    private fun Temporal.toTimeString() = DateTimeFormatter
        .ofLocalizedTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(this)

    private fun Temporal.toDateTimeString() = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM)
        .withLocale(Locale.getDefault())
        .format(this)

    private fun Context.batteryLevel(): Int {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
