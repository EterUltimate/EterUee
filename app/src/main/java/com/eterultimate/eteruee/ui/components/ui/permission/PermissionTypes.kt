package com.eterultimate.eteruee.ui.components.ui.permission

import android.Manifest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.eterultimate.eteruee.R

/**
 * 鏉冮檺淇℃伅鏁版嵁绫?
 * @param permission Android鏉冮檺瀛楃涓?(濡?android.permission.CAMERA)
 * @param usage 鏉冮檺浣跨敤璇存槑鐨凜omposable鍐呭
 * @param required 鏄惁涓哄繀闇€鏉冮檺
 */
data class PermissionInfo(
    val permission: String,
    val displayName: @Composable () -> Unit,
    val usage: @Composable () -> Unit,
    val required: Boolean = false
)

/**
 * 鏉冮檺鐘舵€佹灇涓?
 */
enum class PermissionStatus {
    /** 鏈姹?*/
    NotRequested,
    /** 宸叉巿鏉?*/
    Granted,
    /** 琚嫆缁濅絾鍙互鍐嶆璇锋眰 */
    Denied,
    /** 琚嫆缁濅笖鐢ㄦ埛閫夋嫨"涓嶅啀璇㈤棶" */
    DeniedPermanently
}

/**
 * 鏉冮檺璇锋眰缁撴灉
 */
data class PermissionResult(
    val permission: String,
    val status: PermissionStatus,
    val isGranted: Boolean = status == PermissionStatus.Granted
)

/**
 * 澶氫釜鏉冮檺鐨勮姹傜粨鏋?
 */
data class MultiplePermissionResult(
    val results: Map<String, PermissionResult>,
    val allGranted: Boolean = results.values.all { it.isGranted },
    val allRequiredGranted: Boolean
)

val PermissionCamera = PermissionInfo(
    permission = Manifest.permission.CAMERA,
    displayName = { Text(stringResource(R.string.permission_camera)) },
    usage = { Text(stringResource(R.string.permission_camera_desc)) },
    required = true
)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
val PermissionNotification = PermissionInfo(
    permission = Manifest.permission.POST_NOTIFICATIONS,
    displayName = { Text(stringResource(R.string.permission_notification)) },
    usage = { Text(stringResource(R.string.permission_notification_desc)) },
    required = true
)

