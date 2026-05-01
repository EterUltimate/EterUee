package com.eterultimate.eteruee.ui.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Remembers the current Lifecycle.State of the application's LifecycleOwner
 * (usually the Activity or Fragment hosting the Compose UI).
 *
 * The returned State object will update whenever the lifecycle state changes
 * (e.g., from STARTED to RESUMED).
 *
 * @return A State object holding the current Lifecycle.State.
 */
@Composable
fun rememberAppLifecycleState(): State<Lifecycle.State> {
    // 1. 鑾峰彇褰撳墠鐨?LifecycleOwner銆?
    // LocalLifecycleOwner 鏄竴涓?CompositionLocal锛屾彁渚涗簡褰撳墠缁勫悎涓婁笅鏂囩殑 LifecycleOwner銆?
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    // 2. 浣跨敤 remember 鍒涘缓涓€涓?MutableState锛岀敤浜庡瓨鍌ㄥ綋鍓嶇殑鐢熷懡鍛ㄦ湡鐘舵€併€?
    // 鍒濆鍖栫姸鎬佷负褰撳墠鐨勭敓鍛藉懆鏈熺姸鎬併€?
    val lifecycleState = remember { mutableStateOf(lifecycleOwner.lifecycle.currentState) }
    // 3. 浣跨敤 DisposableEffect 娣诲姞鍜岀Щ闄や竴涓?LifecycleObserver銆?
    // DisposableEffect 閫傜敤浜庨渶瑕佸湪 Composable 杩涘叆鎴栫寮€缁勫悎鏃舵墽琛屽壇浣滅敤鍜屾竻鐞嗘搷浣滅殑鍦烘櫙銆?
    // key 璁剧疆涓?lifecycleOwner.lifecycle锛岀‘淇濆綋 lifecycle 瀵硅薄鏈韩鏀瑰彉鏃讹紙鏋佸皯瑙侊級鏁堟灉鑳芥纭鐞嗐€?
    DisposableEffect(lifecycleOwner.lifecycle) {
        // 鍒涘缓涓€涓?LifecycleEventObserver銆?
        // 褰撲换浣曠敓鍛藉懆鏈熶簨浠跺彂鐢熸椂锛宱nStateChanged 浼氳璋冪敤銆?
        val observer = LifecycleEventObserver { _, _ ->
            // 鍦ㄧ敓鍛藉懆鏈熶簨浠跺彂鐢熷悗锛屾洿鏂?lifecycleState 鐨勫€煎埌褰撳墠鐨勭敓鍛藉懆鏈熺姸鎬併€?
            // 杩欎細瑙﹀彂浣跨敤 lifecycleState 鐨?Composable 杩涜閲嶇粍銆?
            lifecycleState.value = lifecycleOwner.lifecycle.currentState
        }
        // 灏嗚瀵熻€呮坊鍔犲埌鐢熷懡鍛ㄦ湡銆?
        lifecycleOwner.lifecycle.addObserver(observer)
        // onDispose 鍧椾細鍦?Composable 绂诲紑缁勫悎鏃惰璋冪敤锛岀敤浜庢竻鐞嗚祫婧愩€?
        onDispose {
            // 绉婚櫎瑙傚療鑰咃紝闃叉鍐呭瓨娉勬紡銆?
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // 4. 杩斿洖瀛樺偍鐢熷懡鍛ㄦ湡鐘舵€佺殑 State 瀵硅薄銆?
    return lifecycleState
}

