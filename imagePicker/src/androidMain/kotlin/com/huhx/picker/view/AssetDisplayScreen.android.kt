package com.huhx.picker.view

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import coil3.PlatformContext
import coil3.Uri
import coil3.compose.LocalPlatformContext
import coil3.toAndroidUri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.provider.AssetLoader
import com.huhx.picker.util.LocalStoragePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


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

    actual fun launch(context: PlatformContext, uri: Uri?) {
        val androidUri = uri?.toAndroidUri()?.apply {
            cameraUri = uri
        } ?: return
        launcher.launch(androidUri)
    }
}


@Composable
actual fun rememberCameraLauncher(
    scope: CoroutineScope, onCreate: (CameraLauncher) -> Unit,
    callback: CameraLauncher.(AssetInfo?) -> Unit
): CameraLauncher {
    return rememberCameraLauncherForActivityResult(
        scope, onCreate,
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
    callback: CameraLauncher.(AssetInfo?) -> Unit
): CameraLauncher {
    var cameraLauncher: CameraLauncher? = null
    val context = LocalPlatformContext.current
    val launcher =
        rememberLauncherForActivityResult(contract = action) { success ->
            val launcher = cameraLauncher ?: return@rememberLauncherForActivityResult run {
                println("cameraLauncher is null")
            }
            if (success) {
                scope.launch {
                    val info = AssetLoader.load(
                        context,
                        com.huhx.picker.model.RequestType.IMAGE,
                        onlyLast = true
                    ).firstOrNull()?.let {
                        if (it.uriString == launcher.uri.toString()) it else null
                    }
                    callback(launcher, info)
                }
            } else {
                callback(launcher, null)
            }
        }

    return remember {
        CameraLauncher(launcher, scope).apply {
            onCreate(this)
        }
    }.apply {
        cameraLauncher = this
    }
}