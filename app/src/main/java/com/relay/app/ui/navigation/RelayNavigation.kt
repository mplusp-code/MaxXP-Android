package com.relay.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
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

fun getTabIcon(tab: RelayTab): ImageVector {
    return when (tab) {
        RelayTab.TODAY -> Icons.Default.Home
        RelayTab.INBOX -> Icons.Default.Mail
        RelayTab.ACCOUNTS -> Icons.Default.People
        RelayTab.SETUP -> Icons.Default.Settings
    }
}

// 4-Tab Bottom Navigation Bar for Compact/Portrait Mode (PDF Pages 1-5)
@Composable
fun RelayBottomNavBar(
    currentTab: RelayTab,
    onTabSelected: (RelayTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFF1B2E17))
            .border(1.dp, Color(0xFF38572A)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RelayTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isSelected) Color(0xFF8BC34A).copy(alpha = 0.4f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) Color(0xFF8BC34A) else Color.Transparent,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onTabSelected(tab) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = getTabIcon(tab),
                        contentDescription = tab.title,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = tab.title,
                        color = Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// Left Vertical Navigation Rail for Expanded/Unfolded Mode (PDF Pages 6-10)
@Composable
fun RelayLeftNavigationRail(
    currentTab: RelayTab,
    onTabSelected: (RelayTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(68.dp)
            .fillMaxHeight()
            .background(Color(0xFF142412))
            .border(1.dp, Color(0xFF38572A))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RelayTab.values().forEach { tab ->
            val isSelected = tab == currentTab
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isSelected) Color(0xFF8BC34A).copy(alpha = 0.45f)
                        else Color.Transparent
                    )
                    .border(
                        width = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) Color(0xFF8BC34A) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = getTabIcon(tab),
                    contentDescription = tab.title,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = tab.title,
                    color = Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 11.sp
                )
            }
        }
    }
}
