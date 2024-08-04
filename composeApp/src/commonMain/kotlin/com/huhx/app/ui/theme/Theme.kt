package com.huhx.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(64, 151, 246),
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(26, 26, 26),
    surface = Color(26, 26, 26),
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(64, 151, 246),
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color.White,
    surface = Color.White,
    onSurface = Color.Black


    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

expect fun availableKMP(): Boolean
@Composable
expect fun dynamicColorScheme(context: PlatformContext, darkTheme: Boolean): ColorScheme

@Composable
fun Compose_image_pickerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && availableKMP() -> {
            val context = LocalPlatformContext.current
            dynamicColorScheme(context,darkTheme)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

