package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Testo su singola riga che, se troppo lungo per la larghezza disponibile, scorre
 * orizzontalmente con effetto "rimbalzo":
 * fermo all'inizio → scorri (rallenta a fine) → pausa → torna indietro (rallenta all'inizio)
 * → pausa → si ripete. Se il testo ci sta, resta statico.
 */
@Composable
fun BouncingMarqueeText(
    text: String,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier,
    letterSpacing: TextUnit = 0.sp
) {
    val scroll = remember { Animatable(0f) }
    var textWidth by remember(text) { mutableIntStateOf(0) }
    var boxWidth by remember { mutableIntStateOf(0) }

    LaunchedEffect(text, textWidth, boxWidth) {
        scroll.snapTo(0f)
        val overflow = (textWidth - boxWidth).toFloat()
        if (overflow > 1f) {
            val dur = (overflow * 14f).toInt().coerceIn(1800, 9000)
            while (true) {
                delay(2200)                                                      // fermo all'inizio
                scroll.animateTo(-overflow, tween(dur, easing = FastOutSlowInEasing)) // scorri, rallenta a fine
                delay(1200)                                                      // rimbalzo lento a fine
                scroll.animateTo(0f, tween(dur, easing = FastOutSlowInEasing))   // torna indietro
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { boxWidth = it.size.width }
    ) {
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .offset { IntOffset(scroll.value.roundToInt(), 0) }
                .onGloballyPositioned { textWidth = it.size.width }
        )
    }
}
