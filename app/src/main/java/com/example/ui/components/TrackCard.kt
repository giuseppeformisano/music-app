package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite
import com.example.ui.theme.White10
import com.example.ui.theme.White20
import com.example.ui.theme.White30
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc900

@Composable
fun TrackCard(
    track: Track,
    user: User,
    onClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 256.dp,
    cardHeight: Dp = 256.dp
) {
    val dynamicAccent = Color(track.accentColorHex)

    Box(
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(18.dp))
            .background(Zinc900)
            .border(
                width = 1.dp,
                color = White10,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = PureWhite.copy(alpha = 0.15f)),
                onClick = onClick
            )
            .testTag("track_card_${track.id}")
    ) {
        // High-res Album Art covering full card
        AsyncImage(
            model = track.coverUrl,
            contentDescription = "Cover di ${track.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
        )

        // Editorial diagonal gradient overlay (from dynamic / indigo tint to transparent)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            dynamicAccent.copy(alpha = 0.45f),
                            Color.Transparent,
                            BlackPitch.copy(alpha = 0.4f),
                            BlackPitch.copy(alpha = 0.92f)
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )

        // Top-Left: Frosted user avatar backdrop (w-7 h-7 rounded-full bg-white/20 backdrop-blur-md border border-white/30)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(White20)
                .border(1.dp, White30, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onUserClick
                )
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar di ${user.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        // Bottom-Left: Editorial Typography (text-lg font-bold leading-tight & text-sm font-medium text-zinc-400)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = track.title,
                color = PureWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                color = Zinc400,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
