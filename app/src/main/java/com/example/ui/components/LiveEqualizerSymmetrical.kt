package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PureWhite

/**
 * Barrette verticali stilizzate a forma di equalizzatore che decorano e si muovono
 * ai lati (sinistra o destra) della foto profilo.
 */
@Composable
fun LiveEqualizerSymmetrical(
    color: Color = PureWhite,
    brush: androidx.compose.ui.graphics.Brush? = null,
    maxHeight: Dp = 32.dp,
    barCount: Int = 5,
    isReversed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "symmetrical_equalizer")

    val anim1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val anim2 by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val anim3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val anim4 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )
    val anim5 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar5"
    )

    // Base profile: taller near inner side (towards the cover), shorter towards outer edge
    val shapeFactors = if (isReversed) {
        listOf(0.40f, 0.60f, 0.80f, 0.92f, 1.0f)
    } else {
        listOf(1.0f, 0.92f, 0.80f, 0.60f, 0.40f)
    }

    val rawWeights = listOf(anim1, anim2, anim3, anim4, anim5)
    val weights = if (isReversed) rawWeights.reversed() else rawWeights

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(3.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        weights.take(barCount).forEachIndexed { idx, fraction ->
            val factor = shapeFactors.getOrElse(idx) { 0.7f }
            val barHeight = (maxHeight * factor * (0.35f + 0.65f * fraction)).coerceAtLeast(6.dp)
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.5.dp))
                    .then(
                        if (brush != null) Modifier.background(brush)
                        else Modifier.background(color)
                    )
            )
        }
    }
}
