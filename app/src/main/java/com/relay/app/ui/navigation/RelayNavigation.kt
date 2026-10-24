package com.relay.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.relay.app.data.RelayTab
import com.relay.app.ui.theme.*

fun getTabIcon(tab: RelayTab): ImageVector {
    return when (tab) {
        RelayTab.TODAY -> Icons.Default.GridView
        RelayTab.INBOX -> Icons.Default.Inbox
        RelayTab.ACCOUNTS -> Icons.Default.PeopleOutline
        RelayTab.SETUP -> Icons.Default.Tune
    }
}

// Modern 4-Tab Bottom Navigation Bar for Compact/Portrait Mode (Concept v2 Pages 1-5)
@Composable
fun RelayBottomNavBar(
    currentTab: RelayTab,
    onTabSelected: (RelayTab) -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(SurfaceDark)
            .border(width = 1.dp, color = BorderDark)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RelayTab.entries.forEach { tab ->
            val isSelected = tab == currentTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box {
                        Icon(
                            imageVector = getTabIcon(tab),
                            contentDescription = tab.title,
                            tint = if (isSelected) AccentLimeGreen else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )

                        // Dynamic numeric unread badge pill for Inbox
                        if (tab == RelayTab.INBOX && unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 10.dp, y = (-5).dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentLimeGreen)
                                    .padding(horizontal = 4.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = BackgroundDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = tab.title,
                        color = if (isSelected) AccentLimeGreen else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// Modern Left Vertical Navigation Rail for Expanded/Unfolded Mode (Concept v2 Pages 6-10)
@Composable
fun RelayLeftNavigationRail(
    currentTab: RelayTab,
    onTabSelected: (RelayTab) -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
    profileLetter: String = "P",
    onProfileClick: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .fillMaxHeight()
            .background(SurfaceDark)
            .border(width = 1.dp, color = BorderDark)
            .padding(vertical = 16.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            RelayTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                Box(
                    modifier = Modifier
                        .size(width = 60.dp, height = 54.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) AccentLimeGreen.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) AccentLimeGreen.copy(alpha = 0.4f) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onTabSelected(tab) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box {
                            Icon(
                                imageVector = getTabIcon(tab),
                                contentDescription = tab.title,
                                tint = if (isSelected) AccentLimeGreen else TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )

                            if (tab == RelayTab.INBOX && unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 10.dp, y = (-5).dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AccentLimeGreen)
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        color = BackgroundDark,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(3.dp))

                        Text(
                            text = tab.title,
                            color = if (isSelected) AccentLimeGreen else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Profile indicator docked at the bottom of the navigation rail (P / W)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CardElevated)
                .border(1.dp, BorderDark, CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profileLetter,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
