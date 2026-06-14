package com.eterultimate.eteruee.data.ai.tools

import android.content.Context
import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.linux.LinuxEnvironmentManager
import com.eterultimate.eteruee.shell.LocalShellRunner
import com.eterultimate.eteruee.utils.JsonInstant
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object ShellTools {

    fun createShellExecuteTool(
        context: Context,
        linuxEnvironmentManager: LinuxEnvironmentManager,
    ): Tool = Tool(
        name = "shell_execute",
        description = """
            Execute a shell command on the local device through EterUee's built-in Termux terminal layer.
            No external Termux app is required. By default, EterUee uses the managed Linux/proot
            environment when it is installed; otherwise it falls back to the app-local Android shell.
            Set distribution to ubuntu to target the optional Ubuntu/proot environment.
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
                    put("environment", buildJsonObject {
                        put("type", "string")
                        put("description", "Execution environment: auto, linux, or android. Default: auto")
                        put("default", "auto")
                    })
                    put("distribution", buildJsonObject {
                        put("type", "string")
                        put("description", "Linux distribution for proot execution: arch or ubuntu. Default: arch")
                        put("default", "arch")
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
            val timeout = jsonObject["timeout"]?.jsonPrimitive?.intOrNull
                ?: jsonObject["timeout"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                ?: 30
            val environment = jsonObject["environment"]?.jsonPrimitive?.contentOrNull
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
                ?: "auto"
            val distribution = jsonObject["distribution"]?.jsonPrimitive?.contentOrNull
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
                ?: "arch"

            val linuxStatus = linuxEnvironmentManager.getStatus(distribution)
            if (environment !in setOf("auto", "linux", "android")) {
                error("environment must be one of: auto, linux, android")
            }

            when {
                environment != "android" && linuxStatus.canExecuteLinux -> {
                    val result = linuxEnvironmentManager.execute(
                        distribution = distribution,
                        command = command,
                        workingDir = workingDir,
                        stdin = stdin,
                        timeoutSeconds = timeout,
                    )
                    listOf(UIMessagePart.Text(JsonInstant.encodeToString(result)))
                }

                environment == "linux" -> {
                    listOf(UIMessagePart.Text(JsonInstant.encodeToString(linuxStatus)))
                }

                else -> {
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
                        put("linux", JsonInstant.encodeToJsonElement(linuxStatus))
                    }
                    listOf(UIMessagePart.Text(payload.toString()))
                }
            }
        }
    )

    fun createLinuxEnvironmentTool(
        linuxEnvironmentManager: LinuxEnvironmentManager,
    ): Tool = Tool(
        name = "linux_environment",
        description = """
            Inspect or prepare EterUee's managed Linux environment for LLM tools.
            Actions: status returns rootfs/proot readiness; prepare writes the installer helper;
            install downloads the Termux proot runtime and extracts the selected rootfs after approval;
            exec runs a command inside the selected Linux distribution when installation is complete.
        """.trimIndent().replace("\n", " "),
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("action", buildJsonObject {
                        put("type", "string")
                        put("description", "Action: status, prepare, install, or exec")
                    })
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "Command to run when action is exec")
                    })
                    put("workingDir", buildJsonObject {
                        put("type", "string")
                        put("description", "Linux working directory for exec, default /root")
                    })
                    put("stdin", buildJsonObject {
                        put("type", "string")
                        put("description", "Standard input for exec")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "integer")
                        put("description", "Timeout in seconds. Default 60 for exec, up to 600 for install")
                    })
                    put("distribution", buildJsonObject {
                        put("type", "string")
                        put("description", "Linux distribution: arch or ubuntu. Default: arch")
                        put("default", "arch")
                    })
                },
                required = listOf("action")
            )
        },
        needsApproval = true,
        execute = { params ->
            val json = params.jsonObject
            val distribution = json["distribution"]?.jsonPrimitive?.contentOrNull
                ?.lowercase()
                ?.takeIf { it.isNotBlank() }
                ?: "arch"
            when (val action = json["action"]?.jsonPrimitive?.contentOrNull) {
                "status" -> listOf(
                    UIMessagePart.Text(JsonInstant.encodeToString(linuxEnvironmentManager.getStatus(distribution)))
                )

                "prepare" -> listOf(
                    UIMessagePart.Text(
                        JsonInstant.encodeToString(linuxEnvironmentManager.prepareInstallerScript(distribution))
                    )
                )

                "install" -> listOf(
                    UIMessagePart.Text(
                        JsonInstant.encodeToString(
                            linuxEnvironmentManager.install(
                                distribution = distribution,
                                timeoutSeconds = json["timeout"]?.jsonPrimitive?.intOrNull ?: 600,
                            )
                        )
                    )
                )

                "exec" -> {
                    val command = json["command"]?.jsonPrimitive?.contentOrNull
                        ?: error("command is required when action is exec")
                    listOf(
                        UIMessagePart.Text(
                            JsonInstant.encodeToString(
                                linuxEnvironmentManager.execute(
                                    distribution = distribution,
                                    command = command,
                                    workingDir = json["workingDir"]?.jsonPrimitive?.contentOrNull,
                                    stdin = json["stdin"]?.jsonPrimitive?.contentOrNull,
                                    timeoutSeconds = json["timeout"]?.jsonPrimitive?.intOrNull ?: 60,
                                )
                            )
                        )
                    )
                }

                else -> error("action must be one of: status, prepare, install, exec; got $action")
            }
        }
    )
}
