package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
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
    val coroutineScope = rememberCoroutineScope()

    var replyMessage by remember { mutableStateOf("") }
    val floatingReactions = remember { mutableStateListOf<FloatingReaction>() }

    // Interattività Swipe Down: transizione fisica lineare fluida senza rimbalzo
    val dragOffsetY = remember { Animatable(0f) }

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

    val currentDrag = dragOffsetY.value
    val dragFraction = (currentDrag / 500f).coerceIn(0f, 1f)
    val contentScale = 1f - (dragFraction * 0.08f)
    val backgroundDim = (1f - (dragFraction * 0.7f)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BlackPitch)
            // Gesture Swipe Down fluida
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffsetY.value > 80f) {
                            coroutineScope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 900f,
                                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                                )
                                onClose()
                            }
                        } else {
                            coroutineScope.launch {
                                dragOffsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            dragOffsetY.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
                            )
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = (dragOffsetY.value + dragAmount.y).coerceAtLeast(0f)
                        coroutineScope.launch {
                            dragOffsetY.snapTo(newOffset)
                        }
                    }
                )
            }
            .graphicsLayer {
                translationY = dragOffsetY.value
                scaleX = contentScale
                scaleY = contentScale
                alpha = backgroundDim
            }
            .testTag("live_detail_fullscreen")
    ) {
        // ================= 1. COPERTINA SFONDO CON BLUR LEGGERO =================
        AsyncImage(
            model = track.coverUrl,
            contentDescription = "Sfondo ${track.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 8.dp)
        )

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

        // Maniglia indicatrice per lo swipe down
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp)
                .size(width = 38.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(PureWhite.copy(alpha = 0.35f))
        )

        // ================= 2. CONTENUTO VERTICALMENTE CENTRATO =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // PROFILO UTENTE COMPATTO CON VIBRAZIONE LIVE
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Equalizzatore stilizzato a Sinistra
                    LiveEqualizerSymmetrical(
                        color = PureWhite,
                        maxHeight = 32.dp,
                        barCount = 5,
                        isReversed = true,
                        modifier = Modifier.padding(end = 16.dp)
                    )

                    // Avatar profilo
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onOpenUserProfile(user) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = "Foto ${user.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Equalizzatore stilizzato a Destra
                    LiveEqualizerSymmetrical(
                        color = PureWhite,
                        maxHeight = 32.dp,
                        barCount = 5,
                        isReversed = false,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nome Utente e @nickname pulito, fuso con lo sfondo nero #000000
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = user.name.lowercase(),
                        color = PureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.2.sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "@${user.username.lowercase()}",
                        color = PureWhite.copy(alpha = 0.65f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // DATI DI RIPRODUZIONE DEL BRANO
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = track.title,
                    color = PureWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.6).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = track.artist.uppercase(),
                    color = Zinc400,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.8.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(28.dp))

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
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = PureWhite,
                        trackColor = PureWhite.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = elapsedFormatted,
                            color = PureWhite,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = totalFormatted,
                            color = Zinc400,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(34.dp))

            // REACTION CUSTOM E INPUT TESTUALE
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Barra Reaction Borderless
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(PureWhite.copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    allCustomReactions.forEach { reaction ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
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
                            CustomReactionIcon(
                                reaction = reaction,
                                tint = PureWhite,
                                size = 22.dp
                            )
                        }
                    }
                }

                // Input Testuale Sincronizzato Borderless
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(PureWhite.copy(alpha = 0.08f))
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
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (replyMessage.isNotBlank()) PureWhite else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Invia",
                            tint = if (replyMessage.isNotBlank()) BlackPitch else SubtitleGray,
                            modifier = Modifier.size(16.dp)
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
