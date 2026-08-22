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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
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
                    val palette = androidx.palette.graphics.Palette.from(bitmap).generate()
                    val domInt = palette.getVibrantColor(
                        palette.getDominantColor(
                            palette.getLightVibrantColor(
                                palette.getDarkVibrantColor(0)
                            )
                        )
                    )
                    if (domInt != 0) {
                        val prim = Color(domInt)
                        val secInt = palette.getLightVibrantColor(
                            palette.getMutedColor(
                                palette.getDarkVibrantColor(0)
                            )
                        )
                        val sec = if (secInt != 0 && secInt != domInt) Color(secInt) else {
                            val r = (prim.red * 0.75f + prim.blue * 0.25f).coerceIn(0f, 1f)
                            val g = (prim.green * 0.45f + prim.red * 0.55f).coerceIn(0f, 1f)
                            val b = (prim.blue * 0.85f + prim.green * 0.15f).coerceIn(0f, 1f)
                            Color(r, g, b, 1f)
                        }
                        dynamicColors = Pair(prim, sec)
                    }
                }
            }
        }
    }

    val (primaryColor, secondaryColor) = dynamicColors

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
                            BlackPitch.copy(alpha = 0.50f),
                            BlackPitch.copy(alpha = 0.78f),
                            BlackPitch.copy(alpha = 0.95f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Sezione Superiore: Header LIVE @username + Avatar Profilo con Equalizzatori Sincronizzati
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // Header: LIVE @username
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Avatar Profilo Circolare con Cerchio Luminoso + Equalizzatori
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Equalizzatore stilizzato a Sinistra (due colori dalla palette della copertina)
                    LiveEqualizerSymmetrical(
                        brush = Brush.verticalGradient(
                            listOf(primaryColor, secondaryColor)
                        ),
                        maxHeight = 64.dp,
                        barCount = 5,
                        isReversed = true,
                        modifier = Modifier.padding(end = 18.dp)
                    )

                    // Cerchio Avatar Profilo nitido (~190dp) con cerchio/anello luminoso nel colore predominante della copertina
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .shadow(
                                elevation = 24.dp,
                                shape = CircleShape,
                                ambientColor = primaryColor.copy(alpha = 0.45f),
                                spotColor = primaryColor.copy(alpha = 0.75f)
                            )
                            .border(
                                width = 3.dp,
                                color = primaryColor,
                                shape = CircleShape
                            )
                            .padding(4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF141418))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onOpenUserProfile(user) }
                            ),
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

                    // Equalizzatore stilizzato a Destra (due colori dalla palette della copertina)
                    LiveEqualizerSymmetrical(
                        brush = Brush.verticalGradient(
                            listOf(secondaryColor, primaryColor)
                        ),
                        maxHeight = 64.dp,
                        barCount = 5,
                        isReversed = false,
                        modifier = Modifier.padding(start = 18.dp)
                    )
                }
            }

            // 3. SEZIONE BRANO: Copertina quadrata a sinistra + Titolo e Artista a destra + Slide temporale
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Riga Copertina a sinistra + Titolo/Artista subito a destra
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(10.dp),
                                ambientColor = Color.Black.copy(alpha = 0.5f),
                                spotColor = Color.Black.copy(alpha = 0.8f)
                            )
                            .clip(RoundedCornerShape(10.dp))
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

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = track.title,
                            color = PureWhite,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = track.artist,
                            color = Zinc400,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Barra Temporale con colore predominante della copertina
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = primaryColor,
                        trackColor = PureWhite.copy(alpha = 0.15f)
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
