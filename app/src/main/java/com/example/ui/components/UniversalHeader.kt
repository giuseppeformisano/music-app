package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.User
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite

enum class NavigationPage {
    FEED,
    LIVE
}

private val HeaderIconTint = PureWhite.copy(alpha = 0.9f)
private val BadgeBg = Color(0xFFE53935)

/**
 * Top bar delle pagine principali (feed/live): icone distribuite equamente intorno al logo
 * centrale. Ricerca · Notifiche — [logo m + feed/live, tocca per cambiare pagina] — Messaggi · Profilo.
 * Niente barra in basso: tutta la navigazione è qui in alto.
 */
@Composable
fun UniversalHeader(
    currentPage: NavigationPage,
    currentUser: User,
    onHeaderCenterClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onMessagesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    unreadMessages: Int = 0,
    unreadNotifications: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("universal_header"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Gruppo sinistro (peso uguale → logo centrato)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIcon(Icons.Default.Search, "Cerca", onClick = onSearchClick)
            HeaderIcon(Icons.Default.Notifications, "Notifiche", badge = unreadNotifications, onClick = onNotificationsClick)
        }

        // Logo centrale: tocca per alternare feed/live
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHeaderCenterClick
                )
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Box(
                    modifier = Modifier.size(24.dp).clip(CircleShape).background(PureWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "m",
                        color = BlackPitch,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentPage == NavigationPage.FEED) "feed" else "live",
                    color = PureWhite.copy(alpha = 0.95f),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Cursive,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
                )
            }
        }

        // Gruppo destro (peso uguale, allineato a fine)
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderIcon(Icons.Default.Email, "Messaggi", badge = unreadMessages, onClick = onMessagesClick)
            // Profilo = avatar dell'utente (fallback: icona persona)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onProfileClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser.avatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = currentUser.avatarUrl,
                        contentDescription = "Profilo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profilo",
                        tint = HeaderIconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderIcon(
    imageVector: ImageVector,
    contentDescription: String,
    badge: Int = 0,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = HeaderIconTint,
            modifier = Modifier.size(24.dp)
        )
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(if (badge > 9) 16.dp else 13.dp)
                    .clip(CircleShape)
                    .background(BadgeBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (badge > 99) "99+" else badge.toString(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.sp
                )
            }
        }
    }
}
