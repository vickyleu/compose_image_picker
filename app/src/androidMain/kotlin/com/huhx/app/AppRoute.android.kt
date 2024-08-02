package com.huhx.app

import android.os.Build
import androidx.compose.runtime.Composable
import com.huhx.picker.support.PickerPermissions

@Composable
actual fun permissionHandle(content: @Composable () -> Unit) {
    //判断是Android 31及以上版本
   val permissions = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
        listOf(
            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        )
    }else{
        listOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA
        )
    }
    PickerPermissions(permissions = permissions) {
        content.invoke()
    }
}