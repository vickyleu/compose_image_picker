package com.huhx.picker.support

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.LocalPlatformContext
import com.huhx.picker.util.LocalStoragePermission
import com.huhx.picker.util.goToAppSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun PickerPermissions(content: @Composable () -> Unit) {
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var permissionsGranted by rememberSaveable { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current
    val storagePermissionUtil = LocalStoragePermission.current
        ?: throw IllegalStateException("LocalStoragePermission not found")
    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
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
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        permissionsGranted = storagePermissionUtil.checkStoragePermission()
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
            val context = LocalPlatformContext.current
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .border(width = 1.dp, color = Color.White, RoundedCornerShape(5.dp))
                    .clickable {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                if (permissionRequested && permissionsGranted.not()) {
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
