package com.huhx.app.ui.theme

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import coil3.PlatformContext

actual fun availableKMP(): Boolean {
   return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
@RequiresApi(Build.VERSION_CODES.S)
@Composable
actual fun dynamicColorScheme(context:PlatformContext, darkTheme: Boolean): ColorScheme {
   return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}