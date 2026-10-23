package com.relay.app.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.relay.app.data.MessengerType
import com.relay.app.matrix.MatrixAuthRepository
import com.relay.app.matrix.MatrixRoomParser
import com.relay.app.matrix.MatrixSyncEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Calendar

/**
 * Evaluates Quiet Hours windows (such as `23:00` to `07:00` crossing midnight)
 * and handles high-priority UnifiedPush / Matrix push intents even when the app process is killed.
 */
object QuietHoursEvaluator {
    fun parseTimeToMinutes(timeStr: String, defaultMinutes: Int): Int {
        val parts = timeStr.trim().split(":")
        if (parts.size != 2) return defaultMinutes
        val h = parts[0].toIntOrNull() ?: return defaultMinutes
        val m = parts[1].toIntOrNull() ?: return defaultMinutes
        return (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
    }

    fun isWithinQuietHours(
        nowMinutesOfDay: Int,
        startStr: String = "23:00",
        endStr: String = "07:00"
    ): Boolean {
        val start = parseTimeToMinutes(startStr, 23 * 60)
        val end = parseTimeToMinutes(endStr, 7 * 60)
        return if (start <= end) {
            nowMinutesOfDay in start until end
        } else {
            // Overnight window crossing midnight (e.g., 23:00 to 07:00)
            nowMinutesOfDay >= start || nowMinutesOfDay < end
        }
    }

    fun isCurrentlyQuietHours(quietHoursEnabled: Boolean, startStr: String, endStr: String): Boolean {
        if (!quietHoursEnabled) return false
        val cal = Calendar.getInstance()
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        return isWithinQuietHours(nowMinutes, startStr, endStr)
    }
}

class RelayPushReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_UNIFIED_PUSH_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"
        const val ACTION_UNIFIED_PUSH_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
        const val ACTION_RELAY_PUSH_EVENT = "com.relay.app.ACTION_PUSH_EVENT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val appCtx = context.applicationContext

        when (action) {
            ACTION_UNIFIED_PUSH_NEW_ENDPOINT -> {
                val endpoint = intent.getStringExtra("endpoint").orEmpty()
                if (endpoint.isNotBlank()) {
                    val pending = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val client = MatrixSyncEngine.createDefaultHttpClient()
                            val authRepo = MatrixAuthRepository(appCtx, client)
                            authRepo.savePushEndpoint(endpoint)
                            val engine = MatrixSyncEngine(authRepo, MatrixRoomParser(), client) {}
                            engine.registerPusher(
                                pushkey = endpoint,
                                gatewayUrl = MatrixAuthRepository.DEFAULT_PUSH_GATEWAY_URL
                            )
                        } finally {
                            pending.finish()
                        }
                    }
                }
            }

            ACTION_UNIFIED_PUSH_MESSAGE, ACTION_RELAY_PUSH_EVENT -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        handlePushPayload(appCtx, intent)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private fun handlePushPayload(context: Context, intent: Intent) {
        val client = MatrixSyncEngine.createDefaultHttpClient()
        val authRepo = MatrixAuthRepository(context, client)
        val prefs = authRepo.loadSetupPreferences()
        val savedAccounts = authRepo.loadSavedAccounts()

        val rawBytes = intent.getByteArrayExtra("bytesMessage")
        val rawString = rawBytes?.decodeToString()
            ?: intent.getStringExtra("message")
            ?: intent.getStringExtra("payload")
            .orEmpty()

        val jsonObj = runCatching {
            MatrixSyncEngine.sharedJson.parseToJsonElement(rawString).jsonObject
        }.getOrNull()

        val roomId = jsonObj?.get("room_id")?.jsonPrimitive?.content
            ?: intent.getStringExtra("room_id")
            ?: "!push_room:max-os.mooo.com"
        val accountId = jsonObj?.get("account_id")?.jsonPrimitive?.content
            ?: intent.getStringExtra("account_id")
            .orEmpty()
        val sender = jsonObj?.get("sender_display_name")?.jsonPrimitive?.content
            ?: jsonObj?.get("sender")?.jsonPrimitive?.content
            ?: intent.getStringExtra("sender")
            ?: "Incoming Message"
        val snippet = jsonObj?.get("body")?.jsonPrimitive?.content
            ?: intent.getStringExtra("body")
            ?: "New message received"
        val serviceHint = jsonObj?.get("service")?.jsonPrimitive?.content
            ?: intent.getStringExtra("service")
            .orEmpty()

        // 1. Determine matching account & check per-account Notify toggle
        val matchingAccount = savedAccounts.find { it.id == accountId }
            ?: savedAccounts.find { it.serviceType.name.equals(serviceHint, ignoreCase = true) }

        if (matchingAccount != null && !matchingAccount.isNotifyEnabled) {
            // Per-account notifications disabled -> discard immediately
            return
        }

        val serviceType = matchingAccount?.serviceType
            ?: MessengerType.entries.find { it.name.equals(serviceHint, ignoreCase = true) }
            ?: MessengerType.WHATSAPP

        // 2. Check Quiet Hours (23:00 to 07:00 by default)
        val isQuiet = QuietHoursEvaluator.isCurrentlyQuietHours(
            quietHoursEnabled = prefs.quietHoursEnabled,
            startStr = prefs.quietHoursStart,
            endStr = prefs.quietHoursEnd
        )

        // 3. Dispatch immediate rich system notification with Direct Reply
        RelayNotificationManager.showIncomingMessageNotification(
            context = context,
            roomId = roomId,
            accountId = matchingAccount?.id ?: accountId,
            sender = sender,
            snippet = snippet,
            serviceType = serviceType,
            isQuietHoursActive = isQuiet
        )
    }
}
