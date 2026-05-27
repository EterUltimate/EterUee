package com.eterultimate.eteruee.shell

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val TAG = "LocalShellRunner"

object LocalShellRunner {
    const val SHELL_PATH = "/system/bin/sh"
    const val EXECUTOR_NAME = "eteruee-termux-local"

    private val executor = Executors.newCachedThreadPool()

    data class ShellResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val executor: String,
        val shell: String,
        val workingDir: String,
    )

    fun defaultWorkingDir(context: Context): File {
        return context.getExternalFilesDir(null) ?: context.filesDir
    }

    fun defaultEnvironment(context: Context): Map<String, String> {
        val home = context.filesDir.absolutePath
        return mapOf(
            "HOME" to home,
            "TMPDIR" to context.cacheDir.absolutePath,
            "PATH" to "/system/bin:/system/xbin",
            "SHELL" to SHELL_PATH,
            "TERM" to "xterm-256color",
            "LANG" to "en_US.UTF-8",
        )
    }

    fun environmentArray(context: Context): Array<String> {
        return defaultEnvironment(context)
            .map { (key, value) -> "$key=$value" }
            .sorted()
            .toTypedArray()
    }

    fun executeBlocking(
        context: Context,
        command: String,
        workingDir: String? = null,
        stdin: String? = null,
        timeoutSeconds: Int = 30,
    ): ShellResult {
        val cwd = workingDir
            ?.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?: defaultWorkingDir(context)

        return try {
            if (!cwd.exists()) cwd.mkdirs()
            val process = ProcessBuilder(SHELL_PATH, "-c", command)
                .directory(cwd)
                .apply {
                    environment().putAll(defaultEnvironment(context))
                }
                .start()

            if (stdin != null) {
                process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writer.write(stdin)
                }
            } else {
                process.outputStream.close()
            }

            val stdoutFuture = executor.submit(Callable {
                process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            })
            val stderrFuture = executor.submit(Callable {
                process.errorStream.bufferedReader(Charsets.UTF_8).readText()
            })

            val safeTimeout = timeoutSeconds.coerceAtLeast(1).toLong()
            val completed = process.waitFor(safeTimeout, TimeUnit.SECONDS)
            if (!completed) {
                process.destroyForcibly()
            }

            val stdout = runCatching {
                stdoutFuture.get(1, TimeUnit.SECONDS)
            }.getOrDefault("")
            val stderr = runCatching {
                stderrFuture.get(1, TimeUnit.SECONDS)
            }.getOrDefault("")

            if (!completed) {
                ShellResult(
                    stdout = stdout,
                    stderr = stderr + "\n[TIMEOUT] Command timed out after ${safeTimeout}s",
                    exitCode = -1,
                    executor = EXECUTOR_NAME,
                    shell = SHELL_PATH,
                    workingDir = cwd.absolutePath,
                )
            } else {
                ShellResult(
                    stdout = stdout,
                    stderr = stderr,
                    exitCode = process.exitValue(),
                    executor = EXECUTOR_NAME,
                    shell = SHELL_PATH,
                    workingDir = cwd.absolutePath,
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Shell execution failed", e)
            ShellResult(
                stdout = "",
                stderr = e.message ?: e.javaClass.simpleName,
                exitCode = -1,
                executor = EXECUTOR_NAME,
                shell = SHELL_PATH,
                workingDir = cwd.absolutePath,
            )
        }
    }
}
