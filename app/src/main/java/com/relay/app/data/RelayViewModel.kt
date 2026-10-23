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
    val selectedAccountId: String = "",
    val selectedMessageId: String? = null,
    val activeChatSessionId: String = "",
    val isSingleChatOpen: Boolean = false,
    val splitLeftSessionId: String = "",
    val splitRightSessionId: String = "",
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
    val showAddAccountDialog: Boolean = false,
    val matrixStatusBanner: String? = null
)

class RelayViewModel : ViewModel() {

    private val initialServices = MessengerType.entries.map { type ->
        ServiceSummary(type = type, accountCount = 0, unreadCount = 0)
    }

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
            accounts = emptyList()
        )
    )
    val uiState: StateFlow<RelayUiState> = _uiState.asStateFlow()

    private var syncInitialized = false

    fun initMatrixSync(context: Context) {
        if (syncInitialized) return
        syncInitialized = true
        val appCtx = context.applicationContext

        RelayWebViewPool.getInstance(appCtx).setBridgeListener(object : RelayBridgeListener {
            override fun onUnreadCountChanged(sessionId: String, unreadCount: Int) {
                _uiState.update { state ->
                    val updatedAccounts = state.accounts.map { account ->
                        if (account.id == sessionId) account.copy(unreadCount = unreadCount) else account
                    }
                    state.copy(
                        accounts = updatedAccounts,
                        services = recomputeServices(state.services, updatedAccounts)
                    )
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
        val savedAccounts = authRepository.loadSavedAccounts()
        val firstId = savedAccounts.firstOrNull()?.id.orEmpty()
        val secondId = savedAccounts.getOrNull(1)?.id ?: firstId

        _uiState.update { state ->
            state.copy(
                matrixHomeserver = creds.homeserverUrl,
                matrixUserId = creds.userId ?: MatrixAuthRepository.DEFAULT_USER_ID,
                accounts = savedAccounts,
                selectedAccountId = firstId,
                activeChatSessionId = firstId,
                splitLeftSessionId = firstId,
                splitRightSessionId = secondId,
                services = recomputeServices(state.services, savedAccounts)
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

    private fun recomputeServices(
        currentServices: List<ServiceSummary>,
        accounts: List<AccountItem>,
        matrixTotals: Map<MessengerType, Int> = emptyMap()
    ): List<ServiceSummary> {
        return currentServices.map { svc ->
            val matching = accounts.filter { it.serviceType == svc.type }
            val webViewUnread = matching.sumOf { it.unreadCount }
            val matrixUnread = matrixTotals[svc.type] ?: 0
            svc.copy(
                accountCount = matching.size,
                unreadCount = maxOf(webViewUnread, matrixUnread)
            )
        }
    }

    fun setShowAddAccountDialog(show: Boolean) {
        _uiState.update { it.copy(showAddAccountDialog = show) }
    }

    fun addAccount(serviceType: MessengerType, customLabel: String) {
        val cleanLabel = customLabel.trim().ifBlank { serviceType.displayName }
        val fullName = if (cleanLabel.startsWith(serviceType.displayName, ignoreCase = true)) {
            cleanLabel
        } else {
            "${serviceType.displayName} · $cleanLabel"
        }
        val slug = cleanLabel.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "main" }
        val baseId = "${serviceType.letter.lowercase()}_$slug"
        val uniqueId = if (_uiState.value.accounts.any { it.id == baseId }) {
            "${baseId}_${System.currentTimeMillis() % 1000}"
        } else {
            baseId
        }

        val newAccount = AccountItem(
            id = uniqueId,
            accountName = fullName,
            serviceType = serviceType,
            statusText = "Scan QR or sign in to connect",
            isConnected = false,
            isNeedsAttention = true,
            isNotifyEnabled = true,
            unreadCount = 0,
            botMxid = MatrixAuthRepository.defaultBotForService(serviceType)
        )

        _uiState.update { state ->
            val updatedAccounts = state.accounts + newAccount
            authRepository.saveAccounts(updatedAccounts)
            val leftId = state.splitLeftSessionId.ifBlank { uniqueId }
            val rightId = if (state.splitRightSessionId.isBlank() || state.splitRightSessionId == leftId) {
                uniqueId
            } else {
                state.splitRightSessionId
            }
            state.copy(
                accounts = updatedAccounts,
                selectedAccountId = uniqueId,
                activeChatSessionId = state.activeChatSessionId.ifBlank { uniqueId },
                splitLeftSessionId = leftId,
                splitRightSessionId = rightId,
                services = recomputeServices(state.services, updatedAccounts),
                showAddAccountDialog = false
            )
        }
    }

    /**
     * Switches the active chat WebView session when the user taps one of the bottom
     * service switcher icons (`W`, `T`, `I`, `F`, `G`, `V`).
     * If the user has multiple accounts for that service, cycles to the next account.
     * If the user has not created an account for that service yet, auto-creates one and opens it.
     */
    fun switchActiveService(serviceType: MessengerType) {
        val matchingAccounts = _uiState.value.accounts.filter { it.serviceType == serviceType }
        if (matchingAccounts.isNotEmpty()) {
            val currentIdx = matchingAccounts.indexOfFirst { it.id == _uiState.value.activeChatSessionId }
            val nextAccount = if (currentIdx >= 0 && matchingAccounts.size > 1) {
                matchingAccounts[(currentIdx + 1) % matchingAccounts.size]
            } else {
                matchingAccounts.first()
            }
            _uiState.update {
                it.copy(
                    activeChatSessionId = nextAccount.id,
                    selectedAccountId = nextAccount.id
                )
            }
        } else {
            // Auto-create a default account for this service and switch to it immediately
            addAccount(serviceType, "Main")
            val created = _uiState.value.accounts.lastOrNull { it.serviceType == serviceType }
            if (created != null) {
                _uiState.update {
                    it.copy(
                        activeChatSessionId = created.id,
                        selectedAccountId = created.id
                    )
                }
            }
        }
    }

    fun removeAccount(context: Context, accountId: String) {
        val account = _uiState.value.accounts.find { it.id == accountId } ?: return
        RelayWebViewPool.getInstance(context).releaseSession(accountId, account.serviceType.displayName)
        _uiState.update { state ->
            val updatedAccounts = state.accounts.filterNot { it.id == accountId }
            authRepository.saveAccounts(updatedAccounts)
            val nextId = updatedAccounts.firstOrNull()?.id.orEmpty()
            state.copy(
                accounts = updatedAccounts,
                selectedAccountId = if (state.selectedAccountId == accountId) nextId else state.selectedAccountId,
                activeChatSessionId = if (state.activeChatSessionId == accountId) nextId else state.activeChatSessionId,
                services = recomputeServices(state.services, updatedAccounts)
            )
        }
    }

    private fun applyMatrixSyncBatch(batch: ParsedSyncBatch) {
        _uiState.update { state ->
            val cleanExisting = state.messages.filterNot { msg ->
                msg.sender.equals("Admin Room", ignoreCase = true) ||
                    msg.snippet.contains("appservice registered", ignoreCase = true)
            }

            val mergedMessages = if (batch.messages.isEmpty()) {
                cleanExisting
            } else {
                val incomingByRoom = batch.messages.associateBy { it.roomId }
                val updatedExisting = cleanExisting.map { existing ->
                    incomingByRoom[existing.roomId] ?: existing
                }
                val existingRooms = updatedExisting.map { it.roomId }.toSet()
                val brandNew = batch.messages.filter { it.roomId !in existingRooms }
                brandNew + updatedExisting
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

            val updatedServices = recomputeServices(
                currentServices = state.services,
                accounts = updatedAccounts,
                matrixTotals = batch.serviceUnreadTotals
            )

            state.copy(
                messages = mergedMessages,
                services = updatedServices,
                accounts = updatedAccounts
            )
        }
    }

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

    fun triggerBridgeRelink(accountId: String, navigateToAccounts: Boolean = false) {
        val account = _uiState.value.accounts.find { it.id == accountId } ?: return
        val botMxid = account.botMxid ?: MatrixAuthRepository.defaultBotForService(account.serviceType)
        val command = if (account.serviceType == MessengerType.WHATSAPP) "login qr" else "login"

        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id == accountId) {
                    acc.copy(
                        statusText = "Requesting QR from $botMxid…"
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
            delay(300)
            _uiState.update { state ->
                val updatedAccounts = state.accounts.map { acc ->
                    if (acc.id == accountId) {
                        val suffix = if (result.isSuccess) "Sent '$command' to $botMxid · waiting for QR…"
                        else "Sign in via WebView below"
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
            val targetAccount = state.accounts.find { it.id == message.accountId }
                ?: state.accounts.find { it.serviceType == message.serviceType }
            state.copy(
                selectedMessageId = message.id,
                activeChatSessionId = targetAccount?.id ?: state.activeChatSessionId,
                selectedTab = RelayTab.INBOX,
                isSingleChatOpen = !isExpanded && targetAccount != null,
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
            current.copy(unfoldedLayoutMode = next, isSingleChatOpen = false)
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
            if (password.isNotBlank()) {
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
                    return@launch
                }
            }

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
                    showMatrixLoginDialog = false
                )
            }
            syncEngine.restartSync(viewModelScope)
        }
    }

    fun selectAccount(accountId: String) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun openSingleChat(accountId: String) {
        if (accountId.isBlank()) return
        _uiState.update {
            it.copy(
                activeChatSessionId = accountId,
                selectedAccountId = accountId,
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
        val account = _uiState.value.accounts.find { it.id == accountId } ?: return
        RelayWebViewPool.getInstance(context).releaseSession(accountId, account.serviceType.displayName)
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
            authRepository.saveAccounts(updatedAccounts)
            current.copy(accounts = updatedAccounts)
        }
    }
}
