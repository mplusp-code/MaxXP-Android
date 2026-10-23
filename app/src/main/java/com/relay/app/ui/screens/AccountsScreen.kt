package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.AccountItem
import com.relay.app.data.RelayUiState
import com.relay.app.ui.components.RelaySectionHeader
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge

@Composable
fun AccountsScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    onSelectAccount: (String) -> Unit,
    onToggleNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        // Unfolded Master-Detail 2-Pane Layout (PDF Page 9)
        Row(modifier = modifier.fillMaxSize()) {
            AccountsListPane(
                uiState = uiState,
                onSelectAccount = onSelectAccount,
                onToggleNotify = onToggleNotify,
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight()
            )

            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.2f))
            )

            AccountDetailInspectorPane(
                uiState = uiState,
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
            )
        }
    } else {
        // Portrait 1-Pane Accounts Screen (PDF Page 4)
        AccountsListPane(
            uiState = uiState,
            onSelectAccount = onSelectAccount,
            onToggleNotify = onToggleNotify,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
fun AccountsListPane(
    uiState: RelayUiState,
    onSelectAccount: (String) -> Unit,
    onToggleNotify: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        // Table Header: Account | Notify
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("Notify", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(uiState.accounts) { acc ->
                val isSelected = acc.id == uiState.selectedAccountId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .background(
                            if (isSelected) Color(0xFF68BB35).copy(alpha = 0.4f)
                            else Color(0x22000000)
                        )
                        .drawBehind {
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .clickable { onSelectAccount(acc.id) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ServiceBadge(serviceType = acc.serviceType, size = 32)
                        Column {
                            Text(
                                text = acc.accountName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (acc.isNeedsAttention) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                                Text(
                                    text = acc.statusText,
                                    color = if (acc.isNeedsAttention) Color(0xFFFFA000) else Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Skeuomorphic Square Checkbox for Notify
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White)
                            .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .clickable { onToggleNotify(acc.id) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (acc.isNotifyEnabled) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Enabled",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item {
                // Add account row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(Color(0x22000000))
                        .clickable { }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF4CAF50))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Text("Tap here to add an account", color = Color.White.copy(alpha = 0.9f), fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
fun AccountDetailInspectorPane(
    uiState: RelayUiState,
    modifier: Modifier = Modifier
) {
    val selected = uiState.accounts.find { it.id == uiState.selectedAccountId } ?: uiState.accounts.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        if (selected != null) {
            Text(
                text = selected.accountName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (selected.isNeedsAttention) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "Session expired. Scan the QR code to relink.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(14.dp))

            // Live QR relinking container with RetainedWebView (PDF Page 9)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33000000))
                    .border(1.dp, Color(0xFF8BC34A).copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selected.isNeedsAttention) {
                    RetainedWebView(
                        sessionId = selected.id,
                        serviceName = selected.serviceType.displayName,
                        url = selected.serviceType.defaultUrl,
                        displayUrl = selected.serviceType.defaultUrl.replace("https://", ""),
                        showHeader = false
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Account session is active",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons: Relink & Remove account
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF4C8C2B), Color(0xFF2E631F))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Relink", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF3B4A34), Color(0xFF1E281A))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Remove account", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            RelaySectionHeader(title = "Notifications")

            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AccountSettingCheckboxRow(title = "Show unread count on Today", checked = true)
                AccountSettingCheckboxRow(title = "Keep session alive", checked = true)
            }
        }
    }
}

@Composable
fun AccountSettingCheckboxRow(title: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
        Text(text = title, color = Color.White, fontSize = 14.sp)
    }
}
