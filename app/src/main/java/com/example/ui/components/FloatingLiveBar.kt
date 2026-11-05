package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateStart
import androidx.compose.foundation.layout.calculateEnd
import androidx.compose.foundation.layout.calculateBottom
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SpotifyGreen

/**
 * A responsive Floating Live Bar component that adapts to different screen sizes and resolutions.
 * This component displays the currently playing track information and is designed to
 * avoid being cut off on smaller screens or older devices.
 *
 * Key responsive features:
 * 1. Uses WindowInsets for safe area padding to prevent clipping
 * 2. Adaptive padding based on screen density
 * 3. Proper constraints and layout weights
 * 4. Responsive text sizing
 * 5. Visibility handling for different screen configurations
 */
@Composable
fun FloatingLiveBar(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use WindowInsets for safe area padding to prevent cutting off
    val windowInsets = WindowInsets.safeDrawing.asPaddingValues()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BlackPitch.copy(alpha = 0.9f))
            // Adaptive padding for different screen sizes and safe areas
            .padding(
                start = maxOf(12.dp, windowInsets.calculateStart()),
                end = maxOf(12.dp, windowInsets.calculateEnd()),
                top = 12.dp,
                bottom = maxOf(12.dp, windowInsets.calculateBottom())
            )
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art - with proper constraints for different screen densities
        AsyncImage(
            model = track.coverUrl,
            contentDescription = "Track Cover",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Track Info - with proper text truncation and responsive sizing
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track.artist,
                color = PureWhite.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Status indicator - always visible and properly sized
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(SpotifyGreen)
                .clip(CircleShape)
        )
    }
}