package com.eterultimate.eteruee.device

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.ParcelFileDescriptor
import com.eterultimate.eteruee.shell.LocalShellRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
private const val SHIZUKU_PERMISSION_REQUEST_CODE = 62026
private const val ADB_EXECUTOR_NAME = "shizuku-adb-shell"
private const val ADB_SHELL_PATH = "/system/bin/sh"
private const val ADB_DEFAULT_WORKING_DIR = "/data/local/tmp"

class DeviceAgentManager(
    private val context: Context,
) {
    private val packageManager: PackageManager = context.packageManager

    fun getStatus(): DeviceAgentStatus {
        val installed = isPackageInstalled(SHIZUKU_PACKAGE_NAME)
        val running = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        val version = if (running) runCatching { Shizuku.getVersion() }.getOrNull() else null
        val serverUid = if (running) runCatching { Shizuku.getUid() }.getOrNull() else null
        val permissionGranted = running && runCatching {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
        val permissionRationale = running && runCatching {
            Shizuku.shouldShowRequestPermissionRationale()
        }.getOrDefault(false)
        return DeviceAgentStatus(
            shizukuInstalled = installed,
            shizukuRunning = running,
            shizukuVersion = version,
            serverUid = serverUid,
            serverMode = serverUid?.let(::serverModeName),
            wirelessDebuggingReady = running && serverUid == 2000,
            setupHint = shizukuSetupHint(
                installed = installed,
                running = running,
                permissionGranted = permissionGranted,
                serverUid = serverUid,
            ),
            permissionGranted = permissionGranted,
            permissionRationale = permissionRationale,
            canRunAdbShell = running && permissionGranted,
            message = when {
                !installed -> "Shizuku app is not installed"
                !running -> "Shizuku service is not running; start it with wireless or USB debugging"
                !permissionGranted -> "Shizuku is running but EterUee has not been granted API access"
                else -> "Shizuku ADB shell is ready"
            },
        )
    }

    fun requestShizukuPermission(): DeviceAgentStatus {
        val status = getStatus()
        if (!status.shizukuInstalled || !status.shizukuRunning || status.permissionGranted) {
            return status
        }
        if (status.permissionRationale) {
            return status.copy(message = "Shizuku permission was denied; grant it in Shizuku app settings")
        }
        runCatching {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }.onFailure { error ->
            return status.copy(message = error.message ?: error.javaClass.simpleName)
        }
        return getStatus().copy(message = "Shizuku permission request sent")
    }

    suspend fun executeAdbShell(
        command: String,
        workingDir: String? = null,
        stdin: String? = null,
        timeoutSeconds: Int = 30,
    ): DeviceShellResult = withContext(Dispatchers.IO) {
        require(command.isNotBlank()) { "command is required" }
        val status = getStatus()
        require(status.shizukuRunning) { "Shizuku service is not running" }
        require(status.permissionGranted) { "Shizuku permission is not granted" }

        val cwd = workingDir?.takeIf { it.isNotBlank() } ?: ADB_DEFAULT_WORKING_DIR
        val timeout = timeoutSeconds.coerceIn(1, 300).toLong()
        val process = shizukuService().newProcess(
            arrayOf(ADB_SHELL_PATH, "-c", command),
            adbEnvironment(),
            cwd,
        )

        val outputStream = ParcelFileDescriptor.AutoCloseOutputStream(process.outputStream)
        if (stdin != null) {
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(stdin)
            }
        } else {
            outputStream.close()
        }

        val inputStream = ParcelFileDescriptor.AutoCloseInputStream(process.inputStream)
        val errorStream = ParcelFileDescriptor.AutoCloseInputStream(process.errorStream)
        val stdoutFuture = executor.submit(Callable {
            inputStream.bufferedReader(Charsets.UTF_8).readText()
        })
        val stderrFuture = executor.submit(Callable {
            errorStream.bufferedReader(Charsets.UTF_8).readText()
        })

        val completed = process.waitForTimeout(timeout, TimeUnit.SECONDS.toString())
        if (!completed) {
            process.destroy()
        }

        val stdout = runCatching { stdoutFuture.get(1, TimeUnit.SECONDS) }.getOrDefault("")
        val stderr = runCatching { stderrFuture.get(1, TimeUnit.SECONDS) }.getOrDefault("")
        DeviceShellResult(
            stdout = stdout,
            stderr = if (completed) stderr else stderr + "\n[TIMEOUT] Command timed out after ${timeout}s",
            exitCode = if (completed) process.exitValue() else -1,
            executor = ADB_EXECUTOR_NAME,
            shell = ADB_SHELL_PATH,
            workingDir = cwd,
            command = command,
            serverUid = status.serverUid,
            serverMode = status.serverMode,
        )
    }

    fun getDeviceInfo(): DeviceInfo {
        val appInfo = currentPackageInfo()
        val displayMetrics = context.resources.displayMetrics
        val memoryInfo = ActivityManager.MemoryInfo().also { info ->
            (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)?.getMemoryInfo(info)
        }
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val cameras = runCatching {
            (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager).cameraIdList.toList()
        }.getOrDefault(emptyList())

        return DeviceInfo(
            hardware = DeviceHardwareInfo(
                manufacturer = Build.MANUFACTURER,
                brand = Build.BRAND,
                model = Build.MODEL,
                device = Build.DEVICE,
                product = Build.PRODUCT,
                board = Build.BOARD,
                hardware = Build.HARDWARE,
                supportedAbis = Build.SUPPORTED_ABIS.toList(),
                cameraIds = cameras,
                screenWidthPx = displayMetrics.widthPixels,
                screenHeightPx = displayMetrics.heightPixels,
                density = displayMetrics.density,
                memoryTotalBytes = memoryInfo.totalMem,
                memoryAvailableBytes = memoryInfo.availMem,
                storageTotalBytes = context.filesDir.totalSpace,
                storageAvailableBytes = context.filesDir.usableSpace,
            ),
            software = DeviceSoftwareInfo(
                androidRelease = Build.VERSION.RELEASE,
                sdkInt = Build.VERSION.SDK_INT,
                securityPatch = Build.VERSION.SECURITY_PATCH,
                incremental = Build.VERSION.INCREMENTAL,
                appPackage = context.packageName,
                appVersionName = appInfo?.versionName,
                appVersionCode = appInfo?.longVersionCodeCompat(),
                networkType = currentNetworkType(),
                batteryPercent = battery?.batteryPercent(),
                batteryCharging = battery?.batteryCharging(),
            ),
            shizuku = getStatus(),
        )
    }

    fun listInstalledApps(
        includeSystem: Boolean = false,
        limit: Int = 250,
    ): List<DeviceAppInfo> {
        val safeLimit = limit.coerceIn(1, 1000)
        return installedPackages()
            .asSequence()
            .mapNotNull { packageInfo ->
                val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                val isSystem = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                if (!includeSystem && isSystem) return@mapNotNull null
                DeviceAppInfo(
                    packageName = packageInfo.packageName,
                    label = runCatching { appInfo.loadLabel(packageManager).toString() }.getOrNull()
                        ?: packageInfo.packageName,
                    versionName = packageInfo.versionName,
                    versionCode = packageInfo.longVersionCodeCompat(),
                    system = isSystem,
                    enabled = appInfo.enabled,
                )
            }
            .sortedWith(compareBy<DeviceAppInfo> { it.system }.thenBy { it.label.lowercase() })
            .take(safeLimit)
            .toList()
    }

    private fun adbEnvironment(): Array<String> {
        return LocalShellRunner.defaultEnvironment(context)
            .toMutableMap()
            .apply {
                put("HOME", ADB_DEFAULT_WORKING_DIR)
                put("TMPDIR", ADB_DEFAULT_WORKING_DIR)
                put("USER", "shell")
            }
            .map { (key, value) -> "$key=$value" }
            .sorted()
            .toTypedArray()
    }

    private fun shizukuService(): IShizukuService {
        val binder = Shizuku.getBinder()
            ?: throw IllegalStateException("Shizuku binder is not available")
        return IShizukuService.Stub.asInterface(binder)
            ?: throw IllegalStateException("Shizuku service is not available")
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return packageInfo(packageName) != null
    }

    private fun currentPackageInfo(): PackageInfo? = packageInfo(context.packageName)

    @Suppress("DEPRECATION")
    private fun packageInfo(packageName: String): PackageInfo? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                packageManager.getPackageInfo(packageName, 0)
            }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    private fun installedPackages(): List<PackageInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            packageManager.getInstalledPackages(0)
        }
    }

    private fun currentNetworkType(): String {
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "unknown"
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return "offline"
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "vpn"
            else -> "connected"
        }
    }

    private companion object {
        val executor = Executors.newCachedThreadPool()
    }
}

@Serializable
data class DeviceAgentStatus(
    val shizukuInstalled: Boolean,
    val shizukuRunning: Boolean,
    val shizukuVersion: Int? = null,
    val serverUid: Int? = null,
    val serverMode: String? = null,
    val wirelessDebuggingReady: Boolean = false,
    val setupHint: String? = null,
    val permissionGranted: Boolean,
    val permissionRationale: Boolean,
    val canRunAdbShell: Boolean,
    val message: String,
)

@Serializable
data class DeviceShellResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val executor: String,
    val shell: String,
    val workingDir: String,
    val command: String,
    val serverUid: Int? = null,
    val serverMode: String? = null,
)

@Serializable
data class DeviceInfo(
    val hardware: DeviceHardwareInfo,
    val software: DeviceSoftwareInfo,
    val shizuku: DeviceAgentStatus,
)

@Serializable
data class DeviceHardwareInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val board: String,
    val hardware: String,
    val supportedAbis: List<String>,
    val cameraIds: List<String>,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val density: Float,
    val memoryTotalBytes: Long,
    val memoryAvailableBytes: Long,
    val storageTotalBytes: Long,
    val storageAvailableBytes: Long,
)

@Serializable
data class DeviceSoftwareInfo(
    val androidRelease: String,
    val sdkInt: Int,
    val securityPatch: String,
    val incremental: String,
    val appPackage: String,
    val appVersionName: String? = null,
    val appVersionCode: Long? = null,
    val networkType: String,
    val batteryPercent: Int? = null,
    val batteryCharging: Boolean? = null,
)

@Serializable
data class DeviceAppInfo(
    val packageName: String,
    val label: String,
    val versionName: String? = null,
    val versionCode: Long,
    val system: Boolean,
    val enabled: Boolean,
)

private fun serverModeName(uid: Int): String {
    return when (uid) {
        0 -> "root"
        2000 -> "adb"
        else -> "uid:$uid"
    }
}

private fun shizukuSetupHint(
    installed: Boolean,
    running: Boolean,
    permissionGranted: Boolean,
    serverUid: Int?,
): String {
    return when {
        !installed -> "Install Shizuku, then start it from Wireless debugging or USB debugging."
        !running -> "Open Shizuku, pair it with Android Wireless debugging, then start the service."
        !permissionGranted -> "Grant EterUee API access in the Shizuku permission dialog."
        serverUid == 2000 -> "Shizuku is running as ADB shell, suitable for wireless debugging shell commands."
        serverUid == 0 -> "Shizuku is running as root; ADB shell commands are available with elevated privileges."
        else -> "Shizuku is running as uid:$serverUid; shell command scope depends on the current Shizuku mode."
    }
}

private fun PackageInfo.longVersionCodeCompat(): Long {
    @Suppress("DEPRECATION")
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()
}

private fun Intent.batteryPercent(): Int? {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) return null
    return (level * 100) / scale
}

private fun Intent.batteryCharging(): Boolean {
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    return status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
}
