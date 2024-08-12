package com.huhx.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.SlideTransition
import coil3.compose.LocalPlatformContext
import com.huhx.app.data.MomentModelFactory
import com.huhx.app.data.MomentRepository
import com.huhx.app.data.MomentViewModel
import com.huhx.app.ui.theme.Compose_image_pickerTheme
import com.huhx.app.view.CameraLaunchScreen
import com.huhx.app.view.CameraLaunchViewModel
import com.huhx.app.view.LocalNavigatorController
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
            val navController = rememberNavController()
            val viewModel: MomentViewModel = viewModel(
                factory = MomentModelFactory(momentRepository = MomentRepository())
            )
            val context = LocalPlatformContext.current
            val lifecycle = LocalLifecycleOwner.current
            val impl = StoragePermissionUtil(context,lifecycle.lifecycle, MainScope())

            CompositionLocalProvider(LocalStoragePermission provides impl) {
                if (false) {
                    val startScreen = remember {
                        CameraLaunchScreen(CameraLaunchViewModel())
                    }
                    Navigator(
                        screen = startScreen,
                        disposeBehavior = NavigatorDisposeBehavior(
                            disposeNestedNavigators = false,
                            disposeSteps = false
                        ),
                        onBackPressed = { currentScreen ->
                            false
                        }
                    ) {
                        CompositionLocalProvider(LocalNavigatorController provides it) {
                            SlideTransition(
                                it, modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            )
                        }
                    }
                } else {
                    AppRoute(navController = navController, viewModel = viewModel)
                }
            }
        }
    }.apply {
        this.overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleDark
        this.automaticallyAdjustsScrollViewInsets = false
        this.view.backgroundColor = UIColor.whiteColor
    }
    return controller
}
