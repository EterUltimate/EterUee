package com.eterultimate.eteruee.ai.provider.providers

import com.eterultimate.eteruee.ai.ui.UIMessagePart

/**
 * 娑堟伅 parts 鎸夊伐鍏疯竟鐣屽垎缁勭殑缁撴灉
 * - Content: 鏅€氬唴瀹癸紙Text銆両mage銆丷easoning 绛夛級
 * - Tools: 杩炵画鐨勫凡鎵ц宸ュ叿
 */
internal sealed class PartGroup {
    data class Content(val parts: List<UIMessagePart>) : PartGroup()
    data class Tools(val tools: List<UIMessagePart.Tool>) : PartGroup()
}

/**
 * 灏嗘秷鎭?parts 鎸夊伐鍏疯竟鐣屽垎缁?
 *
 * 渚嬪 [Text1, Tool1, Tool2, Text2, Tool3] 浼氬垎缁勪负:
 * - Content([Text1])
 * - Tools([Tool1, Tool2])
 * - Content([Text2])
 * - Tools([Tool3])
 *
 * 杩欐牱鍙互纭繚 tool_use/functionCall 鍚庨潰绱ц窡 tool_result/functionResponse
 */
internal fun groupPartsByToolBoundary(parts: List<UIMessagePart>): List<PartGroup> {
    val groups = mutableListOf<PartGroup>()
    val currentContent = mutableListOf<UIMessagePart>()
    val currentTools = mutableListOf<UIMessagePart.Tool>()

    fun flushContent() {
        if (currentContent.isNotEmpty()) {
            groups.add(PartGroup.Content(currentContent.toList()))
            currentContent.clear()
        }
    }

    fun flushTools() {
        if (currentTools.isNotEmpty()) {
            groups.add(PartGroup.Tools(currentTools.toList()))
            currentTools.clear()
        }
    }

    for (part in parts) {
        if (part is UIMessagePart.Tool && part.isExecuted) {
            flushContent()
            currentTools.add(part)
        } else {
            flushTools()
            currentContent.add(part)
        }
    }

    flushContent()
    flushTools()
    return groups
}

