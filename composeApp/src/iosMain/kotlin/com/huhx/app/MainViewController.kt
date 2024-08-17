package com.huhx.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.LocalPlatformContext
import com.huhx.app.ui.theme.Compose_image_pickerTheme
import com.huhx.picker.util.LocalStoragePermission
import com.huhx.picker.util.StoragePermissionUtil
import kotlinx.coroutines.MainScope
import platform.UIKit.UIColor
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused")
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
fun MainViewController(): UIViewController {
    val controller = ComposeUIViewController() {
        Compose_image_pickerTheme {
            val context = LocalPlatformContext.current
            val lifecycle = LocalLifecycleOwner.current
            val impl = StoragePermissionUtil(context, lifecycle.lifecycle, MainScope())
            CompositionLocalProvider(LocalStoragePermission provides impl) {
                App()
            }
        }
    }.apply {
        this.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleDark
        this.automaticallyAdjustsScrollViewInsets = false
        this.view.backgroundColor = UIColor.whiteColor
    }
    return controller
}
