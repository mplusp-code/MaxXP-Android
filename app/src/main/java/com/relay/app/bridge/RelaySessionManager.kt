package com.relay.app.bridge

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.relay.app.data.MessengerType

enum class MetaBotReplyType {
    FLOW_PROMPT,
    COOKIE_PROMPT,
    SUCCESS,
    ERROR,
    OTHER
}

/**
 * Manages isolated storage and multi-profile contexts for Relay accounts.
 * Guarantees zero credential bleeding between multiple accounts of the same service.
 */
object RelaySessionManager {
    private const val TAG = "RelaySessionManager"

    val IG_REQUIRED_COOKIES = setOf("sessionid", "csrftoken", "ds_user_id")
    val IG_OPTIONAL_COOKIES = setOf("rur", "shbid", "shbts", "mid", "ig_did")

    val FB_REQUIRED_COOKIES = setOf("xs", "c_user", "datr")
    val FB_OPTIONAL_COOKIES = setOf("dpr", "sb", "fr", "wd", "presence", "oo")

    fun parseCookieHeader(rawHeader: String?): Map<String, String> {
        if (rawHeader.isNullOrBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        val pairs = rawHeader.split(";")
        for (pair in pairs) {
            val trimmed = pair.trim()
            if (trimmed.isEmpty()) continue
            val eqIdx = trimmed.indexOf('=')
            if (eqIdx > 0) {
                val key = trimmed.substring(0, eqIdx).trim()
                val value = trimmed.substring(eqIdx + 1).trim()
                if (key.isNotEmpty() && value.isNotEmpty()) {
                    result[key] = value
                }
            }
        }
        return result
    }

    fun extractMetaCookiesFromMap(
        allCookies: Map<String, String>,
        service: MessengerType
    ): Map<String, String>? {
        val (required, optional) = when (service) {
            MessengerType.INSTAGRAM -> IG_REQUIRED_COOKIES to IG_OPTIONAL_COOKIES
            MessengerType.FACEBOOK -> FB_REQUIRED_COOKIES to FB_OPTIONAL_COOKIES
            else -> return null
        }

        val missing = required.filter { allCookies[it].isNullOrBlank() }
        if (missing.isNotEmpty()) {
            return null
        }

        val allowed = required + optional
        return allCookies.filterKeys { it in allowed }
    }

    fun serializeMetaCookiesToJson(cookies: Map<String, String>): String {
        return buildString {
            append("{")
            val entries = cookies.entries.toList()
            for (i in entries.indices) {
                val (k, v) = entries[i]
                append("\"").append(escapeJson(k)).append("\":\"").append(escapeJson(v)).append("\"")
                if (i < entries.size - 1) append(",")
            }
            append("}")
        }
    }

    fun redactSensitiveStatus(text: String): String {
        var result = text
        result = result.replace(Regex("\"(sessionid|csrftoken|xs|c_user|datr)\"\\s*:\\s*\"[^\"]+\""), "\"$1\":\"[REDACTED]\"")
        result = result.replace(Regex("(sessionid|csrftoken|xs|c_user|datr)=[^;\\s]+"), "$1=[REDACTED]")
        return result
    }

    fun classifyMetaBotReply(body: String): MetaBotReplyType {
        val clean = body.trim()
        return when {
            clean.contains("specify a login flow", ignoreCase = true) ||
                clean.contains("login flow", ignoreCase = true) -> MetaBotReplyType.FLOW_PROMPT
            clean.contains("JSON object with your cookies", ignoreCase = true) ||
                (clean.contains("cookies", ignoreCase = true) && clean.contains("cURL", ignoreCase = true)) -> MetaBotReplyType.COOKIE_PROMPT
            clean.contains("Logged in as", ignoreCase = true) ||
                clean.contains("Successfully logged in", ignoreCase = true) ||
                clean.equals("Connected", ignoreCase = true) -> MetaBotReplyType.SUCCESS
            clean.contains("challenge_required", ignoreCase = true) ||
                clean.contains("checkpoint", ignoreCase = true) ||
                clean.contains("invalid cookie", ignoreCase = true) ||
                clean.contains("failed to log in", ignoreCase = true) ||
                clean.startsWith("Error:", ignoreCase = true) -> MetaBotReplyType.ERROR
            else -> MetaBotReplyType.OTHER
        }
    }

    fun getMetaCookies(accountId: String, service: MessengerType): Map<String, String>? {
        if (service != MessengerType.INSTAGRAM && service != MessengerType.FACEBOOK) return null
        val profileName = getProfileNameForAccount(accountId, service.displayName)

        val cookieManager: CookieManager? = try {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                ProfileStore.getInstance().getProfile(profileName)?.cookieManager
                    ?: CookieManager.getInstance()
            } else {
                CookieManager.getInstance()
            }
        } catch (_: Throwable) {
            runCatching { CookieManager.getInstance() }.getOrNull()
        }

        if (cookieManager == null) return null

        val merged = mutableMapOf<String, String>()
        when (service) {
            MessengerType.INSTAGRAM -> {
                val igHeader = cookieManager.getCookie("https://www.instagram.com")
                merged.putAll(parseCookieHeader(igHeader))
            }
            MessengerType.FACEBOOK -> {
                val fbHeader = cookieManager.getCookie("https://www.facebook.com")
                val messengerHeader = cookieManager.getCookie("https://www.messenger.com")
                merged.putAll(parseCookieHeader(fbHeader))
                merged.putAll(parseCookieHeader(messengerHeader))
            }
            else -> return null
        }

        return extractMetaCookiesFromMap(merged, service)
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
    }

    fun getProfileNameForAccount(accountId: String, serviceName: String): String {
        return "relay_${serviceName.lowercase()}_$accountId"
    }

    /**
     * Obtains or creates an isolated Profile for the given account.
     * Applies to WebViewCompat.setProfile().
     */
    fun configureProfileForWebView(webView: WebView, accountId: String, serviceName: String) {
        val profileName = getProfileNameForAccount(accountId, serviceName)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            try {
                val profileStore = ProfileStore.getInstance()
                val profile: Profile = profileStore.getOrCreateProfile(profileName)
                WebViewCompat.setProfile(webView, profile.name)
                Log.d(TAG, "Attached isolated profile [$profileName] to WebView for account [$accountId]")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to assign multi-profile [$profileName]", e)
            }
        } else {
            Log.w(TAG, "WebViewFeature.MULTI_PROFILE is not supported on this device/engine.")
        }
    }

    /**
     * Clears cookies and storage for a specific account upon disconnect or session reset.
     */
    fun clearAccountSession(context: Context, accountId: String, serviceName: String) {
        val profileName = getProfileNameForAccount(accountId, serviceName)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            try {
                val profileStore = ProfileStore.getInstance()
                val profile = profileStore.getProfile(profileName)
                profile?.cookieManager?.removeAllCookies(null)
                profile?.webStorage?.deleteAllData()
                Log.d(TAG, "Cleared session storage for profile [$profileName]")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing profile [$profileName]", e)
            }
        }
    }
}
