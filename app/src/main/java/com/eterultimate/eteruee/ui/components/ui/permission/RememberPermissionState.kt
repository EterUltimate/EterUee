package com.eterultimate.eteruee.ui.components.ui.permission

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * 鍒涘缓骞惰浣忔潈闄愮姸鎬?
 *
 * @param permissions 鏉冮檺淇℃伅闆嗗悎
 * @return PermissionState 鏉冮檺鐘舵€佺鐞嗗璞?
 *
 * 浣跨敤绀轰緥:
 * ```
 * val permissionState = rememberPermissionState(
 *     permissions = setOf(
 *         PermissionInfo(
 *             permission = Manifest.permission.CAMERA,
 *             usage = { Text("闇€瑕佺浉鏈烘潈闄愭潵鎷嶇収") },
 *             required = true
 *         ),
 *         PermissionInfo(
 *             permission = Manifest.permission.RECORD_AUDIO,
 *             usage = { Text("闇€瑕佸綍闊虫潈闄愭潵褰曞埗瑙嗛") },
 *             required = false
 *         )
 *     )
 * )
 *
 * // 璇锋眰鏉冮檺
 * Button(onClick = { permissionState.requestPermissions() }) {
 *     Text("璇锋眰鏉冮檺")
 * }
 *
 * // 妫€鏌ユ潈闄愮姸鎬?
 * if (permissionState.allRequiredPermissionsGranted) {
 *     Text("鎵€鏈夊繀闇€鏉冮檺宸叉巿鏉?)
 * }
 * ```
 */
@Composable
fun rememberPermissionState(
    permissions: Set<PermissionInfo>
): PermissionState {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
        ?: throw IllegalStateException("rememberPermissionState 蹇呴』鍦?ComponentActivity 涓娇鐢?)

    // 鍒涘缓鏉冮檺鐘舵€佸璞?
    val permissionState = remember(permissions) {
        PermissionState(permissions, context, activity)
    }

    // 澶氫釜鏉冮檺璇锋眰鍚姩鍣?
    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionState.handlePermissionResult(results)
    }

    // 鍗曚釜鏉冮檺璇锋眰鍚姩鍣?
    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // 鑾峰彇鏈€鍚庤姹傜殑鏉冮檺锛堥€氳繃褰撳墠rationale鏉冮檺鎴栬€卍enied鏉冮檺鎺ㄦ柇锛?
        val lastRequestedPermission = permissionState.currentRationalePermissions.firstOrNull()?.permission
            ?: permissionState.deniedPermissions.firstOrNull()?.permission

        lastRequestedPermission?.let { permission ->
            permissionState.handleSinglePermissionResult(permission, granted)
        }
    }

    // 璁剧疆鍚姩鍣?
    LaunchedEffect(multiplePermissionLauncher, singlePermissionLauncher) {
        permissionState.setPermissionLaunchers(multiplePermissionLauncher, singlePermissionLauncher)
    }

    // 鐩戝惉鐢熷懡鍛ㄦ湡鍙樺寲锛屾洿鏂版潈闄愮姸鎬?
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // 搴旂敤浠庡悗鍙板洖鍒板墠鍙版椂寮哄埗鍒锋柊鏉冮檺鐘舵€?
                    // 杩欓噷浣跨敤 refreshPermissionStates 鏉ュ鐞嗙敤鎴峰彲鑳藉湪璁剧疆涓慨鏀圭殑鏉冮檺
                    permissionState.refreshPermissionStates()
                }

                Lifecycle.Event.ON_RESUME -> {
                    // 鎭㈠鏃朵篃鍒锋柊涓€娆★紝纭繚鐘舵€佹渶鏂?
                    permissionState.refreshPermissionStates()
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 鍒濆鍖栨椂鏇存柊鏉冮檺鐘舵€?
    LaunchedEffect(Unit) {
        permissionState.updatePermissionStates()
    }

    return permissionState
}

/**
 * 鍒涘缓骞惰浣忓崟涓潈闄愮姸鎬?
 *
 * @param permission 鏉冮檺瀛楃涓?
 * @param usage 鏉冮檺浣跨敤璇存槑
 * @param required 鏄惁涓哄繀闇€鏉冮檺
 * @return PermissionState 鏉冮檺鐘舵€佺鐞嗗璞?
 */
@Composable
fun rememberPermissionState(
    permission: String,
    displayName: @Composable () -> Unit,
    usage: @Composable () -> Unit,
    required: Boolean = false
): PermissionState {
    return rememberPermissionState(
        permissions = setOf(
            PermissionInfo(
                permission = permission,
                displayName = displayName,
                usage = usage,
                required = required,
            )
        )
    )
}

@Composable
fun rememberPermissionState(
    info: PermissionInfo
): PermissionState {
    return rememberPermissionState(
        permissions = setOf(info)
    )
}

