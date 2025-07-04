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
import kotlinx.coroutines.withContext


@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun PickerPermissions(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var permissionsGranted by rememberSaveable { mutableStateOf(false) }
    var isCheckingPermission by remember { mutableStateOf(true) }

    val lifecycle = LocalLifecycleOwner.current
    val storagePermissionUtil = LocalStoragePermission.current?:throw IllegalStateException("LocalStoragePermission not found")
    
    val scope = rememberCoroutineScope()

    // 首次渲染时立即检查并申请权限
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val hasPermission = storagePermissionUtil.checkStoragePermission()
            permissionsGranted = hasPermission
            
            if (!hasPermission && !permissionRequested) {
                // 立即申请权限，不显示"无权限!"界面
                permissionRequested = true
                storagePermissionUtil.requestStoragePermission(
                    onGranted = {
                        permissionsGranted = true
                        isCheckingPermission = false
                    }, 
                    onDenied = {
                        permissionsGranted = false
                        isCheckingPermission = false
                    }
                )
            } else {
                isCheckingPermission = false
            }
        }
    }

    val observer = remember {
        var lastEvent = Lifecycle.Event.ON_ANY
        LifecycleEventObserver { source, event ->
            if (lastEvent == event) {
                return@LifecycleEventObserver
            }
            lastEvent = event
            println("PickerPermissions::LifecycleEventObserver::${event.name}")
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        permissionsGranted = storagePermissionUtil.checkStoragePermission()
                        println("PickerPermissions::LifecycleEventObserver::permissionsGranted::${permissionsGranted}")
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

    when {
        permissionsGranted -> {
            content()
        }
        isCheckingPermission -> {
            // 显示加载状态，而不是"无权限!"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(color = Color.White)
            }
        }
        else -> {
            // 只有在用户明确拒绝权限后才显示"无权限!"界面
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
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        isCheckingPermission = true
                                        storagePermissionUtil.requestStoragePermission(onGranted = {
                                            permissionsGranted = true
                                            isCheckingPermission = false
                                        }, onDenied = {
                                            permissionsGranted = false
                                            isCheckingPermission = false
                                        })
                                    } else {
                                        context.goToAppSetting()
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 15.dp, vertical = 10.dp)
                ) {
                    Text(text = "无权限!", fontSize = 20.sp, color = Color.White)
                }
            }
        }
    }
}
