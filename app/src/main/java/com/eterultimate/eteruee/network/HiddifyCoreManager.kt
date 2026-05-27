package com.eterultimate.eteruee.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

private const val TAG = "HiddifyCoreManager"

class HiddifyCoreManager(
    private val context: Context,
) {
    data class CoreState(
        val isAvailable: Boolean = false,
        val isRunning: Boolean = false,
        val isLoading: Boolean = false,
        val bindingClassName: String? = null,
        val message: String? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(probeState())
    val state: StateFlow<CoreState> = _state.asStateFlow()

    fun refresh() {
        _state.value = probeState(isRunning = _state.value.isRunning)
    }

    suspend fun test(): String = withContext(Dispatchers.IO) {
        val binding = resolveBinding() ?: throw IllegalStateException(missingBindingMessage())
        val result = binding.test?.let { invoke(it) as? String }
            ?: binding.version?.let { invoke(it) as? String }
        result?.let { "Hiddify core binding loaded: $it" } ?: "Hiddify core binding loaded"
    }

    suspend fun start(
        configPath: String,
        configContent: String,
    ) = withContext(Dispatchers.IO) {
        val binding = resolveBinding() ?: throw IllegalStateException(missingBindingMessage())
        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            setup(binding)
            if (binding.start != null) {
                invoke(binding.start, configPath, configContent)
            } else {
                binding.checkConfig?.let { invoke(it, configContent) }
                error(
                    "Hiddify Libbox binding is available, but starting traffic control requires " +
                        "wiring Android VPN command server integration."
                )
            }
            _state.value = probeState(isRunning = true).copy(
                message = "Hiddify core started",
                error = null,
            )
        } catch (e: Exception) {
            _state.value = probeState(isRunning = false).copy(
                error = rootMessage(e),
            )
            throw e
        }
    }

    suspend fun stop() = withContext(Dispatchers.IO) {
        val binding = resolveBinding() ?: throw IllegalStateException(missingBindingMessage())
        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            binding.stop?.let { invoke(it) }
            _state.value = probeState(isRunning = false).copy(
                message = "Hiddify core stopped",
                error = null,
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = rootMessage(e))
            throw e
        }
    }

    suspend fun pause() = withContext(Dispatchers.IO) {
        resolveBinding()?.pause?.let { invoke(it) }
    }

    suspend fun wake() = withContext(Dispatchers.IO) {
        resolveBinding()?.wake?.let { invoke(it) }
    }

    fun defaultConfigPath(): String {
        return File(context.filesDir, "hiddify/config.json").absolutePath
    }

    private fun probeState(isRunning: Boolean = false): CoreState {
        val binding = resolveBinding()
        return CoreState(
            isAvailable = binding != null,
            isRunning = isRunning && binding != null,
            isLoading = false,
            bindingClassName = binding?.mobileClass?.name,
            message = if (binding != null) "Hiddify core binding is available" else null,
            error = if (binding == null) missingBindingMessage() else null,
        )
    }

    private fun setup(binding: Binding) {
        val setup = binding.setup ?: return
        val optionsClass = binding.setupOptionsClass ?: return
        val options = optionsClass.getDeclaredConstructor().newInstance()
        val baseDir = File(context.filesDir, "hiddify").apply { mkdirs() }
        val workingDir = File(baseDir, "working").apply { mkdirs() }
        val tempDir = File(context.cacheDir, "hiddify").apply { mkdirs() }

        setOption(options, "BasePath", baseDir.absolutePath)
        setOption(options, "WorkingDir", workingDir.absolutePath)
        setOption(options, "WorkingPath", workingDir.absolutePath)
        setOption(options, "TempDir", tempDir.absolutePath)
        setOption(options, "TempPath", tempDir.absolutePath)
        setOption(options, "Listen", "127.0.0.1:0")
        setOption(options, "CommandServerListenPort", 0)
        setOption(options, "Secret", "")
        setOption(options, "CommandServerSecret", "")
        setOption(options, "Debug", false)
        setOption(options, "Mode", 0)
        setOption(options, "FixAndroidStack", true)

        if (setup.parameterTypes.size == 1) {
            invoke(setup, options)
        } else {
            invoke(setup, options, null)
        }
    }

    private fun setOption(options: Any, name: String, value: Any) {
        val setterName = "set$name"
        val setter = options.javaClass.methods.firstOrNull { method ->
            method.name == setterName && method.parameterTypes.size == 1
        }
        if (setter != null) {
            setter.invoke(options, value)
            return
        }

        runCatching {
            val field = options.javaClass.getDeclaredField(name)
            field.isAccessible = true
            field.set(options, value)
        }.onFailure {
            Log.d(TAG, "Hiddify setup option $name is not exposed by gomobile binding")
        }
    }

    private fun resolveBinding(): Binding? {
        val mobileClass = MOBILE_CLASS_CANDIDATES.firstNotNullOfOrNull { name ->
            runCatching { Class.forName(name) }.getOrNull()
        } ?: return null

        val setupOptionsClass = SETUP_OPTIONS_CLASS_CANDIDATES.firstNotNullOfOrNull { name ->
            runCatching { Class.forName(name) }.getOrNull()
        }

        return Binding(
            mobileClass = mobileClass,
            setupOptionsClass = setupOptionsClass,
            setup = findMethod(mobileClass, listOf("setup", "Setup"), setOf(1, 2)),
            test = findMethod(mobileClass, listOf("test", "Test"), 0),
            version = findMethod(mobileClass, listOf("version", "Version"), 0),
            checkConfig = findMethod(mobileClass, listOf("checkConfig", "CheckConfig"), 1),
            start = findMethod(mobileClass, listOf("start", "Start"), 2),
            stop = findMethod(mobileClass, listOf("stop", "Stop"), 0),
            pause = findMethod(mobileClass, listOf("pause", "Pause"), 0),
            wake = findMethod(mobileClass, listOf("wake", "Wake"), 0),
        )
    }

    private fun findMethod(
        clazz: Class<*>,
        names: List<String>,
        parameterCount: Int,
    ): Method? = findMethod(clazz, names, setOf(parameterCount))

    private fun findMethod(
        clazz: Class<*>,
        names: List<String>,
        parameterCounts: Set<Int>,
    ): Method? {
        return clazz.methods.firstOrNull { method ->
            method.name in names && method.parameterTypes.size in parameterCounts
        }
    }

    private fun invoke(method: Method?, vararg args: Any?): Any? {
        requireNotNull(method) { "Hiddify core method is not available" }
        return try {
            method.invoke(null, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }
    }

    private fun missingBindingMessage(): String {
        return "hiddify-core.aar was not found. Build C:\\Users\\zacza\\Desktop\\x\\hiddify-core so bin\\hiddify-core.aar exists."
    }

    private fun rootMessage(error: Throwable): String {
        return error.cause?.message ?: error.message ?: error.javaClass.simpleName
    }

    private data class Binding(
        val mobileClass: Class<*>,
        val setupOptionsClass: Class<*>?,
        val setup: Method?,
        val test: Method?,
        val version: Method?,
        val checkConfig: Method?,
        val start: Method?,
        val stop: Method?,
        val pause: Method?,
        val wake: Method?,
    )

    private companion object {
        val MOBILE_CLASS_CANDIDATES = listOf(
            "com.hiddify.core.libbox.Libbox",
            "com.hiddify.core.mobile.Mobile",
            "com.hiddify.core.Mobile",
            "io.nekohasekai.mobile.Mobile",
            "io.nekohasekai.Mobile",
            "mobile.Mobile",
            "go.mobile.Mobile",
        )
        val SETUP_OPTIONS_CLASS_CANDIDATES = listOf(
            "com.hiddify.core.libbox.SetupOptions",
            "com.hiddify.core.mobile.SetupOptions",
            "com.hiddify.core.SetupOptions",
            "io.nekohasekai.mobile.SetupOptions",
            "io.nekohasekai.SetupOptions",
            "mobile.SetupOptions",
            "go.mobile.SetupOptions",
        )
    }
}
