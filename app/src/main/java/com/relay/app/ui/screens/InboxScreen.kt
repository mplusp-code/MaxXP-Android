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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.ui.components.RelaySectionHeader
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge

@Composable
fun InboxScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onMessageClick: (MessageItem) -> Unit = {},
    onServiceFilterSelect: (MessengerType?) -> Unit = {}
) {
    if (isExpanded) {
        // Unfolded Tri-Pane Workspace (PDF Page 7)
        Row(modifier = modifier.fillMaxSize()) {
            // Pane 1: Messengers Column (~22%)
            MessengersFilterColumn(
                uiState = uiState,
                modifier = Modifier
                    .weight(0.22f)
                    .fillMaxHeight(),
                onSelectFilter = onServiceFilterSelect
            )

            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // Pane 2: Message List Column (~38%)
            InboxMessageListPane(
                uiState = uiState,
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight(),
                onMessageClick = onMessageClick,
                isTriPane = true
            )

            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // Pane 3: Active Chat WebView Pane (~40%)
            ActiveChatWorkspacePane(
                uiState = uiState,
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight()
            )
        }
    } else {
        // Portrait 1-Pane Unified Inbox (PDF Page 2)
        InboxMessageListPane(
            uiState = uiState,
            modifier = modifier.fillMaxSize(),
            onMessageClick = onMessageClick,
            isTriPane = false
        )
    }
}

@Composable
fun MessengersFilterColumn(
    uiState: RelayUiState,
    modifier: Modifier = Modifier,
    onSelectFilter: (MessengerType?) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        RelaySectionHeader(title = "Messengers")

        Spacer(Modifier.height(4.dp))

        // All Inbox Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (uiState.selectedServiceFilter == null) Color(0x554CAF50) else Color(0x22000000)
                )
                .clickable { onSelectFilter(null) }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFFA000)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✉", color = Color.White, fontSize = 12.sp)
                }
                Text("All inbox", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Text("29", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        // Service items
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(uiState.services) { svc ->
                val isSelected = uiState.selectedServiceFilter == svc.type
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isSelected) Color(0x444CAF50) else Color.Transparent)
                        .clickable { onSelectFilter(svc.type) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServiceBadge(serviceType = svc.type, size = 24)
                        Text(
                            text = svc.type.displayName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        text = if (svc.unreadCount > 0) "${svc.unreadCount}" else "none",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clickable { }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Text("Add service", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                }
            }
        }

        // Bottom session alert
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFB5651D).copy(alpha = 0.3f))
                .border(1.dp, Color(0xFFFFA000).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA000), modifier = Modifier.size(16.dp))
            Text("1 session needs login", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun InboxMessageListPane(
    uiState: RelayUiState,
    isTriPane: Boolean,
    modifier: Modifier = Modifier,
    onMessageClick: (MessageItem) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        if (!isTriPane) {
            // Dropdown filter bar (PDF Page 2): Service: All | Sort: Date
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RelayFilterDropdown(
                    label = "Service: All",
                    modifier = Modifier.weight(1f)
                )
                RelayFilterDropdown(
                    label = "Sort: Date",
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Tri-pane Search chats bar (PDF Page 7)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x44000000))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Search chats", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
        }

        RelaySectionHeader(title = "Today")

        // Messages Feed
        val filtered = if (uiState.selectedServiceFilter != null) {
            uiState.messages.filter { it.serviceType == uiState.selectedServiceFilter }
        } else {
            uiState.messages
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(filtered) { msg ->
                val isFirstHighlighted = msg.id == "1"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            if (isFirstHighlighted) Color(0xFF68BB35).copy(alpha = 0.45f)
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
                        .clickable { onMessageClick(msg) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ServiceBadge(serviceType = msg.serviceType, size = 32)
                        Column {
                            Text(
                                text = msg.sender,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = msg.snippet,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Text(
                        text = msg.timestamp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveChatWorkspacePane(
    uiState: RelayUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Active Chat WebView via Retained pool
        Box(modifier = Modifier.weight(1f)) {
            RetainedWebView(
                sessionId = uiState.activeChatSessionId,
                serviceName = "Telegram",
                url = "https://web.telegram.org",
                displayUrl = "web.telegram.org"
            )
        }

        // Bottom horizontal service switcher bar (PDF Page 7)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(Color(0xFF1E2E1B))
                .border(1.dp, Color(0xFF38572A))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ServiceBadge(serviceType = MessengerType.WHATSAPP, size = 30)

            // Active Telegram badge with text pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF2196F3).copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ServiceBadge(serviceType = MessengerType.TELEGRAM, size = 26)
                Text("Telegram", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            ServiceBadge(serviceType = MessengerType.INSTAGRAM, size = 30)
            ServiceBadge(serviceType = MessengerType.FACEBOOK, size = 30)
            ServiceBadge(serviceType = MessengerType.VIBER, size = 30)
            ServiceBadge(serviceType = MessengerType.GOOGLE_MESSAGES, size = 30)
        }
    }
}

@Composable
fun RelayFilterDropdown(label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF243B1D))
            .border(1.dp, Color(0xFF4C8C2B).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
    }
}
