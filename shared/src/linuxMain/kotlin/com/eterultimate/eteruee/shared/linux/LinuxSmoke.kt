package com.eterultimate.eteruee.shared.linux

import com.eterultimate.eteruee.shared.EterUeeLinuxBridge

fun main() {
    val capabilitiesJson = EterUeeLinuxBridge.runtimeCapabilitiesJson()
    require("\"platformFamily\":\"LINUX\"" in capabilitiesJson) {
        "Linux smoke expected LINUX platform family: $capabilitiesJson"
    }
    require("\"linuxTargets\"" in capabilitiesJson) {
        "Linux smoke expected Linux targets in capabilities: $capabilitiesJson"
    }

    val targetJson = EterUeeLinuxBridge.supportedLinuxTargetsJson()
    require("\"kotlinTarget\":\"linuxX64\"" in targetJson) {
        "Linux smoke expected linuxX64 target: $targetJson"
    }
    require("\"libc\":\"glibc\"" in targetJson) {
        "Linux smoke expected glibc runtime: $targetJson"
    }
    require("Arcadia" in EterUeeLinuxBridge.sampleRoleplayPrompt()) {
        "Linux smoke expected shared roleplay prompt bridge to be callable"
    }

    val promptResultJson = EterUeeLinuxBridge.buildRoleplayPromptJson(
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
        "Linux smoke expected roleplay world info injection: $promptResultJson"
    }
    require("Arcadia is a port city." in promptResultJson) {
        "Linux smoke expected roleplay prompt content: $promptResultJson"
    }

    println("EterUeeShared Linux x64 smoke OK")
}
