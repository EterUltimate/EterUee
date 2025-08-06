package me.rerere.rikkahub.data.ai

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

class AIRequestInterceptor(private val remoteConfig: FirebaseRemoteConfig) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val host = request.url.host

        if (host == "api.siliconflow.cn") {
            request = processSiliconCloudRequest(request)
        }

        return chain.proceed(request)
    }

    // 处理硅基流动的请求
    private fun processSiliconCloudRequest(request: Request): Request {
        val authHeader = request.header("Authorization")
        val path = request.url.encodedPath

        // 如果没有设置api token, 填入免费api key
        if (authHeader?.trim() == "Bearer" && path in listOf("/v1/chat/completions", "/v1/models")) {
            return request.newBuilder()
                .header("Authorization", "Bearer ${remoteConfig.getString("silicon_cloud_key")}")
                .build()
        }

        return request
    }
}
