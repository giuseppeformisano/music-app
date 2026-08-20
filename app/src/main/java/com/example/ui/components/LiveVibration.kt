package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Modificatore per far VIBRARE i nomi dei profili live in tempo reale,
 * simulando l'energia e la frequenza attiva dell'ascolto contemporaneo.
 */
@Composable
fun Modifier.liveNameVibration(enabled: Boolean = true): Modifier {
    if (!enabled) return this

    val infiniteTransition = rememberInfiniteTransition(label = "live_name_vibration")

    // Vibrazione rapida asse X a micro-ampiezza
    val jitterX by infiniteTransition.animateFloat(
        initialValue = -1.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(90, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vib_x"
    )

    // Vibrazione rapida asse Y asincrona
    val jitterY by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = -0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "vib_y"
    )

    // Micro-pulsazione di scala
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(220, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale_pulse"
    )

    return this.graphicsLayer {
        translationX = jitterX
        translationY = jitterY
        scaleX = scalePulse
        scaleY = scalePulse
    }
}
