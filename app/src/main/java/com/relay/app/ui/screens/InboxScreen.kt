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
import com.relay.app.ui.components.RelaySectionHeader
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge

@Composable
fun InboxScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    onSelectServiceFilter: (MessengerType?) -> Unit,
    onMessageClick: (MessageItem) -> Unit,
    onOpenFullChat: (String) -> Unit,
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
        Row(modifier = Modifier.fillMaxSize()) {
            MessengersFilterColumn(
                uiState = uiState,
                onSelectServiceFilter = onSelectServiceFilter,
                modifier = Modifier
                    .weight(0.25f)
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

            InboxMessageListPane(
                uiState = uiState,
                onMessageClick = onMessageClick,
                onCycleServiceFilter = cycleServiceFilter,
                onToggleSort = onToggleSort,
                onSearchQueryChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(0.35f)
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
                modifier = Modifier
                    .weight(0.40f)
                    .fillMaxHeight()
            )
        }
    } else {
        InboxMessageListPane(
            uiState = uiState,
            onMessageClick = onMessageClick,
            onCycleServiceFilter = cycleServiceFilter,
            onToggleSort = onToggleSort,
            onSearchQueryChange = onSearchQueryChange,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun MessengersFilterColumn(
    uiState: RelayUiState,
    onSelectServiceFilter: (MessengerType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalUnread = uiState.services.sumOf { it.unreadCount }

    Column(modifier = modifier) {
        RelaySectionHeader(title = "Messengers")

        val isAllSelected = uiState.selectedServiceFilter == null
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isAllSelected) Color(0x444C8C2B) else Color.Transparent)
                .clickable { onSelectServiceFilter(null) }
                .drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f
                    )
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFF4CAF50))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(5.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("*", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
                Text("All", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Text("$totalUnread", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        uiState.services.forEach { summary ->
            val isSelected = uiState.selectedServiceFilter == summary.type
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) Color(0x444C8C2B) else Color.Transparent)
                    .clickable { onSelectServiceFilter(summary.type) }
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.15f),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1f
                        )
                    }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ServiceBadge(serviceType = summary.type, size = 28)
                    val label = if (summary.accountCount > 1) {
                        "${summary.type.displayName} (${summary.accountCount})"
                    } else {
                        summary.type.displayName
                    }
                    Text(
                        text = label,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${summary.unreadCount}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun InboxMessageListPane(
    uiState: RelayUiState,
    onMessageClick: (MessageItem) -> Unit,
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x33000000))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                .padding(horizontal = 10.dp, vertical = 5.dp),
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
                        text = "Filter by sender or message…",
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
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No messages in Inbox yet.\nAdd an account in Accounts or link a Mautrix bridge on your Matrix server.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ServiceBadge(serviceType = msg.serviceType, size = 36)

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
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = msg.timestamp,
                                    color = if (msg.isUnread) Color(0xFF8BC34A) else Color.White.copy(alpha = 0.8f),
                                    fontSize = 13.sp,
                                    fontWeight = if (msg.isUnread) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            Text(
                                text = msg.snippet,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
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
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF3E6A2A), Color(0xFF234216))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ActiveChatWorkspacePane(
    uiState: RelayUiState,
    onOpenFullChat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAccount = uiState.accounts.find { it.id == uiState.activeChatSessionId }
        ?: uiState.accounts.firstOrNull()

    Column(modifier = modifier) {
        if (activeAccount == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add a messenger account in Accounts to open its live WebView session here.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
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
                    text = "ok",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color(0x55000000))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MessengerType.entries.forEach { svc ->
                    val targetAccount = uiState.accounts.firstOrNull { it.serviceType == svc }
                    ServiceBadge(
                        serviceType = svc,
                        size = 24,
                        modifier = Modifier.clickable {
                            if (targetAccount != null) {
                                onOpenFullChat(targetAccount.id)
                            }
                        }
                    )
                }
            }
        }
    }
}
