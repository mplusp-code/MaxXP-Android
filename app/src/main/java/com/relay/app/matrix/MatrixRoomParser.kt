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
    private val ignoredSystemRooms = mutableSetOf<String>()

    fun parseRawSyncJson(
        rawJson: String,
        selfUserId: String? = null
    ): ParsedSyncBatch {
        val syncResponse = json.decodeFromString<MatrixSyncResponse>(rawJson)
        return parseSyncResponse(syncResponse, selfUserId)
    }

    /**
     * Identifies internal Conduit server rooms ("Admin Room", "@conduit:...") and system
     * appservice registration notices so they never pollute user messenger inboxes or unread counts.
     */
    fun isSystemOrAdminRoom(
        roomId: String,
        roomName: String?,
        events: List<MatrixEvent>
    ): Boolean {
        if (roomId in ignoredSystemRooms) return true

        val lowerName = roomName?.lowercase().orEmpty()
        if (lowerName == "admin room" || lowerName.contains("conduit admin") || lowerName.contains("admin room")) {
            ignoredSystemRooms.add(roomId)
            return true
        }

        val hasConduitOrAppserviceEvent = events.any { ev ->
            val sender = ev.sender?.lowercase().orEmpty()
            val stateKey = ev.stateKey?.lowercase().orEmpty()
            val body = ev.content["body"]?.jsonPrimitive?.content?.lowercase().orEmpty()
            sender.startsWith("@conduit:") ||
                stateKey.startsWith("@conduit:") ||
                body.contains("appservice registered") ||
                body.contains("appservice unregistered")
        }

        if (hasConduitOrAppserviceEvent) {
            ignoredSystemRooms.add(roomId)
            return true
        }
        return false
    }

    fun detectMessengerType(
        roomId: String,
        events: List<MatrixEvent>
    ): MessengerType {
        // Inspect room ID, sender MXIDs, and non-message state metadata (never raw chat message text)
        val metadataHints = buildString {
            append(roomId.lowercase())
            events.forEach { ev ->
                append(" ")
                append(ev.sender?.lowercase().orEmpty())
                append(" ")
                append(ev.stateKey?.lowercase().orEmpty())
                if (ev.type != "m.room.message") {
                    append(" ")
                    append(ev.content.toString().lowercase())
                }
            }
        }

        val detected = when {
            metadataHints.contains("whatsapp") || metadataHints.contains("@whatsappbot") || metadataHints.contains("!wa_") ->
                MessengerType.WHATSAPP
            metadataHints.contains("telegram") || metadataHints.contains("@telegrambot") || metadataHints.contains("!tg_") ->
                MessengerType.TELEGRAM
            metadataHints.contains("instagram") || metadataHints.contains("@instagrambot") || metadataHints.contains("!ig_") ->
                MessengerType.INSTAGRAM
            metadataHints.contains("facebook") || metadataHints.contains("messenger") || metadataHints.contains("@facebookbot") || metadataHints.contains("!fb_") ->
                MessengerType.FACEBOOK
            metadataHints.contains("gmessages") || metadataHints.contains("googlemessages") || metadataHints.contains("@gmessagesbot") || metadataHints.contains("!gm_") ->
                MessengerType.GOOGLE_MESSAGES
            metadataHints.contains("viber") || metadataHints.contains("@viberbot") || metadataHints.contains("!vb_") ->
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
        if (body.contains("appservice registered", ignoreCase = true)) return null
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

            allEvents.firstOrNull { it.type == "m.room.name" }
                ?.content?.get("name")?.jsonPrimitive?.content
                ?.takeIf { it.isNotBlank() }
                ?.let { roomNameCache[roomId] = it }

            val roomName = roomNameCache[roomId]

            // Filter out Conduit Admin Room & system rooms completely
            if (isSystemOrAdminRoom(roomId, roomName, allEvents)) {
                continue
            }

            val serviceType = detectMessengerType(roomId, allEvents)
            val accountId = detectAccountId(roomId, serviceType, allEvents)

            var latestRoomMessage: Pair<Long, MessageItem>? = null
            var hasBotEventsOnly = true

            for (event in timelineEvents) {
                val sender = event.sender.orEmpty()
                if (isBridgeBotSender(sender)) {
                    parseBridgeBotEvent(roomId, serviceType, sender, event)?.let { botUpdates.add(it) }
                    continue
                }

                if (event.type == "m.room.message" && sender != selfUserId) {
                    val snippet = formatMessageSnippet(event) ?: continue
                    val displaySender = roomName ?: cleanMatrixUserId(sender)
                    if (displaySender.equals("admin room", ignoreCase = true) ||
                        displaySender.equals("conduit", ignoreCase = true)
                    ) {
                        continue
                    }
                    hasBotEventsOnly = false
                    val tsMillis = event.originServerTs ?: 0L
                    val timestamp = formatTimestamp(tsMillis)
                    val unreadCount = joinedRoom.unreadNotifications?.notificationCount ?: 0
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

            if (latestRoomMessage != null && !hasBotEventsOnly) {
                val unreadCount = joinedRoom.unreadNotifications?.notificationCount ?: 0
                roomUnreads[roomId] = unreadCount
                serviceTotals[serviceType] = (serviceTotals[serviceType] ?: 0) + unreadCount
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
            lower.startsWith("@gmessagesbot:") ||
            lower.startsWith("@viberbot:")
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

    fun extractRoomName(stateEvents: List<MatrixEvent>, timelineEvents: List<MatrixEvent> = emptyList()): String? {
        val all = stateEvents + timelineEvents
        return all.firstOrNull { it.type == "m.room.name" }
            ?.content?.get("name")?.jsonPrimitive?.content
            ?.takeIf { it.isNotBlank() }
    }

    fun isSystemOrBotControlSender(sender: String?): Boolean {
        if (sender == null) return false
        val lower = sender.lowercase()
        return lower.startsWith("@conduit:") || isBridgeBotSender(lower)
    }

    fun isInternalOrControlRoomName(roomName: String?): Boolean {
        if (roomName == null) return false
        val lower = roomName.lowercase()
        return lower == "admin room" || lower.contains("conduit admin") || lower.contains("appservice")
    }

    fun cleanSenderDisplayName(sender: String): String =
        cleanMatrixUserId(sender)

    fun detectServiceType(
        roomId: String,
        roomName: String? = null,
        stateEvents: List<MatrixEvent> = emptyList(),
        timelineEvents: List<MatrixEvent> = emptyList(),
        fallbackSenderMxid: String? = null
    ): MessengerType {
        val events = stateEvents + timelineEvents + if (fallbackSenderMxid != null) {
            listOf(MatrixEvent(type = "m.room.member", sender = fallbackSenderMxid))
        } else emptyList()
        return detectMessengerType(roomId, events)
    }

    private fun cleanMatrixUserId(userId: String): String =
        userId.removePrefix("@").substringBefore(":")

    private fun formatTimestamp(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0L) return "Just now"
        return SimpleDateFormat("HH:mm", Locale.US).format(Date(epochMillis))
    }
}
