package com.eterultimate.eteruee.data.ai.tools

import android.content.Context
import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.shell.LocalShellRunner
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object ShellTools {

    fun createShellExecuteTool(context: Context): Tool = Tool(
        name = "shell_execute",
        description = """
            Execute a shell command on the local device through EterUee's built-in Termux terminal layer.
            No external Termux app is required. Commands run in an app-local Android shell environment.
            Returns stdout, stderr, exit code, shell path, and working directory.
            Use for local file operations, system info, and app-scoped automation.
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
                        put("description", "Working directory for the command (default: EterUee app external files directory)")
                    })
                    put("stdin", buildJsonObject {
                        put("type", "string")
                        put("description", "Standard input to pass to the command")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "integer")
                        put("description", "Command execution timeout in seconds (default: 30)")
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

            val result = LocalShellRunner.executeBlocking(
                context = context,
                command = command,
                workingDir = workingDir,
                stdin = stdin,
                timeoutSeconds = timeout,
            )

            val payload = buildJsonObject {
                put("stdout", result.stdout)
                put("stderr", result.stderr)
                put("exitCode", result.exitCode)
                put("executor", result.executor)
                put("shell", result.shell)
                put("workingDir", result.workingDir)
                put("command", command)
            }
            listOf(UIMessagePart.Text(payload.toString()))
        }
    )
}
