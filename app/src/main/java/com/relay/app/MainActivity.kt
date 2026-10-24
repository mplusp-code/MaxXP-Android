package com.relay.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.relay.app.data.InboxViewMode
import com.relay.app.data.RelayTab
import com.relay.app.data.RelayViewModel
import com.relay.app.data.UnfoldedLayoutMode
import com.relay.app.push.RelayNotificationManager
import com.relay.app.ui.components.MatrixServerLoginDialog
import com.relay.app.ui.components.RelayConnectionPill
import com.relay.app.ui.components.RelayTopBar
import com.relay.app.ui.navigation.DevicePosture
import com.relay.app.ui.navigation.RelayBottomNavBar
import com.relay.app.ui.navigation.RelayLeftNavigationRail
import com.relay.app.ui.navigation.rememberDevicePosture
import com.relay.app.ui.screens.AccountsScreen
import com.relay.app.ui.screens.AddMessengerAccountDialog
import com.relay.app.ui.screens.ChatWebViewScreen
import com.relay.app.ui.screens.FlexModeWorkspace
import com.relay.app.ui.screens.InboxScreen
import com.relay.app.ui.screens.SettingsScreen
import com.relay.app.ui.screens.SplitViewScreen
import com.relay.app.ui.screens.TodayScreen
import com.relay.app.ui.theme.BackgroundDark
import com.relay.app.ui.theme.RelayTheme
import org.unifiedpush.android.connector.UnifiedPush

class MainActivity : ComponentActivity() {

    private val viewModel: RelayViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initMatrixSync(this)
        runCatching {
            if (UnifiedPush.getSavedDistributor(this) != null) {
                UnifiedPush.registerApp(this, messageForDistributor = "Relay Matrix push delivery")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        intent?.getStringExtra(RelayNotificationManager.EXTRA_ACCOUNT_ID)?.takeIf { it.isNotBlank() }?.let { accId ->
            viewModel.switchActiveAccount(accId)
            viewModel.setInboxViewMode(InboxViewMode.WEB_MESSENGER)
            viewModel.selectTab(RelayTab.INBOX)
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val windowSizeClass = calculateWindowSizeClass(this)
            val configuration = LocalConfiguration.current
            val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact &&
                configuration.screenWidthDp >= 600
            val devicePosture by rememberDevicePosture(this)

            RelayTheme(
                wallpaperTheme = uiState.preferences.wallpaperTheme,
                themeMode = uiState.preferences.themeMode,
                accentColor = uiState.preferences.accentColor
            ) {
                RelayAppScaffold(
                    viewModel = viewModel,
                    isExpanded = isExpanded,
                    devicePosture = devicePosture
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppForegrounded()
    }

    override fun onStop() {
        viewModel.onAppBackgrounded()
        super.onStop()
    }
}

@Composable
fun RelayAppScaffold(
    viewModel: RelayViewModel,
    isExpanded: Boolean,
    devicePosture: DevicePosture = DevicePosture.Normal
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isExpanded) {
        viewModel.onFoldStateChanged(isExpanded)
    }

    val isFlexTableTopActive = uiState.preferences.useFlexModeWhenHalfFolded &&
        devicePosture is DevicePosture.TableTop
    val isSplitViewActive = isExpanded && !isFlexTableTopActive &&
        uiState.unfoldedLayoutMode == UnfoldedLayoutMode.SPLIT_VIEW && !uiState.isSingleChatOpen
    val isSingleChatActive = uiState.isSingleChatOpen && !isFlexTableTopActive

    if (uiState.showMatrixLoginDialog) {
        MatrixServerLoginDialog(
            initialHomeserver = uiState.matrixHomeserver,
            initialUserId = uiState.matrixUserId,
            syncStatus = uiState.matrixSyncStatus,
            onDismiss = { viewModel.setShowMatrixLoginDialog(false) },
            onConnect = { homeserver, username, password ->
                viewModel.loginMatrixServer(homeserver, username, password)
            }
        )
    }

    if (uiState.showAddAccountDialog) {
        AddMessengerAccountDialog(
            onDismiss = { viewModel.setShowAddAccountDialog(false) },
            onAddAccount = { serviceType, label ->
                viewModel.addAccount(serviceType, label)
            }
        )
    }

    val totalUnread = uiState.messages.count { it.isUnread }.takeIf { it > 0 }
        ?: uiState.services.sumOf { it.unreadCount }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Optional top bar for Flex mode
            if (isFlexTableTopActive) {
                RelayTopBar(
                    title = "Flex Mode",
                    customActions = {
                        RelayConnectionPill(
                            status = uiState.matrixSyncStatus,
                            onClick = { viewModel.setShowMatrixLoginDialog(true) }
                        )
                    }
                )
            }

            // Main Body Area (with Left Navigation Rail when unfolded)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isExpanded) {
                    var railProfile by remember { mutableStateOf("P") }
                    RelayLeftNavigationRail(
                        currentTab = uiState.selectedTab,
                        unreadCount = totalUnread,
                        profileLetter = railProfile,
                        onProfileClick = {
                            railProfile = if (railProfile == "P") "W" else "P"
                        },
                        onTabSelected = { tab ->
                            if (isSplitViewActive) {
                                viewModel.toggleUnfoldedLayoutMode()
                            }
                            viewModel.selectTab(tab)
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when {
                        isFlexTableTopActive -> {
                            val hingeBounds = (devicePosture as? DevicePosture.TableTop)?.hingeBounds
                            FlexModeWorkspace(
                                uiState = uiState,
                                hingeBoundsPx = hingeBounds,
                                onSendQuickReply = { roomId, replyText ->
                                    viewModel.sendQuickReply(roomId, replyText)
                                },
                                onSwitchAccount = { id ->
                                    viewModel.switchActiveAccount(id)
                                }
                            )
                        }
                        isSplitViewActive -> {
                            SplitViewScreen(
                                uiState = uiState,
                                onSwapPanes = { viewModel.swapSplitViewPanes() },
                                onToggleWorkspaceMode = { viewModel.toggleUnfoldedLayoutMode() }
                            )
                        }
                        isSingleChatActive -> {
                            val activeAccount = uiState.accounts.find { it.id == uiState.activeChatSessionId }
                                ?: uiState.accounts.firstOrNull()
                            if (activeAccount != null) {
                                ChatWebViewScreen(
                                    sessionId = activeAccount.id,
                                    serviceType = activeAccount.serviceType,
                                    accountName = activeAccount.accountName,
                                    url = activeAccount.serviceType.defaultUrl,
                                    displayUrl = activeAccount.serviceType.defaultUrl.removePrefix("https://"),
                                    accounts = uiState.accounts,
                                    onSwitchService = { svc -> viewModel.switchActiveService(svc) },
                                    onSwitchAccount = { id -> viewModel.switchActiveAccount(id) },
                                    onBack = { viewModel.closeSingleChat() }
                                )
                            } else {
                                viewModel.closeSingleChat()
                            }
                        }
                        else -> {
                            when (uiState.selectedTab) {
                                RelayTab.TODAY -> TodayScreen(
                                    uiState = uiState,
                                    isExpanded = isExpanded,
                                    onServiceClick = { service ->
                                        viewModel.setServiceFilter(service)
                                        viewModel.switchActiveService(service)
                                        viewModel.setInboxViewMode(InboxViewMode.WEB_MESSENGER)
                                        viewModel.selectTab(RelayTab.INBOX)
                                    },
                                    onMessageClick = { msg ->
                                        viewModel.openChatFromMessage(msg, isExpanded)
                                    },
                                    onNavigateTab = { tab -> viewModel.selectTab(tab) },
                                    onSendQuickReply = { roomId, replyText ->
                                        viewModel.sendQuickReply(roomId, replyText)
                                    },
                                    onRelinkAccount = { accountId ->
                                        viewModel.triggerBridgeRelink(
                                            accountId = accountId,
                                            navigateToAccounts = true
                                        )
                                    },
                                    onAddAccountClick = {
                                        viewModel.selectTab(RelayTab.ACCOUNTS)
                                        viewModel.setShowAddAccountDialog(true)
                                    },
                                    onSearchQueryChange = { query ->
                                        viewModel.setSearchQuery(query)
                                    },
                                    onSearchSubmit = {
                                        viewModel.setInboxViewMode(InboxViewMode.MESSAGES_LIST)
                                        viewModel.selectTab(RelayTab.INBOX)
                                    }
                                )
                                RelayTab.INBOX -> InboxScreen(
                                    uiState = uiState,
                                    isExpanded = isExpanded,
                                    onSelectServiceFilter = { filter ->
                                        viewModel.setServiceFilter(filter)
                                    },
                                    onMessageClick = { msg ->
                                        viewModel.openChatFromMessage(msg, isExpanded)
                                    },
                                    onOpenFullChat = { accountId ->
                                        viewModel.openSingleChat(accountId)
                                    },
                                    onSwitchService = { svc ->
                                        viewModel.switchActiveService(svc)
                                    },
                                    onSwitchAccount = { id ->
                                        viewModel.switchActiveAccount(id)
                                    },
                                    onSelectInboxViewMode = { mode ->
                                        viewModel.setInboxViewMode(mode)
                                    },
                                    onSearchQueryChange = { query ->
                                        viewModel.setSearchQuery(query)
                                    },
                                    onToggleSort = {
                                        viewModel.toggleSortOrder()
                                    }
                                )
                                RelayTab.ACCOUNTS -> AccountsScreen(
                                    uiState = uiState,
                                    isExpanded = isExpanded,
                                    onSelectAccount = { id -> viewModel.selectAccount(id) },
                                    onToggleNotify = { id -> viewModel.toggleAccountNotify(id) },
                                    onOpenAccountChat = { id ->
                                        viewModel.openSingleChat(id)
                                    },
                                    onClearSession = { id ->
                                        viewModel.clearAccountSession(context, id)
                                    },
                                    onRemoveAccount = { id ->
                                        viewModel.removeAccount(context, id)
                                    },
                                    onTriggerBridgeLogin = { id ->
                                        viewModel.triggerBridgeRelink(
                                            accountId = id,
                                            navigateToAccounts = false
                                        )
                                    },
                                    onOpenAddAccount = {
                                        viewModel.setShowAddAccountDialog(true)
                                    },
                                    onOpenMatrixConfig = {
                                        viewModel.setShowMatrixLoginDialog(true)
                                    }
                                )
                                RelayTab.SETUP -> SettingsScreen(
                                    uiState = uiState,
                                    isExpanded = isExpanded,
                                    onSelectWallpaper = { wp -> viewModel.selectWallpaper(wp) },
                                    onUpdatePreferences = { prefs -> viewModel.updatePreferences(prefs) }
                                )
                            }
                        }
                    }
                }
            }

            // Portrait 4-Tab Bottom Navigation Bar (Hidden when unfolded or single chat is open)
            if (!isExpanded && !isSingleChatActive) {
                RelayBottomNavBar(
                    currentTab = uiState.selectedTab,
                    unreadCount = totalUnread,
                    onTabSelected = { tab -> viewModel.selectTab(tab) }
                )
            }
        }
    }
}
