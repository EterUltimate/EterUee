package me.rerere.ai.ui.transformers

import android.os.Build
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.MessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import java.util.TimeZone

object PlaceholderTransformer : MessageTransformer {
    val Placeholders = mapOf(
        "{cur_date}" to "日期",
        "{cur_time}" to "时间",
        "{cur_datetime}" to "日期和时间",
        "{model_id}" to "模型ID",
        "{model_name}" to "模型名称",
        "{locale}" to "语言环境",
        "{timezone}" to "时区",
        "{system_version}" to "系统版本",
        "{device_info}" to "设备信息",
    )

    override fun transform(messages: List<UIMessage>, model: Model): List<UIMessage> {
        return messages.map {
            it.copy(
                parts = it.parts.map { part ->
                    if (part is UIMessagePart.Text) {
                        part.copy(
                            text = part.text
                                .replace("{cur_date}", LocalDate.now().toString())
                                .replace("{cur_time}", LocalTime.now().toString())
                                .replace("{cur_datetime}", LocalDateTime.now().toString())
                                .replace("{model_id}", model.modelId)
                                .replace("{model_name}", model.displayName)
                                .replace("{locale}", Locale.getDefault().displayName)
                                .replace("{timezone}", TimeZone.getDefault().displayName)
                                .replace(
                                    "{system_version}",
                                    "Android SDK ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE}"
                                )
                                .replace("{device_info}", "${Build.BRAND} ${Build.MODEL}")
                        )
                    } else {
                        part
                    }
                }
            )
        }
    }
}