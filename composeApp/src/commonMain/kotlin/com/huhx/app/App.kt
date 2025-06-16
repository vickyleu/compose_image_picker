package com.huhx.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.huhx.app.ui.theme.Compose_image_pickerTheme
import com.huhx.app.view.DisplayScreen

@Composable
fun App(){
    Compose_image_pickerTheme {
        DisplayScreen(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        )
    }
}