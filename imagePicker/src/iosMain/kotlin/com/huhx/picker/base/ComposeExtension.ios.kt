package com.huhx.picker.base

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.statusBarManager

@Composable
internal actual fun getTabBarHeight(): Dp {
    return  if(available(OS.Ios to OSVersion(major = 13))){
        val window = UIApplication.sharedApplication.windows.first {
            (it as UIWindow).isKeyWindow()
        } as? UIWindow
        window?.windowScene?.statusBarManager?.statusBarFrame?.useContents {
            with(LocalDensity.current) {
                size.height.toInt().dp
            }
        }?:0.dp
    }else{
        UIApplication.sharedApplication.statusBarFrame.useContents {
            with(LocalDensity.current) {
                size.height.toInt().dp
            }
        }
    }
}