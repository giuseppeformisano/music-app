package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.BlackPitch

enum class BottomNavMode { MAIN, PROFILE }

private val IconColor = Color(0xFF555555)
private val BadgeBg = Color(0xFFE53935)

@Composable
fun BottomNavBar(
    mode: BottomNavMode = BottomNavMode.MAIN,
    unreadMessages: Int = 0,
    unreadNotifications: Int = 0,
    onSearchClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onProfileOrBackClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BlackPitch)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cerca",
                onClick = onSearchClick
            )
            NavIcon(
                imageVector = Icons.Default.Email,
                contentDescription = "Messaggi",
                badge = unreadMessages,
                onClick = onMessagesClick
            )
            NavIcon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifiche",
                badge = unreadNotifications,
                onClick = onNotificationsClick
            )
            NavIcon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Impostazioni",
                onClick = onSettingsClick
            )
            NavIcon(
                imageVector = if (mode == BottomNavMode.PROFILE)
                    Icons.AutoMirrored.Filled.ArrowBack
                else
                    Icons.Default.Person,
                contentDescription = if (mode == BottomNavMode.PROFILE) "Torna" else "Profilo",
                onClick = onProfileOrBackClick
            )
        }
    }
}

@Composable
private fun NavIcon(
    imageVector: ImageVector,
    contentDescription: String,
    badge: Int = 0,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = IconColor,
            modifier = Modifier.size(22.dp)
        )
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 4.dp)
                    .size(if (badge > 9) 16.dp else 14.dp)
                    .clip(CircleShape)
                    .background(BadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge > 99) "99+" else badge.toString(),
                    color = Color.White,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 8.sp
                )
            }
        }
    }
}
