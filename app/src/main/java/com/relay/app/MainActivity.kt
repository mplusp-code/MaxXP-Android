package com.relay.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.RelayTab
import com.relay.app.data.RelayViewModel
import com.relay.app.ui.components.RelaySoftkeyBar
import com.relay.app.ui.components.RelayTopBar
import com.relay.app.ui.navigation.RelayBottomNavBar
import com.relay.app.ui.navigation.RelayLeftNavigationRail
import com.relay.app.ui.screens.*
import com.relay.app.ui.theme.RelayTheme

class MainActivity : ComponentActivity() {

    private val viewModel: RelayViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpanded = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
            val uiState by viewModel.uiState.collectAsState()

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
    val uiState by viewModel.uiState.collectAsState()

    // Title determination
    val title = when {
        uiState.isSplitViewActive -> "Relay · Split view"
        isExpanded -> when (uiState.currentTab) {
            RelayTab.TODAY -> "Relay · Today"
            RelayTab.INBOX -> "Relay"
            RelayTab.ACCOUNTS -> "Relay · Accounts"
            RelayTab.SETUP -> "Relay · Setup"
        }
        else -> when (uiState.currentTab) {
            RelayTab.TODAY -> "Relay"
            RelayTab.INBOX -> "Inbox"
            RelayTab.ACCOUNTS -> "Accounts"
            RelayTab.SETUP -> "Setup"
        }
    }

    // Softkey action labels
    val leftSoftkey = when {
        uiState.isSplitViewActive -> "New"
        uiState.currentTab == RelayTab.TODAY -> "New"
        uiState.currentTab == RelayTab.INBOX -> if (isExpanded) "New" else "Open"
        uiState.currentTab == RelayTab.ACCOUNTS -> "Add"
        uiState.currentTab == RelayTab.SETUP -> "Done"
        else -> "New"
    }

    val rightSoftkey = when {
        uiState.isSplitViewActive -> "Swap panes"
        else -> "Menu"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Glossy Top Bar with 4-square brand logo
        RelayTopBar(
            title = title,
            actionText = if (uiState.currentTab == RelayTab.SETUP && !isExpanded) "ok" else null,
            onActionClick = { },
            customActions = {
                // In unfolded mode, show layout switch toggle (Workspace vs Split View)
                if (isExpanded) {
                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF263D20))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "☷",
                            color = if (!uiState.isSplitViewActive) Color(0xFF8BC34A) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { viewModel.setSplitView(false) }
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "◫",
                            color = if (uiState.isSplitViewActive) Color(0xFF8BC34A) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .clickable { viewModel.setSplitView(true) }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        )

        // Middle Viewport: Rail + Content (Expanded) OR 1-Pane Content (Compact)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Vertical Navigation Rail
                    RelayLeftNavigationRail(
                        currentTab = uiState.currentTab,
                        onTabSelected = { viewModel.selectTab(it) }
                    )

                    // Multi-Pane Content Area
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        if (uiState.isSplitViewActive) {
                            SplitViewScreen(
                                uiState = uiState,
                                onSwapPanes = { viewModel.swapSplitPanes() }
                            )
                        } else {
                            when (uiState.currentTab) {
                                RelayTab.TODAY -> TodayScreen(uiState = uiState, isExpanded = true)
                                RelayTab.INBOX -> InboxScreen(
                                    uiState = uiState,
                                    isExpanded = true,
                                    onServiceFilterSelect = { viewModel.filterByService(it) }
                                )
                                RelayTab.ACCOUNTS -> AccountsScreen(
                                    uiState = uiState,
                                    isExpanded = true,
                                    onSelectAccount = { viewModel.selectAccount(it) },
                                    onToggleNotify = { viewModel.toggleAccountNotify(it) }
                                )
                                RelayTab.SETUP -> SetupScreen(
                                    uiState = uiState,
                                    isExpanded = true,
                                    onSelectWallpaper = { viewModel.setWallpaper(it) },
                                    onUpdatePreferences = { prefs ->
                                        viewModel.updatePreferences { prefs }
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                // Portrait 1-Pane Content Area
                when (uiState.currentTab) {
                    RelayTab.TODAY -> TodayScreen(uiState = uiState, isExpanded = false)
                    RelayTab.INBOX -> InboxScreen(uiState = uiState, isExpanded = false)
                    RelayTab.ACCOUNTS -> AccountsScreen(
                        uiState = uiState,
                        isExpanded = false,
                        onSelectAccount = { viewModel.selectAccount(it) },
                        onToggleNotify = { viewModel.toggleAccountNotify(it) }
                    )
                    RelayTab.SETUP -> SetupScreen(
                        uiState = uiState,
                        isExpanded = false,
                        onSelectWallpaper = { viewModel.setWallpaper(it) },
                        onUpdatePreferences = { prefs ->
                            viewModel.updatePreferences { prefs }
                        }
                    )
                }
            }
        }

        // Bottom Navigation Bar (ONLY in Portrait / Compact Mode)
        if (!isExpanded) {
            RelayBottomNavBar(
                currentTab = uiState.currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }

        // Tactile 3-Slot Softkey Bar (Always at the absolute bottom)
        RelaySoftkeyBar(
            leftText = leftSoftkey,
            onLeftClick = { },
            rightText = rightSoftkey,
            onRightClick = {
                if (uiState.isSplitViewActive) {
                    viewModel.swapSplitPanes()
                }
            },
            onKeyboardClick = { }
        )
    }
}
