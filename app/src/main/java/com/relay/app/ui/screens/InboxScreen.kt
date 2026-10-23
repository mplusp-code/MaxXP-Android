package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge

@Composable
fun InboxScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    onSelectServiceFilter: (MessengerType?) -> Unit,
    onMessageClick: (MessageItem) -> Unit,
    onOpenFullChat: (String) -> Unit,
    onSwitchService: (MessengerType) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSort: () -> Unit = {}
) {
    val cycleServiceFilter = {
        val allTypes = MessengerType.entries
        val current = uiState.selectedServiceFilter
        val next = if (current == null) {
            allTypes.first()
        } else {
            val idx = allTypes.indexOf(current)
            if (idx + 1 < allTypes.size) allTypes[idx + 1] else null
        }
        onSelectServiceFilter(next)
    }

    if (isExpanded) {
        // Wide Unfolded Workspace: 28% Message List + 72% Wide Active WebView Workspace
        Row(modifier = Modifier.fillMaxSize()) {
            InboxMessageListPane(
                uiState = uiState,
                onMessageClick = onMessageClick,
                onSelectServiceFilter = onSelectServiceFilter,
                onCycleServiceFilter = cycleServiceFilter,
                onToggleSort = onToggleSort,
                onSearchQueryChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(0.28f)
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

            ActiveChatWorkspacePane(
                uiState = uiState,
                onOpenFullChat = onOpenFullChat,
                onSwitchService = onSwitchService,
                modifier = Modifier
                    .weight(0.72f)
                    .fillMaxHeight()
            )
        }
    } else {
        InboxMessageListPane(
            uiState = uiState,
            onMessageClick = onMessageClick,
            onSelectServiceFilter = onSelectServiceFilter,
            onCycleServiceFilter = cycleServiceFilter,
            onToggleSort = onToggleSort,
            onSearchQueryChange = onSearchQueryChange,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun InboxMessageListPane(
    uiState: RelayUiState,
    onMessageClick: (MessageItem) -> Unit,
    onSelectServiceFilter: (MessengerType?) -> Unit = {},
    onCycleServiceFilter: () -> Unit = {},
    onToggleSort: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filtered = uiState.messages
        .filter { msg ->
            (uiState.selectedServiceFilter == null || msg.serviceType == uiState.selectedServiceFilter) &&
                (uiState.searchQuery.isBlank() ||
                    msg.sender.contains(uiState.searchQuery, ignoreCase = true) ||
                    msg.snippet.contains(uiState.searchQuery, ignoreCase = true))
        }
        .let { list ->
            if (uiState.sortAscending) list.reversed() else list
        }

    Column(modifier = modifier) {
        // Compact Service Filter Badge Strip (*, W, T, I, F, G, V)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x44000000))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isAllSelected = uiState.selectedServiceFilter == null
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isAllSelected) Color(0xFF4CAF50) else Color(0x55000000))
                    .border(
                        width = if (isAllSelected) 1.5.dp else 1.dp,
                        color = if (isAllSelected) Color.White else Color.White.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(5.dp)
                    )
                    .clickable { onSelectServiceFilter(null) },
                contentAlignment = Alignment.Center
            ) {
                Text("*", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }

            MessengerType.entries.forEach { svc ->
                val isSelected = uiState.selectedServiceFilter == svc
                Box(
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) Color.White else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable {
                            onSelectServiceFilter(if (isSelected) null else svc)
                        }
                ) {
                    ServiceBadge(serviceType = svc, size = 26)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x33000000))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RelayFilterDropdown(
                label = "Service: ${uiState.selectedServiceFilter?.displayName ?: "All"}",
                onClick = onCycleServiceFilter,
                modifier = Modifier.weight(1f)
            )
            RelayFilterDropdown(
                label = "Sort: ${if (uiState.sortAscending) "Oldest" else "Date"}",
                onClick = onToggleSort,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x22000000))
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(16.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x55000000))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (uiState.searchQuery.isEmpty()) {
                    Text(
                        text = "Filter chats…",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.sp
                    )
                }
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                    cursorBrush = SolidColor(Color(0xFF8BC34A)),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No incoming messages yet.\nSelect any messenger icon on the right to open its live WebView.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.id }) { msg ->
                    val isHighlighted = msg.id == uiState.selectedMessageId ||
                        (uiState.selectedMessageId == null && msg.accountId == uiState.activeChatSessionId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isHighlighted) Color(0x444C8C2B) else Color.Transparent)
                            .clickable { onMessageClick(msg) }
                            .drawBehind {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServiceBadge(serviceType = msg.serviceType, size = 32)

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg.sender,
                                    color = Color.White,
                                    fontWeight = if (msg.isUnread) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = msg.timestamp,
                                    color = if (msg.isUnread) Color(0xFF8BC34A) else Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = if (msg.isUnread) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            Text(
                                text = msg.snippet,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RelayFilterDropdown(
    label: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF3E6A2A), Color(0xFF234216))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun ActiveChatWorkspacePane(
    uiState: RelayUiState,
    onOpenFullChat: (String) -> Unit,
    onSwitchService: (MessengerType) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeAccount = uiState.accounts.find { it.id == uiState.activeChatSessionId }
        ?: uiState.accounts.firstOrNull()

    Column(modifier = modifier) {
        if (activeAccount == null) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap any messenger icon below (W, T, I, F, G, V) or add an account in Accounts to launch its isolated WebView session.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .background(Color(0x44000000))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceBadge(serviceType = activeAccount.serviceType, size = 20)
                    Text(
                        text = activeAccount.accountName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "Full ⤢",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF3B5D2E))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
                        .clickable { onOpenFullChat(activeAccount.id) }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                RetainedWebView(
                    sessionId = activeAccount.id,
                    serviceName = activeAccount.accountName,
                    url = activeAccount.serviceType.defaultUrl,
                    displayUrl = activeAccount.serviceType.defaultUrl.removePrefix("https://"),
                    showHeader = true
                )
            }
        }

        // Bottom service switcher bar (W, T, I, F, G, V) - always interactive
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0x55000000))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MessengerType.entries.forEach { svc ->
                val isCurrentService = activeAccount?.serviceType == svc
                Box(
                    modifier = Modifier
                        .border(
                            width = if (isCurrentService) 1.5.dp else 0.dp,
                            color = if (isCurrentService) Color(0xFF8BC34A) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSwitchService(svc) }
                ) {
                    ServiceBadge(
                        serviceType = svc,
                        size = 28
                    )
                }
            }
        }
    }
}
