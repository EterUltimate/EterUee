package com.eterultimate.eteruee.ui.components.ui.permission

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat

/**
 * 鏉冮檺鐘舵€佺鐞嗙被
 */
@Stable
class PermissionState internal constructor(
    private val permissions: Set<PermissionInfo>,
    private val context: Context,
    private val activity: ComponentActivity
) {
    // 鏉冮檺鐘舵€佹槧灏?
    private val _permissionStates = mutableStateMapOf<String, PermissionStatus>()
    val permissionStates: Map<String, PermissionStatus> = _permissionStates

    // 鏄惁鏄剧ず鏉冮檺璇存槑瀵硅瘽妗?
    var showRationaleDialog by mutableStateOf(false)
        private set

    // 褰撳墠闇€瑕佹樉绀鸿鏄庣殑鏉冮檺
    var currentRationalePermissions by mutableStateOf<List<PermissionInfo>>(emptyList())
        private set

    // 鏉冮檺璇锋眰鍚姩鍣?
    private var permissionLauncher: ActivityResultLauncher<Array<String>>? = null

    // 鍗曚釜鏉冮檺璇锋眰鍚姩鍣?
    private var singlePermissionLauncher: ActivityResultLauncher<String>? = null

    init {
        // 鍒濆鍖栨潈闄愮姸鎬?
        updatePermissionStates()
    }

    /**
     * 璁剧疆鏉冮檺璇锋眰鍚姩鍣?
     */
    internal fun setPermissionLaunchers(
        multiplePermissionLauncher: ActivityResultLauncher<Array<String>>,
        singlePermissionLauncher: ActivityResultLauncher<String>
    ) {
        this.permissionLauncher = multiplePermissionLauncher
        this.singlePermissionLauncher = singlePermissionLauncher
    }

    /**
     * 鏇存柊鎵€鏈夋潈闄愮姸鎬?
     */
    fun updatePermissionStates() {
        permissions.forEach { permissionInfo ->
            val oldStatus = _permissionStates[permissionInfo.permission]
            val newStatus = getPermissionStatus(permissionInfo.permission, oldStatus)
            _permissionStates[permissionInfo.permission] = newStatus
        }
    }

    /**
     * 鑾峰彇鍗曚釜鏉冮檺鐘舵€?
     */
    private fun getPermissionStatus(permission: String, oldStatus: PermissionStatus? = null): PermissionStatus {
        return when {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.Granted
            }
            activity.shouldShowRequestPermissionRationale(permission) -> {
                PermissionStatus.Denied
            }
            // 濡傛灉涔嬪墠琚嫆缁濊繃锛堝寘鎷案涔呮嫆缁濓級锛岀幇鍦ㄥ張涓嶆樉绀簉ationale涓旀湭鎺堟潈锛岃鏄庢槸姘镐箙鎷掔粷
            (oldStatus == PermissionStatus.Denied || oldStatus == PermissionStatus.DeniedPermanently) -> {
                PermissionStatus.DeniedPermanently
            }
            else -> {
                PermissionStatus.NotRequested
            }
        }
    }

    /**
     * 妫€鏌ユ槸鍚︽墍鏈夋潈闄愰兘宸叉巿鏉?
     */
    val allPermissionsGranted: Boolean
        get() = permissions.all { permissionStates[it.permission] == PermissionStatus.Granted }

    /**
     * 妫€鏌ユ槸鍚︽墍鏈夊繀闇€鏉冮檺閮藉凡鎺堟潈
     */
    val allRequiredPermissionsGranted: Boolean
        get() = permissions.filter { it.required }.all { permissionStates[it.permission] == PermissionStatus.Granted }

    /**
     * 鑾峰彇鏈巿鏉冪殑鏉冮檺
     */
    val deniedPermissions: List<PermissionInfo>
        get() = permissions.filter { permissionStates[it.permission] != PermissionStatus.Granted }

    /**
     * 鑾峰彇闇€瑕佹樉绀鸿鏄庣殑鏉冮檺锛堝寘鎷案涔呮嫆缁濈殑鏉冮檺锛?
     */
    private val permissionsNeedRationale: List<PermissionInfo>
        get() = permissions.filter {
            val status = permissionStates[it.permission]
            status == PermissionStatus.Denied && activity.shouldShowRequestPermissionRationale(it.permission) ||
            status == PermissionStatus.DeniedPermanently
        }

    /**
     * 鑾峰彇姘镐箙鎷掔粷鐨勬潈闄?
     */
    val permanentlyDeniedPermissions: List<PermissionInfo>
        get() = permissions.filter { permissionStates[it.permission] == PermissionStatus.DeniedPermanently }

    /**
     * 璇锋眰鎵€鏈夋湭鎺堟潈鐨勬潈闄?
     */
    fun requestPermissions() {
        val deniedPerms = deniedPermissions
        if (deniedPerms.isEmpty()) return

        val rationalePerms = permissionsNeedRationale
        if (rationalePerms.isNotEmpty()) {
            // 鏄剧ず鏉冮檺璇存槑瀵硅瘽妗?
            currentRationalePermissions = rationalePerms
            showRationaleDialog = true
        } else {
            // 鐩存帴璇锋眰鏉冮檺
            launchPermissionRequest(deniedPerms)
        }
    }

    /**
     * 璇锋眰鐗瑰畾鏉冮檺
     */
    fun requestPermission(permission: String) {
        val permissionInfo = permissions.find { it.permission == permission } ?: return
        val status = permissionStates[permission] ?: return

        if (status == PermissionStatus.Granted) return

        when (status) {
            PermissionStatus.Denied -> {
                if (activity.shouldShowRequestPermissionRationale(permission)) {
                    // 鏄剧ず鏉冮檺璇存槑瀵硅瘽妗?
                    currentRationalePermissions = listOf(permissionInfo)
                    showRationaleDialog = true
                } else {
                    // 鐩存帴璇锋眰鏉冮檺
                    singlePermissionLauncher?.launch(permission)
                }
            }
            PermissionStatus.DeniedPermanently -> {
                // 姘镐箙鎷掔粷锛屾樉绀鸿鏄庡璇濇骞跺紩瀵煎埌璁剧疆
                currentRationalePermissions = listOf(permissionInfo)
                showRationaleDialog = true
            }
            else -> {
                // NotRequested 鐘舵€侊紝鐩存帴璇锋眰鏉冮檺
                singlePermissionLauncher?.launch(permission)
            }
        }
    }

    /**
     * 浠庢潈闄愯鏄庡璇濇缁х画璇锋眰鏉冮檺
     */
    fun proceedFromRationale() {
        showRationaleDialog = false

        // 妫€鏌ユ槸鍚︽湁姘镐箙鎷掔粷鐨勬潈闄?
        val permanentlyDenied = currentRationalePermissions.filter {
            permissionStates[it.permission] == PermissionStatus.DeniedPermanently
        }

        if (permanentlyDenied.isNotEmpty()) {
            // 鏈夋案涔呮嫆缁濈殑鏉冮檺锛岀洿鎺ヨ烦杞埌璁剧疆椤甸潰
            openAppSettings()
        } else {
            // 娌℃湁姘镐箙鎷掔粷鐨勬潈闄愶紝姝ｅ父璇锋眰鏉冮檺
            launchPermissionRequest(currentRationalePermissions)
        }

        currentRationalePermissions = emptyList()
    }

    /**
     * 鍙栨秷鏉冮檺璇锋眰
     */
    fun cancelPermissionRequest() {
        showRationaleDialog = false
        currentRationalePermissions = emptyList()
    }

    /**
     * 鍚姩鏉冮檺璇锋眰
     */
    private fun launchPermissionRequest(permissionInfos: List<PermissionInfo>) {
        val permissionsToRequest = permissionInfos.map { it.permission }.toTypedArray()
        if (permissionsToRequest.size == 1) {
            singlePermissionLauncher?.launch(permissionsToRequest[0])
        } else {
            permissionLauncher?.launch(permissionsToRequest)
        }
    }

    /**
     * 璺宠浆鍒板簲鐢ㄨ缃〉闈?
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        activity.startActivity(intent)
    }

    /**
     * 寮哄埗鍒锋柊鏉冮檺鐘舵€侊紙鐢ㄤ簬浠庡悗鍙板洖鍒板墠鍙版椂锛?
     * 杩欎釜鏂规硶浼氶噸鏂版鏌ユ墍鏈夋潈闄愮姸鎬侊紝鐗瑰埆澶勭悊鐢ㄦ埛鍙兘鍦ㄨ缃腑淇敼鐨勬潈闄?
     */
    fun refreshPermissionStates() {
        permissions.forEach { permissionInfo ->
            val currentSystemStatus = ContextCompat.checkSelfPermission(context, permissionInfo.permission)
            val oldStatus = _permissionStates[permissionInfo.permission]

            val newStatus = when {
                // 绯荤粺鏄剧ず宸叉巿鏉?
                currentSystemStatus == PackageManager.PERMISSION_GRANTED -> {
                    PermissionStatus.Granted
                }
                // 绯荤粺鏄剧ず鏈巿鏉冿紝浣嗗彲浠ユ樉绀鸿鏄庡璇濇
                activity.shouldShowRequestPermissionRationale(permissionInfo.permission) -> {
                    PermissionStatus.Denied
                }
                // 绯荤粺鏄剧ず鏈巿鏉冿紝涓斾笉鑳芥樉绀鸿鏄庡璇濇
                else -> {
                    // 濡傛灉涔嬪墠鏄湭璇锋眰鐘舵€侊紝淇濇寔鏈姹?
                    // 濡傛灉涔嬪墠鏄叾浠栫姸鎬侊紝鍒欒涓烘槸姘镐箙鎷掔粷
                    if (oldStatus == PermissionStatus.NotRequested || oldStatus == null) {
                        PermissionStatus.NotRequested
                    } else {
                        PermissionStatus.DeniedPermanently
                    }
                }
            }

            _permissionStates[permissionInfo.permission] = newStatus
        }
    }

    /**
     * 澶勭悊鏉冮檺璇锋眰缁撴灉
     */
    internal fun handlePermissionResult(results: Map<String, Boolean>) {
        results.forEach { (permission, granted) ->
            _permissionStates[permission] = if (granted) {
                PermissionStatus.Granted
            } else {
                if (activity.shouldShowRequestPermissionRationale(permission)) {
                    PermissionStatus.Denied
                } else {
                    PermissionStatus.DeniedPermanently
                }
            }
        }
    }

    /**
     * 澶勭悊鍗曚釜鏉冮檺璇锋眰缁撴灉
     */
    internal fun handleSinglePermissionResult(permission: String, granted: Boolean) {
        handlePermissionResult(mapOf(permission to granted))
    }

    /**
     * 鑾峰彇鏉冮檺缁撴灉
     */
    fun getPermissionResults(): MultiplePermissionResult {
        val results = permissions.associate { permissionInfo ->
            val status = permissionStates[permissionInfo.permission] ?: PermissionStatus.NotRequested
            permissionInfo.permission to PermissionResult(
                permission = permissionInfo.permission,
                status = status
            )
        }

        return MultiplePermissionResult(
            results = results,
            allRequiredGranted = permissions.filter { it.required }
                .all { permissionStates[it.permission] == PermissionStatus.Granted }
        )
    }
}

