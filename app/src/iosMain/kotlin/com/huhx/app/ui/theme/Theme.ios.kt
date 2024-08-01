package com.huhx.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import coil3.PlatformContext

actual fun availableKMP(): Boolean = true
actual fun dynamicColorScheme(context: PlatformContext, darkTheme: Boolean) : ColorScheme {
    TODO("Not yet implemented")
    /*return ColorScheme(
        primary = Color(64, 151, 246),
        secondary = if (darkTheme) PurpleGrey80 else PurpleGrey40,
        tertiary = if (darkTheme) Pink80 else Pink40,
        background = if (darkTheme) Color(26, 26, 26) else Color.White,
        surface = if (darkTheme) Color(26, 26, 26) else Color.White,
        onSurface = if (darkTheme) Color.White else Color.Black
    )*/
}