package com.relay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.relay.app.data.MessengerType
import com.relay.app.matrix.MatrixSyncStatus
import com.relay.app.ui.theme.*

@Composable
fun RelayTopBar(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: () -> Unit = {},
    customActions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(BackgroundDark)
            .border(width = 1.dp, color = BorderDark.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RelayBrandLogo()

                Column {
                    Text(
                        text = "RELAY",
                        color = AccentLimeGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = title.lowercase(),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                customActions()

                if (actionText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardElevated)
                            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                            .clickable { onActionClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = actionText,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RelayConnectionPill(
    status: MatrixSyncStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dotColor = when (status) {
        MatrixSyncStatus.SYNCED -> AccentGreen
        MatrixSyncStatus.CONNECTING -> AccentBlue
        MatrixSyncStatus.OFFLINE -> AmberAttention
    }

    val labelText = when (status) {
        MatrixSyncStatus.SYNCED -> "• Live"
        MatrixSyncStatus.CONNECTING -> "• Connecting"
        MatrixSyncStatus.OFFLINE -> "• Offline"
    }

    Row(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = labelText.removePrefix("• "),
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MatrixServerLoginDialog(
    initialHomeserver: String,
    initialUserId: String,
    syncStatus: MatrixSyncStatus,
    onDismiss: () -> Unit,
    onConnect: (homeserverUrl: String, username: String, password: String) -> Unit
) {
    var homeserver by remember { mutableStateOf(initialHomeserver) }
    var username by remember { mutableStateOf(initialUserId) }
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RELAY",
                        color = AccentLimeGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Matrix Conduit Server",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                RelayConnectionPill(status = syncStatus, onClick = {})
            }

            Text(
                text = "Connect to self-hosted Matrix Conduit + Mautrix bridge hub on Oracle Cloud VM.",
                color = TextSecondary,
                fontSize = 12.sp
            )

            // Homeserver URL field
            Text("Homeserver URL", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = homeserver,
                    onValueChange = { homeserver = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(AccentLimeGreen),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Matrix User ID field
            Text("Matrix User ID", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(AccentLimeGreen),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Password field
            Text("Password (optional if using token)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BackgroundDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (password.isEmpty()) {
                    Text(
                        text = "•••••••••••• (saved token active)",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
                BasicTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                    cursorBrush = SolidColor(AccentLimeGreen),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardElevated)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Cancel", color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(AccentLimeGreen)
                        .clickable { onConnect(homeserver.trim(), username.trim(), password) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sync Now", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun RelayBrandLogo(modifier: Modifier = Modifier) {
    // Clean 2x2 grid brand icon (green and white tiles)
    Column(
        modifier = modifier.size(18.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(AccentLimeGreen))
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(Color.White))
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(AccentLimeGreen))
        }
    }
}

@Composable
fun ServiceBadge(
    serviceType: MessengerType,
    modifier: Modifier = Modifier,
    size: Int = 28
) {
    val bgColor = Color(serviceType.badgeColorHex)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = serviceType.badgeChar,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size * 0.52f).sp
        )
    }
}

@Composable
fun RelaySectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title.uppercase(),
            color = TextSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.8.sp
        )
    }
}

@Deprecated("The retro 3-slot softkey action bar is retired in Concept v2")
@Composable
fun RelaySoftkeyBar(
    leftText: String = "",
    onLeftClick: () -> Unit = {},
    rightText: String = "",
    onRightClick: () -> Unit = {},
    onKeyboardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Retired no-op placeholder for backward compatibility
}
