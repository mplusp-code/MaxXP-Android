package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.AccountItem
import com.relay.app.data.RelayUiState
import com.relay.app.matrix.MatrixSyncStatus
import com.relay.app.ui.components.RelaySectionHeader
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge
import com.relay.app.ui.theme.RelayColors

@Composable
fun AccountsScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    onSelectAccount: (String) -> Unit,
    onToggleNotify: (String) -> Unit,
    onOpenAccountChat: (String) -> Unit,
    onClearSession: (String) -> Unit = {},
    onTriggerBridgeLogin: (String) -> Unit = {},
    onOpenMatrixConfig: () -> Unit = {}
) {
    if (isExpanded) {
        // Dual-pane unfolded Accounts screen (PDF Page 9)
        Row(modifier = Modifier.fillMaxSize()) {
            AccountsListPane(
                uiState = uiState,
                onSelectAccount = onSelectAccount,
                onToggleNotify = onToggleNotify,
                onOpenMatrixConfig = onOpenMatrixConfig,
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.25f),
                            start = Offset(size.width, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2f
                        )
                    }
            )

            AccountDetailInspectorPane(
                uiState = uiState,
                onOpenAccountChat = onOpenAccountChat,
                onClearSession = onClearSession,
                onTriggerBridgeLogin = onTriggerBridgeLogin,
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
            )
        }
    } else {
        // Portrait Cover Screen Accounts (PDF Page 4) + inline Bridge Inspector
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AccountsListPane(
                uiState = uiState,
                onSelectAccount = onSelectAccount,
                onToggleNotify = onToggleNotify,
                onOpenMatrixConfig = onOpenMatrixConfig,
                isScrollable = false,
                modifier = Modifier.fillMaxWidth()
            )

            AccountDetailInspectorPane(
                uiState = uiState,
                onOpenAccountChat = onOpenAccountChat,
                onClearSession = onClearSession,
                onTriggerBridgeLogin = onTriggerBridgeLogin,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun MatrixHomeserverBar(
    userId: String,
    homeserver: String,
    syncStatus: MatrixSyncStatus,
    onConfigureClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.verticalGradient(listOf(Color(0xFF243B1D), Color(0xFF142210)))
            )
            .border(1.dp, Color(0xFF4C8C2B).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Matrix Conduit · $userId",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "${homeserver.removePrefix("https://")} (${syncStatus.label})",
                color = if (syncStatus == MatrixSyncStatus.OFFLINE) RelayColors.StatusWarningAmber
                else Color(0xFF8BC34A),
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF4C8C2B), Color(0xFF2E631F)))
                )
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                .clickable { onConfigureClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("Server", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun AccountsListPane(
    uiState: RelayUiState,
    onSelectAccount: (String) -> Unit,
    onToggleNotify: (String) -> Unit,
    onOpenMatrixConfig: () -> Unit = {},
    isScrollable: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollMod = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Column(modifier = modifier.then(scrollMod)) {
        // Matrix Conduit Server Status & Config Bar
        MatrixHomeserverBar(
            userId = uiState.matrixUserId,
            homeserver = uiState.matrixHomeserver,
            syncStatus = uiState.matrixSyncStatus,
            onConfigureClick = onOpenMatrixConfig
        )

        // Header Row: Account | Notify
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x33000000))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Account", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text("Notify", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))

        uiState.accounts.forEach { acc ->
            val isSelected = acc.id == uiState.selectedAccountId
            AccountRowItem(
                account = acc,
                isSelected = isSelected,
                onClick = { onSelectAccount(acc.id) },
                onToggleNotify = { onToggleNotify(acc.id) }
            )
        }

        // "+ Add messenger account..." button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF3E6A2A), Color(0xFF213D15))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                .clickable {
                    val target = uiState.accounts.firstOrNull { it.isNeedsAttention } ?: uiState.accounts.first()
                    onSelectAccount(target.id)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF8BC34A),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Add messenger account...",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun AccountRowItem(
    account: AccountItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleNotify: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0x444C8C2B) else Color.Transparent)
            .clickable { onClick() }
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.15f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            ServiceBadge(serviceType = account.serviceType, size = 34)
            Column {
                Text(
                    text = account.accountName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = account.statusText,
                    color = if (account.isNeedsAttention) RelayColors.StatusWarningAmber else Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontWeight = if (account.isNeedsAttention) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        // Classic PDA Bevel Checkbox
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (account.isNotifyEnabled) Color(0xFF4CAF50) else Color(0x55000000)
                )
                .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .clickable { onToggleNotify() },
            contentAlignment = Alignment.Center
        ) {
            if (account.isNotifyEnabled) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Enabled",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AccountDetailInspectorPane(
    uiState: RelayUiState,
    onOpenAccountChat: (String) -> Unit,
    onClearSession: (String) -> Unit = {},
    onTriggerBridgeLogin: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selected = uiState.accounts.find { it.id == uiState.selectedAccountId }
        ?: uiState.accounts.first { it.isNeedsAttention }

    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RelaySectionHeader(title = selected.accountName)

        // Status banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x33000000))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selected.isNeedsAttention) {
                    "Status: Needs QR scan · ${selected.statusText}"
                } else {
                    "Status: Connected · ${selected.statusText}"
                },
                color = if (selected.isNeedsAttention) RelayColors.StatusWarningAmber else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        // Dotted Green Border QR Relink Box (PDF Page 9)
        val dashBorderColor = Color(0xFF8BC34A).copy(alpha = 0.75f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0x33000000))
                .drawBehind {
                    drawRoundRect(
                        color = dashBorderColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        ),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                !selected.bridgeQrAscii.isNullOrBlank() -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = selected.bridgeQrAscii,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                lineHeight = 8.sp
                            )
                        }
                        Text(
                            text = "QR code from ${selected.serviceType.defaultUrl.removePrefix("https://")} (${selected.botMxid ?: "Mautrix"})",
                            color = Color(0xFF8BC34A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                selected.isNeedsAttention -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            RetainedWebView(
                                sessionId = selected.id,
                                serviceName = selected.serviceType.displayName,
                                url = selected.serviceType.defaultUrl,
                                displayUrl = selected.serviceType.defaultUrl.removePrefix("https://"),
                                showHeader = false
                            )
                        }
                        Text(
                            text = "QR code from ${selected.serviceType.defaultUrl.removePrefix("https://")}",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "✓ Isolated Profile Active (profile_${selected.id})",
                            color = Color(0xFF8BC34A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Cookies, IndexedDB & Web Storage isolated · Tap Relink for bridge QR",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Action Buttons: [Relink] [Open] [Clear]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountActionButton(
                label = "Relink",
                onClick = { onTriggerBridgeLogin(selected.id) },
                modifier = Modifier.weight(1f)
            )
            AccountActionButton(
                label = "Open",
                onClick = { onOpenAccountChat(selected.id) },
                modifier = Modifier.weight(1f)
            )
            AccountActionButton(
                label = "Clear",
                onClick = { onClearSession(selected.id) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AccountActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4C8C2B), Color(0xFF234216))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}
