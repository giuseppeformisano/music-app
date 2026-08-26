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
 * Pallino di stato utente. Tre stati:
 * - LIVE (isLive): sta ascoltando ora → pulsante verde↔rosso con alone (tipo "on air").
 * - ONLINE (isOnline && !isLive): app aperta ma non in ascolto → verde statico.
 * - OFFLINE (né online né live): grigio statico.
 * Il bordo scuro stacca il pallino dallo sfondo/avatar.
 */
@Composable
fun PresenceDot(
    presenceState: com.example.model.UserPresenceState,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    borderColor: Color = Color(0xFF080808)
) {
    PresenceDot(
        isOnline = presenceState == com.example.model.UserPresenceState.ONLINE || presenceState == com.example.model.UserPresenceState.LIVE,
        isLive = presenceState == com.example.model.UserPresenceState.LIVE,
        modifier = modifier,
        size = size,
        borderColor = borderColor
    )
}

@Composable
fun PresenceDot(
    isOnline: Boolean,
    isLive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    borderColor: Color = Color(0xFF080808)
) {
    val green = Color(0xFF1ED760)
    val red = Color(0xFFFF3B30)
    val grey = Color(0xFF8A94A6)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (isLive) {
            val transition = rememberInfiniteTransition(label = "presence_live")
            val pulse by transition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                label = "pulse"
            )
            val core = red // solo rosso lampeggiante
            val haloScale = 0.72f + pulse * 0.28f
            val haloAlpha = 0.40f * (1f - pulse) + 0.05f
            // Alone pulsante
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(haloScale)
                    .clip(CircleShape)
                    .background(core.copy(alpha = haloAlpha))
            )
            // Nucleo pieno rosso che pulsa
            Box(
                modifier = Modifier
                    .size(size * 0.62f)
                    .clip(CircleShape)
                    .background(core)
                    .border(size * 0.12f, borderColor, CircleShape)
            )
        } else {
            // Online (verde) oppure Offline (grigio), statico
            Box(
                modifier = Modifier
                    .size(size * 0.58f)
                    .clip(CircleShape)
                    .background(if (isOnline) green else grey)
                    .border(size * 0.12f, borderColor, CircleShape)
            )
        }
    }
}
