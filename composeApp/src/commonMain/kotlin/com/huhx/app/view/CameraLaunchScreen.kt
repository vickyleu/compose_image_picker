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
import com.github.jing332.filepicker.base.FileImpl
import com.github.jing332.filepicker.base.inputStream
import com.github.jing332.filepicker.base.useImpl
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
}

class CameraLaunchScreen(
) : BasicScreen<CameraLaunchViewModel>(create = { CameraLaunchViewModel() }) {
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
            val cameraLauncher = rememberCameraLauncher(scope, onCreate = {
                println("cameraLauncher onCreate")
            }) { info ->
                if (info!=null) {
                    scope.launch {
                        info.toUri().path?.let {
                            val buffer = ByteArray(info.size.toInt())
                            FileImpl(it).inputStream().useImpl {
                                it.read(buffer)
                            }
                            navigator.pop()
                            println("bytesRead:  ${it} size=>${info.size.toInt()}")
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
            permissionHandle {
                val state = lifecycle.lifecycle.currentStateFlow.collectAsState()
                val impl = LocalStoragePermission.current!!
                var cameraIsLaunch by remember { mutableStateOf(false) }
                var cameraPermission by remember { mutableStateOf(false) }
                var cameraPermissionRequested by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    cameraPermission = impl.checkCameraPermission()
                }
                val context = LocalPlatformContext.current
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
                        if(cameraIsLaunch){
                            return@launch
                        }
                        cameraIsLaunch = true
                        cameraLauncher.launch(context,getUri())
                    }
                }
            }
        }
    }

}