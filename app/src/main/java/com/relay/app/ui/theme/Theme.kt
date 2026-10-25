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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.relay.app.data.AccentColor
import com.relay.app.data.ThemeMode
import com.relay.app.data.WallpaperTheme

val LocalTextSizeScale = compositionLocalOf { 1.0f }

@Composable
fun RelayTheme(
    wallpaperTheme: WallpaperTheme = WallpaperTheme.MEADOW_GREEN,
    themeMode: ThemeMode = ThemeMode.DARK,
    accentColor: AccentColor = AccentColor.GREEN,
    textSizeProgress: Float = 0.5f,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.AUTO -> isSystemInDarkTheme()
    }

    val primaryAccent = Color(accentColor.colorHex)
    val secondaryAccent = if (accentColor == AccentColor.GREEN) AccentGreen else primaryAccent.copy(alpha = 0.85f)

    val dynamicColors = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = primaryAccent,
        primaryContainer = primaryAccent.copy(alpha = 0.2f),
        onPrimary = Color.White,
        secondary = secondaryAccent,
        background = if (isDark) DefaultBackgroundDark else DefaultBackgroundLight,
        surface = if (isDark) DefaultSurfaceDark else DefaultSurfaceLight,
        surfaceVariant = if (isDark) DefaultCardDark else DefaultCardLight,
        outline = if (isDark) DefaultBorderDark else DefaultBorderLight,
        onBackground = if (isDark) DefaultTextPrimaryDark else DefaultTextPrimaryLight,
        onSurface = if (isDark) DefaultTextPrimaryDark else DefaultTextPrimaryLight,
        onSurfaceVariant = if (isDark) DefaultTextSecondaryDark else DefaultTextSecondaryLight
    )

    val relayColors = RelayColorScheme(
        background = if (isDark) DefaultBackgroundDark else DefaultBackgroundLight,
        surface = if (isDark) DefaultSurfaceDark else DefaultSurfaceLight,
        card = if (isDark) DefaultCardDark else DefaultCardLight,
        cardElevated = if (isDark) DefaultCardElevatedDark else DefaultCardElevatedLight,
        border = if (isDark) DefaultBorderDark else DefaultBorderLight,
        textPrimary = if (isDark) DefaultTextPrimaryDark else DefaultTextPrimaryLight,
        textSecondary = if (isDark) DefaultTextSecondaryDark else DefaultTextSecondaryLight,
        textMuted = if (isDark) DefaultTextMutedDark else DefaultTextMutedLight,
        accent = primaryAccent
    )

    // Scale typography proportions based on user's text size preference (0.0 .. 1.0 -> 0.82x .. 1.25x)
    val fontScale = 0.82f + (textSizeProgress.coerceIn(0f, 1f) * 0.43f)
    val base = Typography()
    val scaledTypography = Typography(
        displayLarge = base.displayLarge.copy(fontSize = base.displayLarge.fontSize * fontScale),
        displayMedium = base.displayMedium.copy(fontSize = base.displayMedium.fontSize * fontScale),
        displaySmall = base.displaySmall.copy(fontSize = base.displaySmall.fontSize * fontScale),
        headlineLarge = base.headlineLarge.copy(fontSize = base.headlineLarge.fontSize * fontScale),
        headlineMedium = base.headlineMedium.copy(fontSize = base.headlineMedium.fontSize * fontScale),
        headlineSmall = base.headlineSmall.copy(fontSize = base.headlineSmall.fontSize * fontScale),
        titleLarge = base.titleLarge.copy(fontSize = base.titleLarge.fontSize * fontScale),
        titleMedium = base.titleMedium.copy(fontSize = base.titleMedium.fontSize * fontScale),
        titleSmall = base.titleSmall.copy(fontSize = base.titleSmall.fontSize * fontScale),
        bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * fontScale),
        bodyMedium = base.bodyMedium.copy(fontSize = base.bodyMedium.fontSize * fontScale),
        bodySmall = base.bodySmall.copy(fontSize = base.bodySmall.fontSize * fontScale),
        labelLarge = base.labelLarge.copy(fontSize = base.labelLarge.fontSize * fontScale),
        labelMedium = base.labelMedium.copy(fontSize = base.labelMedium.fontSize * fontScale),
        labelSmall = base.labelSmall.copy(fontSize = base.labelSmall.fontSize * fontScale)
    )

    CompositionLocalProvider(
        LocalRelayColors provides relayColors,
        LocalTextSizeScale provides fontScale
    ) {
        MaterialTheme(
            colorScheme = dynamicColors,
            typography = scaledTypography,
            content = {
                RelayWallpaperBackground(theme = wallpaperTheme) {
                    content()
                }
            }
        )
    }
}

@Composable
fun RelayWallpaperBackground(
    theme: WallpaperTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        content()
    }
}
