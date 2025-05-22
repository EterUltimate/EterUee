package me.rerere.ai.ui.transformers

import android.content.Context
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.InputMessageTransformer
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.searchTextContent

object SearchTextTransformer : InputMessageTransformer {
    override suspend fun transform(
        context: Context,
        messages: List<UIMessage>,
        model: Model
    ): List<UIMessage> {
        // 找到最后一个带搜索结果的消息
        val lastSearchMessage =
            messages.lastOrNull { it.hasPart<UIMessagePart.Search>() } ?: return messages
        val lastMessageIndex = messages.indexOf(lastSearchMessage)

        if (lastMessageIndex > 0) {
            val prevMessageIndex = lastMessageIndex - 1 // 上一个消息的索引
            if (messages[prevMessageIndex].role == MessageRole.USER) { // 如果上一个消息是用户的话
                return messages.mapIndexed { index, message ->
                    if (index == prevMessageIndex) { // 把上一个消息的parts附加搜索结果
                        message.copy(
                            parts = message.parts.map { part ->
                                if (part is UIMessagePart.Text) {
                                    part.copy(
                                        text = buildString {
                                            append(part.text)
                                            appendLine()
                                            append(
                                                """
                                                <search>
                                                以下是基于这条消息的网络搜索结果，你可以使用搜索结构来更好的回答用户：
                                                <search_results>
                                                ${lastSearchMessage.parts.searchTextContent()}
                                                </search_results>
                                                在我给你的搜索结果中，每个结果都以类xml格式编码，index代表搜索结果序号，title代表搜索结果的标题，content代表搜索结果的内容，url代表搜索结果的链接。
                                                请在适当的情况下在句子末尾引用上下文。请按照引用编号[citation:X]的格式在答案中对应部分引用上下文。如果一句话源自多个上下文，请列出所有相关的引用编号，例如[citation:3][citation:5]，切记不要将引用集中在最后返回引用编号，而是在答案对应部分列出。
                                                注意以下注意事项：
                                                - 今天是{cur_date}
                                                - 并非搜索结果的所有内容都与用户的问题密切相关，你需要结合问题，对搜索结果进行甄别、筛选
                                                - 对于创作类的问题（如写论文），请务必在正文的段落中引用对应的参考编号，例如[citation:3][citation:5]，不能只在文章末尾引用。你需要解读并概括用户的题目要求，选择合适的格式，充分利用搜索结果并抽取重要信息，生成符合用户要求、极具思想深度、富有创造力与专业性的答案。你的创作篇幅需要尽可能延长，对于每一个要点的论述要推测用户的意图，给出尽可能多角度的回答要点，且务必信息量大、论述详尽。
                                                - 对于列举类的问题（如列举所有航班信息），尽量将答案控制在10个要点以内，并告诉用户可以查看搜索来源、获得完整信息。优先提供信息完整、最相关的列举项；如非必要，不要主动告诉用户搜索结果未提供的内容
                                                - 如果回答很长，请尽量结构化、分段落总结。如果需要分点作答，尽量控制在5个点以内，并合并相关的内容
                                                - 对于客观类的问答，如果问题的答案非常简短，可以适当补充一到两句相关信息，以丰富内容
                                                - 你需要根据用户要求和回答内容选择合适、美观的回答格式，确保可读性强
                                                - 除非用户要求，否则你回答的语言需要和用户提问的语言保持一致
                                                - 你的回答应该综合多个相关网页来回答，不能重复引用一个网页
                                                - 搜索结果不一定和用户发送的内容相关，仅供参考
                                                </search>
                                                """.trimIndent().trim()
                                            )
                                        }
                                    )
                                } else {
                                    part
                                }
                            }
                        )
                    } else {
                        message
                    }
                }
            }
        }

        return messages
    }
}