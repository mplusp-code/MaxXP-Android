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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.RelayUiState
import com.relay.app.data.UnfoldedOpenMode
import com.relay.app.data.WallpaperTheme
import com.relay.app.ui.components.RelaySectionHeader
import org.unifiedpush.android.connector.UnifiedPush

@Composable
fun SetupScreen(
    uiState: RelayUiState,
    isExpanded: Boolean,
    onSelectWallpaper: (WallpaperTheme) -> Unit,
    onUpdatePreferences: (com.relay.app.data.SetupPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isExpanded) {
        // Unfolded 2-Pane Setup Screen (PDF Page 10)
        Row(modifier = modifier.fillMaxSize()) {
            SetupAppearancePane(
                uiState = uiState,
                onSelectWallpaper = onSelectWallpaper,
                onUpdatePreferences = onUpdatePreferences,
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            )

            Box(
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.2f))
            )

            SetupFoldableRulesPane(
                uiState = uiState,
                onUpdatePreferences = onUpdatePreferences,
                modifier = Modifier
                    .weight(0.5f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            )
        }
    } else {
        // Portrait Scrollable Setup Screen (Appearance + Foldable/Hibernation rules)
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            SetupAppearancePane(
                uiState = uiState,
                onSelectWallpaper = onSelectWallpaper,
                onUpdatePreferences = onUpdatePreferences,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            SetupFoldableRulesPane(
                uiState = uiState,
                onUpdatePreferences = onUpdatePreferences,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SetupAppearancePane(
    uiState: RelayUiState,
    onSelectWallpaper: (WallpaperTheme) -> Unit,
    onUpdatePreferences: (com.relay.app.data.SetupPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        RelaySectionHeader(title = "Wallpaper")

        Spacer(Modifier.height(8.dp))

        // Wallpaper Radio Options
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WallpaperTheme.values().forEach { theme ->
                val isSelected = prefs.wallpaperTheme == theme
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clickable { onSelectWallpaper(theme) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Skeuomorphic Radio Dot
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E631F))
                            )
                        }
                    }

                    // Color Preview Block
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 22.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(theme.secondaryColor))
                            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                    )

                    Text(
                        text = theme.displayName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        RelaySectionHeader(title = "Text size")

        // Retro Text Size Slider
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            var sliderVal by remember { mutableStateOf(prefs.textSizeProgress) }

            Slider(
                value = sliderVal,
                onValueChange = {
                    sliderVal = it
                    onUpdatePreferences(prefs.copy(textSizeProgress = it))
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2196F3),
                    activeTrackColor = Color.White.copy(alpha = 0.4f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Small", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                Text("Large", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(14.dp))

        RelaySectionHeader(title = "Alerts")

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RelayCheckboxOption(
                title = "Show unread counts on Today",
                checked = prefs.showUnreadOnToday,
                onCheckedChange = { onUpdatePreferences(prefs.copy(showUnreadOnToday = it)) }
            )

            RelayCheckboxOption(
                title = "Keep sessions alive in background",
                checked = prefs.keepSessionsAlive,
                onCheckedChange = { onUpdatePreferences(prefs.copy(keepSessionsAlive = it)) }
            )

            RelayCheckboxOption(
                title = "Quiet hours",
                checked = prefs.quietHoursEnabled,
                onCheckedChange = { onUpdatePreferences(prefs.copy(quietHoursEnabled = it)) }
            )

            // Quiet hours time pickers (23:00 to 07:00)
            val startPresets = listOf("21:00", "22:00", "23:00", "00:00")
            val endPresets = listOf("06:00", "07:00", "08:00", "09:00")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RelayTimePickerButton(
                    time = prefs.quietHoursStart,
                    onClick = {
                        val nextIdx = (startPresets.indexOf(prefs.quietHoursStart) + 1) % startPresets.size
                        onUpdatePreferences(prefs.copy(quietHoursStart = startPresets[nextIdx]))
                    }
                )
                Text("to", color = Color.White, fontSize = 14.sp)
                RelayTimePickerButton(
                    time = prefs.quietHoursEnd,
                    onClick = {
                        val nextIdx = (endPresets.indexOf(prefs.quietHoursEnd) + 1) % endPresets.size
                        onUpdatePreferences(prefs.copy(quietHoursEnd = endPresets[nextIdx]))
                    }
                )
            }

            RelayPushDeliveryRow()
        }
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
    var ackDist by remember {
        mutableStateOf(runCatching { UnifiedPush.getAckDistributor(context) }.getOrNull())
    }

    val distributorLabel = when {
        savedDist == null -> "none"
        savedDist!!.contains("ntfy", ignoreCase = true) -> if (ackDist != null) "ntfy (active)" else "ntfy"
        else -> savedDist!!.substringAfterLast('.')
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Push delivery",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Distributor: $distributorLabel",
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFF2E631F))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
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
                        ackDist = runCatching { UnifiedPush.getAckDistributor(context) }.getOrNull()
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (savedDist == null) "Register" else "Re-register",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (distributors.isEmpty()) {
            Text(
                text = "Install the ntfy app and set server https://max-os.mooo.com:8449",
                color = Color(0xFFFFD54F),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun SetupFoldableRulesPane(
    uiState: RelayUiState,
    onUpdatePreferences: (com.relay.app.data.SetupPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs = uiState.preferences

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        RelaySectionHeader(title = "When unfolded, open")

        Spacer(Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            UnfoldedOpenMode.values().forEach { mode ->
                val isSelected = prefs.unfoldedOpenMode == mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clickable { onUpdatePreferences(prefs.copy(unfoldedOpenMode = mode)) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color.Black.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E631F))
                            )
                        }
                    }

                    Text(text = mode.label, color = Color.White, fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        RelaySectionHeader(title = "Folding and unfolding")

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RelayCheckboxOption(
                title = "Keep open chats when folding",
                checked = prefs.keepOpenChatsWhenFolding,
                onCheckedChange = { onUpdatePreferences(prefs.copy(keepOpenChatsWhenFolding = it)) }
            )
            RelayCheckboxOption(
                title = "Open the chat from the cover screen on unfold",
                checked = prefs.openChatOnUnfold,
                onCheckedChange = { onUpdatePreferences(prefs.copy(openChatOnUnfold = it)) }
            )
            RelayCheckboxOption(
                title = "Use Flex mode when half-folded",
                checked = prefs.useFlexModeWhenHalfFolded,
                onCheckedChange = { onUpdatePreferences(prefs.copy(useFlexModeWhenHalfFolded = it)) }
            )
        }

        Spacer(Modifier.height(14.dp))

        RelaySectionHeader(title = "Web views")

        Spacer(Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RelayCheckboxOption(
                title = "Open chats in tabs",
                checked = prefs.openChatsInTabs,
                onCheckedChange = { onUpdatePreferences(prefs.copy(openChatsInTabs = it)) }
            )
            RelayCheckboxOption(
                title = "Remember the second pane",
                checked = prefs.rememberSecondPane,
                onCheckedChange = { onUpdatePreferences(prefs.copy(rememberSecondPane = it)) }
            )
            RelayCheckboxOption(
                title = "Read chats from web pages (fallback)",
                checked = prefs.useWebViewScraperFallback,
                onCheckedChange = { onUpdatePreferences(prefs.copy(useWebViewScraperFallback = it)) }
            )
        }
    }
}

@Composable
fun RelayCheckboxOption(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.5f), RoundedCornerShape(3.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
        Text(text = title, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
fun RelayTimePickerButton(
    time: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .width(84.dp)
            .height(34.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFF2E631F))
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = time, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
    }
}
