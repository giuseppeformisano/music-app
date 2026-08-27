package com.example.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatMessage
import com.example.model.Track
import com.example.model.User
import com.example.ui.components.LiveEqualizerBadge
import com.example.ui.theme.BlackCard
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SubtitleGray

@Composable
fun ChatScreen(
    recipient: User,
    currentUser: User,
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenProfile: (User) -> Unit,
    applyCoverToFeed: Boolean = false,
    backgroundCoverUrl: String? = null,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Paginazione lato client: si mostra solo un blocco di messaggi (i più recenti);
    // scrollando verso l'alto se ne caricano altri, un blocco per volta.
    val pageSize = 30
    var visibleCount by remember(recipient.id) { mutableStateOf(pageSize) }
    val shown = remember(messages, visibleCount) {
        if (messages.size <= visibleCount) messages else messages.takeLast(visibleCount)
    }

    // Nuovo messaggio (la lista totale cresce) → scorri in fondo
    LaunchedEffect(messages.size) {
        if (shown.isNotEmpty()) listState.animateScrollToItem(shown.size - 1)
    }
    // Scroll verso l'alto → carica un altro blocco più vecchio
    LaunchedEffect(recipient.id) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { idx -> if (idx <= 1 && visibleCount < messages.size) visibleCount += pageSize }
    }

    val myPalette = remember(currentUser.id, currentUser.coverUrl) {
        paletteForUser(currentUser, isCurrent = true)
    }
    val otherPalette = remember(recipient.id, recipient.coverUrl) {
        paletteForUser(recipient, isCurrent = false)
    }

    com.example.ui.components.UtilityDialog(onDismiss = onDismiss) {
        Box(modifier = modifier.fillMaxSize().background(BlackPitch)) {
            // Sfondo atmosferico opzionale (stessa immagine settabile delle altre sezioni)
            if (applyCoverToFeed && !backgroundCoverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = backgroundCoverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = 1.15f; scaleY = 1.15f }
                        .blur(radius = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    BlackPitch.copy(alpha = 0.55f),
                                    BlackPitch.copy(alpha = 0.78f),
                                    BlackPitch.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp)
                    .navigationBarsPadding()
                    .testTag("chat_screen_${recipient.id}")
            ) {
                // Top Header minimale: allineato ai pulsanti del profilo utente (si chiude con lo slide down)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenProfile(recipient) }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        AsyncImage(
                            model = recipient.avatarUrl,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = recipient.name,
                                color = PureWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (recipient.isLiveNow) {
                                    LiveEqualizerBadge(color = SpotifyGreen, height = 8.dp)
                                    Text(
                                        text = "Live su Spotify",
                                        color = SpotifyGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                } else {
                                    Text(
                                        text = "@${recipient.username}",
                                        color = SubtitleGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Messages List
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(shown, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            palette = if (message.isFromMe) myPalette else otherPalette,
                            senderAvatarUrl = if (message.isFromMe) currentUser.avatarUrl else recipient.avatarUrl
                        )
                    }
                }

                // Bottom Pure Text Input Bar (posizionato dinamicamente sopra la barra di navigazione e tastiera)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text("Commenta la musica...", color = SubtitleGray, fontSize = 13.sp)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            }
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PureWhite,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedContainerColor = DarkGraphite,
                            unfocusedContainerColor = DarkGraphite,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            cursorColor = SpotifyGreen
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (inputText.isNotBlank()) PureWhite else DarkGraphite)
                            .clickable(enabled = inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Invia messaggio",
                            tint = if (inputText.isNotBlank()) BlackPitch else SubtitleGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Palette cromatica per l'utente (coppia di colori contrastanti ed eleganti):
 * - Se l'utente ha la copertina/sfondo: i due colori sono derivati dallo sfondo ma con rotazioni
 *   cromatiche e saturazioni ben distinte tra mittente e destinatario.
 * - Se l'utente NON ha la copertina: genera due colori distinti e contrastanti con offset calibrati,
 *   evitando colori troppo spenti o troppo uguali.
 */
data class UserChatPalette(
    val primaryBorder: Color,
    val secondaryBorder: Color,
    val bubbleBackground: Color
)

private fun paletteForUser(user: User, isCurrent: Boolean): UserChatPalette {
    val cover = user.coverUrl
    if (!cover.isNullOrBlank()) {
        // Deriva tonalità primaria e secondaria dalla copertina con forte contrasto angolare
        val coverHash = kotlin.math.abs(cover.hashCode())
        val baseHue = (coverHash % 360).toFloat()
        // Se è l'utente corrente usiamo la dominante e l'accento analogo (+45°),
        // se è l'altro usiamo il polo complementare (+180°) e il suo accento (+225°)
        val h1 = if (isCurrent) baseHue else (baseHue + 180f) % 360f
        val h2 = if (isCurrent) (baseHue + 55f) % 360f else (baseHue + 235f) % 360f

        val color1 = Color(android.graphics.Color.HSVToColor(floatArrayOf(h1, 0.65f, 0.95f)))
        val color2 = Color(android.graphics.Color.HSVToColor(floatArrayOf(h2, 0.52f, 0.88f)))
        val bg = if (isCurrent) DarkGraphite.copy(alpha = 0.96f) else BlackCard.copy(alpha = 0.96f)

        return UserChatPalette(
            primaryBorder = color1,
            secondaryBorder = color2,
            bubbleBackground = bg
        )
    }

    // Utente SENZA copertina: palette a due colori nettamente separata per mittente e destinatario
    val idHash = kotlin.math.abs(user.id.ifBlank { if (isCurrent) "me" else "other" }.hashCode())
    // Offset di 170° garantito tra i due interlocutori
    val baseHue = ((idHash + if (isCurrent) 10 else 180) % 360).toFloat()
    val contrastHue = (baseHue + 45f) % 360f

    // Saturazione e luminosità vivaci ma armoniose (non fluo)
    val color1 = Color(android.graphics.Color.HSVToColor(floatArrayOf(baseHue, 0.58f, 0.92f)))
    val color2 = Color(android.graphics.Color.HSVToColor(floatArrayOf(contrastHue, 0.48f, 0.86f)))
    val bg = if (isCurrent) DarkGraphite else BlackCard

    return UserChatPalette(
        primaryBorder = color1,
        secondaryBorder = color2,
        bubbleBackground = bg
    )
}

@Composable
private fun MessageBubble(message: ChatMessage, palette: UserChatPalette, senderAvatarUrl: String = "") {
    val isMe = message.isFromMe

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        // Attached Track Card if present
        message.attachedTrack?.let { track ->
            Row(
                modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BlackCard)
                    .border(0.7.dp, CharcoalBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = track.title,
                        color = PureWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = track.artist,
                        color = SubtitleGray,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Pure Minimalist Text Bubble con bordi ultra-sottili (0.7.dp) e colori dedicati
        val bubbleShape = RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomStart = if (isMe) 18.dp else 4.dp,
            bottomEnd = if (isMe) 4.dp else 18.dp
        )

        if (message.pulse != null) {
            // Bubble PULSE: tocca per risentire onda + vibrazione
            var pulseTrigger by remember(message.id) { mutableStateOf(0) }
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(palette.bubbleBackground)
                    .border(
                        width = 0.8.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                palette.primaryBorder.copy(alpha = 0.90f),
                                palette.secondaryBorder.copy(alpha = 0.75f)
                            )
                        ),
                        shape = bubbleShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { pulseTrigger++ }
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.PulseWavePlayer(
                        samples = message.pulse,
                        accent = palette.primaryBorder,
                        playTrigger = pulseTrigger,
                        modifier = Modifier.size(56.dp),
                        barCount = 28
                    ) {
                        AsyncImage(
                            model = senderAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = "Pulse", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = "tocca per risentire", color = PureWhite.copy(alpha = 0.5f), fontSize = 10.sp)
                    }
                }
            }
        } else {
        Box(
            modifier = Modifier
                .clip(bubbleShape)
                .background(palette.bubbleBackground)
                .border(
                    width = 0.8.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            palette.primaryBorder.copy(alpha = 0.90f),
                            palette.secondaryBorder.copy(alpha = 0.75f)
                        )
                    ),
                    shape = bubbleShape
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Orario accorpato DENTRO la bubble, in basso a destra (stile WhatsApp)
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = message.text,
                    color = PureWhite,
                    fontSize = 14.sp,
                    lineHeight = 19.sp
                )
                Text(
                    text = message.formattedTime,
                    color = PureWhite.copy(alpha = 0.45f),
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 3.dp, start = 20.dp)
                )
            }
        }
        }
    }
}
