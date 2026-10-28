package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AppNotification
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray

@Composable
fun NotificationsDialog(
    notifications: List<AppNotification>,
    followingIds: Set<String>,
    sentRequestIds: Set<String>,
    onAccept: (AppNotification) -> Unit,
    onReject: (AppNotification) -> Unit,
    onFollowBack: (AppNotification) -> Unit,
    onDismiss: () -> Unit
) {
    val pendingCount = notifications.count { it.type == "follow_request" && it.status == "pending" }

    UtilityOverlay(onDismiss = onDismiss, swipeAnywhere = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp, bottom = 20.dp)
        ) {
            Row(
                modifier = LocalDialogDragHandle.current
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Notifiche",
                        color = PureWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.4).sp
                    )
                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFFF3B30))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = pendingCount.toString(),
                                color = PureWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = SubtitleGray.copy(alpha = 0.25f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nessuna notifica",
                            color = SubtitleGray.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                val dialogScrollConn = LocalDialogScrollConnection.current
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .let { m -> if (dialogScrollConn != null) m.nestedScroll(dialogScrollConn) else m },
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(notifications, key = { it.id }) { notif ->
                        NotificationRow(
                            notif = notif,
                            isFollowing = notif.fromUserId in followingIds,
                            isRequested = notif.fromUserId in sentRequestIds,
                            onAccept = { onAccept(notif) },
                            onReject = { onReject(notif) },
                            onFollowBack = { onFollowBack(notif) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notif: AppNotification,
    isFollowing: Boolean,
    isRequested: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onFollowBack: () -> Unit
) {
    val subtitle = when (notif.status) {
        "accepted" -> "@${notif.fromUserUsername} ha iniziato a seguirti"
        "rejected" -> "Hai rifiutato @${notif.fromUserUsername}"
        else -> "@${notif.fromUserUsername} vuole seguirti"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = notif.fromUserAvatarUrl,
            contentDescription = notif.fromUserName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notif.fromUserName,
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = SubtitleGray.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        when (notif.status) {
            "pending" -> {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PureWhite.copy(alpha = 0.08f))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onReject
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Rifiuta",
                            tint = SubtitleGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onAccept
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Accetta",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            // accepted / rejected: notifica che resta come traccia. Se non seguo la persona,
            // mostro il bottone "Segui" (follow-back), come richiesto.
            else -> {
                when {
                    isFollowing -> FollowPill(text = "Seguito", filled = false, enabled = false, onClick = {})
                    isRequested -> FollowPill(text = "Richiesta inviata", filled = false, enabled = false, onClick = {})
                    else -> FollowPill(text = "Segui", filled = true, enabled = true, onClick = onFollowBack)
                }
            }
        }
    }
}

@Composable
private fun FollowPill(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (filled) PureWhite else PureWhite.copy(alpha = 0.08f))
            .let { m ->
                if (enabled) m.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ) else m
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (filled) Color.Black else SubtitleGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
