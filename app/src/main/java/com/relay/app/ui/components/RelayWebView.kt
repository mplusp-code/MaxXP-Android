package com.relay.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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

// Desktop Chrome User-Agent so WhatsApp Web & Telegram Web load desktop interfaces
const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

object WebViewSessionManager {
    private val sessionPool = mutableMapOf<String, WebView>()

    @SuppressLint("SetJavaScriptEnabled")
    fun getOrCreateWebView(
        context: Context,
        sessionId: String,
        targetUrl: String,
        onUnreadDetected: (Int) -> Unit = {}
    ): WebView {
        return sessionPool.getOrPut(sessionId) {
            WebView(context.applicationContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = DESKTOP_USER_AGENT
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun postUnread(count: Int) {
                        onUnreadDetected(count)
                    }
                }, "RelayBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        return false // keep in session webview
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        // Inject mutation observer to watch document.title for "(3) WhatsApp" unread badges
                        val script = """
                            (function() {
                                if (window._relayObserverActive) return;
                                window._relayObserverActive = true;
                                const checkTitle = () => {
                                    const match = document.title.match(/^\((\d+)\)/);
                                    if (match && window.RelayBridge) {
                                        window.RelayBridge.postUnread(parseInt(match[1], 10));
                                    }
                                };
                                const obs = new MutationObserver(checkTitle);
                                const titleEl = document.querySelector('title');
                                if (titleEl) {
                                    obs.observe(titleEl, { subtree: true, characterData: true, childList: true });
                                }
                                checkTitle();
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(script, null)
                    }
                }

                loadUrl(targetUrl)
            }
        }
    }

    fun detachFromParent(sessionId: String) {
        sessionPool[sessionId]?.let { wv ->
            (wv.parent as? ViewGroup)?.removeView(wv)
        }
    }
}

@Composable
fun RelayWebView(
    sessionId: String,
    url: String,
    displayUrl: String,
    modifier: Modifier = Modifier,
    onReloadRequested: () -> Unit = {}
) {
    val context = LocalContext.current
    var isReloading by remember { mutableStateOf(false) }

    val webView = remember(sessionId) {
        WebViewSessionManager.getOrCreateWebView(context, sessionId, url)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF162514))
    ) {
        // Top URL and Status Bar
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

        // Web view content
        Box(modifier = Modifier.fillMaxSize().weight(1f)) {
            AndroidView(
                factory = {
                    (webView.parent as? ViewGroup)?.removeView(webView)
                    webView
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
