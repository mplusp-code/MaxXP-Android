package com.relay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
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
import com.relay.app.data.MessengerType
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
            .height(48.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF22351E), Color(0xFF0F1A0E))
                )
            )
            .drawBehind {
                // Top subtle gloss reflection
                drawLine(
                    color = Color.White.copy(alpha = 0.25f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
                // Bottom bevel divider
                drawLine(
                    color = Color(0xFF4C8C2B).copy(alpha = 0.6f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
            }
            .padding(horizontal = 12.dp)
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
                // 4-Square Relay brand logo icon
                RelayBrandLogo()

                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                customActions()

                if (actionText != null) {
                    Text(
                        text = actionText,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF3B5D2E), Color(0xFF1E3517))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .clickable { onActionClick() }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RelayBrandLogo(modifier: Modifier = Modifier) {
    // 2x2 grid representing the Windows Mobile / Relay iconic tile icon
    Column(
        modifier = modifier.size(18.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(1.5.dp)).background(Color(0xFF8BC34A)))
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(1.5.dp)).background(Color.White))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(1.5.dp)).background(Color.White))
            Box(Modifier.size(8.dp).clip(RoundedCornerShape(1.5.dp)).background(Color(0xFF8BC34A)))
        }
    }
}

@Composable
fun RelaySoftkeyBar(
    leftText: String,
    onLeftClick: () -> Unit,
    rightText: String = "Menu",
    onRightClick: () -> Unit = {},
    onKeyboardClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF2A3D24), Color(0xFF11190E))
                )
            )
            .drawBehind {
                // Top highlight line
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left slot
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onLeftClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = leftText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Center slot: Retro PDA physical keyboard button
            Box(
                modifier = Modifier
                    .size(width = 46.dp, height = 32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF3E5A33), Color(0xFF1C2A18))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                    .clickable { onKeyboardClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Soft Keyboard",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right slot
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onRightClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rightText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun ServiceBadge(
    serviceType: MessengerType,
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    val bgColor = Color(serviceType.badgeColorHex)

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.6f),
                        Color.Black.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(6.dp)
            )
            .drawBehind {
                // Top-half glossy sheen
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.toFloat() * 0.5f
                    )
                )
            },
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
            .height(26.dp)
            .background(Color(0x33000000))
            .drawBehind {
                drawLine(
                    color = Color.White.copy(alpha = 0.2f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1f
                )
            }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
