package com.huhx.picker.util

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.areNavigationBarsVisible
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@SuppressLint("InternalInsetResource", "DiscouragedApi")
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal actual fun getNavigationBarHeight(): Dp {
    return with(LocalDensity.current) {
        val context = LocalContext.current
        if (WindowInsets.areNavigationBarsVisible) {
            val resourceId =
                context.resources.getIdentifier("navigation_bar_height", "dimen", "android")
            return if (resourceId > 0) {
                context.resources.getDimensionPixelSize(resourceId).toDp()
            } else {
                0.dp
            }
        } else {
            0.dp
        }
    }.let {
        if (it <= 0.dp) {
            16.dp
        } else it
    }
}