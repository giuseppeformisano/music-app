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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray

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
    UtilityOverlay(onDismiss = onDismiss, swipeAnywhere = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 20.dp)
        ) {
            // Header uniforme (freccia indietro + titolo)
            PanelHeader(title = "In ascolto ora")

            Spacer(modifier = Modifier.height(12.dp))

            // Brano in evidenza: copertina + titolo (grande) + artista
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        color = PureWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = SubtitleGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(1.dp)
                    .background(PureWhite.copy(alpha = 0.06f))
            )
            Spacer(modifier = Modifier.height(6.dp))

            when {
                loading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PureWhite, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }

                ranked.isEmpty() -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun altro sta ascoltando questo brano ora",
                        color = SubtitleGray.copy(alpha = 0.5f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                else -> {
                    val dialogScrollConn = LocalDialogScrollConnection.current
                    LazyColumn(
                        modifier = Modifier.weight(1f).padding(horizontal = 24.dp)
                            .let { m -> if (dialogScrollConn != null) m.nestedScroll(dialogScrollConn) else m },
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(ranked, key = { it.first.id }) { (user, affinity) ->
                            ListenerRow(
                                user = user,
                                affinity = affinity,
                                alreadyFollowing = followingIds.contains(user.id),
                                requested = sentRequestIds.contains(user.id),
                                onSendRequest = { onSendFollow(user) },
                                onOpenProfile = { onDismiss(); onOpenProfile(user) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListenerRow(
    user: User,
    affinity: Int,
    alreadyFollowing: Boolean,
    requested: Boolean,
    onSendRequest: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenProfile
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
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
                text = "@${user.username.removePrefix("@")} · $affinity% sintonia",
                color = SubtitleGray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        val isDisabled = alreadyFollowing || requested
        if (!isDisabled) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(PureWhite)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSendRequest
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Segui", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Text(
                text = if (alreadyFollowing) "Segui già" else "Inviata",
                color = SubtitleGray.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}
