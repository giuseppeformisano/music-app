package com.example.ui.screens

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.Conversation
import com.example.model.User
import com.example.ui.components.PresenceDot
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.DarkGraphite
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray

@Composable
fun ChatListScreen(
    conversations: List<Conversation>,
    currentUserId: String,
    searchQuery: String,
    searchResults: List<User>,
    onSearchQueryChanged: (String) -> Unit,
    onOpenChat: (User) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val isSearching = searchQuery.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BlackPitch)
            .padding(top = 28.dp)
            .navigationBarsPadding()
            .testTag("chat_list_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkGraphite)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Torna indietro",
                    tint = PureWhite,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Messaggi",
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp
            )
        }

        // Barra di ricerca: cerca una persona a cui scrivere (click → apre la chat)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            placeholder = { Text("Cerca una persona a cui scrivere", color = SubtitleGray.copy(alpha = 0.6f), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = SubtitleGray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                if (isSearching) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancella", tint = SubtitleGray, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide(); focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PureWhite.copy(alpha = 0.15f),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = PureWhite.copy(alpha = 0.06f),
                unfocusedContainerColor = PureWhite.copy(alpha = 0.06f),
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                cursorColor = PureWhite
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        when {
            // Modalità ricerca: mostra gli utenti trovati, click → apri chat
            isSearching -> {
                if (searchResults.isEmpty()) {
                    EmptyState(title = "Nessuna persona trovata", subtitle = "Prova con un altro nome o @username")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults, key = { it.id }) { user ->
                            UserResultRow(user = user, onClick = { onOpenChat(user) })
                        }
                    }
                }
            }
            conversations.isEmpty() -> {
                EmptyState(
                    title = "Nessuna conversazione",
                    subtitle = "Cerca una persona qui sopra, oppure rispondi a un brano o a una live"
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            currentUserId = currentUserId,
                            onClick = { onOpenChat(conversation.recipientUser) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = title, color = SubtitleGray, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                color = SubtitleGray.copy(alpha = 0.6f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun UserResultRow(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar di ${user.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            PresenceDot(
                presenceState = user.presenceState,
                size = 13.dp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                color = PureWhite,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${user.username}",
                color = SubtitleGray.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit
) {
    val user = conversation.recipientUser
    val timeFormatted = if (conversation.lastMessageAt > 0L) {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(conversation.lastMessageAt))
    } else ""

    val previewPrefix = if (conversation.lastMessageSenderId == currentUserId) "Tu: " else ""
    val preview = previewPrefix + conversation.lastMessageText

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = "Avatar di ${user.name}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.name,
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (timeFormatted.isNotBlank()) {
                    Text(
                        text = timeFormatted,
                        color = SubtitleGray.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (conversation.lastAttachedTrack != null) {
                    AsyncImage(
                        model = conversation.lastAttachedTrack.coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = preview,
                    color = SubtitleGray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
