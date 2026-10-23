package com.relay.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import com.relay.app.data.WallpaperTheme

@Composable
fun RelayTheme(
    wallpaperTheme: WallpaperTheme = WallpaperTheme.MEADOW_GREEN,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = Color(wallpaperTheme.secondaryColor),
        secondary = Color(wallpaperTheme.primaryColor),
        background = Color(0xFF000000),
        surface = Color(0xFF142412)
    )

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
    val topColor = Color(theme.primaryColor)
    val bottomColor = Color(theme.secondaryColor)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor)
                )
            )
            .drawBehind {
                // Organic wave bezier sweeps matching Windows Mobile 6.5 satin wave wallpaper
                val wavePath = Path().apply {
                    moveTo(0f, size.height * 0.45f)
                    cubicTo(
                        size.width * 0.35f, size.height * 0.30f,
                        size.width * 0.70f, size.height * 0.65f,
                        size.width, size.height * 0.40f
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(
                    path = wavePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        start = Offset(0f, size.height * 0.4f),
                        end = Offset(size.width, size.height)
                    ),
                    style = Fill
                )

                val wavePath2 = Path().apply {
                    moveTo(0f, size.height * 0.65f)
                    cubicTo(
                        size.width * 0.40f, size.height * 0.80f,
                        size.width * 0.65f, size.height * 0.50f,
                        size.width, size.height * 0.75f
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }

                drawPath(
                    path = wavePath2,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    style = Fill
                )
            }
    ) {
        content()
    }
}
