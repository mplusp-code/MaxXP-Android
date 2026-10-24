package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.relay.app.data.AccountItem
import com.relay.app.data.MessengerType
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
    onRemoveAccount: (String) -> Unit = {},
    onTriggerBridgeLogin: (String) -> Unit = {},
    onOpenAddAccount: () -> Unit = {},
    onOpenMatrixConfig: () -> Unit = {}
) {
    if (isExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            AccountsListPane(
                uiState = uiState,
                onSelectAccount = onSelectAccount,
                onToggleNotify = onToggleNotify,
                onOpenAddAccount = onOpenAddAccount,
                onOpenMatrixConfig = onOpenMatrixConfig,
                modifier = Modifier
                    .weight(0.36f)
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
                isExpanded = true,
                onOpenAccountChat = onOpenAccountChat,
                onClearSession = onClearSession,
                onRemoveAccount = onRemoveAccount,
                onTriggerBridgeLogin = onTriggerBridgeLogin,
                onOpenAddAccount = onOpenAddAccount,
                modifier = Modifier
                    .weight(0.64f)
                    .fillMaxHeight()
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            AccountsListPane(
                uiState = uiState,
                onSelectAccount = onSelectAccount,
                onToggleNotify = onToggleNotify,
                onOpenAddAccount = onOpenAddAccount,
                onOpenMatrixConfig = onOpenMatrixConfig,
                isScrollable = false,
                modifier = Modifier.fillMaxWidth()
            )

            AccountDetailInspectorPane(
                uiState = uiState,
                isExpanded = false,
                onOpenAccountChat = onOpenAccountChat,
                onClearSession = onClearSession,
                onRemoveAccount = onRemoveAccount,
                onTriggerBridgeLogin = onTriggerBridgeLogin,
                onOpenAddAccount = onOpenAddAccount,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AddMessengerAccountDialog(
    onDismiss: () -> Unit,
    onAddAccount: (serviceType: MessengerType, label: String) -> Unit
) {
    var selectedService by remember { mutableStateOf(MessengerType.WHATSAPP) }
    var accountLabel by remember { mutableStateOf("Personal") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF243B1D), Color(0xFF111C0E)))
                )
                .border(1.5.dp, Color(0xFF8BC34A).copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Add Messenger Account",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Text(
                text = "Each account gets its own isolated WebView profile (separate cookies, IndexedDB & Web Storage).",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )

            Text("Select Service", color = Color(0xFF8BC34A), fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MessengerType.entries.chunked(2).forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rowTypes.forEach { svc ->
                            val isSelected = selectedService == svc
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isSelected) Color(0x554C8C2B) else Color(0x44000000)
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF8BC34A) else Color.White.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { selectedService = svc }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ServiceBadge(serviceType = svc, size = 24)
                                Text(
                                    text = svc.displayName,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Text("Account Label (e.g. Personal, Work, @username)", color = Color(0xFF8BC34A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x88000000))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (accountLabel.isEmpty()) {
                    Text(
                        text = "Personal",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = accountLabel,
                    onValueChange = { accountLabel = it },
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                    cursorBrush = SolidColor(Color(0xFF8BC34A)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF3E4D38), Color(0xFF1F271B))))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF5FA836), Color(0xFF2E631F))))
                        .border(1.dp, Color.White.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                        .clickable { onAddAccount(selectedService, accountLabel) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
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
    onOpenAddAccount: () -> Unit = {},
    onOpenMatrixConfig: () -> Unit = {},
    isScrollable: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollMod = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Column(modifier = modifier.then(scrollMod)) {
        MatrixHomeserverBar(
            userId = uiState.matrixUserId,
            homeserver = uiState.matrixHomeserver,
            syncStatus = uiState.matrixSyncStatus,
            onConfigureClick = onOpenMatrixConfig
        )

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

        if (uiState.accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "No accounts configured yet. Tap '+ Add messenger account...' below to add your real WhatsApp, Telegram, Instagram, or other accounts.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
            }
        } else {
            uiState.accounts.forEach { acc ->
                val isSelected = acc.id == uiState.selectedAccountId
                AccountRowItem(
                    account = acc,
                    isSelected = isSelected,
                    onClick = { onSelectAccount(acc.id) },
                    onToggleNotify = { onToggleNotify(acc.id) }
                )
            }
        }

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
                .clickable { onOpenAddAccount() }
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val dotColor = when {
                        account.isConnected -> Color(0xFF4CAF50)
                        account.isNeedsAttention -> RelayColors.StatusWarningAmber
                        else -> Color(0xFFFFA000)
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    val displayStatus = when {
                        account.isConnected -> "Connected"
                        account.statusText.contains("Relink", ignoreCase = true) || account.isNeedsAttention -> "Relink"
                        account.statusText.isNotBlank() && !account.statusText.contains("Tap Open", ignoreCase = true) && !account.statusText.contains("sign in to connect", ignoreCase = true) -> account.statusText
                        else -> "Link bridge"
                    }
                    Text(
                        text = displayStatus,
                        color = if (account.isConnected) Color(0xFF8BC34A) else if (account.isNeedsAttention) RelayColors.StatusWarningAmber else Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = if (account.isNeedsAttention || account.isConnected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

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
    isExpanded: Boolean = true,
    onOpenAccountChat: (String) -> Unit,
    onClearSession: (String) -> Unit = {},
    onRemoveAccount: (String) -> Unit = {},
    onTriggerBridgeLogin: (String) -> Unit = {},
    onOpenAddAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val selected = uiState.accounts.find { it.id == uiState.selectedAccountId }
        ?: uiState.accounts.firstOrNull()

    if (selected == null) {
        Column(
            modifier = modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RelaySectionHeader(title = "Account Inspector")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33000000))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                    .clickable { onOpenAddAccount() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap '+ Add messenger account...' to create an isolated profile and scan your QR code.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val boxSizeModifier = if (isExpanded) {
            Modifier.weight(1f).fillMaxWidth()
        } else {
            Modifier.fillMaxWidth().height(340.dp)
        }

        RelaySectionHeader(title = selected.accountName)

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
                    "Status: Needs login · ${selected.statusText}"
                } else {
                    "Status: Connected · ${selected.statusText}"
                },
                color = if (selected.isNeedsAttention) RelayColors.StatusWarningAmber else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        val dashBorderColor = Color(0xFF8BC34A).copy(alpha = 0.75f)
        Box(
            modifier = boxSizeModifier
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
                .padding(6.dp),
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
                            text = "QR code from ${selected.botMxid ?: selected.serviceType.defaultUrl.removePrefix("https://")}",
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
                            text = "Live session: ${selected.serviceType.defaultUrl.removePrefix("https://")} · Tap Open for full screen",
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
                            text = "Cookies, IndexedDB & Web Storage isolated · Tap Open to view chats",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val linkActionLabel = when {
                !selected.isConnected && (selected.statusText.isBlank() || selected.statusText.contains("Tap Open", ignoreCase = true) || selected.statusText.contains("sign in to connect", ignoreCase = true)) -> "Link bridge"
                else -> "Relink"
            }
            AccountActionButton(
                label = linkActionLabel,
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
            AccountActionButton(
                label = "Remove",
                onClick = { onRemoveAccount(selected.id) },
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
            fontSize = 13.sp
        )
    }
}
