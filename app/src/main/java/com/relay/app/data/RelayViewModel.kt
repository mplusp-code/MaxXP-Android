package com.relay.app.data

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class RelayTab(val title: String) {
    TODAY("Today"),
    INBOX("Inbox"),
    ACCOUNTS("Accounts"),
    SETUP("Setup")
}

data class RelayUiState(
    val currentTab: RelayTab = RelayTab.TODAY,
    val services: List<ServiceSummary> = emptyList(),
    val messages: List<MessageItem> = emptyList(),
    val accounts: List<AccountItem> = emptyList(),
    val selectedServiceFilter: MessengerType? = null,
    val activeChatSessionId: String = "wa_personal",
    val splitLeftSessionId: String = "wa_personal",
    val splitRightSessionId: String = "tg_main",
    val selectedAccountId: String = "wa_work",
    val isSplitViewActive: Boolean = false,
    val preferences: SetupPreferences = SetupPreferences()
)

class RelayViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RelayUiState())
    val uiState: StateFlow<RelayUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        val mockServices = listOf(
            ServiceSummary(MessengerType.WHATSAPP, 7),
            ServiceSummary(MessengerType.TELEGRAM, 12),
            ServiceSummary(MessengerType.INSTAGRAM, 5),
            ServiceSummary(MessengerType.FACEBOOK, 3),
            ServiceSummary(MessengerType.VIBER, 0),
            ServiceSummary(MessengerType.GOOGLE_MESSAGES, 2)
        )

        val mockMessages = listOf(
            MessageItem("1", "Dev Team", "Kuba: deploy finished, staging is green", "22:14", MessengerType.TELEGRAM),
            MessageItem("2", "Mom", "Call me when you can", "21:50", MessengerType.WHATSAPP),
            MessageItem("3", "ola.photo", "Sent a reel", "21:32", MessengerType.INSTAGRAM),
            MessageItem("4", "Piotr W.", "Still on for Saturday?", "20:47", MessengerType.FACEBOOK),
            MessageItem("5", "Marta K.", "Sent 12 photos", "20:15", MessengerType.WHATSAPP),
            MessageItem("6", "+48 600 ••• 120", "Your pickup code is ready", "19:20", MessengerType.GOOGLE_MESSAGES),
            MessageItem("7", "Saved messages", "Parcel locker code is pinned", "19:10", MessengerType.TELEGRAM),
            MessageItem("8", "Family", "Dad: photos from Sunday", "18:32", MessengerType.WHATSAPP),
            MessageItem("9", "Ania", "ok, see you there", "18:05", MessengerType.VIBER),
            MessageItem("10", "kuba_dev", "Haha, no way", "17:40", MessengerType.INSTAGRAM)
        )

        val mockAccounts = listOf(
            AccountItem("wa_personal", "WhatsApp · Personal", MessengerType.WHATSAPP, "Connected", isConnected = true, isNotifyEnabled = true, unreadCount = 7),
            AccountItem("wa_work", "WhatsApp · Work", MessengerType.WHATSAPP, "Scan QR to relink", isConnected = false, isNeedsAttention = true, isNotifyEnabled = true),
            AccountItem("tg_main", "Telegram · Main", MessengerType.TELEGRAM, "Connected", isConnected = true, isNotifyEnabled = true, unreadCount = 12),
            AccountItem("ig_studio", "Instagram · studio", MessengerType.INSTAGRAM, "Connected", isConnected = true, isNotifyEnabled = true, unreadCount = 5),
            AccountItem("ig_private", "Instagram · private", MessengerType.INSTAGRAM, "Connected", isConnected = true, isNotifyEnabled = true),
            AccountItem("fb_main", "Facebook · Messenger", MessengerType.FACEBOOK, "Connected", isConnected = true, isNotifyEnabled = true, unreadCount = 3),
            AccountItem("vb_main", "Viber · Main", MessengerType.VIBER, "Session expires in 2 days", isConnected = true, isNeedsAttention = true, isNotifyEnabled = true),
            AccountItem("gm_main", "Messages · Phone pair", MessengerType.GOOGLE_MESSAGES, "Connected", isConnected = true, isNotifyEnabled = true, unreadCount = 2)
        )

        _uiState.value = RelayUiState(
            services = mockServices,
            messages = mockMessages,
            accounts = mockAccounts
        )
    }

    fun selectTab(tab: RelayTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun filterByService(serviceType: MessengerType?) {
        _uiState.update { it.copy(selectedServiceFilter = serviceType) }
    }

    fun selectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun toggleAccountNotify(accountId: String) {
        _uiState.update { state ->
            val updated = state.accounts.map {
                if (it.id == accountId) it.copy(isNotifyEnabled = !it.isNotifyEnabled) else it
            }
            state.copy(accounts = updated)
        }
    }

    fun swapSplitPanes() {
        _uiState.update { state ->
            state.copy(
                splitLeftSessionId = state.splitRightSessionId,
                splitRightSessionId = state.splitLeftSessionId
            )
        }
    }

    fun setSplitView(active: Boolean) {
        _uiState.update { it.copy(isSplitViewActive = active) }
    }

    fun setWallpaper(theme: WallpaperTheme) {
        _uiState.update { it.copy(preferences = it.preferences.copy(wallpaperTheme = theme)) }
    }

    fun updatePreferences(transform: (SetupPreferences) -> SetupPreferences) {
        _uiState.update { it.copy(preferences = transform(it.preferences)) }
    }
}
