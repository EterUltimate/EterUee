package com.eterultimate.eteruee.data.ai.transformers

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eterultimate.eteruee.ai.core.MessageRole
import com.eterultimate.eteruee.ai.provider.Model
import com.eterultimate.eteruee.ai.ui.UIMessage
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.R
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.data.model.Assistant
import com.eterultimate.eteruee.utils.LocationProvider
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

data class PlaceholderCtx(
    val context: Context,
    val settingsStore: SettingsStore,
    val model: Model,
    val assistant: Assistant,
)

interface PlaceholderProvider {
    val placeholders: Map<String, PlaceholderInfo>
}

enum class PlaceholderCachePolicy {
    STABLE,
    RUNTIME,
}

data class PlaceholderInfo(
    val displayName: @Composable () -> Unit,
    val cachePolicy: PlaceholderCachePolicy = PlaceholderCachePolicy.STABLE,
    val resolver: (PlaceholderCtx) -> String
)

class PlaceholderBuilder {
    private val placeholders = mutableMapOf<String, PlaceholderInfo>()

    fun placeholder(
        key: String,
        displayName: @Composable () -> Unit,
        cachePolicy: PlaceholderCachePolicy = PlaceholderCachePolicy.STABLE,
        resolver: (PlaceholderCtx) -> String
    ) {
        placeholders[key] = PlaceholderInfo(displayName, cachePolicy, resolver)
    }

    fun build(): Map<String, PlaceholderInfo> = placeholders.toMap()
}

fun buildPlaceholders(block: PlaceholderBuilder.() -> Unit): Map<String, PlaceholderInfo> {
    return PlaceholderBuilder().apply(block).build()
}

object DefaultPlaceholderProvider : PlaceholderProvider {
    override val placeholders: Map<String, PlaceholderInfo> = buildPlaceholders {
        placeholder(
            "cur_date",
            { Text(stringResource(R.string.placeholder_current_date)) },
            PlaceholderCachePolicy.RUNTIME
        ) {
            LocalDate.now().toDateString()
        }

        placeholder(
            "cur_time",
            { Text(stringResource(R.string.placeholder_current_time)) },
            PlaceholderCachePolicy.RUNTIME
        ) {
            LocalTime.now().toTimeString()
        }

        placeholder(
            "cur_datetime",
            { Text(stringResource(R.string.placeholder_current_datetime)) },
            PlaceholderCachePolicy.RUNTIME
        ) {
            LocalDateTime.now().toDateTimeString()
        }

        placeholder("model_id", { Text(stringResource(R.string.placeholder_model_id)) }) {
            it.model.modelId
        }

        placeholder("model_name", { Text(stringResource(R.string.placeholder_model_name)) }) {
            it.model.displayName
        }

        placeholder("locale", { Text(stringResource(R.string.placeholder_locale)) }) {
            Locale.getDefault().displayName
        }

        placeholder("timezone", { Text(stringResource(R.string.placeholder_timezone)) }) {
            TimeZone.getDefault().displayName
        }

        placeholder("system_version", { Text(stringResource(R.string.placeholder_system_version)) }) {
            "Android SDK v${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})"
        }

        placeholder("device_info", { Text(stringResource(R.string.placeholder_device_info)) }) {
            "${Build.BRAND} ${Build.MODEL}"
        }

        placeholder(
            "battery_level",
            { Text(stringResource(R.string.placeholder_battery_level)) },
            PlaceholderCachePolicy.RUNTIME
        ) {
            it.context.batteryLevel().toString()
        }

        placeholder("nickname", { Text(stringResource(R.string.placeholder_nickname)) }) {
            it.settingsStore.settingsFlow.value.displaySetting.userNickname.ifBlank { "user" }
        }

        placeholder("char", { Text(stringResource(R.string.placeholder_char)) }) {
            it.assistant.name.ifBlank { "assistant" }
        }

        placeholder("user", { Text(stringResource(R.string.placeholder_user)) }) {
            it.settingsStore.settingsFlow.value.displaySetting.userNickname.ifBlank { "user" }
        }

        placeholder(
            "current_location",
            { Text(stringResource(R.string.placeholder_current_location)) },
            PlaceholderCachePolicy.RUNTIME
        ) {
            LocationProvider.getCurrentLocation(it.context)
        }
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

object PlaceholderTransformer : InputMessageTransformer, KoinComponent {
    private val defaultProvider = DefaultPlaceholderProvider

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val settingsStore = get<SettingsStore>()
        val placeholderCtx = PlaceholderCtx(
            context = ctx.context,
            settingsStore = settingsStore,
            model = ctx.model,
            assistant = ctx.assistant
        )
        return applyPlaceholdersForCache(
            messages = messages,
            placeholders = defaultProvider.placeholders,
            placeholderCtx = placeholderCtx,
        )
    }
}

internal fun applyPlaceholdersForCache(
    messages: List<UIMessage>,
    placeholders: Map<String, PlaceholderInfo>,
    placeholderCtx: PlaceholderCtx,
): List<UIMessage> {
    return applyPlaceholdersForCache(
        messages = messages,
        placeholders = placeholders,
        resolvePlaceholder = { _, placeholder -> placeholder.resolver(placeholderCtx) },
    )
}

internal fun applyPlaceholdersForCache(
    messages: List<UIMessage>,
    placeholders: Map<String, PlaceholderInfo>,
    resolvePlaceholder: (key: String, placeholder: PlaceholderInfo) -> String,
): List<UIMessage> {
    val systemRuntimeKeys = linkedSetOf<String>()
    val transformed = messages.map { message ->
        message.copy(
            parts = message.parts.map { part ->
                if (part is UIMessagePart.Text) {
                    val text = replacePlaceholders(
                        text = part.text,
                        role = message.role,
                        placeholders = placeholders,
                        resolvePlaceholder = resolvePlaceholder,
                        systemRuntimeKeys = systemRuntimeKeys,
                    )
                    part.copy(text = text)
                } else {
                    part
                }
            }
        )
    }

    if (systemRuntimeKeys.isEmpty()) return transformed

    return transformed.toMutableList().apply {
        add(
            runtimeContextInsertIndex(transformed),
            buildRuntimeContextMessage(systemRuntimeKeys, placeholders, resolvePlaceholder)
        )
    }
}

private fun replacePlaceholders(
    text: String,
    role: MessageRole,
    placeholders: Map<String, PlaceholderInfo>,
    resolvePlaceholder: (key: String, placeholder: PlaceholderInfo) -> String,
    systemRuntimeKeys: MutableSet<String>,
): String {
    var result = text
    placeholders.forEach { (key, placeholderInfo) ->
        if (!result.containsPlaceholder(key)) return@forEach

        val value = if (role == MessageRole.SYSTEM && placeholderInfo.cachePolicy == PlaceholderCachePolicy.RUNTIME) {
            systemRuntimeKeys.add(key)
            "<runtime_context>$key</runtime_context>"
        } else {
            resolvePlaceholder(key, placeholderInfo)
        }
        result = result.replacePlaceholder(key, value)
    }
    return result
}

private fun buildRuntimeContextMessage(
    keys: Set<String>,
    placeholders: Map<String, PlaceholderInfo>,
    resolvePlaceholder: (key: String, placeholder: PlaceholderInfo) -> String,
): UIMessage {
    val content = buildString {
        appendLine("<runtime_context>")
        appendLine("Dynamic values referenced by the stable system prompt:")
        keys.sorted().forEach { key ->
            val placeholder = placeholders[key] ?: return@forEach
            val value = resolvePlaceholder(key, placeholder)
            appendLine("- $key: $value")
        }
        append("</runtime_context>")
    }
    return UIMessage.user(content)
}

private fun runtimeContextInsertIndex(messages: List<UIMessage>): Int {
    if (messages.isEmpty()) return 0
    val firstNonSystemIndex = messages.indexOfFirst { it.role != MessageRole.SYSTEM }
        .takeIf { it >= 0 }
        ?: messages.size
    val targetIndex = (messages.size - 1).coerceAtLeast(firstNonSystemIndex)
    return findSafeInsertIndex(messages, targetIndex)
}

private fun String.containsPlaceholder(key: String): Boolean {
    return contains("{{$key}}", ignoreCase = true) || contains("{$key}", ignoreCase = true)
}

private fun String.replacePlaceholder(key: String, value: String): String {
    return replace(oldValue = "{{$key}}", newValue = value, ignoreCase = true)
        .replace(oldValue = "{$key}", newValue = value, ignoreCase = true)
}

