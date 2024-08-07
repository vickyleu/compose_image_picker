package com.huhx.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import cafe.adriel.voyager.navigator.Navigator
import coil3.Uri
import coil3.compose.LocalPlatformContext
import com.huhx.picker.model.toUri
import com.huhx.picker.provider.AssetLoader
import com.huhx.picker.util.LocalStoragePermission
import com.huhx.picker.util.goToAppSetting
import com.huhx.picker.view.CameraLauncher
import com.huhx.picker.view.permissionHandle
import com.huhx.picker.view.rememberCameraLauncher
import com.huhx.picker.view.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraLaunchViewModel : BasicViewModel() {
    override val showingTitle: Boolean
        get() = false
}

class CameraLaunchScreen(
    viewModel: CameraLaunchViewModel
) : BasicScreen<CameraLaunchViewModel>(create = { viewModel }) {
    @Composable
    override fun modelContent(
        model: CameraLaunchViewModel,
        navigator: Navigator,
        tabbarHeight: Dp
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().background(color = Color.Red),
        ) {
            val lifecycle = LocalLifecycleOwner.current

            val scope = rememberCoroutineScope()
            permissionHandle {
                println("model.cameraLauncher::")
                val state = lifecycle.lifecycle.currentStateFlow.collectAsState()
                val cameraLauncher = rememberCameraLauncher(scope, onCreate = {
                    println("cameraLauncher onCreate")
                }) { info ->
                    println("this@rememberCameraLauncher.uri::${this@rememberCameraLauncher.uri} info:$info")
                    if (info!=null) {
                        scope.launch {
                            info.toUri().path?.let {
                                /*val buffer = ByteArray(info.size.toInt())
                                FileImpl(it).inputStream().useImpl {
                                    it.read(buffer)
                                }
                                navigator.pop()*/
//                                ${buffer.size}
                                println("bytesRead:  ${it} ")
//                                model.callback(listOf(buffer))
                            }?:run{
                                navigator.pop()
                            }
                        }
                    } else {
                        scope.launch {
                            navigator.pop()
                        }
                    }
                }
                val impl = LocalStoragePermission.current.value!!
                var cameraPermission by remember { mutableStateOf(false) }
                var cameraPermissionRequested by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    cameraPermission = impl.checkCameraPermission()
                }
                val context = LocalPlatformContext.current
                println("model.cameraLauncher::permissionHandle ${state.value}")
                scope.launch {
                    suspend fun getUri(): Uri?{
                        return AssetLoader.insertImage(context)
                    }
                    cameraPermission = impl.checkCameraPermission()
                    if (cameraPermission.not()) {
                        withContext(Dispatchers.IO) {
                            if (cameraPermissionRequested) {
                                context.goToAppSetting()
                                return@withContext
                            }

                            impl.requestCameraPermission(
                                onGranted = {
                                    cameraPermissionRequested = true
                                    cameraPermission = true
                                    scope.launch {
                                        cameraLauncher.launch(context,getUri())
                                    }
                                },
                                onDenied = {
                                    cameraPermissionRequested = true
                                    showToast(context, "请授予相机权限")
                                    context.goToAppSetting()
                                }
                            )
                        }
                        return@launch
                    } else {
                        cameraLauncher.launch(context,getUri())
                    }
                }
            }
        }
    }

}