package com.relay.app.bridge

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import com.relay.app.data.AccountItem

/**
 * Application-level pool retaining live WebView instances across fold/unfold gestures,
 * pane swaps, and tab transitions without triggering reloads or losing DOM/WebSocket state.
 */
class RelayWebViewPool private constructor(private val appContext: Context) {

    private val pool = mutableMapOf<String, WebView>()
    private val lastActiveTimestamps = mutableMapOf<String, Long>()
    private val hibernatedSessions = mutableSetOf<String>()
    private var globalListener: RelayBridgeListener? = null

    fun setBridgeListener(listener: RelayBridgeListener) {
        this.globalListener = listener
    }

    /**
     * Pre-warms WebView sessions for all saved accounts so background WebSocket connections
     * and DOM incoming message observers remain active even while viewing Today or Inbox List.
     */
    fun preloadAccountSessions(accounts: List<AccountItem>) {
        accounts.forEach { acc ->
            val wv = acquireWebView(
                sessionId = acc.id,
                serviceName = acc.accountName,
                initialUrl = acc.serviceType.defaultUrl
            )
            wv.onResume()
            wv.resumeTimers()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    fun acquireWebView(
        sessionId: String,
        serviceName: String,
        initialUrl: String
    ): WebView {
        lastActiveTimestamps[sessionId] = System.currentTimeMillis()
        hibernatedSessions.remove(sessionId)
        val webView = pool.getOrPut(sessionId) {
            val isFacebookSession = serviceName.contains("Facebook", ignoreCase = true) ||
                sessionId.startsWith("f_") ||
                initialUrl.contains("messenger.com") ||
                initialUrl.contains("facebook.com/messages")

            WebView(appContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#162514"))

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    // Use actual device-width layout instead of zoomed-out 980px tablet overview mode
                    useWideViewPort = true
                    loadWithOverviewMode = false
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

                    override fun onChatListUpdated(id: String, chatsJson: String) {
                        globalListener?.onChatListUpdated(id, chatsJson)
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

                // 4. WebViewClient with responsive script injection & Facebook Messenger enforcement
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val uri = request?.url ?: return false
                        if (isFacebookSession && shouldRedirectFacebookFeedToMessenger(uri)) {
                            view?.loadUrl("https://www.facebook.com/messages/t/")
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (isFacebookSession && url != null) {
                            val uri = runCatching { Uri.parse(url) }.getOrNull()
                            if (uri != null && shouldRedirectFacebookFeedToMessenger(uri)) {
                                view?.loadUrl("https://www.facebook.com/messages/t/")
                                return
                            }
                        }
                        view?.evaluateJavascript(RelayJsBridge.INJECTION_SCRIPT, null)
                    }
                }

                loadUrl(initialUrl)
            }
        }
        webView.onResume()
        webView.resumeTimers()
        return webView
    }

    fun markSessionActive(sessionId: String) {
        if (sessionId.isBlank()) return
        lastActiveTimestamps[sessionId] = System.currentTimeMillis()
        hibernatedSessions.remove(sessionId)
        pool[sessionId]?.let { wv ->
            wv.onResume()
            wv.resumeTimers()
        }
    }

    fun onSessionBackgrounded(sessionId: String, keepSessionsAlive: Boolean) {
        if (sessionId.isBlank()) return
        lastActiveTimestamps[sessionId] = System.currentTimeMillis()
        if (!keepSessionsAlive) {
            pool[sessionId]?.let { wv ->
                wv.onPause()
                hibernatedSessions.add(sessionId)
            }
        }
    }

    fun onAppBackgrounded(keepSessionsAlive: Boolean) {
        if (!keepSessionsAlive) {
            pool.forEach { (id, wv) ->
                wv.onPause()
                hibernatedSessions.add(id)
            }
            pool.values.firstOrNull()?.pauseTimers()
        }
    }

    fun onAppForegrounded(activeSessionIds: Set<String>) {
        pool.values.firstOrNull()?.resumeTimers()
        activeSessionIds.forEach { id -> markSessionActive(id) }
    }

    /**
     * Detaches and pauses offscreen WebViews inactive for > maxIdleMs (default 10 minutes)
     * when `keepSessionsAlive == false`, freeing GPU/rendering surfaces while preserving cookies & IndexedDB.
     */
    fun evictInactiveSurfaces(
        activeSessionIds: Set<String>,
        keepSessionsAlive: Boolean,
        maxIdleMs: Long = 10 * 60 * 1000L,
        nowMs: Long = System.currentTimeMillis()
    ): List<String> {
        if (keepSessionsAlive) return emptyList()
        val evicted = mutableListOf<String>()
        pool.forEach { (sessionId, wv) ->
            if (sessionId !in activeSessionIds) {
                val lastActive = lastActiveTimestamps[sessionId] ?: 0L
                if (nowMs - lastActive >= maxIdleMs) {
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.onPause()
                    hibernatedSessions.add(sessionId)
                    evicted.add(sessionId)
                }
            }
        }
        return evicted
    }

    fun isSessionHibernated(sessionId: String): Boolean = sessionId in hibernatedSessions

    private fun shouldRedirectFacebookFeedToMessenger(uri: Uri): Boolean {
        val host = uri.host?.lowercase() ?: return false
        if (!host.contains("facebook.com")) return false
        val path = uri.path?.lowercase() ?: "/"
        // Allow login, 2FA, checkpoints, oauth, and /messages paths; redirect main feed to /messages/t/
        val isAuthOrMessagesPath = path.startsWith("/messages") ||
            path.startsWith("/login") ||
            path.startsWith("/checkpoint") ||
            path.startsWith("/two_step_verification") ||
            path.startsWith("/recover") ||
            path.startsWith("/oauth") ||
            path.startsWith("/dialog") ||
            path.startsWith("/consent") ||
            path.startsWith("/cookie") ||
            path.startsWith("/ajax") ||
            path.startsWith("/privacy")
        return !isAuthOrMessagesPath && (path == "/" || path == "/home.php" || path.startsWith("/watch") || path.startsWith("/friends") || path.startsWith("/groups"))
    }

    fun detachFromParent(sessionId: String) {
        pool[sessionId]?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
    }

    fun releaseSession(sessionId: String, serviceName: String) {
        lastActiveTimestamps.remove(sessionId)
        hibernatedSessions.remove(sessionId)
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
