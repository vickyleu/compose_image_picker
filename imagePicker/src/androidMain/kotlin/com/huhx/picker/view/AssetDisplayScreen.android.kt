package com.huhx.picker.view

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.Uri
import coil3.toAndroidUri
import com.huhx.picker.model.AssetInfo
import kotlinx.coroutines.CoroutineScope


actual class CameraLauncher(
    private var launcher: ManagedActivityResultLauncher<android.net.Uri, Boolean>,
    private val scope: CoroutineScope
) {
    private var cameraUri: Uri? by mutableStateOf(null)

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

    actual val uri: Uri?
        get() = cameraUri

    actual fun launch(uri: Uri?) {
        cameraUri = uri
        launcher.launch(cameraUri?.toAndroidUri())
    }
}


@Composable
actual fun rememberCameraLauncher(
    scope: CoroutineScope, onCreate: (CameraLauncher) -> Unit,
    callback: CameraLauncher.(Boolean) -> Unit
): CameraLauncher {
    return rememberCameraLauncherForActivityResult(
        scope,onCreate,
        ActivityResultContracts.TakePicture()
    ) { success ->
        callback(success)
    }
}

@Composable
private fun rememberCameraLauncherForActivityResult(
    scope: CoroutineScope,
    onCreate: (CameraLauncher) -> Unit,
    action: ActivityResultContracts.TakePicture,
    callback: CameraLauncher.(Boolean) -> Unit
): CameraLauncher {
    var cameraLauncher: CameraLauncher? = null
    val launcher = rememberLauncherForActivityResult(action) { success ->
        val launcher = cameraLauncher ?: return@rememberLauncherForActivityResult
        callback(launcher, success)
    }
    return remember {
        CameraLauncher(launcher, scope).apply {
            cameraLauncher = this
            onCreate(this)
        }
    }
}