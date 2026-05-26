package com.eterultimate.eteruee.shared.windows

import com.eterultimate.eteruee.shared.EterUeeWindowsBridge

fun main() {
    val capabilitiesJson = EterUeeWindowsBridge.runtimeCapabilitiesJson()
    require("\"platformFamily\":\"WINDOWS\"" in capabilitiesJson) {
        "Windows smoke expected WINDOWS platform family: $capabilitiesJson"
    }
    require("\"minimumOs\":\"Windows 11\"" in capabilitiesJson) {
        "Windows smoke expected Windows 11 minimum OS: $capabilitiesJson"
    }

    val targetJson = EterUeeWindowsBridge.supportedWindowsTargetsJson()
    require("\"kotlinTarget\":\"mingwX64\"" in targetJson) {
        "Windows smoke expected mingwX64 target: $targetJson"
    }
    require("Arcadia" in EterUeeWindowsBridge.sampleRoleplayPrompt()) {
        "Windows smoke expected shared roleplay prompt bridge to be callable"
    }

    val promptResultJson = EterUeeWindowsBridge.buildRoleplayPromptJson(
        """
        {
          "systemPrompt": "Stay in character.",
          "worldInfoEntries": [
            {
              "key": "Arcadia",
              "content": "Arcadia is a port city.",
              "position": "BEFORE_LAST_USER_MESSAGE"
            }
          ],
          "messages": [
            {
              "role": "USER",
              "content": "Where is Arcadia?"
            }
          ],
          "matchWorldInfoAgainst": "Arcadia"
        }
        """.trimIndent(),
    )
    require("\"injectedEntryCount\":1" in promptResultJson) {
        "Windows smoke expected roleplay world info injection: $promptResultJson"
    }
    require("Arcadia is a port city." in promptResultJson) {
        "Windows smoke expected roleplay prompt content: $promptResultJson"
    }

    println("EterUeeShared Windows 11 smoke OK")
}
