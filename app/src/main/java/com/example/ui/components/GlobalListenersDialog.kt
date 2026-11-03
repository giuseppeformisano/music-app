package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SubtitleGray

// Soglia oltre la quale il primo co-ascoltatore viene messo in evidenza come "Sintonia".
private const val SINTONIA_THRESHOLD = 65

@Composable
fun GlobalListenersDialog(
    track: Track,
    ranked: List<Pair<User, Int>>,
    loading: Boolean,
    followingIds: Set<String>,
    sentRequestIds: Set<String>,
    onOpenProfile: (User) -> Unit,
    onSendFollow: (User) -> Unit,
    onOpenChat: (User) -> Unit,
    onDismiss: () -> Unit
) {
    val topMatch = ranked.firstOrNull()?.takeIf { it.second >= SINTONIA_THRESHOLD }

    UtilityOverlay(onDismiss = onDismiss, swipeAnywhere = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 20.dp)
        ) {
            PanelHeader(title = "In ascolto ora")
            Text(
                text = "${track.title} · ${track.artist}",
                color = SubtitleGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 56.dp, end = 24.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            when {
                loading -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                ranked.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun altro sta ascoltando questo brano ora",
                        color = SubtitleGray,
                        fontSize = 13.sp
                    )
                }

                else -> {
                    val dialogScrollConn = LocalDialogScrollConnection.current
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .let { m -> if (dialogScrollConn != null) m.nestedScroll(dialogScrollConn) else m },
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (topMatch != null) {
                            item(key = "sintonia") {
                                SintoniaCard(
                                    user = topMatch.first,
                                    affinity = topMatch.second,
                                    onMessage = { onOpenChat(topMatch.first) },
                                    onOpenProfile = { onOpenProfile(topMatch.first) }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                        items(ranked, key = { it.first.id }) { (user, affinity) ->
                            // Il top match è già in evidenza nella card Sintonia: non ripeterlo.
                            if (topMatch == null || user.id != topMatch.first.id) {
                                ListenerRow(
                                    user = user,
                                    affinity = affinity,
                                    isFollowing = followingIds.contains(user.id),
                                    requestSent = sentRequestIds.contains(user.id),
                                    onOpenProfile = { onOpenProfile(user) },
                                    onSendFollow = { onSendFollow(user) },
                                    onOpenChat = { onOpenChat(user) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SintoniaCard(
    user: User,
    affinity: Int,
    onMessage: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SpotifyGreen.copy(alpha = 0.12f))
            .border(1.dp, SpotifyGreen.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenProfile
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.username,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(46.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sintonia · $affinity%",
                color = SpotifyGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = user.name.ifBlank { user.username },
                color = PureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "ascoltate lo stesso brano ora",
                color = SubtitleGray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(PureWhite)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onMessage
                )
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = null,
                tint = BlackPitch,
                modifier = Modifier.size(15.dp)
            )
            Text("Saluta", color = BlackPitch, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ListenerRow(
    user: User,
    affinity: Int,
    isFollowing: Boolean,
    requestSent: Boolean,
    onOpenProfile: () -> Unit,
    onSendFollow: () -> Unit,
    onOpenChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF121214))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenProfile
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.username,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(40.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name.ifBlank { user.username },
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.username.lowercase()} · $affinity% sintonia",
                color = SubtitleGray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        when {
            isFollowing -> ActionPill("Messaggio", filled = false, onClick = onOpenChat)
            requestSent -> ActionPill("Inviata", filled = false, enabled = false, onClick = {})
            else -> ActionPill("Segui", filled = true, onClick = onSendFollow)
        }
    }
}

@Composable
private fun ActionPill(
    label: String,
    filled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (filled) PureWhite else Color.Transparent)
            .border(if (filled) 0.dp else 0.8.dp, PureWhite.copy(alpha = if (enabled) 0.3f else 0.12f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            color = if (filled) BlackPitch else PureWhite.copy(alpha = if (enabled) 0.85f else 0.4f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
