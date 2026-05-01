package com.eterultimate.eteruee.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.CustomBody
import me.rerere.ai.provider.CustomHeader
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.core.ReasoningLevel
import com.eterultimate.eteruee.data.ai.tools.LocalToolOption
import kotlin.uuid.Uuid

@Serializable
data class Assistant(
    val id: Uuid = Uuid.random(),
    val chatModelId: Uuid? = null, // 濡傛灉涓簄ull, 浣跨敤鍏ㄥ眬榛樿妯″瀷
    val name: String = "",
    val avatar: Avatar = Avatar.Dummy,
    val useAssistantAvatar: Boolean = false, // 浣跨敤鍔╂墜澶村儚鏇夸唬妯″瀷澶村儚
    val tags: List<Uuid> = emptyList(),
    val systemPrompt: String = "",
    val temperature: Float? = null,
    val topP: Float? = null,
    val contextMessageSize: Int = 0,
    val streamOutput: Boolean = true,
    val enableMemory: Boolean = false,
    val useGlobalMemory: Boolean = false, // 浣跨敤鍏ㄥ眬鍏变韩璁板繂鑰岄潪鍔╂墜闅旂璁板繂
    val enableRecentChatsReference: Boolean = false,
    val messageTemplate: String = "{{ message }}",
    val presetMessages: List<UIMessage> = emptyList(),
    val quickMessageIds: Set<Uuid> = emptySet(),
    val regexes: List<AssistantRegex> = emptyList(),
    val reasoningLevel: ReasoningLevel = ReasoningLevel.AUTO,
    val maxTokens: Int? = null,
    val customHeaders: List<CustomHeader> = emptyList(),
    val customBodies: List<CustomBody> = emptyList(),
    val mcpServers: Set<Uuid> = emptySet(),
    val localTools: List<LocalToolOption> = listOf(LocalToolOption.TimeInfo),
    val background: String? = null,
    val backgroundOpacity: Float = 1.0f,
    val modeInjectionIds: Set<Uuid> = emptySet(),      // 鍏宠仈鐨勬ā寮忔敞鍏?ID
    val lorebookIds: Set<Uuid> = emptySet(),            // 鍏宠仈鐨?Lorebook ID
    val enabledSkills: Set<String> = emptySet(),        // 鍚敤鐨?skill 鍚嶇О鍒楄〃
    val enableTimeReminder: Boolean = false,            // 鏃堕棿闂撮殧鎻愰啋娉ㄥ叆
)

@Serializable
data class QuickMessage(
    val id: Uuid = Uuid.random(),
    val title: String = "",
    val content: String = "",
)

@Serializable
data class AssistantMemory(
    val id: Int,
    val content: String = "",
)

@Serializable
enum class AssistantAffectScope {
    USER,
    ASSISTANT,
}

@Serializable
data class AssistantRegex(
    val id: Uuid,
    val name: String = "",
    val enabled: Boolean = true,
    val findRegex: String = "", // 姝ｅ垯琛ㄨ揪寮?
    val replaceString: String = "", // 鏇挎崲瀛楃涓?
    val affectingScope: Set<AssistantAffectScope> = setOf(),
    val visualOnly: Boolean = false, // 鏄惁浠呭湪瑙嗚涓婂奖鍝?
)

fun String.replaceRegexes(
    assistant: Assistant?,
    scope: AssistantAffectScope,
    visual: Boolean = false
): String {
    if (assistant == null) return this
    if (assistant.regexes.isEmpty()) return this
    return assistant.regexes.fold(this) { acc, regex ->
        if (regex.enabled && regex.visualOnly == visual && regex.affectingScope.contains(scope)) {
            try {
                val result = acc.replace(
                    regex = Regex(regex.findRegex),
                    replacement = regex.replaceString,
                )
                // println("Regex: ${regex.findRegex} -> ${result}")
                result
            } catch (e: Exception) {
                e.printStackTrace()
                // 濡傛灉姝ｅ垯琛ㄨ揪寮忔牸寮忛敊璇紝杩斿洖鍘熷瓧绗︿覆
                acc
            }
        } else {
            acc
        }
    }
}

/**
 * 娉ㄥ叆浣嶇疆
 */
@Serializable
enum class InjectionPosition {
    @SerialName("before_system_prompt")
    BEFORE_SYSTEM_PROMPT,   // 绯荤粺鎻愮ず璇嶄箣鍓?

    @SerialName("after_system_prompt")
    AFTER_SYSTEM_PROMPT,    // 绯荤粺鎻愮ず璇嶄箣鍚庯紙鏈€甯哥敤锛?

    @SerialName("top_of_chat")
    TOP_OF_CHAT,            // 瀵硅瘽鏈€寮€澶达紙绗竴鏉＄敤鎴锋秷鎭箣鍓嶏級

    @SerialName("bottom_of_chat")
    BOTTOM_OF_CHAT,         // 鏈€鏂版秷鎭箣鍓嶏紙褰撳墠鐢ㄦ埛杈撳叆涔嬪墠锛?

    @SerialName("at_depth")
    AT_DEPTH,               // 鍦ㄦ寚瀹氭繁搴︿綅缃彃鍏ワ紙浠庢渶鏂版秷鎭線鍓嶆暟锛?
}

/**
 * 鎻愮ず璇嶆敞鍏?
 *
 * - ModeInjection: 鍩轰簬妯″紡寮€鍏崇殑娉ㄥ叆锛堝瀛︿範妯″紡锛?
 * - RegexInjection: 鍩轰簬姝ｅ垯鍖归厤鐨勬敞鍏ワ紙Lorebook锛?
 */
@Serializable
sealed class PromptInjection {
    abstract val id: Uuid
    abstract val name: String
    abstract val enabled: Boolean
    abstract val priority: Int
    abstract val position: InjectionPosition
    abstract val content: String
    abstract val injectDepth: Int  // 褰?position 涓?AT_DEPTH 鏃朵娇鐢紝琛ㄧず浠庢渶鏂版秷鎭線鍓嶆暟鐨勪綅缃?
    abstract val role: MessageRole  // 娉ㄥ叆瑙掕壊锛歎SER 鎴?ASSISTANT

    /**
     * 妯″紡娉ㄥ叆 - 鍩轰簬寮€鍏崇姸鎬佽Е鍙?
     */
    @Serializable
    @SerialName("mode")
    data class ModeInjection(
        override val id: Uuid = Uuid.random(),
        override val name: String = "",
        override val enabled: Boolean = true,
        override val priority: Int = 0,
        override val position: InjectionPosition = InjectionPosition.AFTER_SYSTEM_PROMPT,
        override val content: String = "",
        override val injectDepth: Int = 4,
        override val role: MessageRole = MessageRole.USER,
    ) : PromptInjection()

    /**
     * 姝ｅ垯娉ㄥ叆 - 鍩轰簬鍐呭鍖归厤瑙﹀彂锛堜笘鐣屼功锛?
     */
    @Serializable
    @SerialName("regex")
    data class RegexInjection(
        override val id: Uuid = Uuid.random(),
        override val name: String = "",
        override val enabled: Boolean = true,
        override val priority: Int = 0,
        override val position: InjectionPosition = InjectionPosition.AFTER_SYSTEM_PROMPT,
        override val content: String = "",
        override val injectDepth: Int = 4,
        override val role: MessageRole = MessageRole.USER,
        val keywords: List<String> = emptyList(),  // 瑙﹀彂鍏抽敭璇?
        val useRegex: Boolean = false,             // 鏄惁浣跨敤姝ｅ垯鍖归厤
        val caseSensitive: Boolean = false,        // 澶у皬鍐欐晱鎰?
        val scanDepth: Int = 4,                    // 鎵弿鏈€杩慛鏉℃秷鎭?
        val constantActive: Boolean = false,       // 甯搁┗婵€娲伙紙鏃犻渶鍖归厤锛?
    ) : PromptInjection()
}

/**
 * Lorebook - 缁勭粐绠＄悊澶氫釜 RegexInjection
 */
@Serializable
data class Lorebook(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val description: String = "",
    val enabled: Boolean = true,
    val entries: List<PromptInjection.RegexInjection> = emptyList(),
)

/**
 * 妫€鏌?RegexInjection 鏄惁琚Е鍙?
 *
 * @param context 瑕佹壂鎻忕殑涓婁笅鏂囨枃鏈?
 * @return 鏄惁瑙﹀彂
 */
fun PromptInjection.RegexInjection.isTriggered(context: String): Boolean {
    if (!enabled) return false
    if (constantActive) return true
    if (keywords.isEmpty()) return false

    return keywords.any { keyword ->
        if (useRegex) {
            try {
                val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
                Regex(keyword, options).containsMatchIn(context)
            } catch (e: Exception) {
                false
            }
        } else {
            if (caseSensitive) {
                context.contains(keyword)
            } else {
                context.contains(keyword, ignoreCase = true)
            }
        }
    }
}

/**
 * 浠庢秷鎭垪琛ㄤ腑鎻愬彇鐢ㄤ簬鍖归厤鐨勪笂涓嬫枃鏂囨湰
 *
 * @param messages 娑堟伅鍒楄〃
 * @param scanDepth 鎵弿娣卞害锛堟渶杩慛鏉℃秷鎭級
 * @return 鎷兼帴鐨勬枃鏈唴瀹?
 */
fun extractContextForMatching(
    messages: List<UIMessage>,
    scanDepth: Int
): String {
    return messages
        .takeLast(scanDepth)
        .joinToString("\n") { it.toText() }
}

/**
 * 鑾峰彇鎵€鏈夎瑙﹀彂鐨勬敞鍏ワ紝鎸変紭鍏堢骇鎺掑簭
 *
 * @param injections 鎵€鏈夋敞鍏ヨ鍒?
 * @param context 涓婁笅鏂囨枃鏈?
 * @return 琚Е鍙戠殑娉ㄥ叆鍒楄〃锛屾寜浼樺厛绾ч檷搴忔帓鍒?
 */
fun getTriggeredInjections(
    injections: List<PromptInjection.RegexInjection>,
    context: String
): List<PromptInjection.RegexInjection> {
    return injections
        .filter { it.isTriggered(context) }
        .sortedByDescending { it.priority }
}

