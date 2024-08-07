package com.huhx.picker.view

import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.content.Intent
import android.os.Parcelable
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import androidx.lifecycle.lifecycleScope
import coil3.PlatformContext
import coil3.Uri
import coil3.compose.LocalPlatformContext
import coil3.toAndroidUri
import com.huhx.picker.model.AssetInfo
import com.huhx.picker.provider.AssetLoader
import com.huhx.picker.util.LocalStoragePermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.jvm.internal.Intrinsics


actual class CameraLauncher(
    private var launcher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
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

    actual fun launch(context: PlatformContext,uri: Uri?) {
       val androidUri = uri?.toAndroidUri()?.apply {
            cameraUri = uri
        }?:return
        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        intent.putExtra("output", androidUri)
        val mutablePendingIntent = PendingIntent.getActivity(
            context,
            10086,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        val intentSenderRequest = IntentSenderRequest.Builder(mutablePendingIntent.intentSender).build()
        launcher.launch(intentSenderRequest)
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
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartIntentSenderForResult()) { activityResult ->
            println("cameraLauncher activityResult::${activityResult.resultCode}")
            val launcher = cameraLauncher ?: return@rememberLauncherForActivityResult
            if (activityResult.resultCode == RESULT_OK) {
                scope.launch {
                    val info = AssetLoader.load(
                        context,
                        com.huhx.picker.model.RequestType.IMAGE,
                        onlyLast = true
                    ).firstOrNull()?.let {
                        if (it.uriString == launcher.uri?.path.toString()) it else null
                    }
                    callback(launcher, info)
                }
            } else {
                callback(launcher, null)
            }
        }
    val current = LocalStoragePermission.current
    val lifecycle = LocalLifecycleOwner.current
    val state = lifecycle.lifecycle.currentStateAsState()

    when(state.value){
        Lifecycle.State.RESUMED -> {

        }
        else->Unit
    }

    return remember {
        CameraLauncher(launcher, scope).apply {
            onCreate(this)
        }
    }.apply {
        cameraLauncher = this
    }
}