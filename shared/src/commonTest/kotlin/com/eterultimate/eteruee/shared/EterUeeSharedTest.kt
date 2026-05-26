package com.eterultimate.eteruee.shared

import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptBuildRequest
import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptEngine
import com.eterultimate.eteruee.shared.roleplay.SharedChatMessage
import com.eterultimate.eteruee.shared.roleplay.SharedInsertionPosition
import com.eterultimate.eteruee.shared.roleplay.SharedMessageRole
import com.eterultimate.eteruee.shared.roleplay.SharedWorldInfoEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EterUeeSharedTest {
    @Test
    fun supportedAppleTargetsCoverIosIpadosAndMacos() {
        val targets = EterUeeShared.supportedAppleTargets.map { it.kotlinTarget }.toSet()

        assertTrue("iosArm64" in targets)
        assertTrue("iosSimulatorArm64" in targets)
        assertTrue("iosX64" in targets)
        assertTrue("macosArm64" in targets)
    }

    @Test
    fun supportedWindowsTargetIsWindows11Only() {
        assertEquals(
            listOf("mingwX64"),
            EterUeeShared.supportedWindowsTargets.map { it.kotlinTarget },
        )
        assertEquals("Windows 11", EterUeeShared.supportedWindowsTargets.single().minimumOs)
    }

    @Test
    fun supportedLinuxTargetCoversLinuxX64() {
        assertEquals(
            listOf("linuxX64"),
            EterUeeShared.supportedLinuxTargets.map { it.kotlinTarget },
        )
        assertEquals(PlatformFamily.LINUX, EterUeeShared.supportedLinuxTargets.single().family)
        assertEquals("glibc", EterUeeShared.supportedLinuxTargets.single().libc)
    }

    @Test
    fun frameworkNameIsStableForSwiftImport() {
        assertEquals("EterUeeShared", EterUeeShared.frameworkName)
    }

    @Test
    fun roleplayPromptInjectsWorldInfoBeforeLastUser() {
        val prompt = RoleplayPromptEngine.buildPrompt(
            systemPrompt = "Stay in character.",
            worldInfoEntries = listOf(
                SharedWorldInfoEntry(
                    key = "Arcadia",
                    content = "Arcadia is a port city.",
                    position = SharedInsertionPosition.BEFORE_LAST_USER_MESSAGE,
                ),
            ),
            messages = listOf(
                SharedChatMessage(SharedMessageRole.ASSISTANT, "Welcome."),
                SharedChatMessage(SharedMessageRole.USER, "Where is Arcadia?"),
            ),
        )

        assertTrue(prompt.contains("=== World Info ==="))
        assertTrue(prompt.indexOf("Arcadia is a port city.") < prompt.indexOf("User: Where is Arcadia?"))
    }

    @Test
    fun roleplayPromptJsonBridgeReturnsEncodedResult() {
        val resultJson = RoleplayPromptEngine.buildPromptJson(
            """
            {
              "systemPrompt": "Stay concise.",
              "worldInfoEntries": [
                {
                  "key": "Arcadia",
                  "content": "Arcadia is a port city.",
                  "order": 1
                }
              ],
              "messages": [
                {
                  "role": "USER",
                  "content": "Tell me about Arcadia."
                }
              ],
              "matchWorldInfoAgainst": "Arcadia"
            }
            """.trimIndent(),
        )

        assertTrue(resultJson.contains("Arcadia is a port city."))
        assertTrue(resultJson.contains("\"injectedEntryCount\":1"))
    }

    @Test
    fun appleBridgeExposesStableSmokeApis() {
        assertEquals("EterUeeShared", EterUeeAppleBridge.frameworkName())
        assertTrue(EterUeeAppleBridge.runtimeCapabilitiesJson().contains("appleTargets"))
        assertTrue(EterUeeAppleBridge.sampleRoleplayPrompt().contains("Arcadia"))
    }

    @Test
    fun windowsBridgeExposesStableSmokeApis() {
        assertEquals("EterUeeShared Windows", EterUeeWindowsBridge.runtimeName())
        assertTrue(EterUeeWindowsBridge.runtimeCapabilitiesJson().contains("windowsTargets"))
        assertTrue(EterUeeWindowsBridge.supportedWindowsTargetsJson().contains("Windows 11"))
        assertTrue(EterUeeWindowsBridge.sampleRoleplayPrompt().contains("Arcadia"))
    }

    @Test
    fun linuxBridgeExposesStableSmokeApis() {
        assertEquals("EterUeeShared Linux", EterUeeLinuxBridge.runtimeName())
        assertTrue(EterUeeLinuxBridge.runtimeCapabilitiesJson().contains("linuxTargets"))
        assertTrue(EterUeeLinuxBridge.supportedLinuxTargetsJson().contains("linuxX64"))
        assertTrue(EterUeeLinuxBridge.sampleRoleplayPrompt().contains("Arcadia"))
    }

    @Test
    fun roleplayPromptResultTracksTruncatedMessages() {
        val result = RoleplayPromptEngine.buildPrompt(
            RoleplayPromptBuildRequest(
                messages = listOf(
                    SharedChatMessage(SharedMessageRole.USER, "first"),
                    SharedChatMessage(SharedMessageRole.ASSISTANT, "second"),
                    SharedChatMessage(SharedMessageRole.USER, "last"),
                ),
                maxContextLength = 30,
            ),
        )

        assertEquals(2, result.truncatedMessageCount)
        assertTrue(result.prompt.contains("User: last"))
        assertTrue(!result.prompt.contains("User: first"))
        assertTrue(!result.prompt.contains("Assistant: second"))
    }
}
