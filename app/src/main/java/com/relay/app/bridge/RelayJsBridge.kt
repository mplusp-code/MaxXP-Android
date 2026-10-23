package com.relay.app.bridge

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

interface RelayBridgeListener {
    fun onUnreadCountChanged(sessionId: String, unreadCount: Int)
    fun onNotificationReceived(sessionId: String, title: String, message: String, tag: String?)
    fun onQrCodeStateChanged(sessionId: String, hasQrCode: Boolean)
    fun onSessionReady(sessionId: String)
    fun onChatListUpdated(sessionId: String, chatsJson: String) {}
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

    @JavascriptInterface
    fun postChatList(chatsJson: String) {
        mainHandler.post {
            listener.onChatListUpdated(sessionId, chatsJson)
        }
    }

    companion object {
        const val BRIDGE_NAME = "RelayBridge"

        val INJECTION_SCRIPT = """
            (function() {
                // Always enforce responsive device-width viewport & responsive CSS on every load/resize
                function applyResponsiveFixes() {
                    try {
                        let meta = document.querySelector('meta[name="viewport"]');
                        if (!meta) {
                            meta = document.createElement('meta');
                            meta.name = 'viewport';
                            document.head.appendChild(meta);
                        }
                        meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no';

                        if (!document.getElementById('relay-responsive-style')) {
                            const style = document.createElement('style');
                            style.id = 'relay-responsive-style';
                            style.innerHTML = `
                                html, body {
                                    min-width: 0 !important;
                                    max-width: 100vw !important;
                                    overflow-x: hidden !important;
                                }
                                /* WhatsApp Web responsive adaptation for folded & unfolded screens */
                                #app > div > div,
                                #app .two,
                                #app ._aigs {
                                    min-width: 0 !important;
                                    min-height: 0 !important;
                                    width: 100vw !important;
                                    max-width: 100vw !important;
                                    height: 100vh !important;
                                }
                                @media (max-width: 650px) {
                                    /* On folded narrow screen, allow WhatsApp sidebar and main chat to stack cleanly */
                                    #app ._aigv._aigw {
                                        flex: 1 1 100% !important;
                                        max-width: 100vw !important;
                                    }
                                    #app #main {
                                        width: 100vw !important;
                                        max-width: 100vw !important;
                                    }
                                }
                                /* Hide Facebook main site top banner when inside Facebook Messages */
                                div[role="banner"][aria-label="Facebook"] {
                                    display: none !important;
                                }
                            `;
                            document.head.appendChild(style);
                        }

                        // Redirect Facebook main feed to Facebook Messenger automatically
                        const host = window.location.hostname || '';
                        const path = window.location.pathname || '';
                        if ((host.includes('facebook.com')) &&
                            (path === '/' || path === '/home.php' || path.startsWith('/watch') || path.startsWith('/friends'))) {
                            window.location.replace('https://www.facebook.com/messages/t/');
                        }
                    } catch(e) {}
                }

                applyResponsiveFixes();
                window.addEventListener('resize', applyResponsiveFixes);

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

                // 3. Document Title Observer & DOM Chat List Scraper for Unread & Incoming Messages
                let lastTitleUnread = 0;
                function parseTitle() {
                    const title = document.title || '';
                    const match = title.match(/\((\d+)\)/);
                    const count = match ? parseInt(match[1], 10) : 0;
                    lastTitleUnread = count;
                    if (window.RelayBridge && window.RelayBridge.postUnread) {
                        window.RelayBridge.postUnread(count);
                    }
                }

                function scanDomChats() {
                    try {
                        const host = (window.location.hostname || '').toLowerCase();
                        const results = [];
                        let domUnreadTotal = 0;

                        if (host.includes('whatsapp.com')) {
                            const rows = document.querySelectorAll('#pane-side [role="listitem"], #pane-side [role="row"], div[aria-label="Chat list"] > div');
                            rows.forEach((row) => {
                                if (results.length >= 20) return;
                                const titleEl = row.querySelector('span[dir="auto"][title]');
                                const sender = titleEl ? (titleEl.getAttribute('title') || titleEl.innerText || '').trim() : '';
                                if (!sender) return;

                                const unreadEl = row.querySelector('span[aria-label*="unread message"], span[aria-label*="unread"], div[aria-label*="unread"], [data-testid="icon-unread-count"]');
                                let unreadCount = 0;
                                if (unreadEl) {
                                    const m = ((unreadEl.getAttribute('aria-label') || '') + ' ' + (unreadEl.innerText || '')).match(/(\d+)/);
                                    unreadCount = m ? parseInt(m[1], 10) : 1;
                                }
                                const isUnread = unreadCount > 0;
                                domUnreadTotal += unreadCount;

                                const rawLines = (row.innerText || '').split('\n').map(s => s.trim()).filter(Boolean);
                                const filteredLines = rawLines.filter(l => l !== sender && !/^\d+$/.test(l));
                                const timeRegex = /^(\d{1,2}:\d{2}(\s?[AP]M)?|Yesterday|Today|Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|\d{1,2}[\/\.\-]\d{1,2}([\/\.\-]\d{2,4})?)$/i;
                                const timestamp = filteredLines.find(l => timeRegex.test(l)) || 'Recent';
                                const snippetLines = filteredLines.filter(l => l !== timestamp && l.length > 0);
                                const snippet = snippetLines.join(' ').replace(/\s*:\s*/g, ': ').trim();
                                if (snippet) {
                                    results.push({
                                        sender: sender,
                                        snippet: snippet,
                                        timestamp: timestamp,
                                        unread: isUnread,
                                        unreadCount: unreadCount
                                    });
                                }
                            });
                        } else if (host.includes('facebook.com') || host.includes('messenger.com')) {
                            const rows = document.querySelectorAll('a[href*="/messages/t/"], a[href*="/t/"], div[aria-label*="Chats"] [role="row"], div[role="grid"] [role="row"]');
                            const seenSenders = new Set();
                            rows.forEach((row) => {
                                if (results.length >= 20) return;
                                const rawLines = (row.innerText || '').split('\n')
                                    .map(s => s.trim())
                                    .filter(s => s && s !== '·' && !/^Active\b/i.test(s) && !/^Unread/i.test(s) && !/^Meta AI$/i.test(s));
                                if (rawLines.length < 2) return;
                                const sender = rawLines[0];
                                if (!sender || sender.length > 60 || seenSenders.has(sender)) return;
                                seenSenders.add(sender);

                                const timeRegex = /^(\d+\s?(m|h|d|w|y)|Just now|\d{1,2}:\d{2}(\s?[AP]M)?|Yesterday|Mon|Tue|Wed|Thu|Fri|Sat|Sun)$/i;
                                const timestamp = rawLines.slice(1).find(l => timeRegex.test(l)) || 'Recent';
                                const snippet = rawLines.slice(1).filter(l => l !== timestamp).join(' ').trim();
                                if (!snippet) return;

                                let isUnread = !!row.querySelector('[aria-label*="Unread"], [aria-label*="unread"], [aria-label*="Mark as read"]');
                                if (!isUnread) {
                                    const spans = row.querySelectorAll('span');
                                    for (let i = 1; i < spans.length; i++) {
                                        const fw = window.getComputedStyle(spans[i]).fontWeight;
                                        if (fw === 'bold' || parseInt(fw, 10) >= 600) {
                                            isUnread = true;
                                            break;
                                        }
                                    }
                                }
                                if (isUnread) domUnreadTotal += 1;
                                results.push({
                                    sender: sender,
                                    snippet: snippet,
                                    timestamp: timestamp,
                                    unread: isUnread,
                                    unreadCount: isUnread ? 1 : 0
                                });
                            });
                        } else if (host.includes('telegram.org')) {
                            const rows = document.querySelectorAll('.chat-list .ListItem, .chatlist-chat, a.ListItem-button');
                            rows.forEach((row) => {
                                if (results.length >= 20) return;
                                const senderEl = row.querySelector('.fullName, .peer-title, .title h3');
                                const snippetEl = row.querySelector('.last-message, .subtitle, .dialog-subtitle');
                                const timeEl = row.querySelector('.LastMessageMeta .time, .message-time, .time');
                                const unreadEl = row.querySelector('.Badge.unread, .unread, .dialog-subtitle-badge-unread');
                                const sender = senderEl ? (senderEl.innerText || '').trim() : '';
                                const snippet = snippetEl ? (snippetEl.innerText || '').trim() : '';
                                const timestamp = timeEl ? (timeEl.innerText || '').trim() : 'Recent';
                                if (!sender || !snippet) return;
                                let unreadCount = 0;
                                if (unreadEl) {
                                    const m = (unreadEl.innerText || '').match(/(\d+)/);
                                    unreadCount = m ? parseInt(m[1], 10) : 1;
                                }
                                domUnreadTotal += unreadCount;
                                results.push({
                                    sender: sender,
                                    snippet: snippet,
                                    timestamp: timestamp,
                                    unread: unreadCount > 0,
                                    unreadCount: unreadCount
                                });
                            });
                        } else if (host.includes('instagram.com') || host.includes('google.com')) {
                            const rows = document.querySelectorAll('mws-conversation-list-item, div[role="listitem"], div[aria-label*="Thread"]');
                            rows.forEach((row) => {
                                if (results.length >= 20) return;
                                const rawLines = (row.innerText || '').split('\n')
                                    .map(s => s.trim())
                                    .filter(s => s && s !== '·' && !/^Active\b/i.test(s));
                                if (rawLines.length < 2) return;
                                const sender = rawLines[0];
                                const snippet = rawLines[1];
                                const timestamp = rawLines[2] || 'Recent';
                                const isUnread = !!row.querySelector('[aria-label*="Unread"], [aria-label*="unread"], .unread');
                                if (isUnread) domUnreadTotal += 1;
                                results.push({
                                    sender: sender,
                                    snippet: snippet,
                                    timestamp: timestamp,
                                    unread: isUnread,
                                    unreadCount: isUnread ? 1 : 0
                                });
                            });
                        }

                        if (results.length > 0 && window.RelayBridge && window.RelayBridge.postChatList) {
                            window.RelayBridge.postChatList(JSON.stringify(results));
                        }
                        const effectiveUnread = Math.max(lastTitleUnread, domUnreadTotal);
                        if (window.RelayBridge && window.RelayBridge.postUnread) {
                            window.RelayBridge.postUnread(effectiveUnread);
                        }
                    } catch(e) {}
                }

                const titleElement = document.querySelector('title');
                if (titleElement) {
                    new MutationObserver(parseTitle).observe(titleElement, {
                        subtree: true,
                        characterData: true,
                        childList: true
                    });
                }
                new MutationObserver(() => {
                    parseTitle();
                }).observe(document.documentElement, {
                    childList: true,
                    subtree: true
                });
                setInterval(scanDomChats, 2500);
                setTimeout(scanDomChats, 1000);
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
