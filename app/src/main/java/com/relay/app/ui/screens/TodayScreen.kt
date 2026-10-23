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
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.MessageItem
import com.relay.app.data.RelayUiState
import com.relay.app.data.ServiceSummary
import com.relay.app.ui.components.RelaySectionHeader
import com.relay.app.ui.components.ServiceBadge

@Composable
fun TodayScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
    onServiceClick: (ServiceSummary) -> Unit = {}
) {
    if (isExpanded) {
        // Unfolded 2-Pane Layout (PDF Page 6)
        Row(modifier = modifier.fillMaxSize()) {
            // Left Pane (Today summary & services)
            TodaySummaryPane(
                uiState = uiState,
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight(),
                onServiceClick = onServiceClick
            )

            // Vertical divider
            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.2f))
            )

            // Right Pane (Quick reply & session health)
            TodayQuickRepliesPane(
                uiState = uiState,
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
            )
        }
    } else {
        // Portrait 1-Pane Layout (PDF Page 1)
        TodaySummaryPane(
            uiState = uiState,
            modifier = modifier.fillMaxSize(),
            onServiceClick = onServiceClick
        )
    }
}

@Composable
fun TodaySummaryPane(
    uiState: RelayUiState,
    modifier: Modifier = Modifier,
    onServiceClick: (ServiceSummary) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Date & Clock Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color(0xFF8BC34A).copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Clock",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(
                    text = "Thursday",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "22 October 2026",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
            }
        }

        // Unread messages banner
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFA000))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✉",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Text(
                text = "29 unread messages",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        // Service listing
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(uiState.services) { service ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0x22000000))
                        .drawBehind {
                            drawLine(
                                color = Color.White.copy(alpha = 0.12f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1f
                            )
                        }
                        .clickable { onServiceClick(service) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ServiceBadge(serviceType = service.type, size = 32)
                        Text(
                            text = service.type.displayName,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = if (service.unreadCount > 0) "${service.unreadCount}" else "none",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            item {
                // Add service row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
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
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Tap here to add a service",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp
                    )
                }
            }
        }

        // Bottom Search input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x44000000))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Search chats",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Session warning banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFB5651D).copy(alpha = 0.35f))
                .border(1.dp, Color(0xFFFFA000).copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp),
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
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = "1 session needs login",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun TodayQuickRepliesPane(
    uiState: RelayUiState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Search all chats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0x44000000))
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Search all chats",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        RelaySectionHeader(title = "Latest · reply without opening")

        // 3 Most recent message cards
        val recentThree = uiState.messages.take(3)
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recentThree.forEach { msg ->
                QuickReplyCard(msg = msg)
            }
        }

        Spacer(Modifier.height(10.dp))

        RelaySectionHeader(title = "Sessions")

        // Account status cards
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SessionHealthCard(
                account = "WhatsApp · Work",
                status = "Scan QR to relink",
                actionLabel = "Relink",
                badgeType = com.relay.app.data.MessengerType.WHATSAPP
            )
            SessionHealthCard(
                account = "Viber · Main",
                status = "Expires in 2 days",
                actionLabel = "Refresh",
                badgeType = com.relay.app.data.MessengerType.VIBER
            )
        }
    }
}

@Composable
fun QuickReplyCard(msg: MessageItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

        // Inline reply input button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0x44000000))
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(3.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Reply to ${msg.sender}",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp
            )
            Text(
                text = "▶",
                color = Color(0xFF8BC34A),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SessionHealthCard(
    account: String,
    status: String,
    actionLabel: String,
    badgeType: com.relay.app.data.MessengerType
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x33000000))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ServiceBadge(serviceType = badgeType, size = 30)
            Column {
                Text(
                    text = account,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = status,
                    color = Color(0xFFFFA000),
                    fontSize = 12.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF4C8C2B), Color(0xFF2E631F))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                .clickable { }
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
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
