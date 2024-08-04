package com.huhx.app

import androidx.compose.runtime.Composable
import com.huhx.picker.support.PickerPermissions

@Composable
actual fun permissionHandle(content: @Composable () -> Unit) {
    PickerPermissions {
        content.invoke()
    }
}