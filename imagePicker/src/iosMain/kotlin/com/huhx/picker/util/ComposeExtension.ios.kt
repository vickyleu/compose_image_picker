package com.huhx.picker.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.useContents
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow

@Composable
internal actual fun getNavigationBarHeight(): Dp {
    return with(LocalDensity.current) {
        val fl =
            (UIApplication.sharedApplication.windows.firstOrNull() as? UIWindow)?.safeAreaInsets?.useContents { this.bottom }
                ?.toFloat() ?: 0f
        fl.dp
    }
}