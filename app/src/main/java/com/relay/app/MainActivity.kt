package com.relay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.InboxViewMode
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayTab
import com.relay.app.data.RelayViewModel
import com.relay.app.data.UnfoldedLayoutMode
import com.relay.app.ui.components.MatrixServerLoginDialog
import com.relay.app.ui.components.RelayConnectionPill
import com.relay.app.ui.components.RelaySoftkeyBar
import com.relay.app.ui.components.RelayTopBar
import com.relay.app.ui.navigation.RelayBottomNavBar
import com.relay.app.ui.navigation.RelayLeftNavigationRail
import com.relay.app.ui.screens.AccountsScreen
import com.relay.app.ui.screens.AddMessengerAccountDialog
import com.relay.app.ui.screens.ChatWebViewScreen
import com.relay.app.ui.screens.InboxScreen
import com.relay.app.ui.screens.SetupScreen
import com.relay.app.ui.screens.SplitViewScreen
import com.relay.app.ui.screens.TodayScreen
import com.relay.app.ui.theme.RelayTheme
import com.relay.app.ui.theme.RelayWallpaperBackground

class MainActivity : ComponentActivity() {

    private val viewModel: RelayViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.initMatrixSync(this)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val windowSizeClass = calculateWindowSizeClass(this)
            val configuration = LocalConfiguration.current
            val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact &&
                configuration.screenWidthDp >= 600

            RelayTheme(wallpaperTheme = uiState.preferences.wallpaperTheme) {
                RelayAppScaffold(
                    viewModel = viewModel,
                    isExpanded = isExpanded
                )
            }
        }
    }
}

@Composable
fun RelayAppScaffold(
    viewModel: RelayViewModel,
    isExpanded: Boolean
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val isSplitViewActive = isExpanded && uiState.unfoldedLayoutMode == UnfoldedLayoutMode.SPLIT_VIEW && !uiState.isSingleChatOpen
    val isSingleChatActive = uiState.isSingleChatOpen

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

    val topBarTitle = when {
        isSplitViewActive -> "Relay · Split view"
        isSingleChatActive -> {
            uiState.accounts.find { it.id == uiState.activeChatSessionId }?.accountName
                ?: "Messenger"
        }
        else -> when (uiState.selectedTab) {
            RelayTab.TODAY -> "Relay"
            RelayTab.INBOX -> "Inbox"
            RelayTab.ACCOUNTS -> "Accounts"
            RelayTab.SETUP -> "Relay · Setup"
        }
    }

    val leftSoftkeyLabel = when {
        isSplitViewActive -> "Account"
        isSingleChatActive -> "Back"
        else -> when (uiState.selectedTab) {
            RelayTab.TODAY -> "New"
            RelayTab.INBOX -> if (uiState.inboxViewMode == InboxViewMode.MESSAGES_LIST) "Web View" else "Messages"
            RelayTab.ACCOUNTS -> "Add"
            RelayTab.SETUP -> "Done"
        }
    }

    val rightSoftkeyLabel = if (isSplitViewActive) "Swap panes" else "Menu"

    RelayWallpaperBackground(
        theme = uiState.preferences.wallpaperTheme,
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. Glossy Top Title Bar
            RelayTopBar(
                title = topBarTitle,
                actionText = "ok",
                onActionClick = {
                    if (isSingleChatActive) {
                        viewModel.closeSingleChat()
                    } else if (uiState.selectedTab == RelayTab.SETUP) {
                        viewModel.selectTab(RelayTab.TODAY)
                    }
                },
                customActions = {
                    RelayConnectionPill(
                        status = uiState.matrixSyncStatus,
                        onClick = { viewModel.setShowMatrixLoginDialog(true) }
                    )

                    if (isExpanded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isSplitViewActive) Color(0xFF4CAF50) else Color(0x44000000)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .clickable { viewModel.toggleUnfoldedLayoutMode() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isSplitViewActive) "Tri-Pane" else "Split View",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )

            // 2. Main Body Area (with Left Navigation Rail when unfolded)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isExpanded) {
                    RelayLeftNavigationRail(
                        currentTab = uiState.selectedTab,
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
                        isSplitViewActive -> {
                            SplitViewScreen(
                                uiState = uiState,
                                onSwapPanes = { viewModel.swapSplitViewPanes() }
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
                                    onSwitchAccount = { id -> viewModel.switchActiveAccount(id) }
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
                                RelayTab.SETUP -> SetupScreen(
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

            // 3. Portrait 4-Tab Navigation Bar (hidden on unfolded inner display)
            if (!isExpanded && !isSingleChatActive) {
                RelayBottomNavBar(
                    currentTab = uiState.selectedTab,
                    onTabSelected = { tab -> viewModel.selectTab(tab) }
                )
            }

            // 4. Bevelled 3-Slot Softkey Action Bar (Always present at bottom)
            RelaySoftkeyBar(
                leftText = leftSoftkeyLabel,
                onLeftClick = {
                    when {
                        isSplitViewActive -> viewModel.selectTab(RelayTab.ACCOUNTS)
                        isSingleChatActive -> viewModel.closeSingleChat()
                        uiState.selectedTab == RelayTab.ACCOUNTS -> viewModel.setShowAddAccountDialog(true)
                        uiState.selectedTab == RelayTab.INBOX -> viewModel.toggleInboxViewMode()
                        uiState.selectedTab == RelayTab.SETUP -> viewModel.selectTab(RelayTab.TODAY)
                        uiState.selectedTab == RelayTab.TODAY -> viewModel.selectTab(RelayTab.INBOX)
                        else -> {}
                    }
                },
                rightText = rightSoftkeyLabel,
                onRightClick = {
                    if (isSplitViewActive) {
                        viewModel.swapSplitViewPanes()
                    } else {
                        viewModel.selectTab(RelayTab.SETUP)
                    }
                }
            )
        }
    }
}
