package com.eterultimate.eteruee.data.ai.tools

import android.content.Context
import com.whl.quickjs.android.QuickJSLoader
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.QuickJSObject
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.provider.ProviderManager
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.event.AppEvent
import com.eterultimate.eteruee.data.event.AppEventBus
import com.eterultimate.eteruee.data.datastore.SettingsStore
import com.eterultimate.eteruee.device.DeviceAgentManager
import com.eterultimate.eteruee.data.files.FilesManager
import com.eterultimate.eteruee.data.repository.GenMediaRepository
import com.eterultimate.eteruee.linux.LinuxEnvironmentManager
import com.eterultimate.eteruee.network.HiddifyCoreManager
import com.eterultimate.eteruee.utils.readClipboardText
import com.eterultimate.eteruee.utils.writeClipboardText
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Serializable
sealed class LocalToolOption {
    @Serializable
    @SerialName("javascript_engine")
    data object JavascriptEngine : LocalToolOption()

    @Serializable
    @SerialName("python_engine")
    data object PythonEngine : LocalToolOption()

    @Serializable
    @SerialName("time_info")
    data object TimeInfo : LocalToolOption()

    @Serializable
    @SerialName("clipboard")
    data object Clipboard : LocalToolOption()

    @Serializable
    @SerialName("tts")
    data object Tts : LocalToolOption()

    @Serializable
    @SerialName("ask_user")
    data object AskUser : LocalToolOption()

    @Serializable
    @SerialName("ssh")
    data object Ssh : LocalToolOption()

    @Serializable
    @SerialName("shell")
    data object Shell : LocalToolOption()

    @Serializable
    @SerialName("linux_environment")
    data object LinuxEnvironment : LocalToolOption()

    @Serializable
    @SerialName("traffic_control")
    data object TrafficControl : LocalToolOption()

    @Serializable
    @SerialName("device_agent")
    data object DeviceAgent : LocalToolOption()

    @Serializable
    @SerialName("video_generation")
    data object VideoGeneration : LocalToolOption()
}

data class ScriptExecutionResult(
    val result: String?,
    val logs: List<String>,
    val error: String?,
)

private const val QUICKJS_MEMORY_LIMIT = 8 * 1024 * 1024 // 8 MB
private const val QUICKJS_STACK_SIZE = 256 * 1024 // 256 KB
private const val QUICKJS_GC_THRESHOLD = 4 * 1024 * 1024 // 4 MB
private const val MAX_SCRIPT_CODE_CHARS = 200_000
private const val MAX_SCRIPT_OUTPUT_CHARS = 100_000
private const val MAX_SCRIPT_LOG_ENTRIES = 200
private const val MAX_SCRIPT_LOG_CHARS = 4_000
private const val PYTHON_TIMEOUT_SECONDS = 30L
private const val PYTHON_BINARY_UNKNOWN = "__unknown__"
private const val PYTHON_BINARY_MISSING = "__missing__"
private val quickJsInitialized = AtomicBoolean(false)
private val pythonBinaryCache = AtomicReference(PYTHON_BINARY_UNKNOWN)
private val scriptIoThreadIndex = AtomicInteger(0)
private val scriptIoExecutor = Executors.newCachedThreadPool { runnable ->
    Thread(runnable, "eteruee-script-io-${scriptIoThreadIndex.incrementAndGet()}").apply {
        isDaemon = true
    }
}

private fun ensureQuickJsInitialized() {
    if (quickJsInitialized.get()) return
    synchronized(quickJsInitialized) {
        if (!quickJsInitialized.get()) {
            QuickJSLoader.init()
            quickJsInitialized.set(true)
        }
    }
}

fun executeJavaScriptCode(code: String?): ScriptExecutionResult {
    val script = code ?: ""
    if (script.length > MAX_SCRIPT_CODE_CHARS) {
        return ScriptExecutionResult(
            result = null,
            logs = emptyList(),
            error = "JavaScript code is too large: max $MAX_SCRIPT_CODE_CHARS characters.",
        )
    }

    val logs = arrayListOf<String>()
    val context = try {
        ensureQuickJsInitialized()
        QuickJSContext.create()
    } catch (t: Throwable) {
        return ScriptExecutionResult(
            result = null,
            logs = logs,
            error = "QuickJS engine is unavailable: ${t.message ?: t.javaClass.simpleName}",
        )
    }
    return try {
        context.setMemoryLimit(QUICKJS_MEMORY_LIMIT)
        context.setMaxStackSize(QUICKJS_STACK_SIZE)
        context.setGCThreshold(QUICKJS_GC_THRESHOLD)
        context.setEnableStackTrace(true)
        context.setConsole(object : QuickJSContext.Console {
            override fun log(info: String?) {
                logs.addBoundedLog("[LOG]", info)
            }

            override fun info(info: String?) {
                logs.addBoundedLog("[INFO]", info)
            }

            override fun warn(info: String?) {
                logs.addBoundedLog("[WARN]", info)
            }

            override fun error(info: String?) {
                logs.addBoundedLog("[ERROR]", info)
            }
        })
        val result = context.evaluate(script)
        ScriptExecutionResult(
            result = when (result) {
                null -> null
                is QuickJSObject -> result.stringify().limitScriptOutput("JavaScript result")
                else -> result.toString().limitScriptOutput("JavaScript result")
            },
            logs = logs,
            error = null,
        )
    } catch (e: Exception) {
        ScriptExecutionResult(
            result = null,
            logs = logs,
            error = e.message ?: e.javaClass.simpleName,
        )
    } finally {
        runCatching { context.runGC() }
        context.destroy()
    }
}

fun executePythonScript(code: String?): ScriptExecutionResult {
    val script = code ?: ""
    if (script.length > MAX_SCRIPT_CODE_CHARS) {
        return ScriptExecutionResult(
            result = null,
            logs = emptyList(),
            error = "Python code is too large: max $MAX_SCRIPT_CODE_CHARS characters.",
        )
    }

    return try {
        // Try Termux python3 first, then system python3
        val pythonBin = findPythonBinary()
            ?: return ScriptExecutionResult(
                result = null,
                logs = emptyList(),
                error = "Python interpreter not found. Install Python via Termux (pkg install python) or disable the Python tool in settings.",
            )
        val process = ProcessBuilder(pythonBin, "-c", script)
            .apply {
                environment()["PYTHONIOENCODING"] = "utf-8"
                environment()["PYTHONUNBUFFERED"] = "1"
            }
            .start()
        process.outputStream.close()

        val stdoutFuture = scriptIoExecutor.submit(Callable {
            process.inputStream.readTextLimited(MAX_SCRIPT_OUTPUT_CHARS)
        })
        val stderrFuture = scriptIoExecutor.submit(Callable {
            process.errorStream.readTextLimited(MAX_SCRIPT_OUTPUT_CHARS)
        })

        val completed = process.waitFor(PYTHON_TIMEOUT_SECONDS, TimeUnit.SECONDS)

        if (!completed) {
            process.destroyForcibly()
            process.waitFor(1, TimeUnit.SECONDS)
        }

        val stdoutResult = stdoutFuture.getLimitedResult()
        val stderrResult = stderrFuture.getLimitedResult()
        val stdout = stdoutResult.toOutputText("stdout")
        val stderr = stderrResult.toOutputText("stderr")

        if (!completed) {
            return ScriptExecutionResult(
                result = stdout.ifBlank { null },
                logs = emptyList(),
                error = "Python script timed out after ${PYTHON_TIMEOUT_SECONDS} seconds.\n$stderr".trim(),
            )
        }

        val exitCode = process.exitValue()
        ScriptExecutionResult(
            result = stdout.ifBlank { null },
            logs = emptyList(),
            error = if (exitCode != 0) stderr.ifBlank { "Exit code: $exitCode" } else stderr.ifBlank { null },
        )
    } catch (e: Exception) {
        ScriptExecutionResult(
            result = null,
            logs = emptyList(),
            error = e.message ?: e.javaClass.simpleName,
        )
    }
}

class LocalTools(
    private val context: Context,
    private val eventBus: AppEventBus,
    private val hiddifyCoreManager: HiddifyCoreManager,
    private val deviceAgentManager: DeviceAgentManager,
    private val linuxEnvironmentManager: LinuxEnvironmentManager,
    private val settingsStore: SettingsStore,
    private val providerManager: ProviderManager,
    private val filesManager: FilesManager,
    private val genMediaRepository: GenMediaRepository,
) {
    val javascriptTool by lazy {
        Tool(
            name = "eval_javascript",
            description = """
                Execute JavaScript code using QuickJS engine (ES2020).
                The result is the value of the last expression in the code.
                For calculations with decimals, use toFixed() to control precision.
                Console output (log/info/warn/error) is captured and returned in 'logs' field.
                No DOM or Node.js APIs available.
                Example: '1 + 2' returns 3; 'const x = 5; x * 2' returns 10.
            """.trimIndent().replace("\n", " "),
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("code", buildJsonObject {
                            put("type", "string")
                            put("description", "The JavaScript code to execute")
                        })
                    },
                    required = listOf("code")
                )
            },
            execute = {
                val code = it.jsonObject["code"]?.jsonPrimitive?.contentOrNull
                val execResult = executeJavaScriptCode(code)
                val payload = buildJsonObject {
                    if (execResult.logs.isNotEmpty()) {
                        put("logs", JsonPrimitive(execResult.logs.joinToString("\n")))
                    }
                    put(
                        key = "result",
                        element = execResult.result?.let { r -> JsonPrimitive(r) } ?: JsonNull
                    )
                    execResult.error?.let { err ->
                        put("error", JsonPrimitive(err))
                    }
                }
                listOf(UIMessagePart.Text(payload.toString()))
            }
        )
    }

    val pythonTool by lazy {
        Tool(
            name = "eval_python",
            description = """
                Execute Python code using the device's Python interpreter.
                Requires Termux with Python installed (pkg install python).
                The result is the stdout output of the script.
                stderr is captured and returned in 'error' field if the exit code is non-zero.
                Example: 'print(1 + 2)' returns '3'.
            """.trimIndent().replace("\n", " "),
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("code", buildJsonObject {
                            put("type", "string")
                            put("description", "The Python code to execute")
                        })
                    },
                    required = listOf("code")
                )
            },
            execute = {
                val code = it.jsonObject["code"]?.jsonPrimitive?.contentOrNull
                val execResult = executePythonScript(code)
                val payload = buildJsonObject {
                    execResult.result?.let { r ->
                        put("result", JsonPrimitive(r))
                    }
                    execResult.error?.let { err ->
                        put("error", JsonPrimitive(err))
                    }
                }
                listOf(UIMessagePart.Text(payload.toString()))
            }
        )
    }

    val timeTool by lazy {
        Tool(
            name = "get_time_info",
            description = """
                Get the current local date and time info from the device.
                Returns year/month/day, weekday, ISO date/time strings, timezone, and timestamp.
            """.trimIndent().replace("\n", " "),
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject { }
                )
            },
            execute = {
                val now = ZonedDateTime.now()
                val date = now.toLocalDate()
                val time = now.toLocalTime().withNano(0)
                val weekday = now.dayOfWeek
                val payload = buildJsonObject {
                    put("year", date.year)
                    put("month", date.monthValue)
                    put("day", date.dayOfMonth)
                    put("weekday", weekday.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                    put("weekday_en", weekday.getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                    put("weekday_index", weekday.value)
                    put("date", date.toString())
                    put("time", time.toString())
                    put("datetime", now.withNano(0).toString())
                    put("timezone", now.zone.id)
                    put("utc_offset", now.offset.id)
                    put("timestamp_ms", now.toInstant().toEpochMilli())
                }
                listOf(UIMessagePart.Text(payload.toString()))
            }
        )
    }

    val clipboardTool by lazy {
        Tool(
            name = "clipboard_tool",
            description = """
                Read or write plain text from the device clipboard.
                Use action: read or write. For write, provide text.
                Do NOT write to the clipboard unless the user has explicitly requested it.
            """.trimIndent().replace("\n", " "),
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("action", buildJsonObject {
                            put("type", "string")
                            put(
                                "enum",
                                kotlinx.serialization.json.buildJsonArray {
                                    add("read")
                                    add("write")
                                }
                            )
                            put("description", "Operation to perform: read or write")
                        })
                        put("text", buildJsonObject {
                            put("type", "string")
                            put("description", "Text to write to the clipboard (required for write)")
                        })
                    },
                    required = listOf("action")
                )
            },
            execute = {
                val params = it.jsonObject
                val action = params["action"]?.jsonPrimitive?.contentOrNull ?: error("action is required")
                when (action) {
                    "read" -> {
                        val payload = buildJsonObject {
                            put("text", context.readClipboardText())
                        }
                        listOf(UIMessagePart.Text(payload.toString()))
                    }

                    "write" -> {
                        val text = params["text"]?.jsonPrimitive?.contentOrNull ?: error("text is required")
                        context.writeClipboardText(text)
                        val payload = buildJsonObject {
                            put("success", true)
                            put("text", text)
                        }
                        listOf(UIMessagePart.Text(payload.toString()))
                    }

                    else -> error("unknown action: $action, must be one of [read, write]")
                }
            }
        )
    }

    val ttsTool by lazy {
        Tool(
            name = "text_to_speech",
            description = """
                Speak text aloud to the user using the device's text-to-speech engine.
                Use this when the user asks you to read something aloud, or when audio output is appropriate.
                The tool returns immediately; audio plays in the background on the device.
                Provide natural, readable text without markdown formatting.
            """.trimIndent().replace("\n", " "),
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("text", buildJsonObject {
                            put("type", "string")
                            put("description", "The text to speak aloud")
                        })
                    },
                    required = listOf("text")
                )
            },
            execute = {
                val text = it.jsonObject["text"]?.jsonPrimitive?.contentOrNull
                    ?: error("text is required")
                eventBus.emit(AppEvent.Speak(text))
                val payload = buildJsonObject {
                    put("success", true)
                }
                listOf(UIMessagePart.Text(payload.toString()))
            }
        )
    }

    val askUserTool by lazy {
        Tool(
            name = "ask_user",
            description = """
                Ask the user one or more questions when you need clarification, additional information, or confirmation.
                Each question can optionally provide a list of suggested options for the user to choose from.
                The user may select an option or provide their own free-text answer for each question.
                The answers will be returned as a JSON object mapping question IDs to the user's responses.
            """.trimIndent().replace("\n", " "),
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("questions", buildJsonObject {
                            put("type", "array")
                            put("description", "List of questions to ask the user")
                            put("items", buildJsonObject {
                                put("type", "object")
                                put("properties", buildJsonObject {
                                    put("id", buildJsonObject {
                                        put("type", "string")
                                        put("description", "Unique identifier for this question")
                                    })
                                    put("question", buildJsonObject {
                                        put("type", "string")
                                        put("description", "The question text to display to the user")
                                    })
                                    put("options", buildJsonObject {
                                        put("type", "array")
                                        put(
                                            "description",
                                            "Optional list of suggested options for the user to choose from"
                                        )
                                        put("items", buildJsonObject {
                                            put("type", "string")
                                        })
                                    })
                                    put("selection_type", buildJsonObject {
                                        put("type", "string")
                                        put(
                                            "enum",
                                            kotlinx.serialization.json.buildJsonArray {
                                                add("text")
                                                add("single")
                                                add("multi")
                                            }
                                        )
                                        put(
                                            "description",
                                            "Answer type: text (free text input, default), single (select exactly one option), multi (select one or more options)"
                                        )
                                    })
                                })
                                put("required", kotlinx.serialization.json.buildJsonArray {
                                    add("id")
                                    add("question")
                                })
                            })
                        })
                    },
                    required = listOf("questions")
                )
            },
            needsApproval = true,
            execute = {
                error("ask_user tool should be handled by HITL flow")
            }
        )
    }

    val sshTool by lazy { SshTools.createSshExecuteTool() }

    val shellTool by lazy { ShellTools.createShellExecuteTool(context, linuxEnvironmentManager) }

    val linuxEnvironmentTool by lazy { ShellTools.createLinuxEnvironmentTool(linuxEnvironmentManager) }

    val trafficControlTool by lazy { TrafficControlTools.createTrafficControlTool(hiddifyCoreManager) }

    val deviceInfoTool by lazy { DeviceAgentTools.createDeviceInfoTool(deviceAgentManager) }

    val adbShellTool by lazy { DeviceAgentTools.createAdbShellTool(deviceAgentManager) }

    val videoGenerationTool by lazy {
        VideoGenerationTools.createVideoGenerationTool(
            settingsStore = settingsStore,
            providerManager = providerManager,
            filesManager = filesManager,
            genMediaRepository = genMediaRepository,
        )
    }

    fun getTools(options: List<LocalToolOption>): List<Tool> {
        val tools = mutableListOf<Tool>()
        if (options.contains(LocalToolOption.JavascriptEngine)) {
            tools.add(javascriptTool)
        }
        if (options.contains(LocalToolOption.PythonEngine)) {
            tools.add(pythonTool)
        }
        if (options.contains(LocalToolOption.TimeInfo)) {
            tools.add(timeTool)
        }
        if (options.contains(LocalToolOption.Clipboard)) {
            tools.add(clipboardTool)
        }
        if (options.contains(LocalToolOption.Tts)) {
            tools.add(ttsTool)
        }
        if (options.contains(LocalToolOption.AskUser)) {
            tools.add(askUserTool)
        }
        if (options.contains(LocalToolOption.Ssh)) {
            tools.add(sshTool)
        }
        if (options.contains(LocalToolOption.Shell)) {
            tools.add(shellTool)
        }
        if (options.contains(LocalToolOption.LinuxEnvironment)) {
            tools.add(linuxEnvironmentTool)
        }
        if (options.contains(LocalToolOption.TrafficControl)) {
            tools.add(trafficControlTool)
        }
        if (options.contains(LocalToolOption.DeviceAgent)) {
            tools.add(deviceInfoTool)
            tools.add(adbShellTool)
        }
        if (options.contains(LocalToolOption.VideoGeneration)) {
            tools.add(videoGenerationTool)
        }
        return tools
    }
}

private fun MutableList<String>.addBoundedLog(prefix: String, message: String?) {
    if (size >= MAX_SCRIPT_LOG_ENTRIES) {
        if (size == MAX_SCRIPT_LOG_ENTRIES) {
            add("[TRUNCATED] Console log limited to $MAX_SCRIPT_LOG_ENTRIES entries.")
        }
        return
    }
    add("$prefix ${(message ?: "null").limitScriptOutput("console log", MAX_SCRIPT_LOG_CHARS)}")
}

private fun String.limitScriptOutput(
    label: String,
    maxChars: Int = MAX_SCRIPT_OUTPUT_CHARS,
): String {
    if (length <= maxChars) return this
    return take(maxChars) + "\n[TRUNCATED] $label limited to $maxChars characters."
}

private data class LimitedText(
    val text: String,
    val truncated: Boolean,
)

private fun InputStream.readTextLimited(maxChars: Int): LimitedText {
    var truncated = false
    val builder = StringBuilder()
    bufferedReader(Charsets.UTF_8).use { reader ->
        val buffer = CharArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = reader.read(buffer)
            if (read < 0) break

            val remaining = maxChars - builder.length
            if (remaining > 0) {
                builder.append(buffer, 0, minOf(read, remaining))
            }
            if (read > remaining) {
                truncated = true
            }
        }
    }
    return LimitedText(builder.toString(), truncated)
}

private fun java.util.concurrent.Future<LimitedText>.getLimitedResult(): LimitedText {
    return runCatching {
        get(1, TimeUnit.SECONDS)
    }.getOrElse {
        LimitedText("", truncated = true)
    }
}

private fun LimitedText.toOutputText(label: String): String {
    if (!truncated) return text
    return text + "\n[TRUNCATED] Python $label limited to $MAX_SCRIPT_OUTPUT_CHARS characters."
}

private fun findPythonBinary(): String? {
    when (val cached = pythonBinaryCache.get()) {
        PYTHON_BINARY_MISSING -> return null
        PYTHON_BINARY_UNKNOWN -> Unit
        else -> return cached
    }

    synchronized(pythonBinaryCache) {
        when (val cached = pythonBinaryCache.get()) {
            PYTHON_BINARY_MISSING -> return null
            PYTHON_BINARY_UNKNOWN -> Unit
            else -> return cached
        }

        val discovered = discoverPythonBinary()
        pythonBinaryCache.set(discovered ?: PYTHON_BINARY_MISSING)
        return discovered
    }
}

private fun discoverPythonBinary(): String? {
    val candidates = listOf(
        "/data/data/com.termux/files/usr/bin/python3",
        "/data/data/com.termux/files/usr/bin/python",
        "python3",
        "python",
    )
    for (bin in candidates) {
        try {
            val process = ProcessBuilder(bin, "--version")
                .redirectErrorStream(true)
                .start()
            val completed = process.waitFor(2, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
            } else if (process.exitValue() == 0) {
                return bin
            }
        } catch (_: Exception) {
            // Not available
        }
    }
    return null
}

