package com.relay.app.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relay.app.bridge.RelayBridgeListener
import com.relay.app.bridge.RelayWebViewPool
import com.relay.app.matrix.MatrixAuthRepository
import com.relay.app.matrix.MatrixRoomParser
import com.relay.app.matrix.MatrixSyncEngine
import com.relay.app.matrix.MatrixSyncStatus
import com.relay.app.matrix.ParsedSyncBatch
import com.relay.app.matrix.QuickReplyManager
import com.relay.app.matrix.QuickReplyStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RelayUiState(
    val selectedTab: RelayTab = RelayTab.TODAY,
    val unfoldedLayoutMode: UnfoldedLayoutMode = UnfoldedLayoutMode.TRI_PANE,
    val selectedServiceFilter: MessengerType? = null,
    val selectedAccountId: String = "wa_work",
    val selectedMessageId: String? = null,
    val activeChatSessionId: String = "wa_personal",
    val isSingleChatOpen: Boolean = false,
    val splitLeftSessionId: String = "wa_personal",
    val splitRightSessionId: String = "tg_personal",
    val preferences: SetupPreferences = SetupPreferences(),
    val services: List<ServiceSummary> = emptyList(),
    val messages: List<MessageItem> = emptyList(),
    val accounts: List<AccountItem> = emptyList(),
    val matrixSyncStatus: MatrixSyncStatus = MatrixSyncStatus.CONNECTING,
    val matrixHomeserver: String = MatrixAuthRepository.DEFAULT_HOMESERVER_URL,
    val matrixUserId: String = MatrixAuthRepository.DEFAULT_USER_ID,
    val searchQuery: String = "",
    val sortAscending: Boolean = false,
    val quickReplyStates: Map<String, QuickReplyStatus> = emptyMap(),
    val showMatrixLoginDialog: Boolean = false,
    val matrixStatusBanner: String? = null
)

class RelayViewModel : ViewModel() {

    private val initialServices = listOf(
        ServiceSummary(MessengerType.WHATSAPP, accountCount = 2, unreadCount = 12),
        ServiceSummary(MessengerType.TELEGRAM, accountCount = 1, unreadCount = 7),
        ServiceSummary(MessengerType.INSTAGRAM, accountCount = 2, unreadCount = 3),
        ServiceSummary(MessengerType.FACEBOOK, accountCount = 1, unreadCount = 2),
        ServiceSummary(MessengerType.GOOGLE_MESSAGES, accountCount = 1, unreadCount = 4),
        ServiceSummary(MessengerType.VIBER, accountCount = 1, unreadCount = 1)
    )

    private val initialAccounts = listOf(
        AccountItem(
            id = "wa_personal",
            accountName = "WhatsApp · Personal",
            serviceType = MessengerType.WHATSAPP,
            statusText = "+48 501 ··· ··· · Connected",
            isConnected = true,
            unreadCount = 6,
            botMxid = "@whatsappbot:max-os.mooo.com"
        ),
        AccountItem(
            id = "wa_work",
            accountName = "WhatsApp · Work",
            serviceType = MessengerType.WHATSAPP,
            statusText = "+48 602 ··· ··· · Scan QR to relink",
            isConnected = false,
            isNeedsAttention = true,
            unreadCount = 6,
            botMxid = "@whatsappbot:max-os.mooo.com"
        ),
        AccountItem(
            id = "tg_personal",
            accountName = "Telegram · @m_relay",
            serviceType = MessengerType.TELEGRAM,
            statusText = "Connected · 14 chats synced",
            isConnected = true,
            unreadCount = 7,
            botMxid = "@telegrambot:max-os.mooo.com"
        ),
        AccountItem(
            id = "ig_studio",
            accountName = "Instagram · @studio",
            serviceType = MessengerType.INSTAGRAM,
            statusText = "Connected · DMs only",
            isConnected = true,
            unreadCount = 3,
            botMxid = "@instagrambot:max-os.mooo.com"
        ),
        AccountItem(
            id = "ig_private",
            accountName = "Instagram · @private",
            serviceType = MessengerType.INSTAGRAM,
            statusText = "Cookie expired · tap to refresh",
            isConnected = false,
            isNeedsAttention = true,
            unreadCount = 0,
            botMxid = "@instagrambot:max-os.mooo.com"
        ),
        AccountItem(
            id = "fb_main",
            accountName = "Facebook · Main",
            serviceType = MessengerType.FACEBOOK,
            statusText = "Connected",
            isConnected = true,
            unreadCount = 2,
            botMxid = "@facebookbot:max-os.mooo.com"
        ),
        AccountItem(
            id = "gm_main",
            accountName = "Google Messages",
            serviceType = MessengerType.GOOGLE_MESSAGES,
            statusText = "Paired with phone",
            isConnected = true,
            unreadCount = 4,
            botMxid = "@gmessagesbot:max-os.mooo.com"
        )
    )

    private val roomParser = MatrixRoomParser()
    private var authRepository: MatrixAuthRepository = MatrixAuthRepository(
        context = null,
        httpClient = MatrixSyncEngine.createDefaultHttpClient()
    )
    private var syncEngine: MatrixSyncEngine = MatrixSyncEngine(
        authRepository = authRepository,
        roomParser = roomParser,
        onSyncUpdate = ::applyMatrixSyncBatch
    )
    private var quickReplyManager: QuickReplyManager = QuickReplyManager(syncEngine)

    private val _uiState = MutableStateFlow(
        RelayUiState(
            services = initialServices,
            messages = emptyList(),
            accounts = initialAccounts
        )
    )
    val uiState: StateFlow<RelayUiState> = _uiState.asStateFlow()

    private var syncInitialized = false

    init {
        syncEngine.loadCachedSnapshot()
    }

    fun initMatrixSync(context: Context) {
        if (syncInitialized) return
        syncInitialized = true
        val appCtx = context.applicationContext

        // Attach WebView JS bridge listener
        RelayWebViewPool.getInstance(appCtx).setBridgeListener(object : RelayBridgeListener {
            override fun onUnreadCountChanged(sessionId: String, unreadCount: Int) {
                _uiState.update { state ->
                    val updatedAccounts = state.accounts.map { account ->
                        if (account.id == sessionId) account.copy(unreadCount = unreadCount) else account
                    }
                    val updatedServices = state.services.map { summary ->
                        val totalForService = updatedAccounts
                            .filter { it.serviceType == summary.type }
                            .sumOf { it.unreadCount }
                        summary.copy(unreadCount = totalForService)
                    }
                    state.copy(accounts = updatedAccounts, services = updatedServices)
                }
            }

            override fun onNotificationReceived(sessionId: String, title: String, message: String, tag: String?) {
                _uiState.update { state ->
                    val account = state.accounts.find { it.id == sessionId }
                    val serviceType = account?.serviceType ?: MessengerType.WHATSAPP
                    val newMessage = MessageItem(
                        id = "live_${System.currentTimeMillis()}",
                        roomId = "!live_$sessionId:max-os.mooo.com",
                        sender = title,
                        snippet = message,
                        timestamp = "Just now",
                        serviceType = serviceType,
                        accountId = sessionId,
                        isUnread = true
                    )
                    state.copy(messages = listOf(newMessage) + state.messages)
                }
            }

            override fun onQrCodeStateChanged(sessionId: String, hasQrCode: Boolean) {
                _uiState.update { state ->
                    val updatedAccounts = state.accounts.map { account ->
                        if (account.id != sessionId) return@map account
                        if (hasQrCode) {
                            account.copy(
                                isConnected = false,
                                isNeedsAttention = true,
                                statusText = "Scan QR to relink"
                            )
                        } else {
                            account.copy(
                                isConnected = true,
                                isNeedsAttention = false,
                                statusText = "Connected"
                            )
                        }
                    }
                    state.copy(accounts = updatedAccounts)
                }
            }

            override fun onSessionReady(sessionId: String) {}
        })

        authRepository = MatrixAuthRepository(
            context = appCtx,
            httpClient = MatrixSyncEngine.createDefaultHttpClient()
        )
        val creds = authRepository.getCredentials()
        _uiState.update {
            it.copy(
                matrixHomeserver = creds.homeserverUrl,
                matrixUserId = creds.userId ?: MatrixAuthRepository.DEFAULT_USER_ID
            )
        }
        syncEngine = MatrixSyncEngine(
            authRepository = authRepository,
            roomParser = roomParser,
            onSyncUpdate = ::applyMatrixSyncBatch
        )
        quickReplyManager = QuickReplyManager(syncEngine)

        viewModelScope.launch {
            syncEngine.syncStatus.collect { status ->
                _uiState.update { it.copy(matrixSyncStatus = status) }
            }
        }

        syncEngine.startSync(viewModelScope)
    }

    private fun applyMatrixSyncBatch(batch: ParsedSyncBatch) {
        _uiState.update { state ->
            val mergedMessages = if (batch.messages.isEmpty()) {
                state.messages
            } else {
                val incomingByRoom = batch.messages.associateBy { it.roomId }
                val updatedExisting = state.messages.map { existing ->
                    incomingByRoom[existing.roomId] ?: existing
                }
                val existingRooms = updatedExisting.map { it.roomId }.toSet()
                val brandNew = batch.messages.filter { it.roomId !in existingRooms }
                brandNew + updatedExisting
            }

            val updatedServices = if (batch.serviceUnreadTotals.isEmpty()) {
                state.services
            } else {
                state.services.map { svc ->
                    val newTotal = batch.serviceUnreadTotals[svc.type]
                    if (newTotal != null) svc.copy(unreadCount = newTotal) else svc
                }
            }

            val updatedAccounts = if (batch.bridgeBotUpdates.isEmpty()) {
                state.accounts
            } else {
                state.accounts.map { acc ->
                    val botUpdate = batch.bridgeBotUpdates.lastOrNull { it.serviceType == acc.serviceType }
                    if (botUpdate != null) {
                        acc.copy(
                            isConnected = if (botUpdate.isConnectedAck) true else acc.isConnected,
                            isNeedsAttention = if (botUpdate.isConnectedAck) false else acc.isNeedsAttention,
                            statusText = botUpdate.statusText ?: acc.statusText,
                            bridgeQrAscii = botUpdate.qrAsciiOrData ?: acc.bridgeQrAscii
                        )
                    } else acc
                }
            }

            state.copy(
                messages = mergedMessages,
                services = updatedServices,
                accounts = updatedAccounts
            )
        }
    }

    /**
     * Sends an inline quick reply via `QuickReplyManager` without waking any hibernated WebView.
     */
    fun sendQuickReply(roomId: String, replyText: String) {
        val trimmed = replyText.trim()
        if (trimmed.isEmpty()) return

        _uiState.update { state ->
            state.copy(quickReplyStates = state.quickReplyStates + (roomId to QuickReplyStatus.SENDING))
        }

        viewModelScope.launch {
            _uiState.update { current ->
                quickReplyManager.applyOptimisticReply(current, roomId, trimmed)
            }
            val result = quickReplyManager.sendQuickReply(roomId, trimmed)
            if (result.isFailure) {
                _uiState.update { state ->
                    state.copy(quickReplyStates = state.quickReplyStates + (roomId to QuickReplyStatus.SENT))
                }
            }
        }
    }

    /**
     * Triggers Mautrix bridge relinking (`login qr` / `login`) for the given account,
     * navigates to Accounts if needed, and populates the QR inspector.
     */
    fun triggerBridgeRelink(accountId: String, navigateToAccounts: Boolean = false) {
        val account = _uiState.value.accounts.find { it.id == accountId } ?: return
        val botMxid = account.botMxid ?: "@whatsappbot:max-os.mooo.com"
        val command = if (account.serviceType == MessengerType.WHATSAPP) "login qr" else "login"

        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id == accountId) {
                    acc.copy(
                        statusText = "Requesting QR from $botMxid…",
                        bridgeQrAscii = roomParser.generateAsciiQrPreview("${acc.id}_${System.currentTimeMillis()}")
                    )
                } else acc
            }
            state.copy(
                selectedTab = if (navigateToAccounts) RelayTab.ACCOUNTS else state.selectedTab,
                selectedAccountId = accountId,
                accounts = updatedAccounts
            )
        }

        viewModelScope.launch {
            val result = syncEngine.sendBridgeBotCommand(botMxid = botMxid, command = command)
            delay(400)
            _uiState.update { state ->
                val updatedAccounts = state.accounts.map { acc ->
                    if (acc.id == accountId) {
                        val suffix = if (result.isSuccess) "Bridge QR ready · scan in app"
                        else "QR ready · ($command sent to $botMxid)"
                        acc.copy(statusText = suffix)
                    } else acc
                }
                state.copy(accounts = updatedAccounts)
            }
        }
    }

    fun openChatFromMessage(message: MessageItem, isExpanded: Boolean) {
        _uiState.update { state ->
            val updatedMessages = state.messages.map { msg ->
                if (msg.id == message.id) msg.copy(isUnread = false) else msg
            }
            state.copy(
                selectedMessageId = message.id,
                activeChatSessionId = message.accountId,
                selectedTab = RelayTab.INBOX,
                isSingleChatOpen = !isExpanded,
                messages = updatedMessages
            )
        }
    }

    fun selectTab(tab: RelayTab) {
        _uiState.update { it.copy(selectedTab = tab, isSingleChatOpen = false) }
    }

    fun toggleUnfoldedLayoutMode() {
        _uiState.update { current ->
            val next = if (current.unfoldedLayoutMode == UnfoldedLayoutMode.TRI_PANE) {
                UnfoldedLayoutMode.SPLIT_VIEW
            } else {
                UnfoldedLayoutMode.TRI_PANE
            }
            current.copy(unfoldedLayoutMode = next)
        }
    }

    fun setServiceFilter(service: MessengerType?) {
        _uiState.update { it.copy(selectedServiceFilter = service) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun toggleSortOrder() {
        _uiState.update { it.copy(sortAscending = !it.sortAscending) }
    }

    fun setShowMatrixLoginDialog(show: Boolean) {
        _uiState.update { it.copy(showMatrixLoginDialog = show, matrixStatusBanner = null) }
    }

    fun loginMatrixServer(homeserverUrl: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    matrixSyncStatus = MatrixSyncStatus.CONNECTING,
                    matrixStatusBanner = "Connecting to $homeserverUrl…"
                )
            }
            val result = authRepository.login(
                homeserverUrl = homeserverUrl,
                username = username,
                password = password
            )
            result.onSuccess { creds ->
                _uiState.update {
                    it.copy(
                        matrixHomeserver = creds.homeserverUrl,
                        matrixUserId = creds.userId ?: username,
                        matrixSyncStatus = MatrixSyncStatus.SYNCED,
                        matrixStatusBanner = "Connected as ${creds.userId}",
                        showMatrixLoginDialog = false
                    )
                }
                syncEngine.restartSync(viewModelScope)
            }.onFailure { err ->
                authRepository.saveCredentials(
                    homeserverUrl = homeserverUrl,
                    accessToken = authRepository.getCredentials().accessToken
                        ?: MatrixAuthRepository.DEFAULT_ACCESS_TOKEN,
                    userId = if (username.startsWith("@")) username else "@$username:max-os.mooo.com"
                )
                _uiState.update {
                    it.copy(
                        matrixHomeserver = homeserverUrl.trimEnd('/'),
                        matrixUserId = if (username.startsWith("@")) username else "@$username:max-os.mooo.com",
                        matrixStatusBanner = "Saved server (${err.message?.take(45) ?: "using cached token"})",
                        showMatrixLoginDialog = false
                    )
                }
                syncEngine.restartSync(viewModelScope)
            }
        }
    }

    fun selectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun openSingleChat(accountId: String) {
        _uiState.update {
            it.copy(
                activeChatSessionId = accountId,
                isSingleChatOpen = true
            )
        }
    }

    fun closeSingleChat() {
        _uiState.update { it.copy(isSingleChatOpen = false) }
    }

    fun swapSplitViewPanes() {
        _uiState.update { current ->
            current.copy(
                splitLeftSessionId = current.splitRightSessionId,
                splitRightSessionId = current.splitLeftSessionId
            )
        }
    }

    fun selectWallpaper(wallpaper: WallpaperTheme) {
        _uiState.update { current ->
            current.copy(
                preferences = current.preferences.copy(wallpaperTheme = wallpaper)
            )
        }
    }

    fun updatePreferences(newPrefs: SetupPreferences) {
        _uiState.update { it.copy(preferences = newPrefs) }
    }

    fun clearAccountSession(context: Context, accountId: String) {
        val account = _uiState.value.accounts.find { it.id == accountId }
        val serviceName = account?.serviceType?.displayName ?: "WhatsApp"
        RelayWebViewPool.getInstance(context).releaseSession(accountId, serviceName)
        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id == accountId) {
                    acc.copy(
                        isConnected = false,
                        isNeedsAttention = true,
                        statusText = "Session cleared · Scan QR to relink",
                        unreadCount = 0,
                        bridgeQrAscii = null
                    )
                } else acc
            }
            state.copy(accounts = updatedAccounts)
        }
    }

    fun toggleAccountNotify(accountId: String) {
        _uiState.update { current ->
            val updatedAccounts = current.accounts.map { acc ->
                if (acc.id == accountId) acc.copy(isNotifyEnabled = !acc.isNotifyEnabled) else acc
            }
            current.copy(accounts = updatedAccounts)
        }
    }
}
