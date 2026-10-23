---
name: multi-session-webview
description: Configures isolated multi-account Android WebViews with desktop User-Agent spoofing, anti-automation flags, QR relinking viewports, and JS Notification/Title observers. Use when building chat panes, Split View, or account relinking.
---

# Multi-Session WebView Architecture for Relay

This skill guides the implementation of persistent, sandboxed WebViews that run web messengers (WhatsApp Web, Telegram Web, Instagram Direct, Facebook Messenger, etc.) with desktop browser fidelity and multi-account profile isolation.

---

## 1. Desktop User-Agent & Viewport Spoofing
Web versions of WhatsApp (`web.whatsapp.com`) and Telegram (`web.telegram.org`) aggressively redirect standard mobile Android browsers to mobile landing pages or app store links.

### Mandatory User-Agent:
```text
Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36
```

### Mandatory `WebSettings`:
```kotlin
settings.apply {
    javaScriptEnabled = true
    domStorageEnabled = true
    databaseEnabled = true
    useWideViewPort = true
    loadWithOverviewMode = true
    setSupportZoom(true)
    builtInZoomControls = true
    displayZoomControls = false
    userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
}
```

---

## 2. Multi-Account Sandboxing (`ProfileStore`)
To run multiple sessions of the same service (e.g. `WhatsApp Personal` and `WhatsApp Work`):
1. **WebViewCompat ProfileStore (AndroidX WebKit):**
   - Check if `WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)` is true.
   - Obtain profile via `ProfileStore.getInstance().getProfile(account.profileKey)`.
   - Set the profile onto the `WebView` using `WebViewCompat.setProfile(webView, profile.name)`.
2. **Fallback / Process Suffix Isolation:**
   - In environments where multi-profile is unsupported, initialize per-account data directories using `WebView.setDataDirectorySuffix(account.id)`.

---

## 3. Persistent Keyed Session Pool
Never instantiate `WebView` directly inside Composable functions like `AndroidView(factory = { WebView(it) })` without a cached pool!

### Architecture:
```kotlin
class WebViewSessionPool(private val context: Context) {
    private val pool = mutableMapOf<String, WebView>()

    fun getOrCreate(accountId: String, url: String): WebView {
        return pool.getOrPut(accountId) {
            WebView(context).apply {
                setupWebSettings(this, accountId)
                loadUrl(url)
            }
        }
    }

    fun detachFromParent(accountId: String) {
        pool[accountId]?.let { webView ->
            (webView.parent as? ViewGroup)?.removeView(webView)
        }
    }
}
```
This guarantees:
- Folding / unfolding never triggers `loadUrl()` or page reloads.
- Pane swaps in Split View preserve ongoing draft messages and scroll positions.

---

## 4. JS Bridge (`RelayJsBridge`) & DOM Observers
Inject a JavaScript Interface to extract unread badges and live notifications without heavy polling:

### Javascript Interface:
```kotlin
class RelayJsBridge(private val onUnreadUpdate: (Int) -> Unit) {
    @JavascriptInterface
    fun postUnreadCount(count: Int) {
        onUnreadUpdate(count)
    }
}
```

### Injected Scripts (`onPageFinished`):
```javascript
(function() {
    // 1. Observe Document Title: e.g. "(3) WhatsApp" -> count = 3
    const titleObserver = new MutationObserver(() => {
        const match = document.title.match(/^\((\d+)\)/);
        const count = match ? parseInt(match[1], 10) : 0;
        if (window.RelayBridge) {
            window.RelayBridge.postUnreadCount(count);
        }
    });
    titleObserver.observe(document.querySelector('title') || document.documentElement, {
        subtree: true,
        characterData: true,
        childList: true
    });

    // 2. Intercept window.Notification
    const OriginalNotification = window.Notification;
    window.Notification = function(title, options) {
        if (window.RelayBridge && window.RelayBridge.onNotification) {
            window.RelayBridge.onNotification(title, options ? options.body : '');
        }
        return new OriginalNotification(title, options);
    };
    window.Notification.permission = "granted";
    window.Notification.requestPermission = () => Promise.resolve("granted");
})();
```

---

## 5. QR Relinking Viewport Cropping
For accounts requiring re-authentication (PDF Page 9):
- The `Accounts` detail pane displays an embedded preview focused directly on the QR code DOM node.
- Inject CSS to hide extraneous headers/footers in the login view:
```javascript
const qrContainer = document.querySelector('canvas[aria-label*="Scan"]') || document.querySelector('[data-ref]');
if (qrContainer) {
    qrContainer.scrollIntoView({ behavior: 'smooth', block: 'center' });
}
```
