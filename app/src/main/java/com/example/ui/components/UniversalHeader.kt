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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.ui.theme.SubtitleGray

enum class NavigationPage {
    FEED,
    LIVE
}

/**
 * Universal Top Header (Universale per entrambe le schermate: Feed e Live)
 * - Nessun bordo bianco/grigio: Fuso al 100% nel nero assoluto #000000.
 * - Blocco unico centrato: Logo "m" minuscola cerchiata stilizzata + scritta dinamica in corsivo minuscolo ("feed" / "live").
 * - Estrema sinistra: Icona di ricerca minimale.
 * - Estrema destra: Icona profilo utente.
 */
@Composable
fun UniversalHeader(
    currentPage: NavigationPage,
    currentUser: User,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit,
    onHeaderCenterClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("universal_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Estrema Sinistra: Icona di ricerca minimale borderless
        IconButton(
            onClick = onSearchClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .testTag("header_search_button")
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Cerca canzoni o utenti",
                tint = PureWhite,
                modifier = Modifier.size(22.dp)
            )
        }

        // Blocco Unico Centrato Compatto (Completamente borderless su sfondo nero)
        Box(
            modifier = Modifier
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onHeaderCenterClick
                )
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Logo minimale: "m" minuscola cerchiata stilizzata
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PureWhite),
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

                Spacer(modifier = Modifier.width(10.dp))

                // Scritta Dinamica in corsivo calligrafico elegante
                Text(
                    text = if (currentPage == NavigationPage.FEED) "feed" else "live",
                    color = PureWhite.copy(alpha = 0.95f),
                    fontSize = 27.sp,
                    fontWeight = FontWeight.Normal,
                    fontStyle = FontStyle.Italic,
                    fontFamily = FontFamily.Cursive,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 1.dp, bottom = 2.dp)
                )
            }
        }

        // Estrema Destra: Avatar / Icona Profilo (senza riquadro o sfondi grigi)
        IconButton(
            onClick = onProfileClick,
            modifier = Modifier
                .size(44.dp)
                .testTag("header_profile_button")
        ) {
            if (currentUser.avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = currentUser.avatarUrl,
                    contentDescription = "Profilo di ${currentUser.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profilo Utente",
                    tint = PureWhite,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
