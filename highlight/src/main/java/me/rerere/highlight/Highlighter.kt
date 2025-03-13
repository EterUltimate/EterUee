package me.rerere.highlight

import android.content.Context
import com.google.gson.GsonBuilder
import com.whl.quickjs.android.QuickJSLoader
import com.whl.quickjs.wrapper.QuickJSArray
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.QuickJSObject

class Highlighter(ctx: Context) {
    init {
        QuickJSLoader.init()
    }

    private val gson by lazy {
        GsonBuilder().create()
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

    data class Token(
        val type: String,
        val content: String,
        val length: Int
    ) : HighlightToken()
}
