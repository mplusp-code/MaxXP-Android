package com.relay.app.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

interface RelayBridgeListener {
    fun onUnreadCountChanged(sessionId: String, unreadCount: Int)
    fun onNotificationReceived(sessionId: String, title: String, message: String, tag: String?)
    fun onQrCodeStateChanged(sessionId: String, hasQrCode: Boolean)
    fun onSessionReady(sessionId: String)
}

class RelayJsBridge(
    private val sessionId: String,
    private val listener: RelayBridgeListener
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun postUnread(count: Int) {
        mainHandler.post {
            listener.onUnreadCountChanged(sessionId, count.coerceAtLeast(0))
        }
    }

    @JavascriptInterface
    fun onNotification(title: String, message: String, tag: String?) {
        mainHandler.post {
            listener.onNotificationReceived(sessionId, title, message, tag)
        }
    }

    @JavascriptInterface
    fun postQrState(isQrVisible: Boolean) {
        mainHandler.post {
            listener.onQrCodeStateChanged(sessionId, isQrVisible)
        }
    }

    @JavascriptInterface
    fun postSessionReady() {
        mainHandler.post {
            listener.onSessionReady(sessionId)
        }
    }

    companion object {
        const val BRIDGE_NAME = "RelayBridge"

        val INJECTION_SCRIPT = """
            (function() {
                if (window._relayInjected) return;
                window._relayInjected = true;

                // 1. Anti-automation & Desktop environment spoofing
                try {
                    Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
                    Object.defineProperty(navigator, 'platform', { get: () => 'Linux x86_64' });
                    Object.defineProperty(navigator, 'vendor', { get: () => 'Google Inc.' });
                    Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 4 });
                    Object.defineProperty(navigator, 'deviceMemory', { get: () => 8 });
                    window.chrome = window.chrome || { runtime: {} };
                } catch(e) {}

                // 2. Intercept window.Notification
                try {
                    const OriginalNotification = window.Notification;
                    window.Notification = function(title, options) {
                        const body = (options && options.body) ? options.body : '';
                        const tag = (options && options.tag) ? options.tag : '';
                        if (window.RelayBridge && window.RelayBridge.onNotification) {
                            window.RelayBridge.onNotification(title, body, tag);
                        }
                        if (OriginalNotification) {
                            try { return new OriginalNotification(title, options); } catch(err) {}
                        }
                        return { close: () => {} };
                    };
                    window.Notification.permission = "granted";
                    window.Notification.requestPermission = () => Promise.resolve("granted");
                } catch(e) {}

                // 3. Document Title Observer for Unread Badge
                function parseTitle() {
                    const title = document.title || '';
                    const match = title.match(/^\((\d+)\)/);
                    const count = match ? parseInt(match[1], 10) : 0;
                    if (window.RelayBridge && window.RelayBridge.postUnread) {
                        window.RelayBridge.postUnread(count);
                    }
                }

                const titleElement = document.querySelector('title');
                if (titleElement) {
                    new MutationObserver(parseTitle).observe(titleElement, {
                        subtree: true,
                        characterData: true,
                        childList: true
                    });
                }
                new MutationObserver(parseTitle).observe(document.documentElement, {
                    childList: true,
                    subtree: true
                });
                parseTitle();

                // 4. WhatsApp QR Canvas Detection & Viewport centering
                function scanForQr() {
                    const qrCanvas = document.querySelector('canvas[aria-label*="Scan"]') ||
                                     document.querySelector('div[data-ref] canvas') ||
                                     document.querySelector('[data-testid="qrcode"]');
                    const hasQr = !!qrCanvas;
                    if (window.RelayBridge && window.RelayBridge.postQrState) {
                        window.RelayBridge.postQrState(hasQr);
                    }
                    if (hasQr && qrCanvas) {
                        qrCanvas.scrollIntoView({ behavior: 'smooth', block: 'center', inline: 'center' });
                    }
                }

                setInterval(scanForQr, 2500);
                scanForQr();

                if (window.RelayBridge && window.RelayBridge.postSessionReady) {
                    window.RelayBridge.postSessionReady();
                }
            })();
        """.trimIndent()
    }
}
