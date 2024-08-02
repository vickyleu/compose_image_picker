package com.huhx.picker.util

import coil3.PlatformContext

actual fun PlatformContext.vibration(milliseconds: Long) {
    // This function is not supported on iOS
}