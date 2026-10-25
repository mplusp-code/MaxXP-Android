package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ViewSidebar
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import com.relay.app.data.InboxViewMode
import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge
import com.relay.app.ui.theme.*

enum class InboxFilterChip(val label: String) {
    ALL("all"),
    UNREAD("unread"),
    MENTIONS("mentions"),
    PINNED("pinned")
}

enum class UnfoldedInboxViewMode(val label: String) {
    COMBINED("Split"),
    MESSAGES_ONLY("Messages"),
    WEB_ONLY("Web")
}

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
    var activeFilterChip by remember { mutableStateOf(InboxFilterChip.ALL) }
    var activeTabAccountId by remember { mutableStateOf(uiState.activeChatSessionId.ifBlank { uiState.accounts.firstOrNull()?.id ?: "wa_personal" }) }
    var unfoldedViewMode by rememberSaveable { mutableStateOf(UnfoldedInboxViewMode.COMBINED) }
    var isMessageListCollapsed by rememberSaveable { mutableStateOf(false) }

    val activeAccount = uiState.accounts.find { it.id == activeTabAccountId } ?: uiState.accounts.firstOrNull()

    if (isExpanded) {
        // Unfolded Tri-Mode Workspace (Combined / Messages Only / Web Only)
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            // Mode Switcher Header in Unfolded Mode (Messages | Split | Web)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "RELAY",
                        color = AccentLimeGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "inbox workspace",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3 View Modes Segmented Control
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UnfoldedInboxViewMode.entries.forEach { mode ->
                        val isSelected = mode == unfoldedViewMode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) AccentLimeGreen else Color.Transparent)
                                .clickable { unfoldedViewMode = mode }
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = mode.label,
                                color = if (isSelected) BackgroundDark else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Main Content Area based on unfoldedViewMode
            when (unfoldedViewMode) {
                UnfoldedInboxViewMode.MESSAGES_ONLY -> {
                    // 1. Full Messages (100% width)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        InboxMessageListPane(
                            uiState = uiState,
                            activeFilterChip = activeFilterChip,
                            onSelectFilterChip = { activeFilterChip = it },
                            onSelectServiceFilter = onSelectServiceFilter,
                            onMessageClick = { msg ->
                                activeTabAccountId = msg.accountId
                                onMessageClick(msg)
                            },
                            onSearchQueryChange = onSearchQueryChange
                        )
                    }
                }
                UnfoldedInboxViewMode.WEB_ONLY -> {
                    // 2. Full Web (100% width)
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        UnfoldedChatWorkspacePane(
                            uiState = uiState,
                            activeAccount = activeAccount,
                            onSelectTab = { accId ->
                                activeTabAccountId = accId
                                onSwitchAccount(accId)
                            },
                            onCloseTab = {
                                uiState.accounts.firstOrNull()?.id?.let { activeTabAccountId = it }
                            }
                        )
                    }
                }
                UnfoldedInboxViewMode.COMBINED -> {
                    // 3. Side-by-Side Workspace with Collapsible/Minimizable Sidebar
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        if (isMessageListCollapsed) {
                            // Slim collapsed rail showing avatars/badges
                            CollapsedInboxRail(
                                uiState = uiState,
                                selectedAccountId = activeTabAccountId,
                                onExpand = { isMessageListCollapsed = false },
                                onSelectAccount = { id ->
                                    activeTabAccountId = id
                                    onSwitchAccount(id)
                                }
                            )
                        } else {
                            // Left Pane: Chat List (with collapse button on boundary)
                            Box(
                                modifier = Modifier
                                    .weight(0.42f)
                                    .fillMaxHeight()
                                    .border(width = 1.dp, color = BorderDark)
                            ) {
                                InboxMessageListPane(
                                    uiState = uiState,
                                    activeFilterChip = activeFilterChip,
                                    onSelectFilterChip = { activeFilterChip = it },
                                    onSelectServiceFilter = onSelectServiceFilter,
                                    onMessageClick = { msg ->
                                        activeTabAccountId = msg.accountId
                                        onMessageClick(msg)
                                    },
                                    onSearchQueryChange = onSearchQueryChange,
                                    onCollapse = { isMessageListCollapsed = true }
                                )
                            }
                        }

                        // Right Pane: Active Chat Tabs + RetainedWebView
                        Box(
                            modifier = Modifier
                                .weight(if (isMessageListCollapsed) 1f else 0.58f)
                                .fillMaxHeight()
                        ) {
                            UnfoldedChatWorkspacePane(
                                uiState = uiState,
                                activeAccount = activeAccount,
                                onSelectTab = { accId ->
                                    activeTabAccountId = accId
                                    onSwitchAccount(accId)
                                },
                                onCloseTab = {
                                    uiState.accounts.firstOrNull()?.id?.let { activeTabAccountId = it }
                                }
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Portrait Layout (Concept v2 Page 2)
        Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
            // Top Mode Switcher if in web messenger mode, or direct list
            if (uiState.inboxViewMode == InboxViewMode.WEB_MESSENGER && activeAccount != null) {
                // View Mode switcher header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "← Back to Messages",
                        color = AccentLimeGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSelectInboxViewMode(InboxViewMode.MESSAGES_LIST) }
                    )

                    Text(
                        text = "Live · ${activeAccount.accountName}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    RetainedWebView(
                        sessionId = activeAccount.id,
                        serviceName = activeAccount.serviceType.displayName,
                        url = activeAccount.serviceType.defaultUrl,
                        displayUrl = activeAccount.serviceType.defaultUrl.removePrefix("https://")
                    )
                }
            } else {
                InboxMessageListPane(
                    uiState = uiState,
                    activeFilterChip = activeFilterChip,
                    onSelectFilterChip = { activeFilterChip = it },
                    onSelectServiceFilter = onSelectServiceFilter,
                    onMessageClick = { msg ->
                        onMessageClick(msg)
                        onOpenFullChat(msg.accountId)
                    },
                    onSearchQueryChange = onSearchQueryChange
                )
            }
        }
    }
}

@Composable
fun InboxMessageListPane(
    uiState: RelayUiState,
    activeFilterChip: InboxFilterChip,
    onSelectFilterChip: (InboxFilterChip) -> Unit,
    onSelectServiceFilter: (MessengerType?) -> Unit,
    onMessageClick: (MessageItem) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onCollapse: (() -> Unit)? = null
) {
    var searchVisible by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf(uiState.searchQuery) }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Top Header: RELAY, inbox, search & edit icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    text = "inbox",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onCollapse != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                            .border(1.dp, BorderDark, CircleShape)
                            .clickable { onCollapse() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "◀", color = TextSecondary, fontSize = 12.sp)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardDark)
                        .border(1.dp, BorderDark, CircleShape)
                        .clickable { searchVisible = !searchVisible },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (searchVisible) AccentLimeGreen else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardDark)
                        .border(1.dp, BorderDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Compose",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Inline Search field when expanded
        if (searchVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        onSearchQueryChange(it)
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(AccentLimeGreen),
                    decorationBox = { inner ->
                        if (searchInput.isEmpty()) {
                            Text("Search messages or senders...", color = TextMuted, fontSize = 13.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Filter row: all, unread, mentions, pinned
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InboxFilterChip.entries.forEach { chip ->
                val isSelected = chip == activeFilterChip
                Column(
                    modifier = Modifier.clickable { onSelectFilterChip(chip) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = chip.label,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    Spacer(Modifier.height(4.dp))

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(2.dp)
                                .clip(RoundedCornerShape(1.dp))
                                .background(AccentLimeGreen)
                        )
                    } else {
                        Box(Modifier.height(2.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Service Carousel Filter Strip
        ServiceCarouselStrip(
            uiState = uiState,
            selectedService = uiState.selectedServiceFilter,
            onSelectServiceFilter = onSelectServiceFilter
        )

        Spacer(Modifier.height(8.dp))

        // Timeline Message Items (Grouped by TODAY, YESTERDAY, EARLIER)
        val filteredMessages = remember(uiState.messages, uiState.selectedServiceFilter, activeFilterChip, searchInput) {
            uiState.messages.filter { msg ->
                val matchesService = uiState.selectedServiceFilter == null || msg.serviceType == uiState.selectedServiceFilter
                val matchesChip = when (activeFilterChip) {
                    InboxFilterChip.ALL -> true
                    InboxFilterChip.UNREAD -> msg.isUnread
                    InboxFilterChip.MENTIONS -> msg.snippet.contains("@")
                    InboxFilterChip.PINNED -> false
                }
                val matchesSearch = searchInput.isBlank() ||
                    msg.sender.contains(searchInput, ignoreCase = true) ||
                    msg.snippet.contains(searchInput, ignoreCase = true)
                matchesService && matchesChip && matchesSearch
            }.sortedByDescending { it.timestampMillis }
        }

        if (filteredMessages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No messages in this filter", color = TextSecondary, fontSize = 13.sp)
            }
        } else {
            // Group messages into Today / Yesterday / Earlier
            val now = remember { System.currentTimeMillis() }
            val oneDayMillis = 24 * 60 * 60 * 1000L

            val grouped = remember(filteredMessages) {
                filteredMessages.groupBy { msg ->
                    val diff = now - msg.timestampMillis
                    when {
                        msg.timestampMillis <= 0L -> "TODAY"
                        diff < oneDayMillis -> "TODAY"
                        diff < 2 * oneDayMillis -> "YESTERDAY"
                        else -> "EARLIER"
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                grouped.forEach { (dateHeader, msgs) ->
                    item {
                        Text(
                            text = dateHeader,
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    items(msgs, key = { it.id }) { msg ->
                        InboxMessageItemCard(
                            message = msg,
                            onClick = { onMessageClick(msg) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceCarouselStrip(
    uiState: RelayUiState,
    selectedService: MessengerType?,
    onSelectServiceFilter: (MessengerType?) -> Unit
) {
    val addedServices = remember(uiState.accounts) {
        uiState.accounts.map { it.serviceType }.distinct()
    }

    if (addedServices.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "All services" pill only if multiple services are added
        if (addedServices.size > 1) {
            val isAllSelected = selectedService == null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isAllSelected) AccentLimeGreen else CardDark)
                    .border(1.dp, if (isAllSelected) AccentLimeGreen else BorderDark, RoundedCornerShape(16.dp))
                    .clickable { onSelectServiceFilter(null) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "All services",
                    color = if (isAllSelected) BackgroundDark else TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Service badges with live unread count pills ONLY for added services
        addedServices.forEach { service ->
            val isSelected = selectedService == service
            val count = uiState.messages.count { it.serviceType == service && it.isUnread }.takeIf { it > 0 }
                ?: uiState.services.find { it.type == service }?.unreadCount ?: 0

            val dotColor = Color(service.badgeColorHex)

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) dotColor.copy(alpha = 0.25f) else CardDark)
                    .border(1.dp, if (isSelected) dotColor else BorderDark, RoundedCornerShape(16.dp))
                    .clickable {
                        onSelectServiceFilter(if (isSelected) null else service)
                    }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceBadge(serviceType = service, size = 16)
                    Text(
                        text = if (count > 0) "${service.displayName} · $count" else service.displayName,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun InboxMessageItemCard(
    message: MessageItem,
    onClick: () -> Unit
) {
    var showSwipeActions by remember { mutableStateOf(false) }

    val initials = remember(message.sender) {
        message.sender.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { message.sender.take(2).uppercase() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar circle with service badge overlay
                Box(modifier = Modifier.size(42.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CardElevated)
                            .border(1.dp, BorderDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                    ) {
                        ServiceBadge(serviceType = message.serviceType, size = 16)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.sender,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(2.dp))

                    val isFailed = message.snippet.contains("failed", ignoreCase = true)
                    Text(
                        text = if (isFailed) "▲ Not sent · tap to retry" else message.snippet,
                        color = if (isFailed) AmberAttention else TextSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right side: Timestamp and green unread count badge circle
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message.timestamp,
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                if (message.isUnread) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(AccentLimeGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "1",
                            color = BackgroundDark,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UnfoldedChatWorkspacePane(
    uiState: RelayUiState,
    activeAccount: com.relay.app.data.AccountItem?,
    onSelectTab: (String) -> Unit,
    onCloseTab: () -> Unit
) {
    if (activeAccount == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundDark),
            contentAlignment = Alignment.Center
        ) {
            Text("No active account session", color = TextSecondary, fontSize = 14.sp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Active Chat Tabs across top: T Dev Team ✕, W Marta K., W Mom, +
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(SurfaceDark)
                .border(1.dp, BorderDark)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            uiState.accounts.forEach { acc ->
                val isSelected = acc.id == activeAccount.id
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) CardElevated else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AccentLimeGreen.copy(alpha = 0.5f) else BorderDark,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectTab(acc.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ServiceBadge(serviceType = acc.serviceType, size = 16)
                    Text(
                        text = acc.accountName,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { onCloseTab() }
                        )
                    }
                }
            }
        }

        // Sub-header status bar: lock icon, URL, • Live, reload icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .background(CardDark)
                .border(1.dp, BorderDark)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = activeAccount.serviceType.defaultUrl.removePrefix("https://"),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(AccentGreen))
                    Text(text = "Live", color = AccentGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // RetainedWebView hosting active chat
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            RetainedWebView(
                sessionId = activeAccount.id,
                serviceName = activeAccount.serviceType.displayName,
                url = activeAccount.serviceType.defaultUrl,
                displayUrl = activeAccount.serviceType.defaultUrl.removePrefix("https://")
            )
        }

        // Bottom Input bar: Write a message + green send button
        var bottomMessageInput by remember { mutableStateOf("") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .border(1.dp, BorderDark)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(BackgroundDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(19.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = bottomMessageInput,
                    onValueChange = { bottomMessageInput = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(AccentLimeGreen),
                    decorationBox = { inner ->
                        if (bottomMessageInput.isEmpty()) {
                            Text("Write a message", color = TextMuted, fontSize = 13.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(AccentLimeGreen)
                    .clickable(enabled = bottomMessageInput.isNotBlank()) {
                        bottomMessageInput = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = BackgroundDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CollapsedInboxRail(
    uiState: RelayUiState,
    selectedAccountId: String,
    onExpand: () -> Unit,
    onSelectAccount: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .fillMaxHeight()
            .background(SurfaceDark)
            .border(1.dp, BorderDark)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CardElevated)
                .border(1.dp, BorderDark, CircleShape)
                .clickable { onExpand() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = "▶", color = AccentLimeGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(uiState.accounts, key = { it.id }) { acc ->
                val isSelected = acc.id == selectedAccountId
                val initials = acc.accountName.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first().uppercase() }
                    .joinToString("")
                    .ifBlank { acc.accountName.take(2).uppercase() }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) CardElevated else CardDark)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) AccentLimeGreen else BorderDark,
                            shape = CircleShape
                        )
                        .clickable { onSelectAccount(acc.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = if (isSelected) AccentLimeGreen else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                    ) {
                        ServiceBadge(serviceType = acc.serviceType, size = 12)
                    }
                    if (acc.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentLimeGreen)
                        )
                    }
                }
            }
        }
    }
}
