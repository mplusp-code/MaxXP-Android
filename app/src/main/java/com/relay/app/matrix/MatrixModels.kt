package com.relay.app.matrix

import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

enum class MatrixSyncStatus(val label: String, val badgeHex: Long) {
    SYNCED("sync", 0xFF4CAF50),      // Glossy Meadow Green
    CONNECTING("sync…", 0xFF2196F3), // Glossy Blue
    OFFLINE("offline", 0xFFFFA000)   // Glossy Amber Warning
}

enum class QuickReplyStatus {
    IDLE,
    SENDING,
    SENT,
    ERROR
}

@Serializable
data class MatrixLoginRequest(
    val type: String = "m.login.password",
    val identifier: MatrixUserIdentifier,
    val password: String,
    @SerialName("initial_device_display_name")
    val initialDeviceDisplayName: String = "Relay PDA Client"
)

@Serializable
data class MatrixUserIdentifier(
    val type: String = "m.id.user",
    val user: String
)

@Serializable
data class MatrixLoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String? = null
)

@Serializable
data class MatrixWhoAmIResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String? = null
)

@Serializable
data class MatrixSyncResponse(
    @SerialName("next_batch") val nextBatch: String,
    val rooms: MatrixRoomsSection? = null
)

@Serializable
data class MatrixRoomsSection(
    val join: Map<String, MatrixJoinedRoom> = emptyMap()
)

@Serializable
data class MatrixJoinedRoom(
    val timeline: MatrixTimeline? = null,
    val state: MatrixStateSection? = null,
    @SerialName("unread_notifications")
    val unreadNotifications: MatrixUnreadNotifications? = null
)

@Serializable
data class MatrixTimeline(
    val events: List<MatrixEvent> = emptyList(),
    @SerialName("prev_batch") val prevBatch: String? = null
)

@Serializable
data class MatrixStateSection(
    val events: List<MatrixEvent> = emptyList()
)

@Serializable
data class MatrixUnreadNotifications(
    @SerialName("notification_count") val notificationCount: Int = 0,
    @SerialName("highlight_count") val highlightCount: Int = 0
)

@Serializable
data class MatrixEvent(
    @SerialName("event_id") val eventId: String? = null,
    val type: String,
    val sender: String? = null,
    @SerialName("origin_server_ts") val originServerTs: Long? = null,
    @SerialName("state_key") val stateKey: String? = null,
    val content: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class MatrixSendMessageRequest(
    val msgtype: String = "m.text",
    val body: String
)

@Serializable
data class MatrixSendEventResponse(
    @SerialName("event_id") val eventId: String
)

@Serializable
data class MatrixCreateRoomRequest(
    val invite: List<String>,
    @SerialName("is_direct") val isDirect: Boolean = true,
    val preset: String = "trusted_private_chat"
)

@Serializable
data class MatrixCreateRoomResponse(
    @SerialName("room_id") val roomId: String
)

@Serializable
data class MatrixPusherData(
    val url: String = "https://max-os.mooo.com:8448/_matrix/push/v1/notify",
    val format: String = "event_id_only"
)

@Serializable
data class MatrixPusherSetRequest(
    val pushkey: String,
    val kind: String? = "http",
    @SerialName("app_id") val appId: String = "com.relay.messenger",
    @SerialName("app_display_name") val appDisplayName: String = "Relay Android",
    @SerialName("device_display_name") val deviceDisplayName: String = "Relay Foldable PDA",
    @SerialName("profile_tag") val profileTag: String = "relay_default",
    val lang: String = "en",
    val data: MatrixPusherData = MatrixPusherData(),
    val append: Boolean = false
)

data class BridgeBotQrUpdate(
    val roomId: String,
    val serviceType: MessengerType,
    val botSender: String,
    val mxcUrl: String? = null,
    val qrAsciiOrData: String? = null,
    val statusText: String? = null,
    val isConnectedAck: Boolean = false
)

data class ParsedSyncBatch(
    val messages: List<MessageItem>,
    val roomUnreadCounts: Map<String, Int>,
    val serviceUnreadTotals: Map<MessengerType, Int>,
    val bridgeBotUpdates: List<BridgeBotQrUpdate>
)
