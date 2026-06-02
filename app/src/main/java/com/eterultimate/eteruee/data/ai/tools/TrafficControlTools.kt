package com.eterultimate.eteruee.data.ai.tools

import com.eterultimate.eteruee.ai.core.InputSchema
import com.eterultimate.eteruee.ai.core.Tool
import com.eterultimate.eteruee.ai.ui.UIMessagePart
import com.eterultimate.eteruee.network.HiddifyCoreManager
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

object TrafficControlTools {
    fun createTrafficControlTool(manager: HiddifyCoreManager): Tool = Tool(
        name = "traffic_control",
        description = """
            Inspect and control EterUee's local Hiddify Core traffic manager.
            Actions: status, test, start, stop, pause, wake.
            start requires configContent and optionally configPath.
        """.trimIndent().replace("\n", " "),
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("action", buildJsonObject {
                        put("type", "string")
                        put("enum", buildJsonArray {
                            add("status")
                            add("test")
                            add("start")
                            add("stop")
                            add("pause")
                            add("wake")
                        })
                        put("description", "Traffic control action")
                    })
                    put("configPath", buildJsonObject {
                        put("type", "string")
                        put("description", "Config path for start; defaults to EterUee's Hiddify config path")
                    })
                    put("configContent", buildJsonObject {
                        put("type", "string")
                        put("description", "Hiddify JSON config content for start")
                    })
                },
                required = listOf("action")
            )
        },
        needsApproval = true,
        execute = { params ->
            val json = params.jsonObject
            val action = json["action"]?.jsonPrimitive?.contentOrNull
                ?: error("action is required")
            val result = when (action) {
                "status" -> manager.statePayload(action)
                "test" -> buildJsonObject {
                    put("action", action)
                    put("result", manager.test())
                    put("state", manager.statePayload("status"))
                }
                "start" -> {
                    val configContent = json["configContent"]?.jsonPrimitive?.contentOrNull
                        ?: error("configContent is required for start")
                    val configPath = json["configPath"]?.jsonPrimitive?.contentOrNull
                        ?: manager.defaultConfigPath()
                    manager.start(configPath, configContent)
                    manager.statePayload(action)
                }
                "stop" -> {
                    manager.stop()
                    manager.statePayload(action)
                }
                "pause" -> {
                    manager.pause()
                    manager.statePayload(action)
                }
                "wake" -> {
                    manager.wake()
                    manager.statePayload(action)
                }
                else -> error("unknown action: $action")
            }
            listOf(UIMessagePart.Text(result.toString()))
        }
    )

    private fun HiddifyCoreManager.statePayload(action: String) = buildJsonObject {
        val state = state.value
        put("action", action)
        put("available", state.isAvailable)
        put("running", state.isRunning)
        put("loading", state.isLoading)
        put("bindingClassName", state.bindingClassName?.let(::JsonPrimitive) ?: JsonNull)
        put("message", state.message?.let(::JsonPrimitive) ?: JsonNull)
        put("error", state.error?.let(::JsonPrimitive) ?: JsonNull)
        put("defaultConfigPath", defaultConfigPath())
    }
}
