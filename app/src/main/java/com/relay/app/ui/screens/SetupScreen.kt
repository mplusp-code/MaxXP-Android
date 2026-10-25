package com.relay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.AccentColor
import com.relay.app.data.RelayUiState
import com.relay.app.data.SetupPreferences
import com.relay.app.data.ThemeMode
import com.relay.app.data.UnfoldedOpenMode
import com.relay.app.data.WallpaperTheme
import com.relay.app.ui.theme.*
import org.unifiedpush.android.connector.UnifiedPush

@Composable
fun SettingsScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    onSelectWallpaper: (WallpaperTheme) -> Unit,
    onUpdatePreferences: (SetupPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences

    if (isExpanded) {
        // Unfolded 2-Column Settings Screen (Concept v2 Page 10)
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column (RELIABILITY, NOTIFICATIONS, FOLDING)
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "settings",
                        color = TextPrimary,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                ReliabilityCard(prefs = prefs, onUpdatePreferences = onUpdatePreferences)
                NotificationsCard(prefs = prefs, onUpdatePreferences = onUpdatePreferences)
                FoldingCard(prefs = prefs, onUpdatePreferences = onUpdatePreferences)
            }

            // Right Column (APPEARANCE, WHEN UNFOLDED OPEN)
            Column(
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppearanceCard(
                    prefs = prefs,
                    onUpdatePreferences = onUpdatePreferences,
                    onSelectWallpaper = onSelectWallpaper
                )
                WhenUnfoldedOpenCard(prefs = prefs, onUpdatePreferences = onUpdatePreferences)
            }
        }
    } else {
        // Portrait Scrollable Settings Screen (Concept v2 Page 5)
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    text = "settings",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            ReliabilityCard(prefs = prefs, onUpdatePreferences = onUpdatePreferences)
            NotificationsCard(prefs = prefs, onUpdatePreferences = onUpdatePreferences)
            AppearanceCard(
                prefs = prefs,
                onUpdatePreferences = onUpdatePreferences,
                onSelectWallpaper = onSelectWallpaper
            )
            FoldingCard(prefs = prefs, onUpdatePreferences = onUpdatePreferences)
            WhenUnfoldedOpenCard(prefs = prefs, onUpdatePreferences = onUpdatePreferences)

            Spacer(Modifier.height(8.dp))
        }
    }
}

// Backward compatibility alias for SetupScreen
@Composable
fun SetupScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    onSelectWallpaper: (WallpaperTheme) -> Unit,
    onUpdatePreferences: (SetupPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsScreen(
        uiState = uiState,
        isExpanded = isExpanded,
        onSelectWallpaper = onSelectWallpaper,
        onUpdatePreferences = onUpdatePreferences,
        modifier = modifier
    )
}

@Composable
fun ReliabilityCard(
    prefs: SetupPreferences,
    onUpdatePreferences: (SetupPreferences) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "RELIABILITY",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Keep sessions alive
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Keep sessions alive", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Refreshes logins before they expire", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = prefs.keepSessionsAlive,
                onCheckedChange = { onUpdatePreferences(prefs.copy(keepSessionsAlive = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = AccentLimeGreen,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CardElevated
                )
            )
        }

        // Resend failed messages
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Resend failed messages", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Retries automatically when back online", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = prefs.resendFailedMessages,
                onCheckedChange = { onUpdatePreferences(prefs.copy(resendFailedMessages = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = AccentLimeGreen,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CardElevated
                )
            )
        }

        // Battery status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Battery", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Background sync is unrestricted", color = TextSecondary, fontSize = 12.sp)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text("Allowed", color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Data saver
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Data saver", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("May delay background sync to conserve data", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = prefs.dataSaverEnabled,
                onCheckedChange = { onUpdatePreferences(prefs.copy(dataSaverEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = AccentLimeGreen,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CardElevated
                )
            )
        }
    }
}

@Composable
fun NotificationsCard(
    prefs: SetupPreferences,
    onUpdatePreferences: (SetupPreferences) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "NOTIFICATIONS",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Read notifications backup listener
        RelayNotificationListenerRow(
            checked = prefs.useNotificationListenerFallback,
            onCheckedChange = { onUpdatePreferences(prefs.copy(useNotificationListenerFallback = it)) }
        )

        // Push delivery (ntfy / UnifiedPush)
        RelayPushDeliveryRow()
    }
}

@Composable
fun AppearanceCard(
    prefs: SetupPreferences,
    onUpdatePreferences: (SetupPreferences) -> Unit,
    onSelectWallpaper: (WallpaperTheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "APPEARANCE",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Theme Segmented Control: Dark, Light, Auto
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Theme", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardElevated)
                    .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    val isSelected = prefs.themeMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AccentLimeGreen else Color.Transparent)
                            .clickable { onUpdatePreferences(prefs.copy(themeMode = mode)) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode.label,
                            color = if (isSelected) BackgroundDark else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Accent Color Swatches
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Accent", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AccentColor.entries.forEach { accent ->
                    val isSelected = prefs.accentColor == accent
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(accent.colorHex))
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onUpdatePreferences(prefs.copy(accentColor = accent)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = BackgroundDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Text size slider
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Text size", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Slider(
                value = prefs.textSizeProgress,
                onValueChange = { onUpdatePreferences(prefs.copy(textSizeProgress = it)) },
                colors = SliderDefaults.colors(
                    thumbColor = AccentBlue,
                    activeTrackColor = AccentBlue,
                    inactiveTrackColor = CardElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FoldingCard(
    prefs: SetupPreferences,
    onUpdatePreferences: (SetupPreferences) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "FOLDING",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Keep open chats when folding", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Continue on the cover screen", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = prefs.keepOpenChatsWhenFolding,
                onCheckedChange = { onUpdatePreferences(prefs.copy(keepOpenChatsWhenFolding = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = AccentLimeGreen,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CardElevated
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Flex mode when half-folded", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("Chat on top, controls below", color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = prefs.useFlexModeWhenHalfFolded,
                onCheckedChange = { onUpdatePreferences(prefs.copy(useFlexModeWhenHalfFolded = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = BackgroundDark,
                    checkedTrackColor = AccentLimeGreen,
                    uncheckedThumbColor = TextMuted,
                    uncheckedTrackColor = CardElevated
                )
            )
        }
    }
}

@Composable
fun WhenUnfoldedOpenCard(
    prefs: SetupPreferences,
    onUpdatePreferences: (SetupPreferences) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "WHEN UNFOLDED, OPEN",
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        val modes = listOf(
            Triple(UnfoldedOpenMode.TRI_PANE_INBOX, "Workspace", "Chat list and web view side by side"),
            Triple(UnfoldedOpenMode.SPLIT_VIEW, "Split view", "Two chats at once"),
            Triple(UnfoldedOpenMode.TODAY_OVERVIEW, "Today", "Quick replies and session health"),
            Triple(UnfoldedOpenMode.LAST_USED, "Last used", "Reopen where you left off")
        )

        modes.forEach { (mode, title, subtitle) ->
            val isSelected = prefs.unfoldedOpenMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onUpdatePreferences(prefs.copy(unfoldedOpenMode = mode)) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) AccentLimeGreen else BorderDark,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AccentLimeGreen)
                        )
                    }
                }

                Column {
                    Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RelayNotificationListenerRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val isGranted = remember {
        runCatching {
            androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }.getOrDefault(false)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Read notifications (backup)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (isGranted) "Permission granted" else "Tap to grant permission",
                color = if (isGranted) AccentGreen else AmberAttentionText,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = {
                if (!isGranted && it) {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                }
                onCheckedChange(it)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = BackgroundDark,
                checkedTrackColor = AccentLimeGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = CardElevated
            )
        )
    }
}

@Composable
private fun RelayPushDeliveryRow() {
    val context = LocalContext.current
    var distributors by remember {
        mutableStateOf(runCatching { UnifiedPush.getDistributors(context) }.getOrDefault(emptyList()))
    }
    var savedDist by remember {
        mutableStateOf(runCatching { UnifiedPush.getSavedDistributor(context) }.getOrNull())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Push delivery (ntfy)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = if (savedDist != null) "UnifiedPush active" else "Install ntfy on port 8449",
                color = if (savedDist != null) AccentGreen else TextSecondary,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CardElevated)
                .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                .clickable {
                    distributors = runCatching { UnifiedPush.getDistributors(context) }.getOrDefault(emptyList())
                    if (distributors.size == 1) {
                        val chosen = distributors.first()
                        UnifiedPush.saveDistributor(context, chosen)
                        UnifiedPush.registerApp(context, messageForDistributor = "Relay Matrix push delivery")
                    } else if (distributors.size > 1) {
                        @Suppress("DEPRECATION")
                        UnifiedPush.registerAppWithDialog(
                            context = context,
                            dialogMessage = "Install the ntfy app and set server https://max-os.mooo.com:8449",
                            messageForDistributor = "Relay Matrix push delivery"
                        )
                    }
                    savedDist = runCatching { UnifiedPush.getSavedDistributor(context) }.getOrNull()
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (savedDist == null) "Register" else "Re-register",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
