package com.eterultimate.eteruee.data.ai.transformers

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import com.eterultimate.eteruee.data.datastore.Settings
import com.eterultimate.eteruee.data.model.Assistant

class TransformerContext(
    val context: Context,
    val model: Model,
    val assistant: Assistant,
    val settings: Settings,
    val processingStatus: MutableStateFlow<String?> = MutableStateFlow(null),
)

interface MessageTransformer {
    /**
     * 娑堟伅杞崲鍣紝鐢ㄤ簬瀵规秷鎭繘琛岃浆鎹?
     *
     * 瀵逛簬杈撳叆娑堟伅锛屾秷鎭細杞崲琚彁渚涚粰API妯″潡
     *
     * 瀵逛簬杈撳嚭娑堟伅锛屼細瀵规秷鎭緭鍑篶hunk杩涜杞崲
     */
    suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        return messages
    }
}

interface InputMessageTransformer : MessageTransformer

interface OutputMessageTransformer : MessageTransformer {
    /**
     * 涓€涓瑙夌殑杞崲锛屼緥濡傝浆鎹hink tag涓簉easoning parts
     * 浣嗘槸涓嶅疄闄呰浆鎹㈡秷鎭紝鍥犱负娴佸紡杈撳嚭闇€瑕佸鐞嗘秷鎭痙elta chunk
     * 涓嶈兘杩樻病缁撴潫鐢熸垚灏眛ransform锛屽洜姝ゆ彁渚涗竴涓獀isualTransform
     */
    suspend fun visualTransform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        return messages
    }

    /**
     * 娑堟伅鐢熸垚瀹屾垚鍚庤皟鐢?
     */
    suspend fun onGenerationFinish(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        return messages
    }
}

suspend fun List<UIMessage>.transforms(
    transformers: List<MessageTransformer>,
    context: Context,
    model: Model,
    assistant: Assistant,
    settings: Settings,
    processingStatus: MutableStateFlow<String?> = MutableStateFlow(null),
): List<UIMessage> {
    val ctx = TransformerContext(context, model, assistant, settings, processingStatus)
    return transformers.fold(this) { acc, transformer ->
        transformer.transform(ctx, acc)
    }
}

suspend fun List<UIMessage>.visualTransforms(
    transformers: List<MessageTransformer>,
    context: Context,
    model: Model,
    assistant: Assistant,
    settings: Settings,
): List<UIMessage> {
    val ctx = TransformerContext(context, model, assistant, settings)
    return transformers.fold(this) { acc, transformer ->
        if (transformer is OutputMessageTransformer) {
            transformer.visualTransform(ctx, acc)
        } else {
            acc
        }
    }
}

suspend fun List<UIMessage>.onGenerationFinish(
    transformers: List<MessageTransformer>,
    context: Context,
    model: Model,
    assistant: Assistant,
    settings: Settings,
): List<UIMessage> {
    val ctx = TransformerContext(context, model, assistant, settings)
    return transformers.fold(this) { acc, transformer ->
        if (transformer is OutputMessageTransformer) {
            transformer.onGenerationFinish(ctx, acc)
        } else {
            acc
        }
    }
}

