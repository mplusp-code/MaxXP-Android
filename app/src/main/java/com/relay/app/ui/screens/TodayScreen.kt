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
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
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
import com.relay.app.data.AccountItem
import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayTab
import com.relay.app.data.RelayUiState
import com.relay.app.matrix.QuickReplyStatus
import com.relay.app.ui.components.ServiceBadge
import com.relay.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    onServiceClick: (MessengerType) -> Unit,
    onMessageClick: (MessageItem) -> Unit,
    onNavigateTab: (RelayTab) -> Unit,
    onSendQuickReply: (roomId: String, text: String) -> Unit = { _, _ -> },
    onRelinkAccount: (accountId: String) -> Unit = {},
    onAddAccountClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchSubmit: () -> Unit = {}
) {
    val attentionAccounts = uiState.accounts.filter { it.isNeedsAttention || !it.isConnected }
    val totalUnread = uiState.messages.count { it.isUnread }.takeIf { it > 0 }
        ?: uiState.services.sumOf { it.unreadCount }

    if (isExpanded) {
        // Unfolded Dual-Column Layout (Concept v2 Page 6)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Unfolded Header: RELAY, today, date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        text = "today",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val formattedDate = remember {
                        SimpleDateFormat("EEEE d MMMM", Locale.getDefault()).format(Date())
                    }
                    Text(
                        text = "$totalUnread unread · $formattedDate",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Attention Banner
            if (attentionAccounts.isNotEmpty()) {
                AttentionBannerCard(
                    accounts = attentionAccounts,
                    onFixClick = { onNavigateTab(RelayTab.ACCOUNTS) }
                )
                Spacer(Modifier.height(14.dp))
            }

            // Dual columns: Left column (Services & Sessions), Right column (Search, New message & Quick Reply)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left Column (50%): SERVICES grid + SESSIONS health
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "SERVICES",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    ServicesGrid2Col(
                        uiState = uiState,
                        onServiceClick = onServiceClick
                    )

                    Text(
                        text = "SESSIONS",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    SessionsHealthCard(
                        accounts = uiState.accounts,
                        onRelinkAccount = onRelinkAccount
                    )
                }

                // Right Column (50%): Search all chats + New message + QUICK REPLY feed
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Right column top action bar: Search all chats + New message
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var searchInput by remember { mutableStateOf(uiState.searchQuery) }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardDark)
                                .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                BasicTextField(
                                    value = searchInput,
                                    onValueChange = {
                                        searchInput = it
                                        onSearchQueryChange(it)
                                    },
                                    singleLine = true,
                                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                                    cursorBrush = SolidColor(AccentLimeGreen),
                                    decorationBox = { innerTextField ->
                                        if (searchInput.isEmpty()) {
                                            Text("Search all chats", color = TextMuted, fontSize = 13.sp)
                                        }
                                        innerTextField()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // New message button
                        Box(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(AccentLimeGreen)
                                .clickable { onNavigateTab(RelayTab.INBOX) }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Create,
                                    contentDescription = null,
                                    tint = BackgroundDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "New message",
                                    color = BackgroundDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "QUICK REPLY",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    val recentMessages = uiState.messages.take(6)
                    if (recentMessages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardDark)
                                .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No unread messages right now", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        recentMessages.forEach { msg ->
                            QuickReplyMessageCard(
                                message = msg,
                                quickReplyStatus = uiState.quickReplyStates[msg.roomId],
                                onSendQuickReply = onSendQuickReply,
                                onMessageClick = { onMessageClick(msg) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Portrait Cover Screen Layout (Concept v2 Page 1)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: RELAY, today, 29 unread pill, Personal dropdown, top-right icons
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
                        text = "today",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "$totalUnread unread",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                            .border(1.dp, BorderDark, CircleShape)
                            .clickable { onNavigateTab(RelayTab.INBOX) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                            .border(1.dp, BorderDark, CircleShape)
                            .clickable { onNavigateTab(RelayTab.INBOX) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "New Message",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Attention Banner Card (if sessions need attention)
            if (attentionAccounts.isNotEmpty()) {
                AttentionBannerCard(
                    accounts = attentionAccounts,
                    onFixClick = { onNavigateTab(RelayTab.ACCOUNTS) }
                )
            }

            // Services Grid (2 Columns, 6 Cards)
            ServicesGrid2Col(
                uiState = uiState,
                onServiceClick = onServiceClick
            )

            // LATEST section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LATEST",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "See all",
                    color = AccentLimeGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onNavigateTab(RelayTab.INBOX) }
                )
            }

            val recentMessages = uiState.messages.take(5)
            if (recentMessages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages yet", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentMessages.forEach { msg ->
                        LatestMessageItemRow(
                            message = msg,
                            onClick = { onMessageClick(msg) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun AttentionBannerCard(
    accounts: List<AccountItem>,
    onFixClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(AmberAttentionBg)
            .border(1.dp, AmberAttentionBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = AmberAttention,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = "${accounts.size} sessions need attention",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = accounts.joinToString(" · ") { it.accountName },
                        color = AmberAttentionText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(AmberAttention)
                    .clickable { onFixClick() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Fix",
                    color = BackgroundDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ServicesGrid2Col(
    uiState: RelayUiState,
    onServiceClick: (MessengerType) -> Unit
) {
    val allServices = MessengerType.entries

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (i in allServices.indices step 2) {
            val s1 = allServices[i]
            val s2 = allServices.getOrNull(i + 1)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.weight(1f)) {
                    ServiceGridCard(
                        service = s1,
                        uiState = uiState,
                        onClick = { onServiceClick(s1) }
                    )
                }

                if (s2 != null) {
                    Box(Modifier.weight(1f)) {
                        ServiceGridCard(
                            service = s2,
                            uiState = uiState,
                            onClick = { onServiceClick(s2) }
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ServiceGridCard(
    service: MessengerType,
    uiState: RelayUiState,
    onClick: () -> Unit
) {
    val unreadCount = uiState.messages.count { it.serviceType == service && it.isUnread }.takeIf { it > 0 }
        ?: uiState.services.find { it.type == service }?.unreadCount ?: 0

    val latestMessage = uiState.messages.firstOrNull { it.serviceType == service }
    val previewText = when {
        latestMessage != null -> "${latestMessage.sender.take(12)}: ${latestMessage.snippet}"
        service == MessengerType.WHATSAPP -> "Marta: sent 12 photos"
        service == MessengerType.TELEGRAM -> "Kuba: deploy finished"
        service == MessengerType.INSTAGRAM -> "ola.photo: sent a reel"
        service == MessengerType.FACEBOOK -> "Piotr: your reply is queued"
        service == MessengerType.VIBER -> "Session expires in 2 days"
        service == MessengerType.GOOGLE_MESSAGES -> "Your pickup code is ready"
        else -> "No messages"
    }

    val cardBg = when (service) {
        MessengerType.WHATSAPP -> WhatsAppCardBg
        MessengerType.TELEGRAM -> TelegramCardBg
        MessengerType.INSTAGRAM -> InstagramCardBg
        MessengerType.FACEBOOK -> FacebookCardBg
        MessengerType.VIBER -> ViberCardBg
        MessengerType.GOOGLE_MESSAGES -> GoogleCardBg
    }

    val hasWarning = uiState.accounts.any { it.serviceType == service && (it.isNeedsAttention || !it.isConnected) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = unreadCount.toString(),
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                if (hasWarning) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "Attention",
                        tint = AmberAttention,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column {
                Text(
                    text = service.displayName,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = previewText,
                    color = TextPrimary.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LatestMessageItemRow(
    message: MessageItem,
    onClick: () -> Unit
) {
    val initials = remember(message.sender) {
        message.sender.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { message.sender.take(2).uppercase() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Circle avatar with initials and bottom-right service badge overlay
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.sender,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = message.timestamp,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = message.snippet,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.Reply,
            contentDescription = "Reply",
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun QuickReplyMessageCard(
    message: MessageItem,
    quickReplyStatus: QuickReplyStatus?,
    onSendQuickReply: (roomId: String, text: String) -> Unit,
    onMessageClick: () -> Unit
) {
    var replyText by remember { mutableStateOf("") }

    val initials = remember(message.sender) {
        message.sender.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifBlank { message.sender.take(2).uppercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onMessageClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.size(38.dp)) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(CardElevated)
                        .border(1.dp, BorderDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                ) {
                    ServiceBadge(serviceType = message.serviceType, size = 15)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.sender,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.snippet,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Direct inline reply input field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(BackgroundDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
                    cursorBrush = SolidColor(AccentLimeGreen),
                    decorationBox = { inner ->
                        if (replyText.isEmpty()) {
                            Text("Reply to ${message.sender}", color = TextMuted, fontSize = 12.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val isSending = quickReplyStatus == QuickReplyStatus.SENDING
            val isSent = quickReplyStatus == QuickReplyStatus.SENT

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSent) AccentGreen else AccentLimeGreen)
                    .clickable(enabled = replyText.isNotBlank() && !isSending) {
                        onSendQuickReply(message.roomId, replyText)
                        replyText = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isSent) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sent",
                        tint = BackgroundDark,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = "Send",
                        tint = BackgroundDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SessionsHealthCard(
    accounts: List<AccountItem>,
    onRelinkAccount: (String) -> Unit
) {
    val attention = accounts.filter { it.isNeedsAttention || !it.isConnected }
    val connectedCount = accounts.count { it.isConnected }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        attention.forEach { acc ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AmberAttentionBg)
                    .border(1.dp, AmberAttentionBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ServiceBadge(serviceType = acc.serviceType, size = 24)
                    Column {
                        Text(
                            text = acc.accountName,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "▲ Session expired",
                            color = AmberAttentionText,
                            fontSize = 11.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(AmberAttention)
                        .clickable { onRelinkAccount(acc.id) }
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Relink", color = BackgroundDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Live accounts summary card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardDark)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "$connectedCount accounts live · Last sync just now",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
