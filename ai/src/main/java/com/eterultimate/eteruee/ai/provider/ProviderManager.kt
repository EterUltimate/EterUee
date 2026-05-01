package com.eterultimate.eteruee.ai.provider

import android.content.Context
import com.eterultimate.eteruee.ai.provider.providers.ClaudeProvider
import com.eterultimate.eteruee.ai.provider.providers.GoogleProvider
import com.eterultimate.eteruee.ai.provider.providers.OpenAIProvider
import okhttp3.OkHttpClient

/**
 * Provider绠＄悊鍣紝璐熻矗娉ㄥ唽鍜岃幏鍙朠rovider瀹炰緥
 */
class ProviderManager(client: OkHttpClient, context: Context) {
    // 瀛樺偍宸叉敞鍐岀殑Provider瀹炰緥
    private val providers = mutableMapOf<String, Provider<*>>()

    init {
        // 娉ㄥ唽榛樿Provider
        registerProvider("openai", OpenAIProvider(client, context))
        registerProvider("google", GoogleProvider(client, context))
        registerProvider("claude", ClaudeProvider(client, context))
    }

    /**
     * 娉ㄥ唽Provider瀹炰緥
     *
     * @param name Provider鍚嶇О
     * @param provider Provider瀹炰緥
     */
    fun registerProvider(name: String, provider: Provider<*>) {
        providers[name] = provider
    }

    /**
     * 鑾峰彇Provider瀹炰緥
     *
     * @param name Provider鍚嶇О
     * @return Provider瀹炰緥锛屽鏋滀笉瀛樺湪鍒欒繑鍥瀗ull
     */
    fun getProvider(name: String): Provider<*> {
        return providers[name] ?: throw IllegalArgumentException("Provider not found: $name")
    }

    /**
     * 鏍规嵁ProviderSetting鑾峰彇瀵瑰簲鐨凱rovider瀹炰緥
     *
     * @param setting Provider璁剧疆
     * @return Provider瀹炰緥锛屽鏋滀笉瀛樺湪鍒欒繑鍥瀗ull
     */
    fun <T : ProviderSetting> getProviderByType(setting: T): Provider<T> {
        @Suppress("UNCHECKED_CAST")
        return when (setting) {
            is ProviderSetting.OpenAI -> getProvider("openai")
            is ProviderSetting.Google -> getProvider("google")
            is ProviderSetting.Claude -> getProvider("claude")
        } as Provider<T>
    }
}

