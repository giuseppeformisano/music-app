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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import com.example.R
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.model.Track
import com.example.model.User
import com.example.ui.components.CustomReaction
import com.example.ui.components.CustomReactionIcon
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
    onSendPulse: (User, String, String?) -> Unit = { _, _, _ -> },
    onSetTrackAsCover: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = user.currentTrack ?: return
    // Dettaglio brano (identico al dettaglio feed) aperto toccando la copertina dell'album
    var showTrackDetail by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var replyMessage by remember { mutableStateOf("") }
    val floatingReactions = remember { mutableStateListOf<FloatingReaction>() }

    // Colori dinamici estratti DIRETTAMENTE dalla COPERTINA DEL BRANO (non dal profilo)
    var dynamicColors by remember(track.id, track.accentColorHex, track.coverUrl) {
        mutableStateOf(extractDynamicTrackGlowColors(track))
    }

    LaunchedEffect(track.coverUrl) {
        if (track.coverUrl.isNotBlank()) {
            val request = ImageRequest.Builder(context)
                .data(track.coverUrl)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    dynamicColors = extractBitmapDominantColors(bitmap)
                }
            }
        }
    }

    val (primaryColor, secondaryColor) = dynamicColors

    // Piattaforma di ascolto (logo + nome) e dispositivo attivo
    val isAmazon = track.source == "amazon_music"
    val platformName = if (isAmazon) "Amazon Music" else "Spotify"
    val platformLogoRes = if (isAmazon) R.drawable.ic_amazon_music else R.drawable.ic_spotify
    val platformTextColor = if (isAmazon) Color(0xFF25D1DA) else Color(0xFF1DB954)
    val deviceIconVec = deviceIcon(track.deviceType)

    // ===== PULSE VOCALE: tieni premuta la foto e parla; vibrazione e onda seguono la tua voce =====
    val pulseAccent = com.example.ui.components.pulseAccentFor(user.id)
    var pulseRecording by remember { mutableStateOf(false) }
    val pulsePressed = remember { mutableStateOf(false) }
    // Intensità delle barre durante la registrazione = livello reale della voce dal microfono
    var pulseIntensity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(pulseRecording) {
        if (!pulseRecording) return@LaunchedEffect
        val recorder = com.example.data.PulseRecorder()
        val started = recorder.start(context)
        val amps = ArrayList<Int>()
        try {
            for (i in 0 until com.example.data.PulseHaptics.SAMPLE_COUNT) {
                kotlinx.coroutines.delay(com.example.data.PulseHaptics.SAMPLE_MS)
                // Se rilasci, termina — ma solo dopo ~1.2s minimi (i clip troppo corti non
                // producono un file AAC valido).
                if (!pulsePressed.value && i > 24) break
                val level = if (started) recorder.level() else 0f
                pulseIntensity = level
                amps.add((level * 255f).toInt().coerceIn(0, 255))
            }
        } catch (_: Exception) {}
        // Ferma e ottieni la voce in AAC base64 (verrà salvata su Firestore).
        val audioB64 = recorder.stopAndGetBase64()
        pulseIntensity = 0f
        pulseRecording = false
        // Pad fino a lunghezza piena (silenzio finale) e invia se c'è voce.
        while (amps.size < com.example.data.PulseHaptics.SAMPLE_COUNT) amps.add(0)
        val env = com.example.data.PulseHaptics.encodeEnvelope(amps.toIntArray())
        if (com.example.data.PulseHaptics.hasContent(env)) onSendPulse(user, env, audioB64)
    }

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

    com.example.ui.components.TrackDialog(coverUrl = track.coverUrl, onDismiss = onClose) {
      Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_detail_fullscreen")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BlackPitch.copy(alpha = 0.15f),
                            BlackPitch.copy(alpha = 0.35f),
                            BlackPitch.copy(alpha = 0.55f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Sezione Centrale Equidistanziata: Tag, Avatar+Equalizzatori, Player Brano
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 36.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. HEADER: LIVE @username
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onOpenUserProfile(user) }
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(primaryColor.copy(alpha = 0.25f))
                            .border(0.8.dp, primaryColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LIVE",
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "@${user.username.lowercase()}",
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                }

                // 2. ONDA AUDIO DINAMICA COLORATA (colori dall'artwork) + AVATAR AL CENTRO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiveAudioWave(
                        primary = primaryColor,
                        secondary = secondaryColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )

                    // Cerchio Avatar Profilo al centro dell'onda
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .shadow(
                                elevation = 22.dp,
                                shape = CircleShape,
                                ambientColor = primaryColor.copy(alpha = 0.45f),
                                spotColor = primaryColor.copy(alpha = 0.75f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = primaryColor.copy(alpha = 0.55f),
                                shape = CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141418))
                            // Tocca/tieni premuto la foto per INCIDERE un Pulse (5s). Il profilo
                            // si apre dal @username in alto.
                            .pointerInput(user.id) {
                                awaitEachGesture {
                                    awaitFirstDown(requireUnconsumed = false)
                                    pulsePressed.value = true
                                    if (!pulseRecording) pulseRecording = true
                                    waitForUpOrCancellation()
                                    pulsePressed.value = false
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.avatarUrl)
                                .crossfade(true)
                                .size(coil.size.Size.ORIGINAL)
                                .build(),
                            contentDescription = "Foto ${user.name}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Onda circolare di registrazione Pulse (visibile solo mentre incidi)
                    if (pulseRecording) {
                        com.example.ui.components.PulseCircleWave(
                            intensity = pulseIntensity,
                            accent = pulseAccent,
                            modifier = Modifier.size(250.dp)
                        ) {}
                    }
                }

                // 3. SEZIONE BRANO: Copertina + Titolo/Artista + Barra Temporale
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(10.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.5f),
                                    spotColor = Color.Black.copy(alpha = 0.8f)
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141418))
                                // Tocca la copertina per aprire il dettaglio brano (come nel feed)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showTrackDetail = true }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = track.coverUrl,
                                contentDescription = "Cover ${track.title}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            // Piattaforma: logo + nome (Spotify o Amazon Music)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Image(
                                    painter = painterResource(id = platformLogoRes),
                                    contentDescription = platformName,
                                    modifier = Modifier
                                        .size(15.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = platformName,
                                    color = platformTextColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = track.title,
                                color = PureWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = track.artist,
                                color = Zinc400,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Solo icona del dispositivo attivo
                        Icon(
                            imageVector = deviceIconVec,
                            contentDescription = "Dispositivo",
                            tint = PureWhite.copy(alpha = 0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Barra Temporale
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Barra a gradiente d'accento (fusa con lo sfondo)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(PureWhite.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(secondaryColor, primaryColor)
                                        )
                                    )
                            )
                        }

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
            }

            // 4. REACTION CUSTOM (Diamond, Soundwave, Star) E INPUT TESTUALE — GIÙ A TUTTO
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Barra Reaction (Diamond, Soundwave, Star)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.example.ui.components.detailReactions.forEach { reaction ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
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
                                size = 23.dp
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
                        .padding(horizontal = 16.dp, vertical = 11.dp),
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

    // Dettaglio brano identico al dettaglio feed (stesse info + pulsante "Imposta come copertina")
    if (showTrackDetail) {
        com.example.ui.components.TrackDetailDialog(
            track = track,
            user = user,
            onDismiss = { showTrackDetail = false },
            onSendTextMessage = { u, text, trk -> onSendLiveReply(u, text, trk) },
            onOpenUserProfile = { u -> onOpenUserProfile(u) },
            onSetAsCover = { trk -> onSetTrackAsCover(trk) }
        )
    }
}

/**
 * Onda audio dinamica e colorata dietro l'avatar. Più linee sovrapposte con envelope
 * (più alta al centro, sfuma ai bordi), animate in continuo (fase + ampiezza pulsante).
 * I colori derivano dall'artwork del brano (primary/secondary) con un tocco caldo.
 */
@Composable
private fun LiveAudioWave(primary: Color, secondary: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val ampPulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ampPulse"
    )
    val brush = Brush.horizontalGradient(
        listOf(secondary, primary, Color(0xFFFFC24B), primary, secondary)
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val midY = h / 2f
        val layers = 6
        val steps = 100
        for (i in 0 until layers) {
            val path = Path()
            val amp = (h * 0.30f) * (1f - i * 0.11f) * ampPulse
            val freq = 2.0f + i * 0.7f
            val layerPhase = phase * (1f + i * 0.15f) + i * 0.6f
            for (s in 0..steps) {
                val t = s.toFloat() / steps
                val x = w * t
                val env = kotlin.math.sin(Math.PI * t).toFloat() // 0 ai bordi, 1 al centro
                val y = midY + amp * env * kotlin.math.sin(freq * 2f * Math.PI.toFloat() * t + layerPhase)
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                brush = brush,
                style = Stroke(width = 2.2f, cap = StrokeCap.Round),
                alpha = (0.55f - i * 0.07f).coerceAtLeast(0.12f)
            )
        }
    }
}

/** Mappa il tipo device Spotify (o il telefono per notifiche) alla relativa icona Material. */
private fun deviceIcon(type: String): ImageVector = when (type.lowercase()) {
    "smartphone" -> Icons.Filled.Smartphone
    "computer" -> Icons.Filled.Computer
    "speaker" -> Icons.Filled.Speaker
    "tv" -> Icons.Filled.Tv
    "automobile" -> Icons.Filled.DirectionsCar
    "gameconsole" -> Icons.Filled.SportsEsports
    "castvideo", "castaudio" -> Icons.Filled.Cast
    "avr", "stb", "audiodongle" -> Icons.Filled.Headphones
    else -> Icons.Filled.Devices
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

/**
 * Estrazione dinamica in tempo reale della palette cromatica per gli equalizzatori live:
 * - Estrae il colore base dalla palette della copertina/traccia (accentColorHex).
 * - Genera una sfumatura secondaria ricca ed elegante per un alone tridimensionale.
 */
private fun extractDynamicTrackGlowColors(track: Track): Pair<Color, Color> {
    val baseHex = track.accentColorHex
    val primary = if (baseHex != 0L && baseHex != 0xFF1DB954) {
        Color(baseHex)
    } else {
        when (track.genre.lowercase()) {
            "progressive rock", "rock" -> Color(0xFFE040FB)
            "ambient", "synthwave" -> Color(0xFF00E5FF)
            "r&b", "soul" -> Color(0xFFFF9100)
            "dream pop / synthpop" -> Color(0xFF7928CA)
            "r&b / experimental" -> Color(0xFF00DF89)
            "french house" -> Color(0xFF0070F3)
            "darkwave / synthpop" -> Color(0xFFE000FF)
            "electro house" -> Color(0xFFFFBE0B)
            "psychedelic rock", "indie" -> Color(0xFFFF5252)
            else -> {
                val hash = kotlin.math.abs((track.title + track.artist + track.id).hashCode())
                when (hash % 8) {
                    0 -> Color(0xFFFF0055)
                    1 -> Color(0xFF7928CA)
                    2 -> Color(0xFF00E5FF)
                    3 -> Color(0xFFFF9900)
                    4 -> Color(0xFF0070F3)
                    5 -> Color(0xFFE000FF)
                    6 -> Color(0xFFFFBE0B)
                    else -> Color(0xFF00E676)
                }
            }
        }
    }

    val secondary = when (primary) {
        Color(0xFFFF0055) -> Color(0xFF9333EA)
        Color(0xFF7928CA) -> Color(0xFF00E5FF)
        Color(0xFF00E5FF) -> Color(0xFF0070F3)
        Color(0xFFFF9100) -> Color(0xFFFF3366)
        Color(0xFF0070F3) -> Color(0xFF00E5FF)
        Color(0xFFE000FF) -> Color(0xFF4F46E5)
        Color(0xFFFFBE0B) -> Color(0xFFF97316)
        Color(0xFF00DF89) -> Color(0xFF10B981)
        Color(0xFFFF5252) -> Color(0xFF9C27B0)
        else -> {
            val r = (primary.red * 0.75f + primary.blue * 0.25f).coerceIn(0f, 1f)
            val g = (primary.green * 0.45f + primary.red * 0.55f).coerceIn(0f, 1f)
            val b = (primary.blue * 0.85f + primary.green * 0.15f).coerceIn(0f, 1f)
            Color(r, g, b, 1f)
        }
    }

    return Pair(primary, secondary)
}

/**
 * Estrae i colori predominanti VIVACI e LUMINOSI direttamente dai pixel del bitmap della copertina del brano.
 * Favorisce fortemente i colori saturi e brillanti (quelli della copertina vera) rispetto
 * ai toni scuri e spenti (che verrebbero dalla versione blurrata/sfondo).
 */
private fun extractBitmapDominantColors(bitmap: android.graphics.Bitmap): Pair<Color, Color> {
    val w = bitmap.width
    val h = bitmap.height
    val stepX = (w / 16).coerceAtLeast(1)
    val stepY = (h / 16).coerceAtLeast(1)

    data class ColorCandidate(val pixel: Int, val score: Float, val hue: Float)

    val candidates = mutableListOf<ColorCandidate>()
    val hsv = FloatArray(3)

    for (x in 0 until w step stepX) {
        for (y in 0 until h step stepY) {
            val pixel = bitmap.getPixel(x, y)
            val alpha = (pixel ushr 24) and 0xFF
            if (alpha < 128) continue

            val r = (pixel ushr 16) and 0xFF
            val g = (pixel ushr 8) and 0xFF
            val b = pixel and 0xFF

            android.graphics.Color.RGBToHSV(r, g, b, hsv)
            val sat = hsv[1]
            val value = hsv[2]

            // Filtra solo pixel luminosi e saturi: esclude neri, grigi, bianchi puri
            // Value >= 0.35 esclude i toni scuri del background blurrato
            // Sat >= 0.25 esclude i grigi e i colori desaturati
            if (value >= 0.35f && value <= 0.98f && sat >= 0.25f) {
                // Score: forte peso su saturazione e luminosità per favorire i colori vivaci
                val score = sat * 0.55f + value * 0.35f + (1f - kotlin.math.abs(value - 0.7f)) * 0.10f
                candidates.add(ColorCandidate(pixel, score, hsv[0]))
            }
        }
    }

    if (candidates.isEmpty()) {
        return Pair(Color(0xFF00E5FF), Color(0xFF0070F3))
    }

    // Ordina per score decrescente e prendi il migliore
    candidates.sortByDescending { it.score }
    val best1 = candidates[0]

    // Per il secondo colore, cerca uno con hue sufficientemente diversa (>30°)
    val best2 = candidates.firstOrNull { c ->
        c.pixel != best1.pixel &&
        kotlin.math.abs(c.hue - best1.hue).let { diff ->
            kotlin.math.min(diff, 360f - diff)
        } > 30f
    } ?: candidates.getOrNull(1)

    // Post-process: aumenta saturazione e luminosità per garantire colori vividi
    fun boostColor(pixel: Int): Color {
        val hsvBoost = FloatArray(3)
        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        android.graphics.Color.RGBToHSV(r, g, b, hsvBoost)
        // Aumenta saturazione a minimo 0.55 e luminosità a minimo 0.55
        hsvBoost[1] = hsvBoost[1].coerceAtLeast(0.55f)
        hsvBoost[2] = hsvBoost[2].coerceAtLeast(0.55f)
        val boosted = android.graphics.Color.HSVToColor(hsvBoost)
        return Color(boosted)
    }

    val prim = boostColor(best1.pixel)
    val sec = if (best2 != null) {
        boostColor(best2.pixel)
    } else {
        val r = (prim.red * 0.75f + prim.blue * 0.25f).coerceIn(0f, 1f)
        val g = (prim.green * 0.45f + prim.red * 0.55f).coerceIn(0f, 1f)
        val b = (prim.blue * 0.85f + prim.green * 0.15f).coerceIn(0f, 1f)
        Color(r, g, b, 1f)
    }

    return Pair(prim, sec)
}
