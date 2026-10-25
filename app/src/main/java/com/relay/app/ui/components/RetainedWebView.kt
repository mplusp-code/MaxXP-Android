package com.relay.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.relay.app.bridge.RelayWebViewPool
import com.relay.app.ui.theme.*

@Composable
fun WebScaleControl(
    zoomPercent: Int,
    onZoomChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardElevated)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Zoom out button [−]
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable {
                    val next = (zoomPercent - 15).coerceAtLeast(40)
                    onZoomChange(next)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "−",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Percentage indicator (tap resets to 100%)
        Text(
            text = "$zoomPercent%",
            color = if (zoomPercent != 100) AccentLimeGreen else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable { onZoomChange(100) }
                .padding(horizontal = 2.dp)
        )

        // Zoom in button [+]
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable {
                    val next = (zoomPercent + 15).coerceAtMost(250)
                    onZoomChange(next)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "+",
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun RetainedWebView(
    sessionId: String,
    serviceName: String,
    url: String,
    displayUrl: String,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    textSizeScale: Float = LocalTextSizeScale.current,
    onReloadRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val pool = remember { RelayWebViewPool.getInstance(context) }

    val webView = remember(sessionId, url) {
        pool.acquireWebView(sessionId, serviceName, url)
    }

    var currentZoom by remember(sessionId) {
        mutableIntStateOf(pool.getZoomLevel(sessionId))
    }

    // Reactively update zoom whenever the user moves the text size slider or adjusts the zoom control
    LaunchedEffect(sessionId, currentZoom, textSizeScale) {
        pool.applyZoomToWebView(webView, currentZoom, textSizeScale)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = displayUrl,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WebScaleControl(
                        zoomPercent = currentZoom,
                        onZoomChange = { newZoom ->
                            currentZoom = newZoom
                            pool.setZoomLevel(sessionId, newZoom)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardElevated)
                            .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                            .clickable {
                                webView.reload()
                                onReloadRequested()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = TextPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }

        // Keyed by sessionId so switching sessions immediately swaps the underlying Android WebView
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            key(sessionId) {
                AndroidView(
                    factory = {
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView.settings.apply {
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        pool.applyZoomToWebView(webView, currentZoom, textSizeScale)
                        webView
                    },
                    update = { view ->
                        view.settings.apply {
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }
                        pool.applyZoomToWebView(view, currentZoom, textSizeScale)
                        view.requestLayout()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
