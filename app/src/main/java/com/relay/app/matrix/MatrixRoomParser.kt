package com.relay.app.matrix

import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MatrixRoomParser(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
) {
    private val roomServiceCache = mutableMapOf<String, MessengerType>()
    private val roomAccountCache = mutableMapOf<String, String>()
    private val roomNameCache = mutableMapOf<String, String>()

    fun parseRawSyncJson(
        rawJson: String,
        selfUserId: String? = null
    ): ParsedSyncBatch {
        val syncResponse = json.decodeFromString<MatrixSyncResponse>(rawJson)
        return parseSyncResponse(syncResponse, selfUserId)
    }

    fun detectMessengerType(
        roomId: String,
        events: List<MatrixEvent>
    ): MessengerType {
        val combinedHints = buildString {
            append(roomId.lowercase())
            events.forEach { ev ->
                append(" ")
                append(ev.sender?.lowercase().orEmpty())
                append(" ")
                append(ev.stateKey?.lowercase().orEmpty())
                append(" ")
                append(ev.content.toString().lowercase())
            }
        }

        val detected = when {
            combinedHints.contains("whatsapp") || combinedHints.contains("@whatsappbot") || combinedHints.contains("!wa_") ->
                MessengerType.WHATSAPP
            combinedHints.contains("telegram") || combinedHints.contains("@telegrambot") || combinedHints.contains("!tg_") ->
                MessengerType.TELEGRAM
            combinedHints.contains("instagram") || combinedHints.contains("@instagrambot") || combinedHints.contains("!ig_") ->
                MessengerType.INSTAGRAM
            combinedHints.contains("facebook") || combinedHints.contains("messenger") || combinedHints.contains("@facebookbot") || combinedHints.contains("!fb_") ->
                MessengerType.FACEBOOK
            combinedHints.contains("gmessages") || combinedHints.contains("googlemessages") || combinedHints.contains("@gmessagesbot") || combinedHints.contains("!gm_") ->
                MessengerType.GOOGLE_MESSAGES
            combinedHints.contains("viber") || combinedHints.contains("@viberbot") || combinedHints.contains("!vb_") ->
                MessengerType.VIBER
            else -> roomServiceCache[roomId] ?: MessengerType.WHATSAPP
        }
        roomServiceCache[roomId] = detected
        return detected
    }

    fun detectAccountId(
        roomId: String,
        serviceType: MessengerType,
        events: List<MatrixEvent>
    ): String {
        val explicitAccount = events.firstNotNullOfOrNull { ev ->
            ev.content["account_id"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
        }
        if (explicitAccount != null) {
            roomAccountCache[roomId] = explicitAccount
            return explicitAccount
        }

        val lowerRoom = roomId.lowercase()
        val roomName = roomNameCache[roomId]?.lowercase().orEmpty()
        val inferred = when (serviceType) {
            MessengerType.WHATSAPP -> if (lowerRoom.contains("work") || roomName.contains("work")) "wa_work" else "wa_personal"
            MessengerType.TELEGRAM -> "tg_personal"
            MessengerType.INSTAGRAM -> if (lowerRoom.contains("private") || roomName.contains("private")) "ig_private" else "ig_studio"
            MessengerType.FACEBOOK -> "fb_main"
            MessengerType.GOOGLE_MESSAGES -> "gm_main"
            MessengerType.VIBER -> "vb_main"
        }
        roomAccountCache[roomId] = inferred
        return inferred
    }

    fun formatMessageSnippet(event: MatrixEvent): String? {
        if (event.type != "m.room.message") return null
        val msgType = event.content["msgtype"]?.jsonPrimitive?.content ?: return null
        val body = event.content["body"]?.jsonPrimitive?.content.orEmpty()
        return when (msgType) {
            "m.text", "m.notice", "m.emote" -> body.ifBlank { null }
            "m.image" -> "[Image]"
            "m.audio" -> "[Voice message]"
            "m.video" -> "[Video]"
            "m.file" -> "[File]"
            else -> body.ifBlank { null }
        }
    }

    fun parseSyncResponse(
        syncResponse: MatrixSyncResponse,
        selfUserId: String? = null
    ): ParsedSyncBatch {
        val joinedRooms = syncResponse.rooms?.join ?: emptyMap()
        val parsedMessages = mutableListOf<Pair<Long, MessageItem>>()
        val roomUnreads = mutableMapOf<String, Int>()
        val serviceTotals = mutableMapOf<MessengerType, Int>()
        val botUpdates = mutableListOf<BridgeBotQrUpdate>()

        for ((roomId, joinedRoom) in joinedRooms) {
            val stateEvents = joinedRoom.state?.events.orEmpty()
            val timelineEvents = joinedRoom.timeline?.events.orEmpty()
            val allEvents = stateEvents + timelineEvents

            val serviceType = detectMessengerType(roomId, allEvents)

            allEvents.firstOrNull { it.type == "m.room.name" }
                ?.content?.get("name")?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() }
                ?.let { roomNameCache[roomId] = it }

            val accountId = detectAccountId(roomId, serviceType, allEvents)

            val unreadCount = joinedRoom.unreadNotifications?.notificationCount ?: 0
            roomUnreads[roomId] = unreadCount
            serviceTotals[serviceType] = (serviceTotals[serviceType] ?: 0) + unreadCount

            var latestRoomMessage: Pair<Long, MessageItem>? = null

            for (event in timelineEvents) {
                val sender = event.sender.orEmpty()
                if (isBridgeBotSender(sender)) {
                    parseBridgeBotEvent(roomId, serviceType, sender, event)?.let { botUpdates.add(it) }
                    continue
                }

                if (event.type == "m.room.message" && sender != selfUserId) {
                    val snippet = formatMessageSnippet(event) ?: continue
                    val displaySender = roomNameCache[roomId] ?: cleanMatrixUserId(sender)
                    val tsMillis = event.originServerTs ?: 0L
                    val timestamp = formatTimestamp(tsMillis)
                    latestRoomMessage = tsMillis to MessageItem(
                        id = roomId,
                        roomId = roomId,
                        sender = displaySender,
                        snippet = snippet,
                        timestamp = timestamp,
                        serviceType = serviceType,
                        accountId = accountId,
                        isUnread = unreadCount > 0
                    )
                }
            }

            if (latestRoomMessage != null) {
                parsedMessages.add(latestRoomMessage)
            }
        }

        val sortedMessages = parsedMessages
            .sortedByDescending { it.first }
            .map { it.second }

        return ParsedSyncBatch(
            messages = sortedMessages,
            roomUnreadCounts = roomUnreads,
            serviceUnreadTotals = serviceTotals,
            bridgeBotUpdates = botUpdates
        )
    }

    fun isBridgeBotSender(sender: String): Boolean {
        val lower = sender.lowercase()
        return lower.startsWith("@whatsappbot:") ||
            lower.startsWith("@telegrambot:") ||
            lower.startsWith("@instagrambot:") ||
            lower.startsWith("@facebookbot:") ||
            lower.startsWith("@gmessagesbot:")
    }

    private fun parseBridgeBotEvent(
        roomId: String,
        serviceType: MessengerType,
        sender: String,
        event: MatrixEvent
    ): BridgeBotQrUpdate? {
        if (event.type != "m.room.message") return null
        val msgType = event.content["msgtype"]?.jsonPrimitive?.content.orEmpty()
        val body = event.content["body"]?.jsonPrimitive?.content.orEmpty()
        val url = event.content["url"]?.jsonPrimitive?.content

        return when {
            msgType == "m.image" && url?.startsWith("mxc://") == true -> {
                BridgeBotQrUpdate(
                    roomId = roomId,
                    serviceType = serviceType,
                    botSender = sender,
                    mxcUrl = url,
                    qrAsciiOrData = generateAsciiQrPreview(url),
                    statusText = "Scan QR from $sender"
                )
            }
            body.contains("█") || body.contains("▄") || body.startsWith("2@") -> {
                BridgeBotQrUpdate(
                    roomId = roomId,
                    serviceType = serviceType,
                    botSender = sender,
                    qrAsciiOrData = if (body.contains("█") || body.contains("▄")) body else generateAsciiQrPreview(body),
                    statusText = "Scan QR code in Messenger app"
                )
            }
            body.contains("Successfully logged in", ignoreCase = true) ||
                body.contains("Connected", ignoreCase = true) -> {
                BridgeBotQrUpdate(
                    roomId = roomId,
                    serviceType = serviceType,
                    botSender = sender,
                    statusText = "Connected",
                    isConnectedAck = true
                )
            }
            body.isNotBlank() -> {
                BridgeBotQrUpdate(
                    roomId = roomId,
                    serviceType = serviceType,
                    botSender = sender,
                    statusText = body.take(80)
                )
            }
            else -> null
        }
    }

    fun generateAsciiQrPreview(seed: String): String {
        val hash = seed.hashCode()
        val rows = mutableListOf(
            "▄▄▄▄▄▄▄ ▄ ▄▄▄ ▄▄▄▄▄▄▄",
            "█ ▄▄▄ █ ██▀▄▀ █ ▄▄▄ █",
            "█ ███ █ ▄▀█▄▄ █ ███ █",
            "█▄▄▄▄▄█ █ ▄ █ █▄▄▄▄▄█",
            "▄▄ ▄ ▄▄▄█▀▀█▄▄ ▄ ▄▄▄▄",
            if (hash % 2 == 0) "██▄▄█▀▄▀▄▄█▀▄▀█▄▄█▀██" else "▀▄█▀▄▄█▀▄▀██▄▀▄█▀▄▄█▀",
            if (hash % 3 == 0) "▄▀█▄▄▀█▄▀▄▄█▀▄▀█▄▄▀█▄" else "█▄▀▄▄█▀▄▀█▄▄▀█▄▀▄▄█▀▄",
            "▄▄▄▄▄▄▄ █▄▄▀█ ▄ ▄ ▄▄▄",
            "█ ▄▄▄ █ ▄▀█▄▀ ▄▄██▀▀█",
            "█ ███ █ ██▄▀▄▀██▄▄▀██",
            "█▄▄▄▄▄█ █▀▄▀█▄▀█▄▄▀██"
        )
        return rows.joinToString("\n")
    }

    private fun cleanMatrixUserId(userId: String): String =
        userId.removePrefix("@").substringBefore(":")

    private fun formatTimestamp(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0L) return "Just now"
        return SimpleDateFormat("HH:mm", Locale.US).format(Date(epochMillis))
    }
}
