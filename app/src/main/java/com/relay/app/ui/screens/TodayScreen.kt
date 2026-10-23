package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.AccountItem
import com.relay.app.data.MessageItem
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayTab
import com.relay.app.data.RelayUiState
import com.relay.app.matrix.QuickReplyStatus
import com.relay.app.ui.components.RelaySectionHeader
import com.relay.app.ui.components.ServiceBadge
import com.relay.app.ui.theme.RelayColors

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
    if (isExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            TodaySummaryPane(
                uiState = uiState,
                onServiceClick = onServiceClick,
                onNavigateTab = onNavigateTab,
                onRelinkAccount = onRelinkAccount,
                onSearchQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
                modifier = Modifier
                    .weight(0.46f)
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

            TodayQuickRepliesPane(
                uiState = uiState,
                onMessageClick = onMessageClick,
                onSendQuickReply = onSendQuickReply,
                onRelinkAccount = onRelinkAccount,
                onAddAccountClick = onAddAccountClick,
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight()
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TodaySummaryPane(
                uiState = uiState,
                onServiceClick = onServiceClick,
                onNavigateTab = onNavigateTab,
                onRelinkAccount = onRelinkAccount,
                onSearchQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
                modifier = Modifier.fillMaxWidth(),
                isScrollable = false
            )

            TodayQuickRepliesPane(
                uiState = uiState,
                onMessageClick = onMessageClick,
                onSendQuickReply = onSendQuickReply,
                onRelinkAccount = onRelinkAccount,
                onAddAccountClick = onAddAccountClick,
                modifier = Modifier.fillMaxWidth(),
                isScrollable = false
            )
        }
    }
}

@Composable
fun TodaySummaryPane(
    uiState: RelayUiState,
    onServiceClick: (MessengerType) -> Unit,
    onNavigateTab: (RelayTab) -> Unit,
    onRelinkAccount: (accountId: String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true
) {
    val scrollMod = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier
    val totalUnread = uiState.services.sumOf { it.unreadCount }
        .takeIf { it > 0 } ?: uiState.messages.count { it.isUnread }
    val attentionAccounts = uiState.accounts.filter { it.isNeedsAttention }

    Column(
        modifier = modifier
            .then(scrollMod)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Friday, Oct 23, 2026",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "14:45",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.2f)))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTab(RelayTab.INBOX) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MarkEmailUnread,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$totalUnread unread messages",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x55000000))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (uiState.searchQuery.isEmpty()) {
                    Text(
                        text = "Search chats",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                    cursorBrush = SolidColor(Color(0xFF8BC34A)),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4C8C2B), Color(0xFF285217))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .clickable { onSearchSubmit() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "↵",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        RelaySectionHeader(title = "Messengers")

        val addedServices = uiState.services.filter { it.accountCount > 0 }
        Column(modifier = Modifier.fillMaxWidth()) {
            if (addedServices.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No messenger apps added yet. Add an account in Accounts to see it here.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            } else {
                addedServices.forEach { summary ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onServiceClick(summary.type) }
                            .drawBehind {
                                drawLine(
                                    color = Color.White.copy(alpha = 0.15f),
                                    start = Offset(0f, size.height),
                                    end = Offset(size.width, size.height),
                                    strokeWidth = 1f
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ServiceBadge(serviceType = summary.type, size = 32)
                            val accountLabel = when {
                                summary.accountCount > 1 -> "${summary.type.displayName} (${summary.accountCount} accts)"
                                summary.accountCount == 1 -> "${summary.type.displayName} (1 acct)"
                                else -> summary.type.displayName
                            }
                            Text(
                                text = accountLabel,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = if (summary.unreadCount > 0) "${summary.unreadCount} new" else "0",
                            color = if (summary.unreadCount > 0) Color.White else Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        val topMessage = uiState.messages.firstOrNull()
        if (topMessage != null) {
            RelaySectionHeader(title = "Latest message")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateTab(RelayTab.INBOX) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${topMessage.sender} (${topMessage.serviceType.displayName})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "\"${topMessage.snippet}\"",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = topMessage.timestamp,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 13.sp
                )
            }
        }

        if (attentionAccounts.isNotEmpty()) {
            val targetAcc = attentionAccounts.first()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRelinkAccount(targetAcc.id) }
                    .background(Color(0x33000000))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = RelayColors.StatusWarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (attentionAccounts.size == 1) {
                        "1 session needs login: ${targetAcc.accountName}"
                    } else {
                        "${attentionAccounts.size} sessions need login: ${targetAcc.accountName}…"
                    },
                    color = RelayColors.StatusWarningAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun TodayQuickRepliesPane(
    uiState: RelayUiState,
    onMessageClick: (MessageItem) -> Unit,
    onSendQuickReply: (roomId: String, text: String) -> Unit,
    onRelinkAccount: (accountId: String) -> Unit,
    onAddAccountClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    isScrollable: Boolean = true
) {
    val scrollMod = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Column(
        modifier = modifier
            .then(scrollMod)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RelaySectionHeader(title = "Latest · reply without opening")

        if (uiState.messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33000000))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "No incoming messages yet. Link an account in Accounts or connect Matrix sync to receive live chats.",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 13.sp
                )
            }
        } else {
            uiState.messages.take(3).forEach { msg ->
                QuickReplyCard(
                    msg = msg,
                    replyStatus = uiState.quickReplyStates[msg.roomId] ?: QuickReplyStatus.IDLE,
                    onSendQuickReply = onSendQuickReply,
                    onOpenChat = { onMessageClick(msg) }
                )
            }
        }

        RelaySectionHeader(title = "Sessions")

        if (uiState.accounts.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF3E6A2A), Color(0xFF213D15))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .clickable { onAddAccountClick() }
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
                    text = "Add your first messenger account…",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        } else {
            val sessionCards: List<AccountItem> = uiState.accounts
                .filter { it.isNeedsAttention }
                .ifEmpty { uiState.accounts.take(3) }

            sessionCards.forEach { acc ->
                SessionHealthCard(
                    accountName = acc.accountName,
                    statusText = acc.statusText,
                    actionLabel = if (acc.isNeedsAttention) "Relink" else "Open",
                    onActionClick = { onRelinkAccount(acc.id) }
                )
            }
        }
    }
}

@Composable
fun QuickReplyCard(
    msg: MessageItem,
    replyStatus: QuickReplyStatus = QuickReplyStatus.IDLE,
    onSendQuickReply: (roomId: String, text: String) -> Unit = { _, _ -> },
    onOpenChat: (MessageItem) -> Unit = {}
) {
    var isEditing by remember(msg.id) { mutableStateOf(false) }
    var replyText by remember(msg.id) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenChat(msg) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ServiceBadge(serviceType = msg.serviceType, size = 26)
                Text(
                    text = msg.sender,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Text(
                text = msg.timestamp,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp
            )
        }

        Text(
            text = msg.snippet,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 13.sp,
            modifier = Modifier.padding(start = 34.dp, top = 2.dp, bottom = 6.dp)
        )

        if (!isEditing && replyStatus != QuickReplyStatus.SENDING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0x44000000))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                    .clickable { isEditing = true }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (replyStatus == QuickReplyStatus.SENT) "✓ Sent via Matrix · Tap to reply again"
                    else "Reply to ${msg.sender}",
                    color = if (replyStatus == QuickReplyStatus.SENT) Color(0xFF8BC34A)
                    else Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
                Text(
                    text = "▶",
                    color = Color(0xFF8BC34A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0x77000000))
                        .border(1.dp, Color(0xFF8BC34A).copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (replyText.isEmpty()) {
                        Text(
                            text = "Type quick reply…",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        singleLine = true,
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                        cursorBrush = SolidColor(Color(0xFF8BC34A)),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val canSend = replyText.isNotBlank() && replyStatus != QuickReplyStatus.SENDING
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                if (canSend) listOf(Color(0xFF5FA836), Color(0xFF2E631F))
                                else listOf(Color(0xFF3B4A34), Color(0xFF1E281A))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                        .clickable(enabled = canSend) {
                            onSendQuickReply(msg.roomId, replyText.trim())
                            replyText = ""
                            isEditing = false
                        }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (replyStatus == QuickReplyStatus.SENDING) "…" else "Send ▶",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SessionHealthCard(
    accountName: String,
    statusText: String,
    actionLabel: String,
    onActionClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = RelayColors.StatusWarningAmber,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    text = accountName,
                    color = RelayColors.StatusWarningAmber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = statusText,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF4C8C2B), Color(0xFF254D17))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .clickable { onActionClick() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = actionLabel,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
