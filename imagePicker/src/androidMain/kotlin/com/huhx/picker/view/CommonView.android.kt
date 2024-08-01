package com.huhx.picker.view

import android.widget.Toast
import coil3.PlatformContext

actual fun showToast(context: PlatformContext, errorMessage: String) {
    Toast
        .makeText(context, errorMessage, Toast.LENGTH_SHORT)
        .show()
}
