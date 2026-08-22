package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray
import com.example.ui.theme.Zinc400

private data class FloatingFeedReaction(
    val id: Long,
    val reaction: CustomReaction,
    val initialXRatio: Float
)

@Composable
fun TrackDetailDialog(
    track: Track,
    user: User?,
    onDismiss: () -> Unit,
    onSendMessage: (User, Track) -> Unit = { _, _ -> },
    onSendTextMessage: ((User, String, Track) -> Unit)? = null,
    onOpenUserProfile: (User) -> Unit = {},
    onShareToMyFeed: (Track) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var commentText by remember { mutableStateOf("") }
    val floatingReactions = remember { mutableStateListOf<FloatingFeedReaction>() }

    TrackDialog(coverUrl = track.coverUrl, onDismiss = onDismiss) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. HEADER: Condiviso da / @username
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp)
                ) {
                    Text(
                        text = "Condiviso da",
                        color = PureWhite.copy(alpha = 0.70f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "@${user?.username?.lowercase() ?: "utente"}",
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (user != null) {
                                    onOpenUserProfile(user)
                                    onDismiss()
                                }
                            }
                        )
                    )
                }

                // 2. COVER ALBUM HERO QUADRATA
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .shadow(
                            elevation = 20.dp,
                            shape = RoundedCornerShape(14.dp),
                            ambientColor = Color.Black.copy(alpha = 0.6f),
                            spotColor = Color.Black.copy(alpha = 0.85f)
                        )
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF141418)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = "Cover ${track.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 3. TITOLO, ARTISTA / ALBUM E PILL GENERE / ANNO
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = track.title,
                        color = PureWhite,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-0.4).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val subtitle = if (track.album.isNotBlank() && !track.album.equals(track.title, ignoreCase = true)) {
                        "${track.artist} · ${track.album}"
                    } else {
                        track.artist
                    }
                    Text(
                        text = subtitle,
                        color = Zinc400,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pill Genere e Anno di Rilascio
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (track.genre.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF141418).copy(alpha = 0.85f))
                                    .border(0.75.dp, PureWhite.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = track.genre,
                                    color = PureWhite.copy(alpha = 0.90f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        if (track.releaseYear.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF141418).copy(alpha = 0.85f))
                                    .border(0.75.dp, PureWhite.copy(alpha = 0.15f), CircleShape)
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = track.releaseYear,
                                    color = PureWhite.copy(alpha = 0.90f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // 4. REACTION CUSTOM (Diamond, Soundwave, Star) E INPUT TESTUALE COMMENTO
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Barra Reaction (Diamond, Soundwave, Star)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        detailReactions.forEach { reaction ->
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            floatingReactions.add(
                                                FloatingFeedReaction(
                                                    id = System.currentTimeMillis(),
                                                    reaction = reaction,
                                                    initialXRatio = (0.2f + Math.random().toFloat() * 0.6f)
                                                )
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomReactionIcon(
                                    reaction = reaction,
                                    tint = PureWhite,
                                    size = 24.dp
                                )
                            }
                        }
                    }

                    // Input Testuale Commento
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color(0xFF141418).copy(alpha = 0.85f))
                            .border(0.8.dp, PureWhite.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = commentText,
                            onValueChange = { commentText = it },
                            modifier = Modifier.weight(1f),
                            textStyle = TextStyle(
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.2.sp
                            ),
                            cursorBrush = SolidColor(PureWhite),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (commentText.isNotBlank()) {
                                        if (user != null) {
                                            if (onSendTextMessage != null) {
                                                onSendTextMessage(user, commentText.trim(), track)
                                            } else {
                                                onSendMessage(user, track)
                                            }
                                        }
                                        commentText = ""
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        onDismiss()
                                    }
                                }
                            ),
                            decorationBox = { innerTextField ->
                                if (commentText.isEmpty()) {
                                    Text(
                                        text = "Commenta con @${user?.username?.lowercase() ?: "utente"}...",
                                        color = SubtitleGray,
                                        fontSize = 14.sp,
                                        letterSpacing = 0.2.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        IconButton(
                            onClick = {
                                if (commentText.isNotBlank()) {
                                    if (user != null) {
                                        if (onSendTextMessage != null) {
                                            onSendTextMessage(user, commentText.trim(), track)
                                        } else {
                                            onSendMessage(user, track)
                                        }
                                    }
                                    commentText = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                    onDismiss()
                                }
                            },
                            enabled = commentText.isNotBlank(),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Invia",
                                tint = if (commentText.isNotBlank()) PureWhite else SubtitleGray.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Floating Reactions
            floatingReactions.forEach { item ->
                FloatingFeedReactionEffect(
                    reaction = item.reaction,
                    xRatio = item.initialXRatio,
                    onFinished = { floatingReactions.remove(item) }
                )
            }
        }
    }
}

@Composable
private fun FloatingFeedReactionEffect(
    reaction: CustomReaction,
    xRatio: Float,
    onFinished: () -> Unit
) {
    val animY = remember { Animatable(1f) }
    val animAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        animY.animateTo(0.05f, animationSpec = tween(1200, easing = LinearEasing))
        animAlpha.animateTo(0f, animationSpec = tween(350, easing = LinearEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {},
        contentAlignment = Alignment.BottomStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = (xRatio * 280).dp)
                .padding(bottom = (animY.value * 380).dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(PureWhite.copy(alpha = 0.15f * animAlpha.value)),
            contentAlignment = Alignment.Center
        ) {
            CustomReactionIcon(
                reaction = reaction,
                tint = PureWhite.copy(alpha = animAlpha.value),
                size = 22.dp
            )
        }
    }
}
