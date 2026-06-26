package com.eterultimate.eteruee.data.ai.tools

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.event.AppEvent
import com.eterultimate.eteruee.data.event.AppEventBus
import com.eterultimate.eteruee.utils.hasUsageStatsPermission
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object ScreenTimeTools {
    fun createScreenTimeTool(context: Context, eventBus: AppEventBus): Tool = Tool(
        name = "get_screen_time",
        description = """
            Get the user's app screen usage (screen time) over a time range.
            Specify a custom interval with 'begin'/'end', or use the 'range' preset (today/week).
            Returns the total foreground time and a per-app breakdown sorted by usage time (descending).
            The device timezone is '${ZoneId.systemDefault()}' (UTC offset ${OffsetDateTime.now().offset});
            times without an explicit offset are interpreted in this timezone.
            Requires the 'Usage access' special permission; if it is not granted, the device's usage
            access settings page is opened automatically and an error is returned.
        """.trimIndent().replace("\n", " "),
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("begin", buildJsonObject {
                        put("type", "string")
                        put(
                            "description",
                            "Start time (inclusive). Accepts an ISO-8601 date 'yyyy-MM-dd', a local " +
                                "date-time 'yyyy-MM-ddTHH:mm:ss', an offset date-time, or epoch milliseconds. " +
                                "When provided, 'range' is ignored."
                        )
                    })
                    put("end", buildJsonObject {
                        put("type", "string")
                        put("description", "End time (exclusive), same formats as 'begin'. Defaults to now.")
                    })
                    put("range", buildJsonObject {
                        put("type", "string")
                        put(
                            "enum",
                            buildJsonArray {
                                add("today")
                                add("week")
                            }
                        )
                        put(
                            "description",
                            "Convenience preset, used only when 'begin' is omitted: today or week. Default today."
                        )
                    })
                    put("top", buildJsonObject {
                        put("type", "integer")
                        put("description", "Maximum number of top apps to return, sorted by usage time. Default 10.")
                    })
                }
            )
        },
        execute = screenTimeExecute@{
            if (!context.hasUsageStatsPermission()) {
                eventBus.emit(AppEvent.OpenUsageAccessSettings)
                val payload = buildJsonObject {
                    put("error", "NO_PERMISSION")
                    put(
                        "message",
                        "Usage access permission is not granted. The system settings page has been " +
                            "opened; please ask the user to enable 'Usage access' for this app and try again."
                    )
                }
                return@screenTimeExecute listOf(UIMessagePart.Text(payload.toString()))
            }

            val params = it.jsonObject
            val top = params["top"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()?.coerceIn(1, 50) ?: 10

            val now = ZonedDateTime.now()
            val zone = now.zone
            val beginRaw = params["begin"]?.jsonPrimitive?.contentOrNull
            val endRaw = params["end"]?.jsonPrimitive?.contentOrNull
            val rangePreset = params["range"]?.jsonPrimitive?.contentOrNull ?: "today"

            val startTime: ZonedDateTime
            val endTime: ZonedDateTime
            try {
                endTime = endRaw?.let { raw -> parseUsageTime(raw, zone) } ?: now
                startTime = if (beginRaw != null) {
                    parseUsageTime(beginRaw, zone)
                } else when (rangePreset) {
                    "week" -> now.minusDays(7)
                    else -> now.toLocalDate().atStartOfDay(zone)
                }
            } catch (e: Exception) {
                val payload = buildJsonObject {
                    put("error", "INVALID_TIME")
                    put("message", e.message ?: "Invalid time format for begin/end.")
                }
                return@screenTimeExecute listOf(UIMessagePart.Text(payload.toString()))
            }

            if (!startTime.isBefore(endTime)) {
                val payload = buildJsonObject {
                    put("error", "INVALID_RANGE")
                    put("message", "begin must be earlier than end.")
                }
                return@screenTimeExecute listOf(UIMessagePart.Text(payload.toString()))
            }

            val isCustom = beginRaw != null || endRaw != null
            val endMs = endTime.toInstant().toEpochMilli()
            val startMs = startTime.toInstant().toEpochMilli()
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm = context.packageManager
            val launcherPackages = resolveLauncherPackages(pm)
            val foregroundMs = computeForegroundTime(usageStatsManager, startMs, endMs, launcherPackages)

            val sorted = foregroundMs.entries
                .filter { entry -> entry.value > 0 }
                .sortedByDescending { entry -> entry.value }
            val totalMs = sorted.sumOf { entry -> entry.value }

            val payload = buildJsonObject {
                put("range", if (isCustom) "custom" else rangePreset)
                put("start", startTime.withNano(0).toString())
                put("end", endTime.withNano(0).toString())
                put("total_ms", totalMs)
                put("total_minutes", totalMs / 60000)
                put("apps", buildJsonArray {
                    sorted.take(top).forEach { entry ->
                        add(buildJsonObject {
                            put("package", entry.key)
                            put("app_name", resolveAppName(pm, entry.key))
                            put("total_ms", entry.value)
                            put("total_minutes", entry.value / 60000)
                        })
                    }
                })
            }
            listOf(UIMessagePart.Text(payload.toString()))
        }
    )
}

private const val LOOKBACK_MS = 12L * 60 * 60 * 1000

@Suppress(
    "DEPRECATION",
    "NewApi",
)
private fun computeForegroundTime(
    usageStatsManager: UsageStatsManager,
    startMs: Long,
    endMs: Long,
    excludedPackages: Set<String>,
): Map<String, Long> {
    val foregroundMs = HashMap<String, Long>()
    val events = usageStatsManager.queryEvents(startMs - LOOKBACK_MS, endMs)
    val event = UsageEvents.Event()

    var currentPkg: String? = null
    var currentStart = 0L

    fun settle(until: Long) {
        val pkg = currentPkg
        currentPkg = null
        if (pkg == null || pkg in excludedPackages) return
        val from = maxOf(currentStart, startMs)
        val duration = until - from
        if (duration > 0) {
            foregroundMs[pkg] = (foregroundMs[pkg] ?: 0L) + duration
        }
    }

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                if (event.packageName != currentPkg) {
                    settle(event.timeStamp)
                    currentPkg = event.packageName
                    currentStart = event.timeStamp
                }
            }

            UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                if (event.packageName == currentPkg) {
                    settle(event.timeStamp)
                }
            }

            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                settle(event.timeStamp)
            }
        }
    }
    settle(endMs)
    return foregroundMs
}

private fun resolveLauncherPackages(pm: PackageManager): Set<String> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    return runCatching {
        pm.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()
    }.getOrDefault(emptySet())
}

private fun resolveAppName(pm: PackageManager, packageName: String): String {
    return runCatching {
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
}

private fun parseUsageTime(raw: String, zone: ZoneId): ZonedDateTime {
    val text = raw.trim()
    text.toLongOrNull()?.let { return Instant.ofEpochMilli(it).atZone(zone) }
    runCatching { return OffsetDateTime.parse(text).atZoneSameInstant(zone) }
    runCatching { return Instant.parse(text).atZone(zone) }
    runCatching { return LocalDateTime.parse(text).atZone(zone) }
    runCatching { return LocalDate.parse(text).atStartOfDay(zone) }
    error("Invalid time format: '$raw'. Use ISO-8601 date/date-time or epoch milliseconds.")
}
