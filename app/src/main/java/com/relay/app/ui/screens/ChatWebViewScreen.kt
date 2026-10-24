package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.AccountItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge
import com.relay.app.ui.theme.*

@Composable
fun ChatWebViewScreen(
    sessionId: String = "wa_personal",
    serviceType: MessengerType,
    accountName: String,
    url: String,
    displayUrl: String,
    accounts: List<AccountItem> = emptyList(),
    onSwitchService: (MessengerType) -> Unit = {},
    onSwitchAccount: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val initials = remember(accountName) {
        accountName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { accountName.take(2).uppercase() }
    }

    Column(modifier = modifier.fillMaxSize().background(BackgroundDark)) {
        // Modern Top Bar (Concept v2 Page 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(SurfaceDark)
                .border(1.dp, BorderDark)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onBack() }
                )

                Box(modifier = Modifier.size(36.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(CardElevated)
                            .border(1.dp, BorderDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                    ) {
                        ServiceBadge(serviceType = serviceType, size = 14)
                    }
                }

                Column {
                    Text(
                        text = accountName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${serviceType.displayName} · Personal",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = "Swap",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Sub-header Status Bar: • Live · synced 00:23 / Lock displayUrl
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .background(CardDark)
                .border(1.dp, BorderDark)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                Text(
                    text = "Live · synced 00:23",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = displayUrl,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // WebView Surface
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RetainedWebView(
                sessionId = sessionId,
                serviceName = serviceType.displayName,
                url = url,
                displayUrl = displayUrl,
                showHeader = false
            )
        }

        // Bottom docked quick service switcher strip (Concept v2 Page 3)
        if (accounts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    val isCurrent = acc.id == sessionId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isCurrent) Color(acc.serviceType.badgeColorHex) else CardDark)
                            .border(1.dp, if (isCurrent) Color.White.copy(alpha = 0.5f) else BorderDark, RoundedCornerShape(14.dp))
                            .clickable { onSwitchAccount(acc.id) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = acc.serviceType.badgeChar,
                                color = if (isCurrent) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            if (acc.unreadCount > 0) {
                                Text(
                                    text = acc.unreadCount.toString(),
                                    color = if (isCurrent) Color.White else AccentLimeGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SplitViewScreen(
    uiState: RelayUiState,
    onSwapPanes: () -> Unit,
    onToggleWorkspaceMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val leftAccount = uiState.accounts.find { it.id == uiState.splitLeftSessionId }
        ?: uiState.accounts.firstOrNull()
    val rightAccount = uiState.accounts.find { it.id == uiState.splitRightSessionId }
        ?: uiState.accounts.getOrNull(1)
        ?: leftAccount

    if (leftAccount == null || rightAccount == null) {
        Box(
            modifier = modifier.fillMaxSize().background(BackgroundDark).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add at least one messenger account in Accounts to use Split View.",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        return
    }

    Column(modifier = modifier.fillMaxSize().background(BackgroundDark)) {
        // Modern Split View Top Header with Workspace / Split Switcher (Concept v2 Page 8)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(SurfaceDark)
                .border(1.dp, BorderDark)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "RELAY",
                    color = AccentLimeGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "split view",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Workspace / Split Switcher segmented control
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleWorkspaceMode() }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ViewSidebar,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Workspace",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentLimeGreen)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = BackgroundDark,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Split",
                            color = BackgroundDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Dual-Chat Split Panes side-by-side (50% / 50%)
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Left Pane (50%)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                ModernSplitPaneHeader(
                    account = leftAccount,
                    onSwap = onSwapPanes,
                    onClose = {}
                )

                Box(modifier = Modifier.weight(1f)) {
                    RetainedWebView(
                        sessionId = leftAccount.id,
                        serviceName = leftAccount.serviceType.displayName,
                        url = leftAccount.serviceType.defaultUrl,
                        displayUrl = leftAccount.serviceType.defaultUrl.removePrefix("https://"),
                        showHeader = false
                    )
                }

                SplitPaneBottomInput(placeholder = "Type a message")
            }

            // Center vertical divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(BorderDark)
            )

            // Right Pane (50%)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                ModernSplitPaneHeader(
                    account = rightAccount,
                    onSwap = onSwapPanes,
                    onClose = {}
                )

                Box(modifier = Modifier.weight(1f)) {
                    val rightSessionKey = if (rightAccount.id == leftAccount.id) "${rightAccount.id}_right" else rightAccount.id
                    RetainedWebView(
                        sessionId = rightSessionKey,
                        serviceName = rightAccount.serviceType.displayName,
                        url = rightAccount.serviceType.defaultUrl,
                        displayUrl = rightAccount.serviceType.defaultUrl.removePrefix("https://"),
                        showHeader = false
                    )
                }

                SplitPaneBottomInput(placeholder = "Write a message")
            }
        }
    }
}

@Composable
fun ModernSplitPaneHeader(
    account: AccountItem,
    onSwap: () -> Unit,
    onClose: () -> Unit
) {
    val initials = remember(account.accountName) {
        account.accountName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { account.accountName.take(2).uppercase() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(SurfaceDark)
            .border(1.dp, BorderDark)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.size(32.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(CardElevated)
                        .border(1.dp, BorderDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                ) {
                    ServiceBadge(serviceType = account.serviceType, size = 12)
                }
            }

            Column {
                Text(
                    text = account.accountName,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "${account.serviceType.displayName} · Personal · Live",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Swap",
                tint = TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onSwap() }
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onClose() }
            )
        }
    }
}

@Composable
fun SplitPaneBottomInput(placeholder: String) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(SurfaceDark)
            .border(1.dp, BorderDark)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(BackgroundDark)
                .border(1.dp, BorderDark, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
                cursorBrush = SolidColor(AccentLimeGreen),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(placeholder, color = TextMuted, fontSize = 12.sp)
                    }
                    inner()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(AccentLimeGreen)
                .clickable(enabled = text.isNotBlank()) { text = "" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = BackgroundDark,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// Retained for backward compatibility
@Deprecated("Use ModernSplitPaneHeader")
@Composable
fun SplitPaneHeader(
    serviceType: MessengerType,
    title: String,
    subtitle: String,
    onSwap: () -> Unit,
    onClose: () -> Unit
) {
    ModernSplitPaneHeader(
        account = AccountItem(
            id = "compat",
            accountName = title,
            serviceType = serviceType,
            statusText = subtitle,
            isConnected = true
        ),
        onSwap = onSwap,
        onClose = onClose
    )
}
