package com.huhx.app

import androidx.compose.runtime.Composable
@Composable
actual fun permissionHandle(content: @Composable () -> Unit) {
    content.invoke()
}