package me.rerere.rikkahub.ui.hooks

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Stable
sealed class PermissionStatus {
    object Granted : PermissionStatus()
    object Denied : PermissionStatus()
    object ShowRationale : PermissionStatus()
    object Unknown : PermissionStatus()

    val isGranted: Boolean get() = this == Granted
}

@Stable
interface PermissionRequestState {
    val permission: String
    val status: PermissionStatus
    fun launchRequest()
    fun checkStatus() // 提供一个手动检查状态的方法
}

@Stable
interface MultiplePermissionsRequestState {
    val permissions: List<String>

    // key: permission name, value: status
    val statusMap: Map<String, PermissionStatus>
    val allGranted: Boolean
    fun launchRequest()
    fun checkStatus() // 提供一个手动检查状态的方法
}

@Composable
fun rememberPermissionRequestState(
    permission: String,
    onResult: (isGranted: Boolean, shouldShowRationale: Boolean?) -> Unit = { _, _ -> } // 可选的回调
): PermissionRequestState {
    val context = LocalContext.current
    var currentStatus: PermissionStatus by remember { mutableStateOf(PermissionStatus.Unknown) }
    // 检查当前状态的函数
    val checkCurrentStatus = remember(context, permission) {
        {
            val granted = ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                currentStatus = PermissionStatus.Granted
            } else {
                // 检查是否需要显示理由 (需要 Activity)
                val activity = context as? Activity
                val showRationale = activity?.let {
                    ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
                } ?: false // 如果 context 不是 Activity，则无法判断，假设 false
                currentStatus =
                    if (showRationale) PermissionStatus.ShowRationale else PermissionStatus.Denied
            }
        }
    }
    // ActivityResultLauncher 用于请求权限
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            val activity = context as? Activity
            val showRationale = if (!isGranted && activity != null) {
                // 请求被拒后，再次检查 Rationale 状态
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            } else null // 授予或无法检查时为 null
            currentStatus = if (isGranted) PermissionStatus.Granted else {
                if (showRationale == true) PermissionStatus.ShowRationale else PermissionStatus.Denied
            }
            onResult(isGranted, showRationale) // 调用回调
        }
    )
    // 在 Composable 首次进入或权限变化时检查初始状态
    LaunchedEffect(permission, context) {
        checkCurrentStatus()
    }
    // 返回实现了接口的对象
    // 使用 remember 确保在重组时返回同一个实例 (只要 permission 不变)
    return remember(permission) {
        object : PermissionRequestState {
            override val permission: String = permission
            override val status: PermissionStatus
                get() = currentStatus // 通过 getter 暴露状态

            override fun launchRequest() {
                launcher.launch(permission)
            }

            override fun checkStatus() {
                checkCurrentStatus()
            }
        }
    }
}

// --- 多权限请求 Hook ---
@Composable
fun rememberMultiplePermissionsRequestState(
    permissions: List<String>,
    // Map<Permission, IsGranted>
    onResult: (Map<String, Boolean>) -> Unit = {}
): MultiplePermissionsRequestState {
    val context = LocalContext.current
    // 使用 MutableStateMap 来跟踪每个权限的状态
    var statusMapState by remember { mutableStateOf<Map<String, PermissionStatus>>(emptyMap()) }
    // 检查所有权限状态的函数
    val checkCurrentStatuses = remember(context, permissions) {
        {
            val activity = context as? Activity
            val newStatusMap = permissions.associateWith { permission ->
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    PermissionStatus.Granted
                } else {
                    val showRationale = activity?.let { act ->
                        ActivityCompat.shouldShowRequestPermissionRationale(act, permission)
                    } ?: false
                    if (showRationale) PermissionStatus.ShowRationale else PermissionStatus.Denied
                }
            }
            statusMapState = newStatusMap
        }
    }
    // ActivityResultLauncher 用于请求多个权限
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsGrantResults ->
            // 请求结束后，重新检查所有权限的状态，因为 Rationale 状态可能改变
            checkCurrentStatuses()
            onResult(permissionsGrantResults) // 调用回调
        }
    )
    // 初始状态检查
    LaunchedEffect(permissions, context) {
        checkCurrentStatuses()
    }
    // 返回实现了接口的对象
    return remember(permissions) {
        object : MultiplePermissionsRequestState {
            override val permissions: List<String> = permissions
            override val statusMap: Map<String, PermissionStatus>
                get() = statusMapState
            override val allGranted: Boolean
                get() = statusMapState.values.all { it.isGranted } // 检查是否所有权限都已授予

            override fun launchRequest() {
                launcher.launch(permissions.toTypedArray())
            }

            override fun checkStatus() {
                checkCurrentStatuses()
            }
        }
    }
}