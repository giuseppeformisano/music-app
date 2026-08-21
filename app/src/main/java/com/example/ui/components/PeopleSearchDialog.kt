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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.User
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray

private val RowBg = Color(0xFF0E0E0E)
private val FieldBg = Color(0xFF111111)

@Composable
fun PeopleSearchDialog(
    searchQuery: String,
    searchResults: List<User>,
    sentRequestIds: Set<String>,
    followingIds: Set<String> = emptySet(),
    onQueryChanged: (String) -> Unit,
    onSendRequest: (User) -> Unit,
    onOpenProfile: (User) -> Unit,
    onDismiss: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    UtilityDialog(onDismiss = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.78f)
                    .padding(horizontal = 4.dp)
            ) {
                Text(
                    text = "Cerca persone",
                    color = PureWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onQueryChanged,
                    placeholder = { Text("Nome o @username", color = SubtitleGray.copy(alpha = 0.6f), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SubtitleGray.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancella", tint = SubtitleGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboardController?.hide(); focusManager.clearFocus()
                    }),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PureWhite.copy(alpha = 0.15f),
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = FieldBg,
                        unfocusedContainerColor = FieldBg,
                        focusedTextColor = PureWhite,
                        unfocusedTextColor = PureWhite,
                        cursorColor = PureWhite
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isBlank()) "Cerca qualcuno per nome o username"
                                   else "Nessun risultato per \"$searchQuery\"",
                            color = SubtitleGray.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(searchResults, key = { it.id }) { user ->
                            val alreadyFollowing = followingIds.contains(user.id)
                            val requested = sentRequestIds.contains(user.id)
                            PeopleResultRow(
                                user = user,
                                alreadyFollowing = alreadyFollowing,
                                requested = requested,
                                onSendRequest = { onSendRequest(user) },
                                onOpenProfile = { onDismiss(); onOpenProfile(user) }
                            )
                        }
                    }
                }
            }
        }
    }

@Composable
private fun PeopleResultRow(
    user: User,
    alreadyFollowing: Boolean,
    requested: Boolean,
    onSendRequest: () -> Unit,
    onOpenProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(RowBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onOpenProfile
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(42.dp)) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
            )
            PresenceDot(
                isOnline = user.isOnline,
                isLive = user.currentTrack != null,
                size = 13.dp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
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
                Text(
                    text = "Segui",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = if (alreadyFollowing) "Segui già" else "Inviata",
                color = SubtitleGray.copy(alpha = 0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
