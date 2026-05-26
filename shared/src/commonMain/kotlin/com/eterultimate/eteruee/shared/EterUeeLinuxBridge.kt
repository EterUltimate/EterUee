package com.eterultimate.eteruee.shared

import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptEngine
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EterUeeLinuxBridge {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun runtimeName(): String = "EterUeeShared Linux"

    fun runtimeCapabilitiesJson(): String = EterUeeShared.runtimeCapabilitiesJson()

    fun supportedLinuxTargetsJson(): String = json.encodeToString(EterUeeShared.supportedLinuxTargets)

    fun sampleRoleplayPrompt(): String = RoleplayPromptEngine.samplePrompt()

    fun buildRoleplayPromptJson(requestJson: String): String = RoleplayPromptEngine.buildPromptJson(requestJson)
}
