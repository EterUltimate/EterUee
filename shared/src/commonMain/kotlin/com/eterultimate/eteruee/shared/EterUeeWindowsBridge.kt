package com.eterultimate.eteruee.shared

import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EterUeeWindowsBridge {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun runtimeName(): String = "EterUeeShared Windows"

    fun runtimeCapabilitiesJson(): String = EterUeeShared.runtimeCapabilitiesJson()

    fun supportedWindowsTargetsJson(): String = json.encodeToString(EterUeeShared.supportedWindowsTargets)

    fun sampleRoleplayPrompt(): String = RoleplayPromptEngine.samplePrompt()

    fun buildRoleplayPromptJson(requestJson: String): String = RoleplayPromptEngine.buildPromptJson(requestJson)
}
