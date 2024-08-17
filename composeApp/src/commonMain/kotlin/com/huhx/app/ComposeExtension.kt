package com.huhx.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal expect fun getTabBarHeight(): Dp
@Composable
internal expect fun getNavigationBarHeight(): Dp