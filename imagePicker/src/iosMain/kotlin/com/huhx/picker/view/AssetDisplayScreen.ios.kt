package com.huhx.picker.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import coil3.PlatformContext
import coil3.Uri
import coil3.toUri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.model.toUri
import kotlinx.coroutines.CoroutineScope
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

actual class CameraLauncher(
    private val callback: CameraLauncher.(AssetInfo?) -> Unit,
    onCreate: (CameraLauncher) -> Unit,
    private val cameraController: IOSCameraController
) {
    init {
        onCreate(this)
    }

    private var assetInfo: AssetInfo? = null
    actual fun launch(context: PlatformContext, uri: Uri?) {
        cameraController.startCamera {
            assetInfo = it
            callback(it)
        }
    }

    actual val uri: Uri?
        get() = assetInfo?.uriString?.toUri()

    actual fun fetchCameraUri(assets: Map<String, List<AssetInfo>>): AssetInfo? {
        var assetInfo: AssetInfo? = null
        if (this.assetInfo != null) {
            val ass = this.assetInfo!!
            assets.values.forEach each@{
                it.forEach {
                    println("it.uriString: ${it.uriString}  assetInfo.uriString: ${ass.uriString}")
                    if (it.uriString == ass.uriString) {
                        assetInfo = it
                        this.assetInfo = null
                        return it
                    }
                }
            }
        }
        return assetInfo
    }
}

@Composable
actual fun rememberCameraLauncher(scope: CoroutineScope,
                                  onCreate: (CameraLauncher) -> Unit,
                                  callback: CameraLauncher.(AssetInfo?) -> Unit): CameraLauncher {
    return remember { CameraLauncher(callback,onCreate,IOSCameraController(scope,(UIApplication.sharedApplication.windows.first() as UIWindow).rootViewController!!)) }
}