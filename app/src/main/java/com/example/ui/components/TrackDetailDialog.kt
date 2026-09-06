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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wallpaper
import kotlinx.coroutines.delay
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
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import com.example.model.Track
import com.example.model.User
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray
import com.example.ui.theme.Zinc400
import androidx.compose.ui.platform.LocalContext

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
    onShareToMyFeed: (Track) -> Unit = {},
    isMyTrack: Boolean = false,
    onDeleteTrack: ((Track) -> Unit)? = null,
    onSetAsCover: ((Track) -> Unit)? = null,
    appListenersCount: Int = 0,
    myListenCount: Int = 0,
    globalListeners: List<User> = emptyList()
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var commentText by remember { mutableStateOf("") }
    val floatingReactions = remember { mutableStateListOf<FloatingFeedReaction>() }
    var sessionReactionCount by remember { mutableStateOf(0) }
    var showPlatformSheet by remember { mutableStateOf(false) }

    // Colori dinamici estratti dalla COPERTINA VERA del brano (non dal background blurrato)
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

    TrackDialog(coverUrl = track.coverUrl, onDismiss = onDismiss, swipeAnywhere = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Sezione Centrale Equidistanziata (Header, Cover Grande, Info Brano)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 36.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 1. HEADER: Avatar piccolo + @username (centrato)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (user != null) {
                                        onOpenUserProfile(user)
                                        onDismiss()
                                    }
                                }
                            )
                    ) {
                        if (user?.avatarUrl?.isNotBlank() == true) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = "Avatar ${user.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .border(0.8.dp, PureWhite.copy(alpha = 0.4f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = "@${user?.username?.lowercase() ?: "utente"}",
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                    }

                    // 2. COVER ALBUM PIÙ GRANDE AL CENTRO
                    Box(
                        modifier = Modifier
                            .size(265.dp)
                            .shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = Color.Black.copy(alpha = 0.6f),
                                spotColor = Color.Black.copy(alpha = 0.85f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF141418)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = "Cover ${track.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Pulsante modesto "Imposta come copertina" in basso a destra della cover.
                        // Dopo il tap conferma con "✓ Fatto" per ~1.4s, poi torna allo stato normale.
                        if (onSetAsCover != null) {
                            var coverSet by remember(track.id) { mutableStateOf(false) }
                            LaunchedEffect(coverSet) {
                                if (coverSet) { delay(1400); coverSet = false }
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(0.7.dp, PureWhite.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            if (!coverSet) {
                                                onSetAsCover(track)
                                                coverSet = true
                                            }
                                        }
                                    )
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (coverSet) Icons.Default.Check else Icons.Default.Wallpaper,
                                    contentDescription = null,
                                    tint = PureWhite.copy(alpha = 0.9f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (coverSet) "Fatto" else "Imposta copertina",
                                    color = PureWhite.copy(alpha = 0.9f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // 3. TITOLO, ARTISTA / ALBUM E PILL GENERE / ANNO
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = track.title,
                            color = PureWhite,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            letterSpacing = (-0.3).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(3.dp))

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
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

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
                                    .padding(horizontal = 11.dp, vertical = 4.dp)
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
                                        .padding(horizontal = 11.dp, vertical = 4.dp)
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

                        Spacer(modifier = Modifier.height(14.dp))

                        // Listener globali — carosello avatar chi ascolta ora
                        if (globalListeners.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val visibleG = globalListeners.take(6)
                                val extraG = (globalListeners.size - 6).coerceAtLeast(0)
                                val avDp = 24; val stepDp = 18
                                val slots = visibleG.size + if (extraG > 0) 1 else 0
                                Box(
                                    modifier = Modifier
                                        .width((avDp + (slots - 1) * stepDp).dp)
                                        .height(avDp.dp)
                                ) {
                                    visibleG.forEachIndexed { idx, u ->
                                        Box(
                                            modifier = Modifier
                                                .offset(x = (idx * stepDp).dp)
                                                .size(avDp.dp)
                                                .border(1.5.dp, Color(0xFF0A0A0A), CircleShape)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1A1A1E))
                                        ) {
                                            AsyncImage(
                                                model = u.avatarUrl,
                                                contentDescription = u.username,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    if (extraG > 0) {
                                        Box(
                                            modifier = Modifier
                                                .offset(x = (visibleG.size * stepDp).dp)
                                                .size(avDp.dp)
                                                .border(1.5.dp, Color(0xFF0A0A0A), CircleShape)
                                                .clip(CircleShape)
                                                .background(Color(0xFF2A2A2E)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+$extraG",
                                                color = PureWhite.copy(alpha = 0.7f),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "ascoltano ora",
                                    color = PureWhite.copy(alpha = 0.4f),
                                    fontSize = 11.sp,
                                    letterSpacing = 0.2.sp
                                )
                            }
                        }

                        // Chip singolo "Ascolta su ▸" — apre bottom sheet con le piattaforme
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1A1A1F))
                                .border(0.8.dp, PureWhite.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showPlatformSheet = true }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "▸  ascolta su...",
                                color = PureWhite.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mini stat row: tuoi ascolti + reazioni sessione
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (myListenCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF141418).copy(alpha = 0.85f))
                                        .border(0.75.dp, Color(0xFFF0B429).copy(alpha = 0.4f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$myListenCount ascolti",
                                        color = Color(0xFFF0B429),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            if (sessionReactionCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color(0xFF141418).copy(alpha = 0.85f))
                                        .border(0.75.dp, PureWhite.copy(alpha = 0.2f), CircleShape)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "+$sessionReactionCount",
                                        color = PureWhite.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. REACTION CUSTOM (Diamond, Soundwave, Star) E INPUT TESTUALE COMMENTO GIÙ A TUTTO
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
                        detailReactions.forEach { reaction ->
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
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
                                            sessionReactionCount++
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CustomReactionIcon(
                                    reaction = reaction,
                                    tint = PureWhite,
                                    size = 23.dp
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
                            .padding(horizontal = 16.dp, vertical = 11.dp),
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

            // Pulsante Cestino in alto a destra (sotto la status bar)
            if (isMyTrack && onDeleteTrack != null) {
                IconButton(
                    onClick = {
                        onDeleteTrack(track)
                        onDismiss()
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 10.dp, end = 16.dp) // Stesso padding del profilo (16.dp laterale, 10.dp dall'alto)
                        .size(44.dp) 
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Elimina canzone dal feed",
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
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

    // Bottom sheet piattaforme streaming
    if (showPlatformSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showPlatformSheet = false },
            containerColor = Color(0xFF111116),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Apri su",
                    color = PureWhite.copy(alpha = 0.45f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                platforms.forEach { platform ->
                    val isSource = when (platform.label) {
                        "Spotify" -> track.source.contains("spotify", ignoreCase = true)
                        "Amazon" -> track.source.contains("amazon", ignoreCase = true)
                        else -> false
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSource) platform.color.copy(alpha = 0.12f)
                                else Color(0xFF1A1A1F)
                            )
                            .border(
                                0.8.dp,
                                if (isSource) platform.color.copy(alpha = 0.5f) else PureWhite.copy(alpha = 0.08f),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showPlatformSheet = false
                                val uri = platform.buildUri(track)
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                val resolved = context.packageManager.queryIntentActivities(intent, 0)
                                if (resolved.isNotEmpty()) {
                                    context.startActivity(intent)
                                } else {
                                    if (platform.label == "Amazon") {
                                        val q = Uri.encode("${track.title} ${track.artist}")
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://music.amazon.com/search/$q")))
                                    } else {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = platform.label,
                            color = if (isSource) platform.color else PureWhite.copy(alpha = 0.75f),
                            fontSize = 15.sp,
                            fontWeight = if (isSource) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isSource) {
                            Text(
                                text = "sorgente",
                                color = platform.color.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                }
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

/**
 * Estrazione dinamica in tempo reale della palette cromatica per il dettaglio feed:
 * - Estrae il colore base dalla palette della copertina/traccia (accentColorHex).
 * - Genera una sfumatura secondaria ricca ed elegante.
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

private data class StreamingPlatform(
    val label: String,
    val color: Color,
    val buildUri: (Track) -> Uri
)

private val platforms = listOf(
    StreamingPlatform("Spotify", Color(0xFF1DB954)) { track ->
        val query = Uri.encode("${track.title} ${track.artist}")
        Uri.parse("spotify:search:$query")
    },
    StreamingPlatform("Apple Music", Color(0xFFFC3D3D)) { track ->
        val query = Uri.encode("${track.title} ${track.artist}")
        Uri.parse("https://music.apple.com/search?term=$query")
    },
    StreamingPlatform("Amazon", Color(0xFF00A8E1)) { track ->
        val query = Uri.encode("${track.title} ${track.artist}")
        Uri.parse("amznmp3://search?phrase=$query").let { amzUri ->
            amzUri
        }
    },
    StreamingPlatform("YT Music", Color(0xFFFF0000)) { track ->
        val query = Uri.encode("${track.title} ${track.artist}")
        Uri.parse("https://music.youtube.com/search?q=$query")
    }
)

@Composable
private fun PlatformButtons(track: Track, context: android.content.Context) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        platforms.forEach { platform ->
            val isSource = when (platform.label) {
                "Spotify" -> track.source.contains("spotify", ignoreCase = true)
                "Amazon" -> track.source.contains("amazon", ignoreCase = true)
                else -> false
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSource) platform.color.copy(alpha = 0.25f)
                        else Color(0xFF141418).copy(alpha = 0.75f)
                    )
                    .border(
                        width = if (isSource) 0.8.dp else 0.6.dp,
                        color = if (isSource) platform.color.copy(alpha = 0.7f) else PureWhite.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        val uri = platform.buildUri(track)
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        val resolved = context.packageManager.queryIntentActivities(intent, 0)
                        if (resolved.isNotEmpty()) {
                            context.startActivity(intent)
                        } else {
                            // Fallback web per Amazon Music
                            if (platform.label == "Amazon") {
                                val query = Uri.encode("${track.title} ${track.artist}")
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://music.amazon.com/search/$query"))
                                )
                            } else {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                            }
                        }
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = platform.label,
                    color = if (isSource) platform.color else PureWhite.copy(alpha = 0.55f),
                    fontSize = 10.sp,
                    fontWeight = if (isSource) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
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

            if (value >= 0.35f && value <= 0.98f && sat >= 0.25f) {
                val score = sat * 0.55f + value * 0.35f + (1f - kotlin.math.abs(value - 0.7f)) * 0.10f
                candidates.add(ColorCandidate(pixel, score, hsv[0]))
            }
        }
    }

    if (candidates.isEmpty()) {
        return Pair(Color(0xFF00E5FF), Color(0xFF0070F3))
    }

    candidates.sortByDescending { it.score }
    val best1 = candidates[0]

    val best2 = candidates.firstOrNull { c ->
        c.pixel != best1.pixel &&
        kotlin.math.abs(c.hue - best1.hue).let { diff ->
            kotlin.math.min(diff, 360f - diff)
        } > 30f
    } ?: candidates.getOrNull(1)

    fun boostColor(pixel: Int): Color {
        val hsvBoost = FloatArray(3)
        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        android.graphics.Color.RGBToHSV(r, g, b, hsvBoost)
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
