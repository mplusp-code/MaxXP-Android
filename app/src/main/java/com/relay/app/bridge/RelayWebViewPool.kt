package com.relay.app.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.webkit.*

/**
 * Application-level pool retaining live WebView instances across fold/unfold gestures,
 * pane swaps, and tab transitions without triggering reloads or losing DOM/WebSocket state.
 */
class RelayWebViewPool private constructor(private val appContext: Context) {

    private val pool = mutableMapOf<String, WebView>()
    private var globalListener: RelayBridgeListener? = null

    fun setBridgeListener(listener: RelayBridgeListener) {
        this.globalListener = listener
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun acquireWebView(
        sessionId: String,
        serviceName: String,
        initialUrl: String
    ): WebView {
        return pool.getOrPut(sessionId) {
            WebView(appContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#162514"))

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mediaPlaybackRequiresUserGesture = false
                    userAgentString =
                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
                }

                // 1. Isolate Storage & Cookies per account profile
                RelaySessionManager.configureProfileForWebView(this, sessionId, serviceName)

                // 2. Attach Bridge
                val bridge = RelayJsBridge(sessionId, object : RelayBridgeListener {
                    override fun onUnreadCountChanged(id: String, unreadCount: Int) {
                        globalListener?.onUnreadCountChanged(id, unreadCount)
                    }

                    override fun onNotificationReceived(id: String, title: String, message: String, tag: String?) {
                        globalListener?.onNotificationReceived(id, title, message, tag)
                    }

                    override fun onQrCodeStateChanged(id: String, hasQrCode: Boolean) {
                        globalListener?.onQrCodeStateChanged(id, hasQrCode)
                    }

                    override fun onSessionReady(id: String) {
                        globalListener?.onSessionReady(id)
                    }
                })
                addJavascriptInterface(bridge, RelayJsBridge.BRIDGE_NAME)

                // 3. WebChromeClient for WebRTC voice note permissions
                webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.let {
                            val requestedResources = it.resources
                            if (requestedResources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
                                it.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                            } else {
                                it.grant(it.resources)
                            }
                        }
                    }
                }

                // 4. WebViewClient with script injection
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        return false // keep in session webview
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        view?.evaluateJavascript(RelayJsBridge.INJECTION_SCRIPT, null)
                    }
                }

                loadUrl(initialUrl)
            }
        }
    }

    /**
     * Detaches a WebView from its current Composable AndroidView parent so it can be cleanly
     * reattached into another container (e.g. Split View pane swap or fold/unfold layout rebuild).
     */
    fun detachFromParent(sessionId: String) {
        pool[sessionId]?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
    }

    /**
     * Releases an individual session when the user explicitly removes the account.
     */
    fun releaseSession(sessionId: String, serviceName: String) {
        pool.remove(sessionId)?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.stopLoading()
            webView.clearHistory()
            webView.removeAllViews()
            webView.destroy()
        }
        RelaySessionManager.clearAccountSession(appContext, sessionId, serviceName)
    }

    companion object {
        @Volatile
        private var instance: RelayWebViewPool? = null

        fun getInstance(context: Context): RelayWebViewPool {
            return instance ?: synchronized(this) {
                instance ?: RelayWebViewPool(context.applicationContext).also { instance = it }
            }
        }
    }
}
