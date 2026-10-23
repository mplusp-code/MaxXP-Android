package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.MessengerType
import com.relay.app.data.RelayUiState
import com.relay.app.ui.components.RelayWebView
import com.relay.app.ui.components.ServiceBadge

@Composable
fun ChatWebViewScreen(
    serviceType: MessengerType,
    accountName: String,
    url: String,
    displayUrl: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            RelayWebView(
                sessionId = "single_chat_${serviceType.name}",
                url = url,
                displayUrl = displayUrl
            )
        }

        // Bottom horizontal service switcher bar (PDF Page 3)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(Color(0xFF1E2E1B))
                .border(1.dp, Color(0xFF38572A))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Selected badge with label pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.5f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ServiceBadge(serviceType = MessengerType.WHATSAPP, size = 26)
                Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            ServiceBadge(serviceType = MessengerType.TELEGRAM, size = 30)
            ServiceBadge(serviceType = MessengerType.INSTAGRAM, size = 30)
            ServiceBadge(serviceType = MessengerType.FACEBOOK, size = 30)
            ServiceBadge(serviceType = MessengerType.VIBER, size = 30)
            ServiceBadge(serviceType = MessengerType.GOOGLE_MESSAGES, size = 30)
        }
    }
}

@Composable
fun SplitViewScreen(
    uiState: RelayUiState,
    onSwapPanes: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dual-Chat Split View (PDF Page 8)
    Row(modifier = modifier.fillMaxSize()) {
        // Left Chat Pane (50%)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            SplitPaneHeader(
                serviceType = MessengerType.WHATSAPP,
                title = "Marta K.",
                subtitle = "WhatsApp · Personal · web.whatsapp.com",
                onSwap = onSwapPanes,
                onClose = {}
            )

            Box(modifier = Modifier.weight(1f)) {
                RelayWebView(
                    sessionId = uiState.splitLeftSessionId,
                    url = "https://web.whatsapp.com",
                    displayUrl = "web.whatsapp.com"
                )
            }
        }

        // Center Vertical Split Divider
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Color(0xFF38572A))
        )

        // Right Chat Pane (50%)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            SplitPaneHeader(
                serviceType = MessengerType.TELEGRAM,
                title = "Dev Team",
                subtitle = "Telegram · Main · web.telegram.org",
                onSwap = onSwapPanes,
                onClose = {}
            )

            Box(modifier = Modifier.weight(1f)) {
                RelayWebView(
                    sessionId = uiState.splitRightSessionId,
                    url = "https://web.telegram.org",
                    displayUrl = "web.telegram.org"
                )
            }
        }
    }
}

@Composable
fun SplitPaneHeader(
    serviceType: MessengerType,
    title: String,
    subtitle: String,
    onSwap: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF22361E), Color(0xFF132010))
                )
            )
            .border(1.dp, Color(0xFF38572A))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            ServiceBadge(serviceType = serviceType, size = 26)
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Swap",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onSwap() }
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClose() }
            )
        }
    }
}
