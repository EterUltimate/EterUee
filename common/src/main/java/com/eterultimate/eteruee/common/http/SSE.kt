package com.eterultimate.eteruee.common.http

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/**
 * 浠ｈ〃 SSE 杩炴帴涓殑鍚勭浜嬩欢
 */
sealed class SseEvent {
    /**
     * 杩炴帴鎴愬姛鎵撳紑
     */
    data object Open : SseEvent()

    /**
     * 鏀跺埌涓€涓叿浣撲簨浠?
     * @param id 浜嬩欢ID
     * @param type 浜嬩欢绫诲瀷
     * @param data 浜嬩欢鏁版嵁
     */
    data class Event(val id: String?, val type: String?, val data: String) : SseEvent()

    /**
     * 杩炴帴琚叧闂?
     */
    data object Closed : SseEvent()

    /**
     * 鍙戠敓閿欒
     * @param throwable 寮傚父淇℃伅
     * @param response 閿欒鏃剁殑鍝嶅簲锛堝彲鑳戒负null锛?
     */
    data class Failure(val throwable: Throwable?, val response: Response?) : SseEvent()
}


/**
 * 涓?OkHttpClient 鍒涘缓 SSE (Server-Sent Events) 杩炴帴鐨勬墿灞曞嚱鏁?
 * 
 * 灏?OkHttp 鐨?EventSource 灏佽鎴?Kotlin Flow锛屾彁渚涘搷搴斿紡鐨?SSE 浜嬩欢娴?
 * 
 * @param request HTTP 璇锋眰锛岀敤浜庡缓绔?SSE 杩炴帴
 * @return Flow<SseEvent> 鍖呭惈 SSE 浜嬩欢鐨勫搷搴斿紡娴?
 */
fun OkHttpClient.sseFlow(request: Request): Flow<SseEvent> {
    return callbackFlow {
        // 1. 鍒涘缓 EventSourceListener
        // 鐩戝惉 SSE 杩炴帴鐨勫悇绉嶄簨浠跺苟杞崲涓?Flow 浜嬩欢
        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                // 浠庡洖璋冧腑瀹夊叏鍦板彂閫佷簨浠跺埌 Flow
                // 杩炴帴鎴愬姛寤虹珛鏃惰Е鍙?
                trySend(SseEvent.Open)
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                // 鏀跺埌鏈嶅姟鍣ㄥ彂閫佺殑鏁版嵁浜嬩欢鏃惰Е鍙?
                // 灏嗕簨浠舵暟鎹皝瑁呭悗鍙戦€佸埌 Flow
                trySend(SseEvent.Event(id, type, data))
            }

            override fun onClosed(eventSource: EventSource) {
                // 杩炴帴姝ｅ父鍏抽棴鏃惰Е鍙?
                trySend(SseEvent.Closed)
                channel.close() // 鍏抽棴 Flow 閫氶亾
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                // 杩炴帴鍙戠敓閿欒鏃惰Е鍙?
                trySend(SseEvent.Failure(t, response))
                channel.close(t) // 浠ュ紓甯稿叧闂?Flow 閫氶亾
            }
        }

        // 2. 鍒涘缓 EventSource
        // 浣跨敤褰撳墠 OkHttpClient 鍒涘缓 EventSource 宸ュ巶
        val factory = EventSources.createFactory(this@sseFlow)
        val eventSource = factory.newEventSource(request, listener)

        // 3. awaitClose 鐢ㄤ簬鍦?Flow 琚彇娑堟椂鎵ц娓呯悊鎿嶄綔
        // 褰撴敹闆?Flow 鐨勫崗绋嬭鍙栨秷鏃讹紝杩欎釜鍧椾細琚皟鐢?
        awaitClose {
            // 鍏抽棴 SSE 杩炴帴锛岄噴鏀捐祫婧?
            eventSource.cancel()
        }
    }
}

