package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import com.example.data.PulseHaptics
import kotlin.math.cos
import kotlin.math.sin

/** Colore d'accento stabile e distinto per persona, derivato dall'id (tonalità HSV). */
fun pulseAccentFor(id: String): Color {
    if (id.isBlank()) return Color(0xFF7C4DFF)
    val hue = (kotlin.math.abs(id.hashCode()) % 360).toFloat()
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.62f, 0.98f)))
}

/**
 * Corona di barrette attorno a un contenuto centrale (foto profilo). Le barre hanno una
 * micro-oscillazione lenta e casuale (idle) e "scattano" con [intensity] (0..1). Usata sia in
 * registrazione (intensity = pressione), sia in riproduzione (intensity = campione corrente).
 */
@Composable
fun PulseCircleWave(
    intensity: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 44,
    content: @Composable BoxScope.() -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulseIdle")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val inner = size.minDimension * 0.40f
            val maxBar = size.minDimension * 0.12f
            for (k in 0 until barCount) {
                val ang = (2.0 * Math.PI * k / barCount).toFloat()
                val idle = 0.16f + 0.14f * (0.5f + 0.5f * sin(phase + k * 1.7f))
                val amp = (idle + intensity * (0.55f + 0.45f * sin(phase * 1.3f + k))).coerceIn(0f, 1f)
                val len = maxBar * amp + maxBar * 0.15f
                val sx = cx + cos(ang) * inner
                val sy = cy + sin(ang) * inner
                val ex = cx + cos(ang) * (inner + len)
                val ey = cy + sin(ang) * (inner + len)
                drawLine(
                    color = accent.copy(alpha = 0.45f + 0.55f * amp),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = 3.2f,
                    cap = StrokeCap.Round
                )
            }
        }
        content()
    }
}

/**
 * Riproduce un Pulse: a ogni cambio di [playTrigger] (>0) fa partire vibrazione + onda
 * perfettamente sincronizzate (stessi campioni). Idle quando non sta riproducendo.
 */
@Composable
fun PulseWavePlayer(
    samples: String,
    accent: Color,
    playTrigger: Int,
    modifier: Modifier = Modifier,
    barCount: Int = 44,
    audioFilePath: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    var intensity by remember { mutableFloatStateOf(0f) }
    val envelope = remember(samples) { PulseHaptics.decodeEnvelope(samples) }

    LaunchedEffect(playTrigger) {
        if (playTrigger <= 0 || envelope.isEmpty()) return@LaunchedEffect
        val totalMs = envelope.size * PulseHaptics.SAMPLE_MS
        // Audio (voce) + vibrazione + onda partono dallo STESSO istante; l'indice del campione
        // è ricavato dal TEMPO REALE trascorso → nessuna deriva, tutto sincronizzato.
        var mp: android.media.MediaPlayer? = null
        try {
            if (audioFilePath != null) {
                mp = try {
                    android.media.MediaPlayer().apply {
                        setDataSource(audioFilePath)
                        prepare()
                        start()
                    }
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Pulse: voce non riproducibile (${e.javaClass.simpleName})", android.widget.Toast.LENGTH_SHORT).show()
                    null
                }
            }
            PulseHaptics.play(context, samples)
            val startNanos = System.nanoTime()
            while (true) {
                val frame = withFrameNanos { it }
                val elapsedMs = (frame - startNanos) / 1_000_000L
                if (elapsedMs >= totalMs) break
                val idx = (elapsedMs / PulseHaptics.SAMPLE_MS).toInt().coerceIn(0, envelope.size - 1)
                // Intensità = ampiezza del campione (voce), salita immediata e discesa morbida.
                val target = envelope[idx] / 255f
                intensity = if (target >= intensity) target else intensity * 0.6f
            }
        } finally {
            intensity = 0f
            mp?.let { try { it.stop() } catch (_: Exception) {}; try { it.release() } catch (_: Exception) {} }
        }
    }

    PulseCircleWave(
        intensity = intensity,
        accent = accent,
        modifier = modifier,
        barCount = barCount,
        content = content
    )
}
