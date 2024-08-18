package com.huhx.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import cafe.adriel.voyager.transitions.SlideTransition
import com.huhx.app.ui.theme.Compose_image_pickerTheme
import com.huhx.app.view.CameraLaunchScreen
import com.huhx.app.view.DisplayScreen
import com.huhx.app.view.LocalNavigatorController

@Composable
fun App(){
    Compose_image_pickerTheme {
        val startScreen = remember {
            if (true) {
                CameraLaunchScreen()
            } else {
                DisplayScreen()
            }
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
    }
}