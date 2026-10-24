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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
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
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge
import com.relay.app.ui.theme.*

enum class AccountsProfileFilter(val label: String) {
    ALL("All"),
    PERSONAL("Personal"),
    WORK("Work")
}

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
    var activeFilter by remember { mutableStateOf(AccountsProfileFilter.ALL) }
    var selectedAccountId by remember {
        mutableStateOf(uiState.selectedAccountId.ifBlank { uiState.accounts.firstOrNull()?.id ?: "" })
    }

    val selectedAccount = uiState.accounts.find { it.id == selectedAccountId }
        ?: uiState.accounts.firstOrNull()

    if (isExpanded) {
        // Unfolded Master-Detail Inspector (Concept v2 Page 9)
        Row(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            // Left Pane (Accounts List)
            Box(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight()
                    .border(width = 1.dp, color = BorderDark)
            ) {
                AccountsListPane(
                    uiState = uiState,
                    activeFilter = activeFilter,
                    selectedAccountId = selectedAccount?.id,
                    onSelectFilter = { activeFilter = it },
                    onSelectAccount = { id ->
                        selectedAccountId = id
                        onSelectAccount(id)
                    },
                    onToggleNotify = onToggleNotify,
                    onTriggerBridgeLogin = onTriggerBridgeLogin,
                    onOpenAddAccount = onOpenAddAccount,
                    onOpenMatrixConfig = onOpenMatrixConfig
                )
            }

            // Right Pane (Inspector / QR Viewport)
            Box(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight()
            ) {
                UnfoldedAccountInspectorPane(
                    account = selectedAccount,
                    onTriggerBridgeLogin = onTriggerBridgeLogin,
                    onRemoveAccount = onRemoveAccount,
                    onToggleNotify = onToggleNotify,
                    onOpenAddAccount = onOpenAddAccount
                )
            }
        }
    } else {
        // Portrait Layout (Concept v2 Page 4)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .verticalScroll(rememberScrollState())
        ) {
            AccountsListPane(
                uiState = uiState,
                activeFilter = activeFilter,
                selectedAccountId = selectedAccount?.id,
                onSelectFilter = { activeFilter = it },
                onSelectAccount = { id ->
                    selectedAccountId = id
                    onSelectAccount(id)
                },
                onToggleNotify = onToggleNotify,
                onTriggerBridgeLogin = onTriggerBridgeLogin,
                onOpenAddAccount = onOpenAddAccount,
                onOpenMatrixConfig = onOpenMatrixConfig
            )
        }
    }
}

@Composable
fun AccountsListPane(
    uiState: RelayUiState,
    activeFilter: AccountsProfileFilter,
    selectedAccountId: String?,
    onSelectFilter: (AccountsProfileFilter) -> Unit,
    onSelectAccount: (String) -> Unit,
    onToggleNotify: (String) -> Unit,
    onTriggerBridgeLogin: (String) -> Unit,
    onOpenAddAccount: () -> Unit,
    onOpenMatrixConfig: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: RELAY, accounts, '+' add account button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = "RELAY",
                    color = AccentLimeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "accounts",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(1.dp, BorderDark, CircleShape)
                    .clickable { onOpenAddAccount() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Account",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Profile Filter Tabs: All, Personal, Work
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountsProfileFilter.entries.forEach { filter ->
                val isSelected = filter == activeFilter
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) AccentLimeGreen else CardDark)
                        .border(1.dp, if (isSelected) AccentLimeGreen else BorderDark, RoundedCornerShape(16.dp))
                        .clickable { onSelectFilter(filter) }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        color = if (isSelected) BackgroundDark else TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        val filteredAccounts = remember(uiState.accounts, activeFilter) {
            uiState.accounts.filter { acc ->
                when (activeFilter) {
                    AccountsProfileFilter.ALL -> true
                    AccountsProfileFilter.PERSONAL -> !acc.accountName.contains("work", ignoreCase = true)
                    AccountsProfileFilter.WORK -> acc.accountName.contains("work", ignoreCase = true)
                }
            }
        }

        val attentionAccounts = filteredAccounts.filter { it.isNeedsAttention || !it.isConnected }
        val connectedAccounts = filteredAccounts.filter { it.isConnected && !it.isNeedsAttention }

        // NEEDS ATTENTION Section
        if (attentionAccounts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "NEEDS ATTENTION",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                attentionAccounts.forEach { acc ->
                    AttentionAccountRow(
                        account = acc,
                        onRelinkClick = { onTriggerBridgeLogin(acc.id) }
                    )
                }
            }
        }

        // CONNECTED Section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "CONNECTED · ${connectedAccounts.size}",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (connectedAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No connected accounts in this profile", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                connectedAccounts.forEach { acc ->
                    val isSelected = acc.id == selectedAccountId
                    ConnectedAccountRow(
                        account = acc,
                        isSelected = isSelected,
                        onClick = { onSelectAccount(acc.id) },
                        onToggleNotify = { onToggleNotify(acc.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun AttentionAccountRow(
    account: AccountItem,
    onRelinkClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AmberAttentionBg)
            .border(1.dp, AmberAttentionBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ServiceBadge(serviceType = account.serviceType, size = 32)
                Column {
                    Text(
                        text = account.accountName,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "▲ Session expired",
                        color = AmberAttentionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmberAttention)
                    .clickable { onRelinkClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Relink",
                    color = BackgroundDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ConnectedAccountRow(
    account: AccountItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onToggleNotify: () -> Unit
) {
    val isWork = account.accountName.contains("work", ignoreCase = true)
    val profileLabel = if (isWork) "Work" else "Personal"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CardElevated else CardDark)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentLimeGreen.copy(alpha = 0.5f) else BorderDark,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ServiceBadge(serviceType = account.serviceType, size = 32)
            Column {
                Text(
                    text = account.accountName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(AccentGreen))
                    Text(
                        text = "Live · $profileLabel",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Green toggle switch
        Switch(
            checked = account.isNotifyEnabled,
            onCheckedChange = { onToggleNotify() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundDark,
                checkedTrackColor = AccentLimeGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardElevated
            )
        )
    }
}

@Composable
fun UnfoldedAccountInspectorPane(
    account: AccountItem?,
    onTriggerBridgeLogin: (String) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onToggleNotify: (String) -> Unit,
    onOpenAddAccount: () -> Unit
) {
    if (account == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundDark).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Select an account to view pairing status", color = TextSecondary, fontSize = 14.sp)
        }
        return
    }

    var selectedProfileOption by remember {
        mutableStateOf(if (account.accountName.contains("work", ignoreCase = true)) "Work" else "Personal")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Account Header: Service badge, account name, status pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ServiceBadge(serviceType = account.serviceType, size = 36)
                Column {
                    Text(
                        text = account.accountName,
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (account.isNeedsAttention) "▲ Session expired" else "• Live · $selectedProfileOption",
                        color = if (account.isNeedsAttention) AmberAttentionText else AccentGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Dashed QR Code Viewport Card (Concept v2 Page 9)
        val dashBorderColor = BorderDark
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .drawBehind {
                    drawRoundRect(
                        color = dashBorderColor,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        ),
                        cornerRadius = CornerRadius(16.dp.toPx())
                    )
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!account.bridgeQrAscii.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = account.bridgeQrAscii,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
                        lineHeight = 7.sp
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = "QR Code",
                        tint = TextSecondary,
                        modifier = Modifier.size(54.dp)
                    )
                    Text(
                        text = "QR code appears here",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "From ${account.serviceType.defaultUrl.removePrefix("https://")}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Text(
            text = "Open ${account.serviceType.displayName} on your phone, then Linked devices, then scan.",
            color = TextSecondary,
            fontSize = 12.sp
        )

        // Action Buttons: Relink & Remove account
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentLimeGreen)
                    .clickable { onTriggerBridgeLogin(account.id) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Relink", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardElevated)
                    .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                    .clickable { onRemoveAccount(account.id) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Remove account", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
            }
        }

        // Account-Specific Settings
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(CardDark)
                .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Notifications toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = account.isNotifyEnabled,
                    onCheckedChange = { onToggleNotify(account.id) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = AccentLimeGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardElevated
                    )
                )
            }

            // Keep session alive toggle
            var keepAlive by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Keep session alive", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Switch(
                    checked = keepAlive,
                    onCheckedChange = { keepAlive = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BackgroundDark,
                        checkedTrackColor = AccentLimeGreen,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = CardElevated
                    )
                )
            }

            // Profile dropdown selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Profile", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardElevated)
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .clickable {
                            selectedProfileOption = if (selectedProfileOption == "Personal") "Work" else "Personal"
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = selectedProfileOption,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    text = "Add Messenger Account",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Text(
                text = "Each account gets its own isolated WebView profile (separate cookies, IndexedDB & storage).",
                color = TextSecondary,
                fontSize = 12.sp
            )

            Text("Select Service", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MessengerType.entries.chunked(2).forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTypes.forEach { svc ->
                            val isSelected = selectedService == svc
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CardElevated else BackgroundDark)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AccentLimeGreen else BorderDark,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedService = svc }
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ServiceBadge(serviceType = svc, size = 20)
                                Text(
                                    text = svc.displayName,
                                    color = if (isSelected) TextPrimary else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

            Text("Account Label", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = accountLabel,
                    onValueChange = { accountLabel = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(AccentLimeGreen),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardElevated)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentLimeGreen)
                        .clickable { onAddAccount(selectedService, accountLabel) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Add Account", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
