package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.User
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.ElectricPink
import com.example.ui.theme.IndigoVivid
import com.example.ui.theme.OrangeVivid
import com.example.ui.theme.PureWhite
import com.example.ui.theme.PurpleVivid
import com.example.ui.theme.RedVivid
import com.example.ui.theme.SunsetYellow
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc700
import com.example.ui.theme.Zinc800

@Composable
fun StoryAvatarCircle(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dynamicColor = Color(user.currentTrack?.accentColorHex ?: 0xFF1DB954)

    // Editorial gradient list based on user ID / live status
    val gradientBrush = when {
        user.isCurrentUser -> Brush.linearGradient(listOf(ElectricPink, SunsetYellow))
        user.id.hashCode() % 3 == 0 -> Brush.linearGradient(listOf(IndigoVivid, PurpleVivid))
        user.id.hashCode() % 3 == 1 -> Brush.linearGradient(listOf(OrangeVivid, RedVivid))
        else -> Brush.linearGradient(listOf(dynamicColor, ElectricPink))
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val borderPulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 6.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 34.dp),
                onClick = onClick
            )
            .testTag("story_avatar_${user.id}")
    ) {
        // Editorial Circular container (w-16 h-16 rounded-full p-[2px] bg-gradient)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .scale(if (user.isLiveNow) borderPulse else 1f)
                .clip(CircleShape)
                .background(if (user.isLiveNow) gradientBrush else Brush.linearGradient(listOf(Zinc700, Zinc800)))
                .padding(2.dp) // 2px outer gradient gap
        ) {
            // Inner 2px black ring wrapper (bg-black p-[2px])
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(BlackPitch)
                    .padding(2.dp)
            ) {
                // Inner Avatar Image
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "Avatar di ${user.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Zinc800)
                )

                // Small live equalizer badge
                if (user.isLiveNow) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 1.dp, bottom = 1.dp)
                    ) {
                        LiveEqualizerBadge(color = dynamicColor, height = 9.dp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (user.isCurrentUser) "YOU" else user.name.split(" ").firstOrNull()?.uppercase() ?: user.name.uppercase(),
            color = if (user.isCurrentUser) PureWhite else Zinc400,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
