package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.components.CustomReaction
import com.example.ui.components.CustomReactionIcon
import com.example.ui.components.LiveEqualizerSymmetrical
import com.example.ui.components.allCustomReactions
import com.example.ui.components.liveNameVibration
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray
import com.example.ui.theme.Zinc400
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class FloatingReaction(
    val id: Long,
    val reaction: CustomReaction,
    val initialXRatio: Float
)

/**
 * Schermata Dettaglio Live Fullscreen
 * - Nome profilo compatto con VIBRAZIONE LIVE in tempo reale.
 * - Formato lettere e tipografia moderna e raffinata.
 * - Swipe Down fluido e senza rimbalzi.
 * - Sfondo copertina nitido con blur leggero e bilanciato.
 * - Centratura verticale perfetta.
 */
@Composable
fun LiveDetailScreen(
    user: User,
    onClose: () -> Unit,
    onSendLiveReply: (User, String, Track) -> Unit,
    onOpenUserProfile: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val track = user.currentTrack ?: return
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var replyMessage by remember { mutableStateOf("") }
    val floatingReactions = remember { mutableStateListOf<FloatingReaction>() }

    // Pulsazione audio dinamica
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Minutaggio REALE: durata dal brano; posizione estrapolata dalla posizione
    // catturata (trackProgressMs) + tempo trascorso da trackProgressAt.
    val totalSeconds = remember(track.id, track.durationMs, track.durationText) {
        if (track.durationMs > 0L) (track.durationMs / 1000L).toInt()
        else track.durationText.split(":").let { p ->
            if (p.size == 2) (p[0].toIntOrNull() ?: 0) * 60 + (p[1].toIntOrNull() ?: 0) else 0
        }
    }.coerceAtLeast(1)

    var elapsedSeconds by remember(track.id, user.trackProgressAt) {
        val base = (user.trackProgressMs / 1000L).toInt()
        val drift = if (user.trackProgressAt > 0L)
            ((System.currentTimeMillis() - user.trackProgressAt) / 1000L).toInt() else 0
        mutableIntStateOf((base + drift).coerceIn(0, totalSeconds))
    }

    LaunchedEffect(track.id, user.trackProgressAt) {
        while (true) {
            delay(1000)
            if (elapsedSeconds < totalSeconds) elapsedSeconds += 1
        }
    }

    val elapsedFormatted = String.format("%d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    val totalFormatted = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)
    val progressFraction = (elapsedSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)

    // Stesso scaffold immersivo del dettaglio feed (fullscreen sopra la status bar,
    // swipe-giù da ovunque con blur-fade lineare, cover sfocata di sfondo, niente X),
    // ma con i CONTENUTI live: timer/progress reale, reazioni, "rispondi in live".
    com.example.ui.components.TrackDialog(coverUrl = track.coverUrl, onDismiss = onClose) {
      Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_detail_fullscreen")
    ) {
        // Overlay Scrim scuro per contrasto ottimale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BlackPitch.copy(alpha = 0.50f),
                            BlackPitch.copy(alpha = 0.78f),
                            BlackPitch.copy(alpha = 0.95f)
                        )
                    )
                )
        )


        // ================= 2. CONTENUTO VERTICALMENTE CENTRATO =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. HEADER: In ascolto da / @username
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text(
                    text = "In ascolto da",
                    color = PureWhite.copy(alpha = 0.70f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@${user.username.lowercase()}",
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onOpenUserProfile(user) }
                    )
                )
            }

            // 2. COVER ALBUM CENTRALE FIANCATA DA EQUALIZZATORI GRADIENTI GLOW
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Equalizzatore stilizzato a Sinistra (Cyan/Blue neon glow)
                LiveEqualizerSymmetrical(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF00E5FF), Color(0xFF0084FF))
                    ),
                    maxHeight = 60.dp,
                    barCount = 5,
                    isReversed = true,
                    modifier = Modifier.padding(end = 10.dp)
                )

                // Cover album centrale quadrata con angoli arrotondati
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale)
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

                // Equalizzatore stilizzato a Destra (Magenta/Pink/Sunset neon glow)
                LiveEqualizerSymmetrical(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFF007A), Color(0xFFFF5252), Color(0xFFFFBE0B))
                    ),
                    maxHeight = 60.dp,
                    barCount = 5,
                    isReversed = false,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            // 3. DATI DI RIPRODUZIONE DEL BRANO
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

                Spacer(modifier = Modifier.height(18.dp))

                // Barra Temporale con minutaggio monospace
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PureWhite,
                        trackColor = PureWhite.copy(alpha = 0.20f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = elapsedFormatted,
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = totalFormatted,
                            color = Zinc400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // 4. REACTION CUSTOM (Diamond, Soundwave, Star) E INPUT TESTUALE
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
                    com.example.ui.components.detailReactions.forEach { reaction ->
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {
                                        floatingReactions.add(
                                            FloatingReaction(
                                                id = System.currentTimeMillis(),
                                                reaction = reaction,
                                                initialXRatio = (0.2f + Math.random().toFloat() * 0.6f)
                                            )
                                        )
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            com.example.ui.components.CustomReactionIcon(
                                reaction = reaction,
                                tint = PureWhite,
                                size = 24.dp
                            )
                        }
                    }
                }

                // Input Testuale Sincronizzato Borderless
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
                        value = replyMessage,
                        onValueChange = { replyMessage = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("live_reply_input"),
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
                                if (replyMessage.isNotBlank()) {
                                    onSendLiveReply(user, replyMessage.trim(), track)
                                    replyMessage = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (replyMessage.isEmpty()) {
                                Text(
                                    text = "rispondi in live...",
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
                            if (replyMessage.isNotBlank()) {
                                onSendLiveReply(user, replyMessage.trim(), track)
                                replyMessage = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        },
                        enabled = replyMessage.isNotBlank(),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Invia",
                            tint = if (replyMessage.isNotBlank()) PureWhite else SubtitleGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Particelle reazioni
        floatingReactions.forEach { item ->
            FloatingReactionEffect(
                reaction = item.reaction,
                xRatio = item.initialXRatio,
                onFinished = { floatingReactions.remove(item) }
            )
        }
      }
    }
}

@Composable
private fun FloatingReactionEffect(
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
