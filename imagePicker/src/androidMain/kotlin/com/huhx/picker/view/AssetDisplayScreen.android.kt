package com.huhx.picker.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.PlatformContext
import coil3.Uri
import coil3.toAndroidUri


actual class CameraLauncher(private var launcher: androidx.activity.compose.ManagedActivityResultLauncher<android.net.Uri, Boolean>) {
    actual fun launch(cameraUri: Uri?) {
        launcher.launch(cameraUri?.toAndroidUri())
    }
}


@Composable
actual fun rememberCameraLauncher(callback: (Boolean) -> Unit): CameraLauncher {
    return rememberCameraLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        callback(success)
    }
}

@Composable
private fun rememberCameraLauncherForActivityResult(
    action: ActivityResultContracts.TakePicture,
    callback: (Boolean) -> Unit
): CameraLauncher {
    val launcher = rememberLauncherForActivityResult(action) { success ->
        callback(success)
    }
    return remember {
        CameraLauncher(launcher)
    }
}


@Composable
actual fun getScreenSize(current: PlatformContext): Dp {
    // android 通过 context 获取屏幕宽高
    with(LocalDensity.current) {
        return current.resources.displayMetrics.widthPixels.toDp()
    }
}