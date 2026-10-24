package com.relay.app.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.relay.app.bridge.MetaBotReplyType
import com.relay.app.bridge.RelayBridgeListener
import com.relay.app.bridge.RelaySessionManager
import com.relay.app.bridge.RelayWebViewPool
import com.relay.app.matrix.MatrixAuthRepository
import com.relay.app.matrix.MatrixRoomParser
import com.relay.app.matrix.MatrixSyncEngine
import com.relay.app.matrix.MatrixSyncStatus
import com.relay.app.matrix.ParsedSyncBatch
import com.relay.app.matrix.QuickReplyManager
import com.relay.app.matrix.QuickReplyStatus
import com.relay.app.push.QuietHoursEvaluator
import com.relay.app.push.RelayNotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class RelayUiState(
    val selectedTab: RelayTab = RelayTab.TODAY,
    val unfoldedLayoutMode: UnfoldedLayoutMode = UnfoldedLayoutMode.TRI_PANE,
    val inboxViewMode: InboxViewMode = InboxViewMode.MESSAGES_LIST,
    val lastUnfoldedTab: RelayTab = RelayTab.INBOX,
    val lastUnfoldedLayoutMode: UnfoldedLayoutMode = UnfoldedLayoutMode.TRI_PANE,
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
    private var appContext: Context? = null
    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private data class MetaLoginSession(
        val accountId: String,
        val service: MessengerType,
        val cookies: Map<String, String>
    )
    private val activeMetaLogins = mutableMapOf<String, MetaLoginSession>()

    fun initMatrixSync(context: Context) {
        if (syncInitialized) return
        syncInitialized = true
        val appCtx = context.applicationContext
        this.appContext = appCtx

        RelayNotificationManager.ensureChannels(appCtx)

        val webViewPool = RelayWebViewPool.getInstance(appCtx)
        webViewPool.isScraperEnabledProvider = { _uiState.value.preferences.useWebViewScraperFallback }
        webViewPool.setBridgeListener(object : RelayBridgeListener {
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
                    val account = state.accounts.find { it.id == sessionId } ?: return@update state
                    val serviceType = account.serviceType
                    val msgId = "wv_${sessionId}_${title.lowercase().hashCode()}"
                    val roomId = "!wv_${sessionId}_${title.lowercase().hashCode()}:webview"
                    val newMessage = MessageItem(
                        id = msgId,
                        roomId = roomId,
                        sender = title,
                        snippet = message,
                        timestamp = "Just now",
                        serviceType = serviceType,
                        accountId = sessionId,
                        isUnread = true
                    )
                    if (account.isNotifyEnabled) {
                        val isQuiet = QuietHoursEvaluator.isCurrentlyQuietHours(
                            quietHoursEnabled = state.preferences.quietHoursEnabled,
                            startStr = state.preferences.quietHoursStart,
                            endStr = state.preferences.quietHoursEnd
                        )
                        RelayNotificationManager.showIncomingMessageNotification(
                            context = appCtx,
                            roomId = roomId,
                            accountId = sessionId,
                            sender = title,
                            snippet = message,
                            serviceType = serviceType,
                            isQuietHoursActive = isQuiet
                        )
                    }
                    val others = state.messages.filterNot { it.id == msgId }
                    val merged = listOf(newMessage) + others
                    val updatedAccounts = state.accounts.map { acc ->
                        if (acc.id == sessionId) {
                            acc.copy(
                                isConnected = true,
                                isNeedsAttention = false,
                                statusText = "Connected",
                                unreadCount = maxOf(acc.unreadCount, merged.count { it.accountId == sessionId && it.isUnread })
                            )
                        } else acc
                    }
                    state.copy(
                        messages = merged,
                        accounts = updatedAccounts,
                        services = recomputeServices(state.services, updatedAccounts)
                    )
                }
            }

            override fun onChatListUpdated(sessionId: String, chatsJson: String) {
                if (!_uiState.value.preferences.useWebViewScraperFallback) {
                    return
                }
                val parsedArray = runCatching { jsonParser.parseToJsonElement(chatsJson).jsonArray }.getOrNull()
                    ?: return
                _uiState.update { state ->
                    val account = state.accounts.find { it.id == sessionId } ?: return@update state
                    val scrapedMessages = parsedArray.mapNotNull { elem ->
                        val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@mapNotNull null
                        val sender = obj["sender"]?.jsonPrimitive?.content?.trim().orEmpty()
                        val snippet = obj["snippet"]?.jsonPrimitive?.content?.trim().orEmpty()
                        val timestamp = obj["timestamp"]?.jsonPrimitive?.content?.trim().orEmpty().ifBlank { "Recent" }
                        val isUnread = obj["unread"]?.jsonPrimitive?.booleanOrNull ?: false
                        if (sender.isBlank() || snippet.isBlank()) return@mapNotNull null
                        val msgId = "wv_${sessionId}_${sender.lowercase().hashCode()}"
                        MessageItem(
                            id = msgId,
                            roomId = "!wv_${sessionId}_${sender.lowercase().hashCode()}:webview",
                            sender = sender,
                            snippet = snippet,
                            timestamp = timestamp,
                            serviceType = account.serviceType,
                            accountId = sessionId,
                            isUnread = isUnread
                        )
                    }
                    if (scrapedMessages.isEmpty()) return@update state

                    val otherMessages = state.messages.filterNot { it.accountId == sessionId }
                    val combined = (scrapedMessages + otherMessages)
                        .sortedByDescending { it.isUnread }
                    val unreadTotalForAccount = scrapedMessages.count { it.isUnread }

                    val updatedAccounts = state.accounts.map { acc ->
                        if (acc.id == sessionId) {
                            acc.copy(
                                isConnected = true,
                                isNeedsAttention = false,
                                statusText = "Connected",
                                unreadCount = maxOf(acc.unreadCount, unreadTotalForAccount)
                            )
                        } else acc
                    }
                    state.copy(
                        messages = combined,
                        accounts = updatedAccounts,
                        services = recomputeServices(state.services, updatedAccounts)
                    )
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
        val savedPrefs = authRepository.loadSetupPreferences()
        val firstId = savedAccounts.firstOrNull()?.id.orEmpty()
        val secondId = savedAccounts.getOrNull(1)?.id ?: firstId

        _uiState.update { state ->
            state.copy(
                matrixHomeserver = creds.homeserverUrl,
                matrixUserId = creds.userId ?: MatrixAuthRepository.DEFAULT_USER_ID,
                preferences = savedPrefs,
                accounts = savedAccounts,
                selectedAccountId = firstId,
                activeChatSessionId = firstId,
                splitLeftSessionId = firstId,
                splitRightSessionId = secondId,
                services = recomputeServices(state.services, savedAccounts)
            )
        }

        // Pre-warm saved WebView accounts so incoming messages arrive in the background
        if (savedAccounts.isNotEmpty()) {
            RelayWebViewPool.getInstance(appCtx).preloadAccountSessions(savedAccounts)
        }

        // Periodic background WebView surface eviction (>10m idle when keepSessionsAlive is false)
        viewModelScope.launch {
            while (isActive) {
                delay(60_000L)
                val current = _uiState.value
                val activeIds = buildSet {
                    if (current.activeChatSessionId.isNotBlank()) add(current.activeChatSessionId)
                    if (current.unfoldedLayoutMode == UnfoldedLayoutMode.SPLIT_VIEW) {
                        if (current.splitLeftSessionId.isNotBlank()) add(current.splitLeftSessionId)
                        if (current.splitRightSessionId.isNotBlank()) add(current.splitRightSessionId)
                    }
                }
                RelayWebViewPool.getInstance(appCtx).evictInactiveSurfaces(
                    activeSessionIds = activeIds,
                    keepSessionsAlive = current.preferences.keepSessionsAlive
                )
            }
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
        matrixTotals: Map<MessengerType, Int> = emptyMap(),
        useWebViewScraperFallback: Boolean = false
    ): List<ServiceSummary> {
        return currentServices.map { svc ->
            val matching = accounts.filter { it.serviceType == svc.type }
            val webViewUnread = if (useWebViewScraperFallback) matching.sumOf { it.unreadCount } else 0
            val matrixUnread = matrixTotals[svc.type] ?: 0
            val effectiveUnread = if (useWebViewScraperFallback) maxOf(webViewUnread, matrixUnread) else matrixUnread
            svc.copy(
                accountCount = matching.size,
                unreadCount = effectiveUnread
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
        appContext?.let { ctx ->
            RelayWebViewPool.getInstance(ctx).preloadAccountSessions(listOf(newAccount))
        }
    }

    /**
     * Switches the active chat WebView session when the user taps one of the added
     * service switcher icons (`W`, `T`, `I`, `F`, `G`, `V`).
     * Only switches between accounts the user has actually added.
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
            switchActiveAccount(nextAccount.id)
        }
    }

    fun switchActiveAccount(accountId: String) {
        if (_uiState.value.accounts.none { it.id == accountId }) return
        val prevId = _uiState.value.activeChatSessionId
        val keepAlive = _uiState.value.preferences.keepSessionsAlive
        appContext?.let { ctx ->
            val pool = RelayWebViewPool.getInstance(ctx)
            if (prevId.isNotBlank() && prevId != accountId) {
                pool.onSessionBackgrounded(prevId, keepAlive)
            }
            pool.markSessionActive(accountId)
        }
        _uiState.update {
            it.copy(
                activeChatSessionId = accountId,
                selectedAccountId = accountId
            )
        }
    }

    fun removeAccount(context: Context, accountId: String) {
        val account = _uiState.value.accounts.find { it.id == accountId } ?: return
        RelayWebViewPool.getInstance(context).releaseSession(accountId, account.serviceType.displayName)
        _uiState.update { state ->
            val updatedAccounts = state.accounts.filterNot { it.id == accountId }
            val updatedMessages = state.messages.filterNot { it.accountId == accountId }
            authRepository.saveAccounts(updatedAccounts)
            val nextId = updatedAccounts.firstOrNull()?.id.orEmpty()
            val validFilter = state.selectedServiceFilter?.takeIf { filter ->
                updatedAccounts.any { it.serviceType == filter }
            }
            state.copy(
                accounts = updatedAccounts,
                messages = updatedMessages,
                selectedServiceFilter = validFilter,
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

            val activeAccounts = state.accounts
            val activeServices = activeAccounts.map { it.serviceType }.toSet()

            // If user has accounts, only keep messages for services with accounts
            val validExisting = if (activeServices.isNotEmpty()) {
                cleanExisting.filter { it.serviceType in activeServices }
            } else cleanExisting

            val validIncoming = if (activeServices.isNotEmpty()) {
                batch.messages.filter { it.serviceType in activeServices }
            } else batch.messages

            val mergedMessages = if (validIncoming.isEmpty()) {
                validExisting
            } else {
                val incomingByRoom = validIncoming.associateBy { it.roomId }
                val updatedExisting = validExisting.map { existing ->
                    incomingByRoom[existing.roomId] ?: existing
                }
                val existingRooms = updatedExisting.map { it.roomId }.toSet()
                val brandNew = validIncoming.filter { it.roomId !in existingRooms }
                (brandNew + updatedExisting).sortedWith(
                    compareByDescending<MessageItem> { it.timestampMillis }
                        .thenByDescending { it.isUnread }
                )
            }

            val updatedAccounts = if (batch.bridgeBotUpdates.isEmpty()) {
                state.accounts
            } else {
                state.accounts.map { acc ->
                    val botUpdate = batch.bridgeBotUpdates.lastOrNull { it.serviceType == acc.serviceType }
                    if (botUpdate != null) {
                        val botText = botUpdate.statusText.orEmpty()
                        val metaSession = activeMetaLogins[acc.id]

                        if (metaSession != null && (acc.serviceType == MessengerType.INSTAGRAM || acc.serviceType == MessengerType.FACEBOOK)) {
                            val replyType = RelaySessionManager.classifyMetaBotReply(botText)
                            when (replyType) {
                                MetaBotReplyType.FLOW_PROMPT -> {
                                    val cmd = if (acc.serviceType == MessengerType.INSTAGRAM) "login instagram" else "login messenger"
                                    val botMxid = acc.botMxid ?: MatrixAuthRepository.defaultBotForService(acc.serviceType)
                                    viewModelScope.launch {
                                        syncEngine.sendBridgeBotCommand(botMxid, cmd)
                                    }
                                    acc.copy(statusText = "Selecting login flow…")
                                }
                                MetaBotReplyType.COOKIE_PROMPT -> {
                                    val cookieJson = RelaySessionManager.serializeMetaCookiesToJson(metaSession.cookies)
                                    val botMxid = acc.botMxid ?: MatrixAuthRepository.defaultBotForService(acc.serviceType)
                                    viewModelScope.launch {
                                        syncEngine.sendBridgeBotCommand(botMxid, cookieJson)
                                    }
                                    acc.copy(statusText = "Submitting session cookies to bridge…")
                                }
                                MetaBotReplyType.SUCCESS -> {
                                    activeMetaLogins.remove(acc.id)
                                    acc.copy(
                                        isConnected = true,
                                        isNeedsAttention = false,
                                        statusText = "Connected"
                                    )
                                }
                                MetaBotReplyType.ERROR -> {
                                    activeMetaLogins.remove(acc.id)
                                    acc.copy(
                                        isConnected = false,
                                        isNeedsAttention = true,
                                        statusText = RelaySessionManager.redactSensitiveStatus(botText.ifBlank { "Login failed" })
                                    )
                                }
                                MetaBotReplyType.OTHER -> {
                                    acc.copy(statusText = RelaySessionManager.redactSensitiveStatus(botText))
                                }
                            }
                        } else {
                            val isAck = botUpdate.isConnectedAck ||
                                botText.contains("Connected", ignoreCase = true) ||
                                botText.contains("Logged in as", ignoreCase = true)
                            val isRelinkNeeded = botText.contains("checkpoint", ignoreCase = true) ||
                                botText.contains("challenge_required", ignoreCase = true) ||
                                botText.contains("invalid", ignoreCase = true) ||
                                botText.contains("relogin", ignoreCase = true)
                            acc.copy(
                                isConnected = if (isAck) true else if (isRelinkNeeded) false else acc.isConnected,
                                isNeedsAttention = if (isAck) false else if (isRelinkNeeded) true else acc.isNeedsAttention,
                                statusText = RelaySessionManager.redactSensitiveStatus(botUpdate.statusText ?: acc.statusText),
                                bridgeQrAscii = botUpdate.qrAsciiOrData ?: acc.bridgeQrAscii
                            )
                        }
                    } else acc
                }
            }

            val updatedServices = recomputeServices(
                currentServices = state.services,
                accounts = updatedAccounts,
                matrixTotals = batch.serviceUnreadTotals,
                useWebViewScraperFallback = state.preferences.useWebViewScraperFallback
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

        if (account.serviceType == MessengerType.INSTAGRAM || account.serviceType == MessengerType.FACEBOOK) {
            val cookies = RelaySessionManager.getMetaCookies(accountId, account.serviceType)
            if (cookies == null) {
                val serviceName = account.serviceType.displayName
                _uiState.update { state ->
                    val updatedAccounts = state.accounts.map { acc ->
                        if (acc.id == accountId) {
                            acc.copy(
                                isConnected = false,
                                isNeedsAttention = true,
                                statusText = "Please log in to $serviceName in Web view first"
                            )
                        } else acc
                    }
                    state.copy(
                        selectedTab = if (navigateToAccounts) RelayTab.ACCOUNTS else state.selectedTab,
                        selectedAccountId = accountId,
                        accounts = updatedAccounts
                    )
                }
                if (!navigateToAccounts) {
                    openSingleChat(accountId)
                }
                return
            }

            activeMetaLogins[accountId] = MetaLoginSession(accountId, account.serviceType, cookies)
            val initialCmd = if (account.serviceType == MessengerType.INSTAGRAM) "login instagram" else "login messenger"

            _uiState.update { state ->
                val updatedAccounts = state.accounts.map { acc ->
                    if (acc.id == accountId) {
                        acc.copy(
                            statusText = "Connecting to ${account.serviceType.displayName} bridge…"
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
                syncEngine.sendBridgeBotCommand(botMxid = botMxid, command = initialCmd)
            }
            return
        }

        val command = if (account.serviceType == MessengerType.WHATSAPP) "login qr" else "login"

        _uiState.update { state ->
            val updatedAccounts = state.accounts.map { acc ->
                if (acc.id == accountId) {
                    acc.copy(
                        statusText = if (account.serviceType == MessengerType.WHATSAPP) "Requesting QR from $botMxid…" else "Starting login with $botMxid…"
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
                        val suffix = if (result.isSuccess) {
                            if (account.serviceType == MessengerType.WHATSAPP) "Sent '$command' to $botMxid · waiting for QR…"
                            else "Sent '$command' to $botMxid · enter code in chat"
                        } else "Sign in via WebView below"
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
                inboxViewMode = InboxViewMode.WEB_MESSENGER,
                isSingleChatOpen = false,
                messages = updatedMessages
            )
        }
    }

    fun setInboxViewMode(mode: InboxViewMode) {
        _uiState.update { it.copy(inboxViewMode = mode) }
    }

    fun toggleInboxViewMode() {
        _uiState.update { current ->
            val next = if (current.inboxViewMode == InboxViewMode.MESSAGES_LIST) {
                InboxViewMode.WEB_MESSENGER
            } else {
                InboxViewMode.MESSAGES_LIST
            }
            current.copy(inboxViewMode = next)
        }
    }

    private var previousExpandedState: Boolean? = null

    fun onFoldStateChanged(isExpanded: Boolean) {
        val wasExpanded = previousExpandedState
        previousExpandedState = isExpanded
        if (wasExpanded == null || wasExpanded == isExpanded) return

        _uiState.update { state ->
            val prefs = state.preferences
            if (isExpanded) {
                // Unfolding: Cover Portrait (false) -> Unfolded Inner Display (true)
                val coverChatWasOpen = (state.isSingleChatOpen ||
                    (state.selectedTab == RelayTab.INBOX && state.inboxViewMode == InboxViewMode.WEB_MESSENGER)) &&
                    state.activeChatSessionId.isNotBlank()

                if (prefs.openChatOnUnfold && coverChatWasOpen) {
                    state.copy(
                        selectedTab = RelayTab.INBOX,
                        unfoldedLayoutMode = UnfoldedLayoutMode.TRI_PANE,
                        inboxViewMode = InboxViewMode.WEB_MESSENGER,
                        isSingleChatOpen = false
                    )
                } else {
                    when (prefs.unfoldedOpenMode) {
                        UnfoldedOpenMode.TRI_PANE_INBOX -> state.copy(
                            selectedTab = RelayTab.INBOX,
                            unfoldedLayoutMode = UnfoldedLayoutMode.TRI_PANE,
                            isSingleChatOpen = false
                        )
                        UnfoldedOpenMode.SPLIT_VIEW -> state.copy(
                            selectedTab = RelayTab.INBOX,
                            unfoldedLayoutMode = UnfoldedLayoutMode.SPLIT_VIEW,
                            isSingleChatOpen = false
                        )
                        UnfoldedOpenMode.TODAY_OVERVIEW -> state.copy(
                            selectedTab = RelayTab.TODAY,
                            unfoldedLayoutMode = UnfoldedLayoutMode.TRI_PANE,
                            isSingleChatOpen = false
                        )
                        UnfoldedOpenMode.LAST_USED -> state.copy(
                            selectedTab = state.lastUnfoldedTab,
                            unfoldedLayoutMode = state.lastUnfoldedLayoutMode,
                            isSingleChatOpen = false
                        )
                    }
                }
            } else {
                // Folding closed: Unfolded Inner Display (true) -> Cover Portrait (false)
                val remembered = state.copy(
                    lastUnfoldedTab = state.selectedTab,
                    lastUnfoldedLayoutMode = state.unfoldedLayoutMode
                )
                val wasChatActive = state.activeChatSessionId.isNotBlank() &&
                    (state.unfoldedLayoutMode == UnfoldedLayoutMode.SPLIT_VIEW ||
                        (state.selectedTab == RelayTab.INBOX && state.inboxViewMode == InboxViewMode.WEB_MESSENGER))
                if (prefs.keepOpenChatsWhenFolding && wasChatActive) {
                    remembered.copy(
                        selectedTab = RelayTab.INBOX,
                        inboxViewMode = InboxViewMode.WEB_MESSENGER,
                        isSingleChatOpen = false
                    )
                } else {
                    remembered.copy(isSingleChatOpen = false)
                }
            }
        }
    }

    fun onAppBackgrounded() {
        val keepAlive = _uiState.value.preferences.keepSessionsAlive
        appContext?.let { ctx ->
            RelayWebViewPool.getInstance(ctx).onAppBackgrounded(keepAlive)
        }
    }

    fun onAppForegrounded() {
        val current = _uiState.value
        val activeIds = buildSet {
            if (current.activeChatSessionId.isNotBlank()) add(current.activeChatSessionId)
            if (current.unfoldedLayoutMode == UnfoldedLayoutMode.SPLIT_VIEW) {
                if (current.splitLeftSessionId.isNotBlank()) add(current.splitLeftSessionId)
                if (current.splitRightSessionId.isNotBlank()) add(current.splitRightSessionId)
            }
        }
        appContext?.let { ctx ->
            RelayWebViewPool.getInstance(ctx).onAppForegrounded(activeIds)
        }
    }

    fun selectTab(tab: RelayTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                lastUnfoldedTab = tab,
                isSingleChatOpen = false
            )
        }
    }

    fun toggleUnfoldedLayoutMode() {
        _uiState.update { current ->
            val next = if (current.unfoldedLayoutMode == UnfoldedLayoutMode.TRI_PANE) {
                UnfoldedLayoutMode.SPLIT_VIEW
            } else {
                UnfoldedLayoutMode.TRI_PANE
            }
            current.copy(
                unfoldedLayoutMode = next,
                lastUnfoldedLayoutMode = next,
                isSingleChatOpen = false
            )
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
            val updated = current.preferences.copy(wallpaperTheme = wallpaper)
            authRepository.saveSetupPreferences(updated)
            current.copy(preferences = updated)
        }
    }

    fun updatePreferences(newPrefs: SetupPreferences) {
        authRepository.saveSetupPreferences(newPrefs)
        appContext?.let { ctx ->
            RelayWebViewPool.getInstance(ctx).isScraperEnabledProvider = { newPrefs.useWebViewScraperFallback }
        }
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
