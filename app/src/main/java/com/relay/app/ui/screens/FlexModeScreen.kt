package com.relay.app.ui.screens

import android.graphics.Rect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.RelayUiState
import com.relay.app.ui.components.RetainedWebView
import com.relay.app.ui.components.ServiceBadge
import com.relay.app.ui.theme.*

/**
 * Half-Folded Flex Mode Workspace (Tabletop 90° Posture, PDF Page 10):
 * - Upper Half (Above Hinge): Renders the active chat conversation WebView and incoming message banner.
 * - Hinge Separator: Exact height matching physical hinge crease so zero UI elements are obscured.
 * - Lower Half (Below Hinge): Renders the tactile PDA keyboard input deck, quick-response buttons,
 *   and added-account switcher tabs.
 */
@Composable
fun FlexModeWorkspace(
    uiState: RelayUiState,
    hingeBoundsPx: Rect?,
    onSendQuickReply: (roomId: String, text: String) -> Unit,
    onSwitchAccount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val activeAccount = uiState.accounts.find { it.id == uiState.activeChatSessionId }
        ?: uiState.accounts.firstOrNull()
    val latestMessage = uiState.messages.firstOrNull { it.accountId == activeAccount?.id }
        ?: uiState.messages.firstOrNull()
    var replyDraft by remember { mutableStateOf("") }

    val hingeHeightDp = hingeBoundsPx?.let {
        with(density) { it.height().toDp().coerceAtLeast(8.dp) }
    } ?: 8.dp

    Column(modifier = modifier.fillMaxSize()) {
        // UPPER HALF (Above the Physical Hinge Crease): Active Chat Conversation & Preview
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
        ) {
            if (activeAccount == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add a messenger account in Accounts to use Tabletop Flex Mode.",
                        color = Color.White,
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
            }
        }

        // PHYSICAL HINGE CREASE SEPARATOR
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(hingeHeightDp)
                .background(BorderDark)
        )

        // LOWER HALF (Below the Physical Hinge Crease): Keyboard Input Area & Quick Action Controls
        Column(
            modifier = Modifier
                .weight(0.5f)
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (latestMessage != null) {
                        "FLEX DECK · Reply to ${latestMessage.sender}: \"${latestMessage.snippet}\""
                    } else {
                        "FLEX DECK · Tabletop PDA Input & Quick Replies"
                    },
                    color = AccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick-response action buttons
            val presets = listOf("On my way", "In a meeting", "Call you in 5m", "Sounds good 👍")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardElevated)
                            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            .clickable {
                                val targetRoom = latestMessage?.roomId ?: activeAccount?.id.orEmpty()
                                if (targetRoom.isNotBlank()) {
                                    onSendQuickReply(targetRoom, preset)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = preset,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Input Box + Send Button
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (replyDraft.isEmpty()) {
                        Text(
                            text = "Type message in Tabletop Flex Mode…",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                    BasicTextField(
                        value = replyDraft,
                        onValueChange = { replyDraft = it },
                        singleLine = true,
                        textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                        cursorBrush = SolidColor(AccentLimeGreen),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(AccentLimeGreen)
                        .clickable {
                            val targetRoom = latestMessage?.roomId ?: activeAccount?.id.orEmpty()
                            if (replyDraft.isNotBlank() && targetRoom.isNotBlank()) {
                                onSendQuickReply(targetRoom, replyDraft.trim())
                                replyDraft = ""
                            }
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Send ▶", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Added Accounts Switcher Strip (only shows added accounts)
            if (uiState.accounts.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    uiState.accounts.forEach { acc ->
                        val isCurrent = acc.id == activeAccount?.id
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isCurrent) Color(acc.serviceType.badgeColorHex).copy(alpha = 0.55f)
                                    else Color(0x44000000)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .clickable { onSwitchAccount(acc.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            ServiceBadge(serviceType = acc.serviceType, size = 20)
                            Text(
                                text = acc.accountName,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
