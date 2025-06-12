package com.huhx.picker.support

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.huhx.picker.util.LocalStoragePermission
import com.huhx.picker.util.goToAppSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun PickerPermissions(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var permissionsGranted by rememberSaveable { mutableStateOf(false) }

    var cameraSettingsIsLaunch by remember { mutableStateOf(false) }
    var cameraIsLaunch by remember { mutableStateOf(false) }

    val lifecycle = LocalLifecycleOwner.current
    val storagePermissionUtil = LocalStoragePermission.current?:throw IllegalStateException("LocalStoragePermission not found")
    LaunchedEffect(permissionsGranted) {
        withContext(Dispatchers.IO) {
            println("PickerPermissions::LifecycleEventObserver::11111")
            permissionsGranted = storagePermissionUtil.checkStoragePermission()
        }
    }
    val scope = rememberCoroutineScope()

    val observer = remember {
        var lastEvent = Lifecycle.Event.ON_ANY
        LifecycleEventObserver { source, event ->
            if (lastEvent == event) {
                return@LifecycleEventObserver
            }
            lastEvent = event
            println("PickerPermissions::LifecycleEventObserver::${event.name}")
            if (event == Lifecycle.Event.ON_RESUME) {
                runBlocking {
                    withContext(Dispatchers.IO) {
                        permissionsGranted = storagePermissionUtil.checkStoragePermission()
                        println("PickerPermissions::LifecycleEventObserver::permissionsGranted::${permissionsGranted}")
                    }
                }
            }
        }
    }
    if (permissionsGranted) {
        return content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentSize()
                    .border(width = 1.dp, color = Color.White, RoundedCornerShape(5.dp))
                    .clickable {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (permissionRequested && permissionsGranted.not()) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        storagePermissionUtil.requestStoragePermission(onGranted = {
                                            permissionsGranted = true
                                        }, onDenied = {
                                            permissionsGranted = false
                                        })
                                    } else {
                                        context.goToAppSetting()
                                    }
                                }
                            }
                        }

                    }
                    .padding(horizontal = 15.dp, vertical = 10.dp)
            ) {
                Text(text = "无权限!", fontSize = 20.sp, color = Color.White)
            }

        }
        if (permissionRequested.not()) {
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    storagePermissionUtil.checkStoragePermission().let {
                        permissionsGranted = it

                        if (it.not()) {
                            permissionRequested = true
                            storagePermissionUtil.requestStoragePermission(onGranted = {
                                permissionsGranted = true
                            }, onDenied = {
                                permissionsGranted = false
                            })
                        }
                    }
                }
            }
        }
    }
    DisposableEffect(Unit) {
        lifecycle.lifecycle.addObserver(observer)
        onDispose {
            lifecycle.lifecycle.removeObserver(observer)
        }
    }
}
