package com.huhx.picker.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import coil3.PlatformContext
import coil3.Uri

actual class CameraLauncher {
    actual fun launch(cameraUri: Uri?) {
    }
}
@Composable
actual fun rememberCameraLauncher(callback: (Boolean) -> Unit): CameraLauncher {
    TODO("Not yet implemented")
}

@Composable
actual fun getScreenSize(current: PlatformContext): Dp {
    TODO("Not yet implemented")
}