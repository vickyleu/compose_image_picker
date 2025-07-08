package com.huhx.app.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import coil3.Uri
import coil3.compose.LocalPlatformContext
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterState
import com.github.jing332.filepicker.base.FileImpl
import com.github.jing332.filepicker.base.inputStream
import com.github.jing332.filepicker.base.useImpl
import com.huhx.picker.model.toUri
import com.huhx.picker.provider.AssetLoader
import com.huhx.picker.util.LocalStoragePermission
import com.huhx.picker.util.goToAppSetting
import com.huhx.picker.view.permissionHandle
import com.huhx.picker.view.rememberCameraLauncher
import com.huhx.picker.view.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CameraLaunchViewModel(val toasterState:ToasterState?) : BasicViewModel() {
}

@Composable
fun CameraLaunchScreen(
    modifier: Modifier = Modifier,
    toasterState: ToasterState? = null,
    onImageCaptured: ((String?) -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(color = Color.Red),
    ) {
        val lifecycle = LocalLifecycleOwner.current

        val scope = rememberCoroutineScope()
        val cameraLauncher = rememberCameraLauncher(scope, onCreate = {
            println("cameraLauncher onCreate")
        }) { info ->
            if (info != null) {
                scope.launch {
                    info.toUri().path?.let {
                        val buffer = ByteArray(info.size.toInt())
                        FileImpl(it).inputStream().useImpl {
                            it.read(buffer)
                        }
                        onImageCaptured?.invoke(it)
                        onClose?.invoke()
                        println("bytesRead:  ${it} size=>${info.size.toInt()}")
                    } ?: run {
                        onClose?.invoke()
                    }
                }
            } else {
                scope.launch {
                    onClose?.invoke()
                }
            }
        }
        
        // 显示保存进度指示器
        if (cameraLauncher.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(50.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在保存图片... ${cameraLauncher.savingProgress}%",
                        color = Color.White,
                        fontSize = 16.sp
                    )
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
                suspend fun getUri(): Uri? {
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
                                    cameraLauncher.launch(context, getUri())
                                }
                            },
                            onDenied = {
                                cameraPermissionRequested = true
                                toasterState?.apply {
                                    this.show(
                                        "请授予相机权限",
                                        type = ToastType.Toast
                                    )
                                } ?: showToast(context, "请授予相机权限")
                                context.goToAppSetting()
                            }
                        )
                    }
                    return@launch
                } else {
                    if (cameraIsLaunch) {
                        return@launch
                    }
                    cameraIsLaunch = true
                    cameraLauncher.launch(context, getUri())
                }
            }
        }
    }
}