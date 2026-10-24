package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.relay.app.data.InboxViewMode
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
    onSwitchAccount: (String) -> Unit = {},
    onSelectInboxViewMode: (InboxViewMode) -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onToggleSort: () -> Unit = {}
) {
    val addedServiceTypes = uiState.accounts.map { it.serviceType }.distinct()

    val cycleServiceFilter = {
        if (addedServiceTypes.isEmpty()) {
            onSelectServiceFilter(null)
        } else {
            val current = uiState.selectedServiceFilter
            val next = if (current == null) {
                addedServiceTypes.first()
            } else {
                val idx = addedServiceTypes.indexOf(current)
                if (idx >= 0 && idx + 1 < addedServiceTypes.size) addedServiceTypes[idx + 1] else null
            }
            onSelectServiceFilter(next)
        }
    }

    val activeAccount = uiState.accounts.find { it.id == uiState.activeChatSessionId }
        ?: uiState.accounts.firstOrNull()
    val unreadCount = uiState.messages.count { it.isUnread }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Segmented Switch Bar (Messages List vs Web Messenger) in both Folded & Unfolded modes
        InboxViewModeSwitchBar(
            currentMode = uiState.inboxViewMode,
            unreadCount = unreadCount,
            totalMessages = uiState.messages.size,
            activeAccountName = activeAccount?.accountName,
            onSelectMode = onSelectInboxViewMode
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (uiState.inboxViewMode) {
                InboxViewMode.MESSAGES_LIST -> {
                    InboxMessageListPane(
                        uiState = uiState,
                        addedServiceTypes = addedServiceTypes,
                        onMessageClick = onMessageClick,
                        onSelectServiceFilter = onSelectServiceFilter,
                        onCycleServiceFilter = cycleServiceFilter,
                        onToggleSort = onToggleSort,
                        onSearchQueryChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                InboxViewMode.WEB_MESSENGER -> {
                    ActiveChatWorkspacePane(
                        uiState = uiState,
                        onOpenFullChat = onOpenFullChat,
                        onSwitchService = onSwitchService,
                        onSwitchAccount = onSwitchAccount,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun InboxViewModeSwitchBar(
    currentMode: InboxViewMode,
    unreadCount: Int,
    totalMessages: Int,
    activeAccountName: String?,
    onSelectMode: (InboxViewMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x55000000))
            .border(1.dp, Color.White.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isListActive = currentMode == InboxViewMode.MESSAGES_LIST
        val listLabel = when {
            unreadCount > 0 -> "✉ Messages ($unreadCount new)"
            totalMessages > 0 -> "✉ Messages ($totalMessages)"
            else -> "✉ New Messages"
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.verticalGradient(
                        if (isListActive) listOf(Color(0xFF5FA836), Color(0xFF2E631F))
                        else listOf(Color(0xFF283A22), Color(0xFF172214))
                    )
                )
                .border(
                    width = if (isListActive) 1.5.dp else 1.dp,
                    color = if (isListActive) Color.White else Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { onSelectMode(InboxViewMode.MESSAGES_LIST) }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = listLabel,
                color = Color.White,
                fontWeight = if (isListActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        val isWebActive = currentMode == InboxViewMode.WEB_MESSENGER
        val webLabel = if (!activeAccountName.isNullOrBlank()) {
            "🌐 Web · $activeAccountName"
        } else {
            "🌐 Web Version"
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.verticalGradient(
                        if (isWebActive) listOf(Color(0xFF5FA836), Color(0xFF2E631F))
                        else listOf(Color(0xFF283A22), Color(0xFF172214))
                    )
                )
                .border(
                    width = if (isWebActive) 1.5.dp else 1.dp,
                    color = if (isWebActive) Color.White else Color.White.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable { onSelectMode(InboxViewMode.WEB_MESSENGER) }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = webLabel,
                color = Color.White,
                fontWeight = if (isWebActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun InboxMessageListPane(
    uiState: RelayUiState,
    addedServiceTypes: List<MessengerType>,
    onMessageClick: (MessageItem) -> Unit,
    onSelectServiceFilter: (MessengerType?) -> Unit = {},
    onCycleServiceFilter: () -> Unit = {},
    onToggleSort: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sortedBase = uiState.messages.sortedByDescending {
        if (it.timestampMillis > 0L) it.timestampMillis else System.currentTimeMillis()
    }
    val filtered = sortedBase
        .filter { msg ->
            (addedServiceTypes.isEmpty() || msg.serviceType in addedServiceTypes) &&
                (uiState.selectedServiceFilter == null || msg.serviceType == uiState.selectedServiceFilter) &&
                (uiState.searchQuery.isBlank() ||
                    msg.sender.contains(uiState.searchQuery, ignoreCase = true) ||
                    msg.snippet.contains(uiState.searchQuery, ignoreCase = true))
        }
        .let { list ->
            if (uiState.sortAscending) list.reversed() else list
        }

    Column(modifier = modifier) {
        // Only show the Service Filter Badge Strip if at least one messenger account is added
        if (addedServiceTypes.isNotEmpty()) {
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

                addedServiceTypes.forEach { svc ->
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
                    .padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (uiState.accounts.isEmpty()) {
                        "No messenger accounts added yet.\nGo to Accounts (+ Add) to connect your messengers."
                    } else {
                        "Waiting for incoming messages from your connected accounts…\nSwitch to '🌐 Web' above to view or sign into your live messenger."
                    },
                    color = Color.White.copy(alpha = 0.75f),
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
    onSwitchAccount: (String) -> Unit = {},
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
                    text = "No messenger accounts added yet.\nGo to the Accounts tab and tap '+ Add' to link your first messenger.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                RetainedWebView(
                    sessionId = activeAccount.id,
                    serviceName = activeAccount.accountName,
                    url = activeAccount.serviceType.defaultUrl,
                    displayUrl = activeAccount.serviceType.defaultUrl.removePrefix("https://"),
                    showHeader = true
                )
            }

            // Bottom switcher bar showing ONLY added accounts
            if (uiState.accounts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(Color(0x55000000))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.accounts.forEach { acc ->
                        val isSelected = acc.id == activeAccount.id
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (isSelected) Color(acc.serviceType.badgeColorHex).copy(alpha = 0.55f)
                                    else Color(0x44000000)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .clickable { onSwitchAccount(acc.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ServiceBadge(serviceType = acc.serviceType, size = 22)
                            Text(
                                text = acc.accountName,
                                color = Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
