package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.components.LiveEqualizerBadge
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray

import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton

@Composable
fun LiveStoryScreen(
    stories: List<User>,
    currentIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    onReplyMessage: (User, String, Track) -> Unit,
    onOpenProfile: (User) -> Unit,
    onShareToFeed: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeUser = stories.getOrNull(currentIndex) ?: return
    val activeTrack = activeUser.currentTrack ?: return
    val dynamicAccent = Color(activeTrack.accentColorHex)
    val focusManager = LocalFocusManager.current

    var replyText by remember(activeUser.id) { mutableStateOf("") }
    val progress = remember(activeUser.id) { Animatable(0f) }

    // Auto-advance progress bar after 6 seconds
    LaunchedEffect(activeUser.id) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 6000, easing = LinearEasing)
        )
        onNext()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BlackPitch)
            .pointerInput(activeUser.id) {
                detectDragGestures { _, dragAmount ->
                    // Swipe down to dismiss
                    if (dragAmount.y > 25f) {
                        onClose()
                    }
                }
            }
            .testTag("live_story_screen")
    ) {
        // Immersive Blurred Background Artwork
        AsyncImage(
            model = activeTrack.coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 12.dp)
        )

        // Dark Vignette Gradients for crisp readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BlackPitch.copy(alpha = 0.75f),
                            Color.Transparent,
                            BlackPitch.copy(alpha = 0.5f),
                            BlackPitch.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        // Center: High-Res Album Card / Visual Display
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 120.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .shadow(32.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.5.dp, dynamicAccent.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            ) {
                AsyncImage(
                    model = activeTrack.coverUrl,
                    contentDescription = "Cover di ${activeTrack.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Tap Gestures Area (Left = Previous, Right = Next)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp, top = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus()
                                onPrevious()
                            }
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                focusManager.clearFocus()
                                onNext()
                            }
                        )
                    }
            )
        }

        // Top Controls: Instagram-Style Progress Bars & User Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 28.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Segmented Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                stories.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(CharcoalBorder)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(segmentProgress)
                                .fillMaxHeight()
                                .background(if (index == currentIndex) dynamicAccent else PureWhite)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // User Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClick = { onOpenProfile(activeUser) })
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = activeUser.avatarUrl,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, dynamicAccent, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = activeUser.name,
                                color = PureWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            LiveEqualizerBadge(color = dynamicAccent, height = 10.dp)
                        }
                        Text(
                            text = "@${activeUser.username}",
                            color = SubtitleGray,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BlackPitch.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi Story",
                        tint = PureWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Bottom Info: Massive Bold Track Title, Artist Name, and Pure-Text Reply Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Genre tag pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(dynamicAccent.copy(alpha = 0.2f))
                    .border(1.dp, dynamicAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = activeTrack.genre.uppercase(),
                    color = dynamicAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = activeTrack.title,
                color = PureWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${activeTrack.artist} • ${activeTrack.album}",
                color = SubtitleGray,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Action: Condividi questa canzone nel mio Feed
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkGraphite.copy(alpha = 0.9f))
                    .border(1.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                    .clickable { onShareToFeed(activeTrack) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Condividi nel mio Feed",
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Aggiungi +",
                    color = dynamicAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Direct Reply Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = {
                        Text(
                            text = "Rispondi a ${activeUser.name.split(" ").first()}...",
                            color = SubtitleGray,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (replyText.isNotBlank()) {
                                onReplyMessage(activeUser, replyText, activeTrack)
                                replyText = ""
                                focusManager.clearFocus()
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("story_reply_input"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite,
                        unfocusedBorderColor = CharcoalBorder,
                        focusedContainerColor = DarkGraphite.copy(alpha = 0.8f),
                        unfocusedContainerColor = DarkGraphite.copy(alpha = 0.8f),
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        cursorColor = dynamicAccent
                    )
                )

                if (replyText.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .clickable {
                                onReplyMessage(activeUser, replyText, activeTrack)
                                replyText = ""
                                focusManager.clearFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Invia commento",
                            tint = BlackPitch,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
