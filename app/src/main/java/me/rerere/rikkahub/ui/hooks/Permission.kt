package me.rerere.rikkahub.ui.hooks

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
fun rememberPermissionState(
    permissions: List<String>
): PermissionState {
    val context = LocalContext.current

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {

        }

    val state = remember(permissions, context) {
        PermissionState(
            permissions = permissions,
            context = context,
            launcher = launcher
        )
    }

    // Check permission on state init
    LaunchedEffect(state) {
        state.checkPermission()
    }

    return state
}

class PermissionState(
    val permissions: List<String>,
    val context: Context,
    val launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>
) {
    var granted by mutableStateOf(false)
        private set

    fun checkPermission() {
        granted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermission() {
        launcher.launch(permissions.toTypedArray())
    }
}