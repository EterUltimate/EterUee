package me.rerere.highlight

import android.content.Context
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.whl.quickjs.android.QuickJSLoader
import com.whl.quickjs.wrapper.QuickJSArray
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.QuickJSObject

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

    fun highlight(code: String, language: String) = kotlin.runCatching {
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
                    println(json)
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

    sealed class Token(
        val type: String,
        val length: Int
    ) : HighlightToken() {
        class StringContent(
            val content: String,
            type: String,
            length: Int,
        ) : Token(type, length)

        class StringListContent(
            val content: List<String>,
            type: String,
            length: Int,
        ) : Token(type, length)
    }
}

class HighlightTokenAdapter : com.google.gson.JsonDeserializer<HighlightToken.Token> {
    override fun deserialize(
        json: com.google.gson.JsonElement,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext
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