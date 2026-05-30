package com.eterultimate.eteruee.data.ai.tools

import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.device.DeviceAgentManager
import com.eterultimate.eteruee.utils.JsonInstant
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object DeviceAgentTools {
    fun createDeviceInfoTool(manager: DeviceAgentManager): Tool = Tool(
        name = "device_info",
        description = """
            Read EterUee's device agent status and Android software/hardware summary.
            Can optionally include installed app package summaries.
            Does not execute commands.
        """.trimIndent().replace("\n", " "),
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("includeApps", buildJsonObject {
                        put("type", "boolean")
                        put("description", "Whether to include installed app package summaries")
                    })
                    put("includeSystemApps", buildJsonObject {
                        put("type", "boolean")
                        put("description", "Whether installed app summaries include system packages")
                    })
                    put("appLimit", buildJsonObject {
                        put("type", "integer")
                        put("description", "Maximum number of app summaries to include, 1..1000")
                    })
                }
            )
        },
        execute = { params ->
            val json = params.jsonObject
            val includeApps = json["includeApps"]?.jsonPrimitive?.booleanOrNull ?: false
            val includeSystemApps = json["includeSystemApps"]?.jsonPrimitive?.booleanOrNull ?: false
            val appLimit = json["appLimit"]?.jsonPrimitive?.intOrNull ?: 100
            val payload = buildJsonObject {
                put("device", JsonInstant.encodeToJsonElement(manager.getDeviceInfo()))
                if (includeApps) {
                    put(
                        "apps",
                        JsonInstant.encodeToJsonElement(
                            manager.listInstalledApps(
                                includeSystem = includeSystemApps,
                                limit = appLimit,
                            )
                        )
                    )
                }
            }
            listOf(UIMessagePart.Text(payload.toString()))
        }
    )

    fun createAdbShellTool(manager: DeviceAgentManager): Tool = Tool(
        name = "adb_shell_execute",
        description = """
            Execute a shell command on the Android device through Shizuku.
            Requires Shizuku to be running and EterUee to have Shizuku API permission.
            Commands run with Shizuku's server identity, normally ADB shell for wireless/USB debugging.
            Returns stdout, stderr, exit code, executor, shell, working directory, and server mode.
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
                        put("description", "Working directory for the command, default /data/local/tmp")
                    })
                    put("stdin", buildJsonObject {
                        put("type", "string")
                        put("description", "Standard input to pass to the command")
                    })
                    put("timeout", buildJsonObject {
                        put("type", "integer")
                        put("description", "Command timeout in seconds, 1..300, default 30")
                    })
                },
                required = listOf("command")
            )
        },
        needsApproval = true,
        execute = { params ->
            val json = params.jsonObject
            val command = json["command"]?.jsonPrimitive?.contentOrNull
                ?: error("command is required")
            val result = manager.executeAdbShell(
                command = command,
                workingDir = json["workingDir"]?.jsonPrimitive?.contentOrNull,
                stdin = json["stdin"]?.jsonPrimitive?.contentOrNull,
                timeoutSeconds = json["timeout"]?.jsonPrimitive?.intOrNull ?: 30,
            )
            listOf(UIMessagePart.Text(JsonInstant.encodeToString(result)))
        }
    )
}
