package com.relay.app.data

import androidx.compose.ui.graphics.Color
import com.relay.app.ui.theme.RelayColors

enum class WallpaperTheme(
    val displayName: String,
    val primaryColor: Long,
    val secondaryColor: Long
) {
    MEADOW_GREEN("Meadow green", 0xFF1B4D20, 0xFF4C8C2B),
    DEEP_TEAL("Deep teal", 0xFF0A2E38, 0xFF0F4C5C),
    CLASSIC_BLUE("Classic blue", 0xFF102038, 0xFF1D3557),
    AMBER("Amber", 0xFF6B380B, 0xFFB5651D)
}

enum class UnfoldedOpenMode(val label: String) {
    TRI_PANE_INBOX("Tri-pane Inbox"),
    TODAY_OVERVIEW("Today overview"),
    SPLIT_VIEW("Split view (2 chats)")
}

data class SetupPreferences(
    val wallpaperTheme: WallpaperTheme = WallpaperTheme.MEADOW_GREEN,
    val textSizeProgress: Float = 0.5f,
    val showUnreadOnToday: Boolean = true,
    val keepSessionsAlive: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "23:00",
    val quietHoursEnd: String = "07:00",
    val unfoldedOpenMode: UnfoldedOpenMode = UnfoldedOpenMode.TRI_PANE_INBOX,
    val keepOpenChatsWhenFolding: Boolean = true,
    val openChatOnUnfold: Boolean = true,
    val useFlexModeWhenHalfFolded: Boolean = true,
    val openChatsInTabs: Boolean = true,
    val rememberSecondPane: Boolean = true
)

enum class MessengerType(
    val letter: String,
    val displayName: String,
    val badgeColor: Color,
    val badgeColorHex: Long,
    val defaultUrl: String
) {
    WHATSAPP("W", "WhatsApp", RelayColors.BadgeWhatsApp, 0xFF4CAF50, "https://web.whatsapp.com"),
    TELEGRAM("T", "Telegram", RelayColors.BadgeTelegram, 0xFF2196F3, "https://web.telegram.org"),
    INSTAGRAM("I", "Instagram", RelayColors.BadgeInstagram, 0xFFC2185B, "https://www.instagram.com/direct/inbox/"),
    FACEBOOK("F", "Facebook", RelayColors.BadgeFacebook, 0xFF1976D2, "https://www.messenger.com"),
    GOOGLE_MESSAGES("G", "Google Messages", RelayColors.BadgeGoogleMessages, 0xFFFFA000, "https://messages.google.com/web"),
    VIBER("V", "Viber", RelayColors.BadgeViber, 0xFF7B1FA2, "https://account.viber.com");

    val badgeChar: String get() = letter
}

data class ServiceSummary(
    val type: MessengerType,
    val accountCount: Int,
    val unreadCount: Int
)

data class MessageItem(
    val id: String,
    val roomId: String = id,
    val sender: String,
    val snippet: String,
    val timestamp: String,
    val serviceType: MessengerType,
    val accountId: String = "wa_personal",
    val isUnread: Boolean = true
)

data class AccountItem(
    val id: String,
    val accountName: String,
    val serviceType: MessengerType,
    val statusText: String,
    val isConnected: Boolean,
    val isNeedsAttention: Boolean = false,
    val isNotifyEnabled: Boolean = true,
    val unreadCount: Int = 0,
    val botMxid: String? = null,
    val bridgeQrAscii: String? = null
)

enum class RelayTab(val label: String) {
    TODAY("Today"),
    INBOX("Inbox"),
    ACCOUNTS("Accounts"),
    SETUP("Setup");

    val title: String get() = label
}

enum class UnfoldedLayoutMode(val label: String) {
    TRI_PANE("Tri-Pane"),
    SPLIT_VIEW("Split View")
}
