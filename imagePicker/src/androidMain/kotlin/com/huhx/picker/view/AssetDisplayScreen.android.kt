package com.huhx.picker.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil3.PlatformContext

typealias CameraLauncher = androidx.activity.compose.ManagedActivityResultLauncher<android.net.Uri, Boolean>

@Composable
actual fun rememberCameraLauncher(callback: (Boolean) -> Unit): CameraLauncher {
    return rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        callback(success)
    }
}

@Composable
actual fun getScreenSize(current: PlatformContext): Dp {
    // android 通过 context 获取屏幕宽高
    with(LocalDensity.current) {
        return current.resources.displayMetrics.widthPixels.toDp()
    }
}