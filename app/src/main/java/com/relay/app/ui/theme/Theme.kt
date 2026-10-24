package com.relay.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.relay.app.data.AccentColor
import com.relay.app.data.ThemeMode
import com.relay.app.data.WallpaperTheme

@Composable
fun RelayTheme(
    wallpaperTheme: WallpaperTheme = WallpaperTheme.MEADOW_GREEN,
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColor = AccentColor.GREEN,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val primaryAccent = Color(accentColor.colorHex)

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryAccent,
            secondary = AccentGreen,
            background = BackgroundDark,
            surface = SurfaceDark,
            surfaceVariant = CardDark,
            outline = BorderDark,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            secondary = AccentGreen,
            background = Color(0xFFF4F4F6),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFEAEAEE),
            outline = Color(0xFFD4D4D8),
            onBackground = Color(0xFF18181B),
            onSurface = Color(0xFF18181B)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = {
            RelayWallpaperBackground(theme = wallpaperTheme) {
                content()
            }
        }
    )
}

@Composable
fun RelayWallpaperBackground(
    theme: WallpaperTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Clean modern flat surface (#121214) without retro wave textures
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        content()
    }
}
