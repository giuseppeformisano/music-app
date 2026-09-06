package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.Track
import kotlin.math.cos
import kotlin.math.sin

/** Piattaforma di streaming: logo brand, colore, e builder dell'URI di apertura. */
internal data class ListenPlatform(
    val label: String,
    val color: Color,
    val iconRes: Int,
    val buildUri: (Track) -> Uri
)

internal val listenPlatforms = listOf(
    ListenPlatform("Spotify", Color(0xFF2EE06A), R.drawable.ic_brand_spotify) { track ->
        Uri.parse("spotify:search:${Uri.encode("${track.title} ${track.artist}")}")
    },
    ListenPlatform("Amazon Music", Color(0xFF2BC4F0), R.drawable.ic_brand_amazon_music) { track ->
        Uri.parse("amznmp3://search?phrase=${Uri.encode("${track.title} ${track.artist}")}")
    },
    ListenPlatform("YouTube", Color(0xFFFF6B6B), R.drawable.ic_brand_youtube) { track ->
        Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode("${track.title} ${track.artist}")}")
    }
)

private fun openPlatform(context: android.content.Context, platform: ListenPlatform, track: Track) {
    val uri = platform.buildUri(track)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val resolved = context.packageManager.queryIntentActivities(intent, 0)
    if (resolved.isNotEmpty()) {
        context.startActivity(intent)
    } else if (platform.label == "Amazon Music") {
        val q = Uri.encode("${track.title} ${track.artist}")
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://music.amazon.com/search/$q")))
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

/**
 * Pulsante play che, al tap, apre le piattaforme in una mezzaluna a 180° SOTTO il play.
 * Ogni logo brand ha un alone luminoso (glow) del proprio colore, tutto fuso col fondo.
 */
@Composable
internal fun ListenOnPill(track: Track, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 440, easing = FastOutSlowInEasing),
        label = "fanProgress"
    )

    val radiusDp = 78f
    val n = listenPlatforms.size

    // Altezza: play (56) + apertura verso il basso (radius + icona)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Icone brand che si aprono a ventaglio sotto
        listenPlatforms.forEachIndexed { i, platform ->
            val angle = Math.PI * (i.toFloat() / (n - 1).toFloat())   // 0..180°
            val targetX = (-cos(angle) * radiusDp).toFloat()          // sinistra→destra
            val targetY = (sin(angle) * radiusDp).toFloat()           // verso il basso
            val itemScale = 0.4f + 0.6f * progress

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (targetX * progress).dp.roundToPx(),
                            y = (targetY * progress).dp.roundToPx() + 4.dp.roundToPx()
                        )
                    }
                    .size(46.dp)
                    .alpha(progress)
                    .graphicsLayer { scaleX = itemScale; scaleY = itemScale }
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = expanded,
                        onClick = {
                            openPlatform(context, platform, track)
                            expanded = false
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Glow: alone radiale del colore brand
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    platform.color.copy(alpha = 0.55f),
                                    platform.color.copy(alpha = 0.0f)
                                )
                            ),
                            shape = CircleShape
                        )
                )
                Icon(
                    painter = painterResource(id = platform.iconRes),
                    contentDescription = platform.label,
                    tint = platform.color,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Pulsante play centrale — vetro fuso col fondo
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(0.5.dp, Color.White.copy(alpha = 0.14f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = !expanded }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.PlayArrow,
                contentDescription = if (expanded) "Chiudi" else "Ascolta su",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer { rotationZ = progress * 90f }
            )
        }
    }
}
