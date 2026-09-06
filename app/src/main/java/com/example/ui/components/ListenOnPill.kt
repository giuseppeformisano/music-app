package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track

/** Piattaforma di streaming con colore brand e builder dell'URI di apertura. */
internal data class ListenPlatform(
    val label: String,
    val short: String,
    val color: Color,
    val buildUri: (Track) -> Uri
)

internal val listenPlatforms = listOf(
    ListenPlatform("Spotify", "Spotify", Color(0xFF1DB954)) { track ->
        Uri.parse("spotify:search:${Uri.encode("${track.title} ${track.artist}")}")
    },
    ListenPlatform("Apple Music", "Apple", Color(0xFFFC3D3D)) { track ->
        Uri.parse("https://music.apple.com/search?term=${Uri.encode("${track.title} ${track.artist}")}")
    },
    ListenPlatform("Amazon", "Amazon", Color(0xFF00A8E1)) { track ->
        Uri.parse("amznmp3://search?phrase=${Uri.encode("${track.title} ${track.artist}")}")
    },
    ListenPlatform("YT Music", "YT", Color(0xFFFF3B3B)) { track ->
        Uri.parse("https://music.youtube.com/search?q=${Uri.encode("${track.title} ${track.artist}")}")
    }
)

private fun openPlatform(context: android.content.Context, platform: ListenPlatform, track: Track) {
    val uri = platform.buildUri(track)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    val resolved = context.packageManager.queryIntentActivities(intent, 0)
    if (resolved.isNotEmpty()) {
        context.startActivity(intent)
    } else if (platform.label == "Amazon") {
        val q = Uri.encode("${track.title} ${track.artist}")
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://music.amazon.com/search/$q")))
    } else {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

/**
 * Pill segmentata "ascolta su": una riga con tutte le piattaforme, la sorgente
 * evidenziata col colore brand pieno, le altre ghost. Compatta e social.
 */
@Composable
internal fun ListenOnPill(track: Track, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF17171C))
            .padding(4.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listenPlatforms.forEach { platform ->
            val isSource = when (platform.label) {
                "Spotify" -> track.source.contains("spotify", ignoreCase = true)
                "Amazon" -> track.source.contains("amazon", ignoreCase = true)
                else -> false
            }
            Text(
                text = platform.short,
                color = if (isSource) platform.color else PureWhiteRef.copy(alpha = 0.55f),
                fontSize = 12.sp,
                fontWeight = if (isSource) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .then(
                        if (isSource) Modifier
                            .background(platform.color.copy(alpha = 0.15f))
                            .border(0.6.dp, platform.color.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        else Modifier
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { openPlatform(context, platform, track) }
                    )
                    .padding(vertical = 9.dp)
            )
        }
    }
}

private val PureWhiteRef = Color(0xFFFFFFFF)
