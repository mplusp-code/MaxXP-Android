package com.relay.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.relay.app.bridge.RelayWebViewPool

@Composable
fun RetainedWebView(
    sessionId: String,
    serviceName: String,
    url: String,
    displayUrl: String,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true,
    onReloadRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    val pool = remember { RelayWebViewPool.getInstance(context) }

    val webView = remember(sessionId, url) {
        pool.acquireWebView(sessionId, serviceName, url)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF162514))
    ) {
        if (showHeader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF23361E), Color(0xFF131F10))
                        )
                    )
                    .border(width = 1.dp, color = Color(0xFF38572A))
                    .padding(horizontal = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
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
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = displayUrl,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFF3E602F), Color(0xFF223819))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .clickable {
                                webView.reload()
                                onReloadRequested()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reload",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
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
                        webView
                    },
                    update = { view ->
                        view.requestLayout()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
