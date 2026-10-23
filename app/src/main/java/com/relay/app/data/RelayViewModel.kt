package com.relay.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.relay.app.bridge.RelayBridgeListener
import com.relay.app.bridge.RelayWebViewPool
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
    val activeChatSessionId: String = "tg_main",
    val splitLeftSessionId: String = "wa_personal",
    val splitRightSessionId: String = "tg_main",
    val selectedAccountId: String = "wa_work",
    val isSplitViewActive: Boolean = false,
    val preferences: SetupPreferences = SetupPreferences()
)

class RelayViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(RelayUiState())
    val uiState: StateFlow<RelayUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
        setupBridgeListener()
    }

    private fun setupBridgeListener() {
        val pool = RelayWebViewPool.getInstance(getApplication())
        pool.setBridgeListener(object : RelayBridgeListener {
            override fun onUnreadCountChanged(sessionId: String, unreadCount: Int) {
                updateSessionUnreadCount(sessionId, unreadCount)
            }

            override fun onNotificationReceived(sessionId: String, title: String, message: String, tag: String?) {
                handleIncomingNotification(sessionId, title, message)
            }

            override fun onQrCodeStateChanged(sessionId: String, hasQrCode: Boolean) {
                handleQrStateChanged(sessionId, hasQrCode)
            }

            override fun onSessionReady(sessionId: String) {
                markSessionConnected(sessionId)
            }
        })
    }

    private fun updateSessionUnreadCount(sessionId: String, count: Int) {
        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id == sessionId) acc.copy(unreadCount = count) else acc
            }
            val updatedServices = recalculateServiceSummaries(updatedAccounts, state.services)
            state.copy(accounts = updatedAccounts, services = updatedServices)
        }
    }

    private fun handleIncomingNotification(sessionId: String, title: String, message: String) {
        _uiState.update { state ->
            val account = state.accounts.find { it.id == sessionId }
            val service = account?.serviceType ?: MessengerType.WHATSAPP
            val newMsg = MessageItem(
                id = System.currentTimeMillis().toString(),
                sender = title,
                snippet = message,
                timestamp = "Just now",
                serviceType = service,
                isUnread = true
            )
            state.copy(messages = listOf(newMsg) + state.messages)
        }
    }

    private fun handleQrStateChanged(sessionId: String, hasQrCode: Boolean) {
        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id == sessionId) {
                    if (hasQrCode) {
                        acc.copy(
                            statusText = "Scan QR to relink",
                            isConnected = false,
                            isNeedsAttention = true
                        )
                    } else {
                        acc.copy(
                            statusText = "Connected",
                            isConnected = true,
                            isNeedsAttention = false
                        )
                    }
                } else acc
            }
            state.copy(accounts = updatedAccounts)
        }
    }

    private fun markSessionConnected(sessionId: String) {
        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id == sessionId && !acc.isNeedsAttention) {
                    acc.copy(isConnected = true, statusText = "Connected")
                } else acc
            }
            state.copy(accounts = updatedAccounts)
        }
    }

    private fun recalculateServiceSummaries(
        accounts: List<AccountItem>,
        currentServices: List<ServiceSummary>
    ): List<ServiceSummary> {
        return currentServices.map { svc ->
            val totalForType = accounts
                .filter { it.serviceType == svc.type }
                .sumOf { it.unreadCount }
            svc.copy(unreadCount = totalForType)
        }
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
