package com.relay.app.push

import android.content.Context
import android.content.Intent
import com.relay.app.data.MessengerType
import com.relay.app.matrix.MatrixAuthRepository
import com.relay.app.matrix.MatrixRoomParser
import com.relay.app.matrix.MatrixSyncEngine
import com.relay.app.matrix.MatrixSyncResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.unifiedpush.android.connector.MessagingReceiver
import java.util.Calendar

data class ParsedPushPayload(
    val roomId: String? = null,
    val eventId: String? = null,
    val sender: String? = null,
    val body: String? = null,
    val accountId: String? = null,
    val serviceHint: String? = null
)

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

class RelayPushReceiver : MessagingReceiver() {

    companion object {
        const val ACTION_UNIFIED_PUSH_MESSAGE = "org.unifiedpush.android.connector.MESSAGE"
        const val ACTION_UNIFIED_PUSH_NEW_ENDPOINT = "org.unifiedpush.android.connector.NEW_ENDPOINT"
        const val ACTION_UNIFIED_PUSH_UNREGISTERED = "org.unifiedpush.android.connector.UNREGISTERED"
        const val ACTION_RELAY_PUSH_EVENT = "com.relay.app.ACTION_PUSH_EVENT"

        /**
         * Pure parser for UnifiedPush / Matrix Push Gateway payloads, supporting both
         * `event_id_only` (`{"notification":{"event_id":"...","room_id":"..."}}` or flat)
         * and full event notification payloads.
         */
        fun parsePushPayload(rawString: String): ParsedPushPayload {
            if (rawString.isBlank()) return ParsedPushPayload()
            val root = runCatching {
                MatrixSyncEngine.sharedJson.parseToJsonElement(rawString).jsonObject
            }.getOrNull() ?: return ParsedPushPayload()

            val notifObj = root["notification"] as? JsonObject ?: root
            val contentObj = (notifObj["content"] as? JsonObject) ?: (root["content"] as? JsonObject)

            val roomId = notifObj["room_id"]?.jsonPrimitive?.contentOrNull
                ?: root["room_id"]?.jsonPrimitive?.contentOrNull
            val eventId = notifObj["event_id"]?.jsonPrimitive?.contentOrNull
                ?: root["event_id"]?.jsonPrimitive?.contentOrNull
            val sender = notifObj["sender_display_name"]?.jsonPrimitive?.contentOrNull
                ?: notifObj["sender"]?.jsonPrimitive?.contentOrNull
                ?: root["sender_display_name"]?.jsonPrimitive?.contentOrNull
                ?: root["sender"]?.jsonPrimitive?.contentOrNull
            val body = notifObj["body"]?.jsonPrimitive?.contentOrNull
                ?: contentObj?.get("body")?.jsonPrimitive?.contentOrNull
                ?: root["body"]?.jsonPrimitive?.contentOrNull
            val accountId = notifObj["account_id"]?.jsonPrimitive?.contentOrNull
                ?: root["account_id"]?.jsonPrimitive?.contentOrNull
            val serviceHint = notifObj["service"]?.jsonPrimitive?.contentOrNull
                ?: root["service"]?.jsonPrimitive?.contentOrNull

            return ParsedPushPayload(
                roomId = roomId?.takeIf { it.isNotBlank() },
                eventId = eventId?.takeIf { it.isNotBlank() },
                sender = sender?.takeIf { it.isNotBlank() },
                body = body?.takeIf { it.isNotBlank() },
                accountId = accountId?.takeIf { it.isNotBlank() },
                serviceHint = serviceHint?.takeIf { it.isNotBlank() }
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == ACTION_RELAY_PUSH_EVENT || !intent.hasExtra("token")) {
            val appCtx = context.applicationContext
            when (action) {
                ACTION_UNIFIED_PUSH_NEW_ENDPOINT -> {
                    val endpoint = intent.getStringExtra("endpoint").orEmpty()
                    onNewEndpoint(appCtx, endpoint, "default")
                }
                ACTION_UNIFIED_PUSH_UNREGISTERED -> {
                    onUnregistered(appCtx, "default")
                }
                ACTION_UNIFIED_PUSH_MESSAGE, ACTION_RELAY_PUSH_EVENT -> {
                    val rawBytes = intent.getByteArrayExtra("bytesMessage")
                    val rawString = rawBytes?.decodeToString()
                        ?: intent.getStringExtra("message")
                        ?: intent.getStringExtra("payload")
                        .orEmpty()
                    val pending = runCatching { goAsync() }.getOrNull()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            handlePushPayload(appCtx, rawString, intent)
                        } finally {
                            pending?.finish()
                        }
                    }
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onNewEndpoint(context: Context, endpoint: String, instance: String) {
        if (endpoint.isBlank()) return
        val appCtx = context.applicationContext
        val pending = runCatching { goAsync() }.getOrNull()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = MatrixSyncEngine.createDefaultHttpClient()
                val authRepo = MatrixAuthRepository(appCtx, client)
                authRepo.savePushEndpoint(endpoint)
                val engine = MatrixSyncEngine(authRepo, MatrixRoomParser(), client) {}
                engine.registerPusher(
                    pushkey = endpoint,
                    gatewayUrl = MatrixAuthRepository.DEFAULT_PUSH_GATEWAY_URL,
                    enable = true
                )
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onUnregistered(context: Context, instance: String) {
        val appCtx = context.applicationContext
        val pending = runCatching { goAsync() }.getOrNull()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = MatrixSyncEngine.createDefaultHttpClient()
                val authRepo = MatrixAuthRepository(appCtx, client)
                val lastEndpoint = authRepo.getPushEndpoint()
                authRepo.savePushEndpoint(null)
                if (!lastEndpoint.isNullOrBlank()) {
                    val engine = MatrixSyncEngine(authRepo, MatrixRoomParser(), client) {}
                    engine.registerPusher(
                        pushkey = lastEndpoint,
                        gatewayUrl = MatrixAuthRepository.DEFAULT_PUSH_GATEWAY_URL,
                        enable = false
                    )
                }
            } finally {
                pending?.finish()
            }
        }
    }

    override fun onMessage(context: Context, message: ByteArray, instance: String) {
        val appCtx = context.applicationContext
        val rawString = message.decodeToString()
        val pending = runCatching { goAsync() }.getOrNull()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handlePushPayload(appCtx, rawString, null)
            } finally {
                pending?.finish()
            }
        }
    }

    private suspend fun handlePushPayload(
        context: Context,
        rawString: String,
        fallbackIntent: Intent?
    ) {
        val client = MatrixSyncEngine.createDefaultHttpClient()
        val authRepo = MatrixAuthRepository(context, client)
        val roomParser = MatrixRoomParser()
        val engine = MatrixSyncEngine(authRepo, roomParser, client) {}
        val prefs = authRepo.loadSetupPreferences()
        val savedAccounts = authRepo.loadSavedAccounts()
        val creds = authRepo.getCredentials()

        val parsed = parsePushPayload(rawString)
        val roomId = parsed.roomId
            ?: fallbackIntent?.getStringExtra("room_id")?.takeIf { it.isNotBlank() }
            ?: return
        val eventId = parsed.eventId
            ?: fallbackIntent?.getStringExtra("event_id")?.takeIf { it.isNotBlank() }
        val accountId = parsed.accountId
            ?: fallbackIntent?.getStringExtra("account_id").orEmpty()
        var sender = parsed.sender
            ?: fallbackIntent?.getStringExtra("sender")?.takeIf { it.isNotBlank() }
        var snippet = parsed.body
            ?: fallbackIntent?.getStringExtra("body")?.takeIf { it.isNotBlank() }
        val serviceHint = parsed.serviceHint
            ?: fallbackIntent?.getStringExtra("service").orEmpty()

        // Look up cached room metadata if available
        val cachedSync = runCatching {
            MatrixSyncEngine.sharedJson.decodeFromString<MatrixSyncResponse>(authRepo.getCachedSyncJson())
        }.getOrNull()
        val cachedRoom = cachedSync?.rooms?.join?.get(roomId)
        val cachedStateEvents = cachedRoom?.state?.events.orEmpty()
        val cachedRoomName = roomParser.extractRoomName(cachedStateEvents)

        var rawSenderMxid: String? = sender

        // When format is event_id_only, fetch event details via GET /_matrix/client/v3/rooms/{roomId}/event/{eventId}
        if (snippet.isNullOrBlank() && !eventId.isNullOrBlank()) {
            val fetchedEvent = engine.fetchEventDetails(roomId, eventId) ?: return
            if (fetchedEvent.type != "m.room.message") return
            val eventSender = fetchedEvent.sender
            if (!creds.userId.isNullOrBlank() && eventSender == creds.userId) return
            if (roomParser.isSystemOrBotControlSender(eventSender)) return

            val fetchedBody = fetchedEvent.content["body"]?.jsonPrimitive?.contentOrNull
                ?.takeIf { it.isNotBlank() }
                ?: return
            snippet = fetchedBody
            rawSenderMxid = eventSender

            val resolvedRoomName = cachedRoomName ?: engine.fetchRoomName(roomId)
            sender = resolvedRoomName?.takeIf { !roomParser.isInternalOrControlRoomName(it) }
                ?: roomParser.cleanSenderDisplayName(eventSender ?: "Incoming Message")
        }

        if (snippet.isNullOrBlank()) return
        val finalSender = sender ?: cachedRoomName ?: "Incoming Message"
        if (roomParser.isInternalOrControlRoomName(finalSender)) return

        val detectedServiceType = when {
            serviceHint.isNotBlank() -> MessengerType.entries.find {
                it.name.equals(serviceHint, ignoreCase = true)
            }
            cachedRoom != null -> roomParser.detectServiceType(
                roomId = roomId,
                roomName = finalSender,
                stateEvents = cachedStateEvents,
                timelineEvents = cachedRoom.timeline?.events.orEmpty()
            )
            rawSenderMxid != null -> roomParser.detectServiceType(
                roomId = roomId,
                roomName = finalSender,
                fallbackSenderMxid = rawSenderMxid
            )
            else -> null
        }

        // 1. Determine matching account & check per-account Notify toggle
        val matchingAccount = savedAccounts.find { it.id == accountId && accountId.isNotBlank() }
            ?: detectedServiceType?.let { svc -> savedAccounts.firstOrNull { it.serviceType == svc } }

        if (matchingAccount != null && !matchingAccount.isNotifyEnabled) {
            return
        }

        val serviceType = matchingAccount?.serviceType
            ?: detectedServiceType
            ?: savedAccounts.firstOrNull()?.serviceType
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
            sender = finalSender,
            snippet = snippet,
            serviceType = serviceType,
            isQuietHoursActive = isQuiet
        )
    }
}
