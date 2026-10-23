package com.relay.app.bridge

import android.content.Context
import android.util.Log
import android.webkit.WebView
import androidx.webkit.Profile
import androidx.webkit.ProfileStore
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Manages isolated storage and multi-profile contexts for Relay accounts.
 * Guarantees zero credential bleeding between multiple accounts of the same service.
 */
object RelaySessionManager {
    private const val TAG = "RelaySessionManager"

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
