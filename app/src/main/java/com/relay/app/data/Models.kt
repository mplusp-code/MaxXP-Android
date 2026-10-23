package com.relay.app.data

enum class MessengerType(
    val displayName: String,
    val badgeChar: String,
    val badgeColorHex: Long,
    val defaultUrl: String
) {
    WHATSAPP("WhatsApp", "W", 0xFF4CAF50, "https://web.whatsapp.com"),
    TELEGRAM("Telegram", "T", 0xFF2196F3, "https://web.telegram.org"),
    INSTAGRAM("Instagram", "I", 0xFFC2185B, "https://www.instagram.com/direct"),
    FACEBOOK("Facebook", "F", 0xFF1976D2, "https://www.messenger.com"),
    VIBER("Viber", "V", 0xFF7B1FA2, "https://www.viber.com"),
    GOOGLE_MESSAGES("Google Messages", "G", 0xFFFFA000, "https://messages.google.com/web")
}

data class ServiceSummary(
    val type: MessengerType,
    val unreadCount: Int // 0 means 'none'
)

data class MessageItem(
    val id: String,
    val sender: String,
    val snippet: String,
    val timestamp: String,
    val serviceType: MessengerType,
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
    val unreadCount: Int = 0
)

enum class WallpaperTheme(val displayName: String, val primaryColor: Long, val secondaryColor: Long) {
    MEADOW_GREEN("Meadow green", 0xFF1B4D20, 0xFF4C8C2B),
    DEEP_TEAL("Deep teal", 0xFF07262E, 0xFF0F4C5C),
    CLASSIC_BLUE("Classic blue", 0xFF0D1B2A, 0xFF1D3557),
    AMBER("Amber", 0xFF4A2800, 0xFFB5651D)
}

enum class UnfoldedOpenMode(val label: String) {
    WORKSPACE("Workspace · list and chat side by side"),
    SPLIT_VIEW("Split view · two chats at once"),
    TODAY_QUICK_REPLIES("Today · quick replies"),
    LAST_USED("Last used")
}

data class SetupPreferences(
    val wallpaperTheme: WallpaperTheme = WallpaperTheme.MEADOW_GREEN,
    val textSizeProgress: Float = 0.5f,
    val showUnreadOnToday: Boolean = true,
    val keepSessionsAlive: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "23:00",
    val quietHoursEnd: String = "07:00",
    val unfoldedOpenMode: UnfoldedOpenMode = UnfoldedOpenMode.WORKSPACE,
    val keepOpenChatsWhenFolding: Boolean = true,
    val openChatOnUnfold: Boolean = true,
    val useFlexModeWhenHalfFolded: Boolean = true,
    val openChatsInTabs: Boolean = false,
    val rememberSecondPane: Boolean = true
)
