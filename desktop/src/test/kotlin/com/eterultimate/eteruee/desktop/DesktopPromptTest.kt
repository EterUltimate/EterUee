package com.eterultimate.eteruee.desktop

import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptBuildRequest
import com.eterultimate.eteruee.shared.roleplay.RoleplayPromptEngine
import com.eterultimate.eteruee.shared.roleplay.SharedChatMessage
import com.eterultimate.eteruee.shared.roleplay.SharedMessageRole
import com.eterultimate.eteruee.shared.roleplay.SharedWorldInfoEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopPromptTest {
    @Test
    fun desktopRoleplayPreviewUsesSharedPromptEngine() {
        val result = RoleplayPromptEngine.buildPrompt(
            RoleplayPromptBuildRequest(
                systemPrompt = "Stay in character.",
                worldInfoEntries = listOf(
                    SharedWorldInfoEntry(
                        key = "Arcadia",
                        content = "Arcadia is visible from the desktop package smoke test.",
                    ),
                ),
                messages = listOf(
                    SharedChatMessage(
                        role = SharedMessageRole.USER,
                        content = "What happened in Arcadia?",
                    ),
                ),
                matchWorldInfoAgainst = "Arcadia",
            ),
        )

        assertEquals(1, result.injectedEntryCount)
        assertTrue(result.prompt.contains("Arcadia is visible"))
        assertTrue(result.prompt.contains("User: What happened in Arcadia?"))
    }
}
