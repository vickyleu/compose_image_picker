package com.huhx.picker.support

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun PickerPermission(permission: String, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    return content()
    /*val permissionState = rememberPermissionState(permission) { permissionRequested = true }
    if (permissionState.status.isGranted) return content()

    if (!permissionRequested && !permissionState.status.shouldShowRationale) {
        SideEffect(permissionState::launchPermissionRequest)
    } else if (permissionRequested && permissionState.status.shouldShowRationale) {
        SideEffect(permissionState::launchPermissionRequest)
    } else {
        goToSetting(context)
    }*/
}

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun PickerPermissions(permissions: List<String>, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    return content()
    val permissionState = rememberMultiplePermissionsState(permissions) { permissionRequested = true }
    if (permissionState.allPermissionsGranted) return content()

    if (!permissionRequested && !permissionState.shouldShowRationale) {
        SideEffect(permissionState::launchMultiplePermissionRequest)
    } else if (permissionRequested && permissionState.shouldShowRationale) {
        SideEffect(permissionState::launchMultiplePermissionRequest)
    } else {
        goToSetting(context)
    }
}

@RequiresApi(Build.VERSION_CODES.R)
fun resolveAllFileAccessInfo(context: Context): Intent? {
    val packageManager = context.packageManager
    val intentWrap = try {
        val intent =
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.addCategory("android.intent.category.DEFAULT")
        intent.data = Uri.parse(
            String.format(
                "package:%s",
                context.packageName
            )
        )
        intent
    } catch (e: Exception) {
        val intent = Intent()
        intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
        intent
    }
    val resolveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.resolveActivity(
            intentWrap,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.resolveActivity(intentWrap, 0)?.let {
            val intent = Intent()
            intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
            intent
        }
    }
    return if (resolveInfo != null) intentWrap else null
}

private fun goToSetting(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        resolveAllFileAccessInfo(when (context) {
            is Activity -> context
            is Fragment -> return //context.requireActivity()
            else -> return
        })
    } else {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(intent)
}