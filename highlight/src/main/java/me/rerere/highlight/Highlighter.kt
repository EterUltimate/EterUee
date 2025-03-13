package me.rerere.highlight

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.whl.quickjs.android.QuickJSLoader
import com.whl.quickjs.wrapper.QuickJSArray
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.QuickJSObject
import java.lang.reflect.Type

class Highlighter(ctx: Context) {
    init {
        QuickJSLoader.init()
    }

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(HighlightToken.Token::class.java, HighlightTokenAdapter())
            .create()
    }

    private val script: String by lazy {
        ctx.resources.openRawResource(R.raw.prism).use {
            it.bufferedReader().readText()
        }
    }

    private val context: QuickJSContext by lazy {
        QuickJSContext.create().also {
            it.evaluate(script)
        }
    }

    private val highlightFn by lazy {
        context.globalObject.getJSFunction("highlight")
    }

    fun highlight(code: String, language: String) = runCatching {
        val result = highlightFn.call(code, language)
        require(result is QuickJSArray) {
            "highlight result must be an array"
        }
        val tokens = arrayListOf<HighlightToken>()
        for (i in 0 until result.length()) {
            when (val element = result[i]) {
                is String -> tokens.add(
                    HighlightToken.Plain(
                        content = element,
                    )
                )

                is QuickJSObject -> {
                    val json = element.stringify()
                    val token = gson.fromJson(json, HighlightToken.Token::class.java)
                    tokens.add(token)
                }

                else -> error("Unknown type: ${element::class.java.name}")
            }
        }
        result.release()

        tokens
    }.onFailure {
        it.printStackTrace()
    }

    fun destroy() {
        context.destroy()
    }
}

sealed class HighlightToken {
    data class Plain(
        val content: String,
    ) : HighlightToken()

    sealed class Token : HighlightToken() {
        data class StringContent(
            val content: String,
            val type: String,
            val length: Int,
        ) : Token()

        data class StringListContent(
            val content: List<String>,
            val type: String,
            val length: Int,
        ) : Token()
    }
}

class HighlightTokenAdapter : JsonDeserializer<HighlightToken.Token> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): HighlightToken.Token {
        val jsonObject = json.asJsonObject
        val type = jsonObject.get("type").asString
        val length = jsonObject.get("length").asInt
        val content = jsonObject.get("content")

        return if(content is JsonArray) {
            HighlightToken.Token.StringListContent(
                content = content.map { it.asString },
                type = type,
                length = length
            )
        } else {
            HighlightToken.Token.StringContent(
                content = content.asString,
                type = type,
                length = length
            )
        }
    }
}