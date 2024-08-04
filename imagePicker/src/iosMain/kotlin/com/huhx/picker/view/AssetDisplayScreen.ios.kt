package com.huhx.picker.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.Uri
import com.huhx.picker.model.AssetInfo
import kotlinx.coroutines.CoroutineScope
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

actual class CameraLauncher(
    private val callback: CameraLauncher.(Boolean) -> Unit,
    private val cameraController: IOSCameraController
) {
    private var cameraUri: Uri? = null
    actual fun launch(uri: Uri?) {
        cameraController.startCamera {
            cameraUri = it
            callback(it != null)
        }
    }

    actual val uri: Uri?
        get() = cameraUri

    actual fun fetchCameraUri(assets: Map<String, List<AssetInfo>>): AssetInfo? {
        var assetInfo: AssetInfo? = null
        if (cameraUri != null) {
            assets.values.forEach each@{
                it.forEach {
                    if (it.uriString == cameraUri.toString()) {
                        assetInfo = it
                        cameraUri = null
                        return@each
                    }
                }
            }
        }
        return assetInfo
    }
}

@Composable
actual fun rememberCameraLauncher(scope: CoroutineScope, callback: CameraLauncher.(Boolean) -> Unit): CameraLauncher {
    return remember { CameraLauncher(callback,IOSCameraController(scope,(UIApplication.sharedApplication.windows.first() as UIWindow).rootViewController!!)) }
}