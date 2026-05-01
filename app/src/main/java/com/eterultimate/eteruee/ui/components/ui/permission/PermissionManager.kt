package com.eterultimate.eteruee.ui.components.ui.permission

import androidx.compose.runtime.Composable

/**
 * 鏉冮檺绠＄悊鍣ㄧ粍浠?
 * 鑷姩澶勭悊鏉冮檺璇锋眰瀵硅瘽妗嗙殑鏄剧ず鍜岄殣钘?
 *
 * 浣跨敤鏂瑰紡锛?
 * ```
 * val permissionState = rememberPermissionState(permissions)
 *
 * PermissionManager(permissionState = permissionState) {
 *     // 浣犵殑UI鍐呭
 *     YourContent()
 * }
 * ```
 */
@Composable
fun PermissionManager(
    permissionState: PermissionState,
    content: @Composable () -> Unit = {},
) {
    // 鏄剧ず鏉冮檺璇锋眰璇存槑瀵硅瘽妗?
    if (permissionState.showRationaleDialog && permissionState.currentRationalePermissions.isNotEmpty()) {
        PermissionRationaleDialog(
            permissions = permissionState.currentRationalePermissions,
            permanentlyDeniedPermissions = permissionState.permanentlyDeniedPermissions,
            onProceed = {
                permissionState.proceedFromRationale()
            },
            onCancel = {
                permissionState.cancelPermissionRequest()
            },
            onOpenSettings = {
                permissionState.openAppSettings()
                permissionState.cancelPermissionRequest()
            }
        )
    }

    // 涓昏鍐呭
    content()
}

