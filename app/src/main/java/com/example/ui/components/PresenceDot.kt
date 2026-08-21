package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Pallino di stato utente. Due stati distinti:
 * - LIVE (isLive): sta ascoltando qualcosa in questo momento → verde pulsante con alone.
 * - ONLINE (isOnline && !isLive): connesso e sta usando l'app ma non in ascolto → grigio statico.
 * - Offline: nulla.
 * Il bordo scuro stacca il pallino dallo sfondo/avatar.
 */
@Composable
fun PresenceDot(
    isOnline: Boolean,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    borderColor: Color = Color(0xFF080808)
) {
    if (!isOnline && !isLive) return

    val liveColor = Color(0xFF1ED760)   // verde Spotify — in ascolto
    val onlineColor = Color(0xFF8A94A6) // grigio-azzurro — connesso ma non in ascolto

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (isLive) {
            val transition = rememberInfiniteTransition(label = "presence_live")
            val haloScale by transition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "halo"
            )
            val haloAlpha by transition.animateFloat(
                initialValue = 0.45f,
                targetValue = 0.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "halo_alpha"
            )
            // Alone pulsante
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(liveColor.copy(alpha = haloAlpha))
            )
            // Nucleo pieno
            Box(
                modifier = Modifier
                    .size(size * 0.62f)
                    .clip(CircleShape)
                    .background(liveColor)
                    .border(size * 0.12f, borderColor, CircleShape)
            )
        } else {
            // Connesso, non in ascolto: pallino statico più piccolo
            Box(
                modifier = Modifier
                    .size(size * 0.58f)
                    .clip(CircleShape)
                    .background(onlineColor)
                    .border(size * 0.12f, borderColor, CircleShape)
            )
        }
    }
}
