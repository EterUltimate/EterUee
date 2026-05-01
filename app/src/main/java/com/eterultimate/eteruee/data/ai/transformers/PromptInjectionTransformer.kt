package com.eterultimate.eteruee.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import com.eterultimate.eteruee.data.model.Assistant
import com.eterultimate.eteruee.data.model.InjectionPosition
import com.eterultimate.eteruee.data.model.PromptInjection
import com.eterultimate.eteruee.data.model.Lorebook
import com.eterultimate.eteruee.data.model.extractContextForMatching
import com.eterultimate.eteruee.data.model.isTriggered

/**
 * 鎻愮ず璇嶆敞鍏ヨ浆鎹㈠櫒
 *
 * 鏍规嵁 Assistant 鍏宠仈鐨?ModeInjection 鍜?Lorebook 杩涜鎻愮ず璇嶆敞鍏?
 */
object PromptInjectionTransformer : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        return transformMessages(
            messages = messages,
            assistant = ctx.assistant,
            modeInjections = ctx.settings.modeInjections,
            lorebooks = ctx.settings.lorebooks
        )
    }
}

/**
 * 鏍稿績娉ㄥ叆閫昏緫锛堝彲娴嬭瘯鐨勭函鍑芥暟锛?
 */
internal fun transformMessages(
    messages: List<UIMessage>,
    assistant: Assistant,
    modeInjections: List<PromptInjection.ModeInjection>,
    lorebooks: List<Lorebook>
): List<UIMessage> {
    // 鏀堕泦鎵€鏈夐渶瑕佹敞鍏ョ殑鍐呭
    val injections = collectInjections(
        messages = messages,
        assistant = assistant,
        modeInjections = modeInjections,
        lorebooks = lorebooks
    )

    if (injections.isEmpty()) {
        return messages
    }

    // 鎸変綅缃拰浼樺厛绾у垎缁?
    val byPosition = injections
        .sortedByDescending { it.priority }
        .groupBy { it.position }

    // 搴旂敤娉ㄥ叆
    return applyInjections(messages, byPosition)
}

/**
 * 鏀堕泦闇€瑕佹敞鍏ョ殑鍐呭
 */
internal fun collectInjections(
    messages: List<UIMessage>,
    assistant: Assistant,
    modeInjections: List<PromptInjection.ModeInjection>,
    lorebooks: List<Lorebook>
): List<PromptInjection> {
    val injections = mutableListOf<PromptInjection>()

    // 1. 鑾峰彇鍏宠仈鐨?ModeInjection
    modeInjections
        .filter { it.enabled && assistant.modeInjectionIds.contains(it.id) }
        .forEach { injections.add(it) }

    // 2. 鑾峰彇鍏宠仈鐨?Lorebook 涓瑙﹀彂鐨?RegexInjection
    val enabledLorebooks = lorebooks.filter {
        it.enabled && assistant.lorebookIds.contains(it.id)
    }
    if (enabledLorebooks.isNotEmpty()) {
        // 鎻愬彇涓婁笅鏂囩敤浜庡尮閰嶏紙鍙彇闈?SYSTEM 娑堟伅锛?
        val nonSystemMessages = messages.filter { it.role != MessageRole.SYSTEM }

        enabledLorebooks.forEach { lorebook ->
            lorebook.entries
                .filter { entry ->
                    val context = extractContextForMatching(nonSystemMessages, entry.scanDepth)
                    entry.isTriggered(context)
                }
                .forEach { injections.add(it) }
        }
    }

    return injections
}

/**
 * 搴旂敤娉ㄥ叆鍒版秷鎭垪琛?
 */
internal fun applyInjections(
    messages: List<UIMessage>,
    byPosition: Map<InjectionPosition, List<PromptInjection>>
): List<UIMessage> {
    val result = messages.toMutableList()

    // 鎵惧埌绯荤粺娑堟伅鐨勭储寮曪紙閫氬父鏄涓€鏉★級
    val systemIndex = result.indexOfFirst { it.role == MessageRole.SYSTEM }

    // 澶勭悊 BEFORE_SYSTEM_PROMPT 鍜?AFTER_SYSTEM_PROMPT
    if (systemIndex >= 0) {
        val beforeContent = byPosition[InjectionPosition.BEFORE_SYSTEM_PROMPT]
            ?.joinToString("\n") { it.content } ?: ""
        val afterContent = byPosition[InjectionPosition.AFTER_SYSTEM_PROMPT]
            ?.joinToString("\n") { it.content } ?: ""

        if (beforeContent.isNotEmpty() || afterContent.isNotEmpty()) {
            val systemMessage = result[systemIndex]
            val originalText = systemMessage.parts
                .filterIsInstance<UIMessagePart.Text>()
                .joinToString("") { it.text }

            val newText = buildString {
                if (beforeContent.isNotEmpty()) {
                    append(beforeContent)
                    appendLine()
                }
                append(originalText)
                if (afterContent.isNotEmpty()) {
                    appendLine()
                    append(afterContent)
                }
            }

            result[systemIndex] = systemMessage.copy(
                parts = listOf(UIMessagePart.Text(newText))
            )
        }
    } else {
        // 娌℃湁绯荤粺娑堟伅鏃讹紝鍒涘缓涓€涓柊鐨勭郴缁熸秷鎭?
        val beforeContent = byPosition[InjectionPosition.BEFORE_SYSTEM_PROMPT]
            ?.joinToString("\n") { it.content } ?: ""
        val afterContent = byPosition[InjectionPosition.AFTER_SYSTEM_PROMPT]
            ?.joinToString("\n") { it.content } ?: ""

        val combinedContent = buildString {
            if (beforeContent.isNotEmpty()) {
                append(beforeContent)
            }
            if (afterContent.isNotEmpty()) {
                if (isNotEmpty()) appendLine()
                append(afterContent)
            }
        }

        if (combinedContent.isNotEmpty()) {
            result.add(0, UIMessage.system(combinedContent))
        }
    }

    // 澶勭悊 TOP_OF_CHAT锛氬湪绗竴鏉＄敤鎴锋秷鎭箣鍓嶆彃鍏?
    val topInjections = byPosition[InjectionPosition.TOP_OF_CHAT]
    if (!topInjections.isNullOrEmpty()) {
        // 閲嶆柊璁＄畻绱㈠紩锛堝洜涓哄彲鑳芥彃鍏ヤ簡绯荤粺娑堟伅锛?
        var insertIndex = result.indexOfFirst { it.role == MessageRole.USER }
            .takeIf { it >= 0 } ?: result.size
        insertIndex = findSafeInsertIndex(result, insertIndex)
        createMergedInjectionMessages(topInjections).forEach { message ->
            result.add(insertIndex, message)
            insertIndex++
        }
    }

    // 澶勭悊 BOTTOM_OF_CHAT锛氬湪鏈€鍚庝竴鏉℃秷鎭箣鍓嶆彃鍏?
    val bottomInjections = byPosition[InjectionPosition.BOTTOM_OF_CHAT]
    if (!bottomInjections.isNullOrEmpty()) {
        var insertIndex = (result.size - 1).coerceAtLeast(0)
        insertIndex = findSafeInsertIndex(result, insertIndex)
        createMergedInjectionMessages(bottomInjections).forEach { message ->
            result.add(insertIndex, message)
            insertIndex++
        }
    }

    // 澶勭悊 AT_DEPTH锛氬湪鎸囧畾娣卞害浣嶇疆鎻掑叆锛堜粠鏈€鏂版秷鎭線鍓嶆暟锛?
    // 鎸?injectDepth 鍒嗙粍锛岀浉鍚屾繁搴︾殑鍚堝苟锛屾寜娣卞害浠庡ぇ鍒板皬澶勭悊锛堥伩鍏嶇储寮曞彉鍖栭棶棰橈級
    val atDepthInjections = byPosition[InjectionPosition.AT_DEPTH]
    if (!atDepthInjections.isNullOrEmpty()) {
        val byDepth = atDepthInjections.groupBy { it.injectDepth }
        byDepth.keys.sortedDescending().forEach { depth ->
            val injections = byDepth[depth] ?: return@forEach
            // 璁＄畻鎻掑叆浣嶇疆锛歳esult.size - depth锛屼絾瑕佺‘淇濆湪鏈夋晥鑼冨洿鍐?
            // depth=1 琛ㄧず鍦ㄦ渶鍚庝竴鏉℃秷鎭箣鍓嶏紝depth=2 琛ㄧず鍦ㄥ€掓暟绗簩鏉′箣鍓?..
            var insertIndex = (result.size - depth.coerceAtLeast(1)).coerceIn(0, result.size)
            insertIndex = findSafeInsertIndex(result, insertIndex)
            createMergedInjectionMessages(injections).forEach { message ->
                result.add(insertIndex, message)
                insertIndex++
            }
        }
    }

    return result
}

/**
 * 灏嗗悓涓€ role 鐨勬敞鍏ュ悎骞舵垚娑堟伅鍒楄〃
 * 鎸?role 鍒嗙粍鍚庡悎骞跺唴瀹癸紝杩斿洖鍚堝苟鍚庣殑娑堟伅鍒楄〃
 */
private fun createMergedInjectionMessages(injections: List<PromptInjection>): List<UIMessage> {
    return injections
        .groupBy { it.role }
        .map { (role, grouped) ->
            val mergedContent = grouped.joinToString("\n") { it.content }
            when (role) {
                MessageRole.ASSISTANT -> UIMessage.assistant(mergedContent)
                else -> UIMessage.user(mergedContent)
            }
        }
}

/**
 * 鏌ユ壘瀹夊叏鐨勬彃鍏ヤ綅缃紝閬垮厤娉ㄥ叆鍒?USER 鈫?ASSISTANT(鍚玊ool) 涔嬮棿
 *
 * 鏌愪簺鎻愪緵鍟嗭紙濡?deepseek锛夎姹?USER 涔嬪悗绱ц窡甯﹀伐鍏风殑 ASSISTANT锛?
 * 鍦ㄤ袱鑰呬箣闂存彃鍏ユ秷鎭細瀵艰嚧鎶ラ敊鎴栫牬鍧忔帹鐞嗚繛缁€с€?
 */
internal fun findSafeInsertIndex(messages: List<UIMessage>, targetIndex: Int): Int {
    var index = targetIndex.coerceIn(0, messages.size)

    // 鍚戝墠鏌ユ壘锛岀洿鍒版壘鍒颁竴涓畨鍏ㄧ殑浣嶇疆
    while (index > 0) {
        val prevMessage = messages.getOrNull(index - 1)
        val currentMessage = messages.getOrNull(index)

        // 涓嶈兘鎻掑叆鍒?USER 鈫?ASSISTANT(鍚玊ool) 涔嬮棿
        val isPrevUser = prevMessage?.role == MessageRole.USER
        val isCurrentAssistantWithTools = currentMessage?.role == MessageRole.ASSISTANT
            && currentMessage.getTools().isNotEmpty()

        if (isPrevUser && isCurrentAssistantWithTools) {
            index--
        } else {
            break
        }
    }

    return index
}

