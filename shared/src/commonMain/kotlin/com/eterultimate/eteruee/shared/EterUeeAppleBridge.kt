package com.eterultimate.eteruee.shared

import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptEngine

object EterUeeAppleBridge {
    fun frameworkName(): String = EterUeeShared.frameworkName

    fun runtimeCapabilitiesJson(): String = EterUeeShared.runtimeCapabilitiesJson()

    fun sampleRoleplayPrompt(): String = RoleplayPromptEngine.samplePrompt()

    fun buildRoleplayPromptJson(requestJson: String): String = RoleplayPromptEngine.buildPromptJson(requestJson)
}
