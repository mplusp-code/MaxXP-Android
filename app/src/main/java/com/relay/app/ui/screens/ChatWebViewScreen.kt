package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.AccountItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge

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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            RetainedWebView(
                sessionId = sessionId,
                serviceName = serviceType.displayName,
                url = url,
                displayUrl = displayUrl
            )
        }

        // Bottom horizontal switcher bar showing ONLY added accounts
        if (accounts.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(Color(0xFF1E2E1B))
                    .border(1.dp, Color(0xFF38572A))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                accounts.forEach { acc ->
                    val isCurrent = acc.id == sessionId
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (isCurrent) Color(acc.serviceType.badgeColorHex).copy(alpha = 0.55f)
                                else Color(0x44000000)
                            )
                            .border(
                                width = if (isCurrent) 1.5.dp else 1.dp,
                                color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onSwitchAccount(acc.id) }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        ServiceBadge(serviceType = acc.serviceType, size = 24)
                        Text(
                            text = acc.accountName,
                            color = Color.White,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
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
    modifier: Modifier = Modifier
) {
    val leftAccount = uiState.accounts.find { it.id == uiState.splitLeftSessionId }
        ?: uiState.accounts.firstOrNull()
    val rightAccount = uiState.accounts.find { it.id == uiState.splitRightSessionId }
        ?: uiState.accounts.getOrNull(1)
        ?: leftAccount

    if (leftAccount == null || rightAccount == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Add at least one messenger account in Accounts to use Split View.",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        return
    }

    // Dual-Chat Split View (PDF Page 8) with persistent RetainedWebViews
    Row(modifier = modifier.fillMaxSize()) {
        // Left Chat Pane (50%)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            SplitPaneHeader(
                serviceType = leftAccount.serviceType,
                title = leftAccount.accountName,
                subtitle = "${leftAccount.serviceType.displayName} · ${leftAccount.serviceType.defaultUrl.replace("https://", "")}",
                onSwap = onSwapPanes,
                onClose = {}
            )

            Box(modifier = Modifier.weight(1f)) {
                RetainedWebView(
                    sessionId = leftAccount.id,
                    serviceName = leftAccount.serviceType.displayName,
                    url = leftAccount.serviceType.defaultUrl,
                    displayUrl = leftAccount.serviceType.defaultUrl.replace("https://", ""),
                    showHeader = false
                )
            }
        }

        // Center Vertical Split Divider
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color(0xFF38572A))
        )

        // Right Chat Pane (50%)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            SplitPaneHeader(
                serviceType = rightAccount.serviceType,
                title = rightAccount.accountName,
                subtitle = "${rightAccount.serviceType.displayName} · ${rightAccount.serviceType.defaultUrl.replace("https://", "")}",
                onSwap = onSwapPanes,
                onClose = {}
            )

            Box(modifier = Modifier.weight(1f)) {
                val rightSessionKey = if (rightAccount.id == leftAccount.id) "${rightAccount.id}_right" else rightAccount.id
                RetainedWebView(
                    sessionId = rightSessionKey,
                    serviceName = rightAccount.serviceType.displayName,
                    url = rightAccount.serviceType.defaultUrl,
                    displayUrl = rightAccount.serviceType.defaultUrl.replace("https://", ""),
                    showHeader = false
                )
            }
        }
    }
}

@Composable
fun SplitPaneHeader(
    serviceType: MessengerType,
    title: String,
    subtitle: String,
    onSwap: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF22361E), Color(0xFF132010))
                )
            )
            .border(1.dp, Color(0xFF38572A))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            ServiceBadge(serviceType = serviceType, size = 26)
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Swap",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onSwap() }
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClose() }
            )
        }
    }
}
