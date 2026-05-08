package com.eterultimate.eteruee.data.ai.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object ShellTools {

    private const val TAG = "ShellTools"
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val TERMUX_RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"

    private val executor = Executors.newCachedThreadPool()

    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun createShellExecuteTool(context: Context): Tool = Tool(
        name = "shell_execute",
        description = """
            Execute a shell command on the local device.
            If Termux is installed, commands are sent to Termux for full Linux tool support.
            Otherwise, commands run via Android Runtime.exec with limited shell support.
            Returns stdout, stderr, and exit code.
            Use for file operations, system info, package management, etc.
        """.trimIndent().replace("\n", " "),
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "The shell command to execute")
                    })
                    put("workingDir", buildJsonObject {
                        put("type", "string")
                        put("description", "Working directory for the command (Termux only, default: \$HOME)")
                    })
                    put("stdin", buildJsonObject {
                        put("type", "string")
                        put("description", "Standard input to pass to the command (Termux only)")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "integer")
                        put("description", "Command execution timeout in seconds (default: 30, non-Termux only)")
                        put("default", 30)
                    })
                },
                required = listOf("command")
            )
        },
        needsApproval = true,
        execute = { params ->
            val jsonObject = params.jsonObject
            val command = jsonObject["command"]?.jsonPrimitive?.contentOrNull
                ?: error("command is required")
            val workingDir = jsonObject["workingDir"]?.jsonPrimitive?.contentOrNull
            val stdin = jsonObject["stdin"]?.jsonPrimitive?.contentOrNull
            val timeout = jsonObject["timeout"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 30

            val result = if (isTermuxInstalled(context)) {
                executeViaTermux(context, command, workingDir, stdin)
            } else {
                executeViaRuntime(command, timeout)
            }

            val payload = buildJsonObject {
                put("stdout", result.stdout)
                put("stderr", result.stderr)
                put("exitCode", result.exitCode)
                put("executor", result.executor)
                put("command", command)
            }
            listOf(UIMessagePart.Text(payload.toString()))
        }
    )

    private data class ShellResult(
        val stdout: String,
        val stderr: String,
        val exitCode: Int,
        val executor: String,
    )

    private fun executeViaTermux(
        context: Context,
        command: String,
        workingDir: String?,
        stdin: String?,
    ): ShellResult {
        return try {
            val intent = Intent().apply {
                setClassName(TERMUX_PACKAGE, TERMUX_RUN_COMMAND_SERVICE)
                action = TERMUX_RUN_COMMAND_ACTION
                putExtra("com.termux.RUN_COMMAND_PATH", "/usr/bin/bash")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                if (!workingDir.isNullOrBlank()) {
                    putExtra("com.termux.RUN_COMMAND_WORKDIR", workingDir)
                }
                if (!stdin.isNullOrBlank()) {
                    putExtra("com.termux.RUN_COMMAND_STDIN", stdin)
                }
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            context.startService(intent)
            ShellResult(
                stdout = "",
                stderr = "Command sent to Termux. Output is displayed in the Termux terminal session.",
                exitCode = 0,
                executor = "termux",
            )
        } catch (e: Exception) {
            Log.e(TAG, "Termux execution failed, falling back to Runtime.exec", e)
            executeViaRuntime(command, 30)
        }
    }

    private fun executeViaRuntime(command: String, timeoutSeconds: Int): ShellResult {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.outputStream.close()

            val stdoutFuture = executor.submit(Callable {
                process.inputStream.bufferedReader(Charsets.UTF_8).readText()
            })
            val stderrFuture = executor.submit(Callable {
                process.errorStream.bufferedReader(Charsets.UTF_8).readText()
            })

            val completed = process.waitFor(timeoutSeconds.toLong(), TimeUnit.SECONDS)

            val stdout = try { stdoutFuture.get(timeoutSeconds.toLong(), TimeUnit.SECONDS) } catch (_: Exception) { "" }
            val stderr = try { stderrFuture.get(timeoutSeconds.toLong(), TimeUnit.SECONDS) } catch (_: Exception) { "" }

            if (!completed) {
                process.destroyForcibly()
                ShellResult(
                    stdout = stdout,
                    stderr = stderr + "\n[TIMEOUT] Command timed out after ${timeoutSeconds}s",
                    exitCode = -1,
                    executor = "runtime",
                )
            } else {
                ShellResult(
                    stdout = stdout,
                    stderr = stderr,
                    exitCode = process.exitValue(),
                    executor = "runtime",
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Runtime execution failed", e)
            ShellResult(
                stdout = "",
                stderr = e.message ?: e.javaClass.simpleName,
                exitCode = -1,
                executor = "runtime",
            )
        }
    }
}
