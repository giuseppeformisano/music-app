package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.PureWhite

sealed class CustomReaction(val id: String, val label: String) {
    object Diamond : CustomReaction("diamond", "Diamond")
    object Soundwave : CustomReaction("soundwave", "Vibe")
    object Star : CustomReaction("star", "Star")
    object Flame : CustomReaction("flame", "Fire")
    object Vinyl : CustomReaction("vinyl", "Vinyl")
    object HeartPulse : CustomReaction("heart_pulse", "Pulse")
    object Spark : CustomReaction("spark", "Energy")
}

val detailReactions = listOf(
    CustomReaction.Diamond,
    CustomReaction.Soundwave,
    CustomReaction.Star
)

val allCustomReactions = listOf(
    CustomReaction.Diamond,
    CustomReaction.Soundwave,
    CustomReaction.Star,
    CustomReaction.Flame,
    CustomReaction.Vinyl,
    CustomReaction.HeartPulse,
    CustomReaction.Spark
)

@Composable
fun CustomReactionIcon(
    reaction: CustomReaction,
    tint: Color = PureWhite,
    size: Dp = 22.dp,
    modifier: Modifier = Modifier
) {
    when (reaction) {
        is CustomReaction.Diamond -> DiamondIcon(tint = tint, size = size, modifier = modifier)
        is CustomReaction.Soundwave -> SoundwaveIcon(tint = tint, size = size, modifier = modifier)
        is CustomReaction.Star -> StarIcon(tint = tint, size = size, modifier = modifier)
        is CustomReaction.Flame -> FlameIcon(tint = tint, size = size, modifier = modifier)
        is CustomReaction.Vinyl -> VinylIcon(tint = tint, size = size, modifier = modifier)
        is CustomReaction.HeartPulse -> HeartPulseIcon(tint = tint, size = size, modifier = modifier)
        is CustomReaction.Spark -> SparkIcon(tint = tint, size = size, modifier = modifier)
    }
}

@Composable
fun FlameIcon(tint: Color = PureWhite, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // Outer contour flame
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.65f, h * 0.25f, w * 0.9f, h * 0.45f, w * 0.85f, h * 0.7f)
            cubicTo(w * 0.8f, h * 0.92f, w * 0.65f, h * 0.98f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.35f, h * 0.98f, w * 0.2f, h * 0.92f, w * 0.15f, h * 0.7f)
            cubicTo(w * 0.1f, h * 0.48f, w * 0.35f, h * 0.3f, w * 0.4f, h * 0.15f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Inner flame ember
        val innerPath = Path().apply {
            moveTo(w * 0.5f, h * 0.55f)
            cubicTo(w * 0.6f, h * 0.65f, w * 0.65f, h * 0.78f, w * 0.5f, h * 0.88f)
            cubicTo(w * 0.35f, h * 0.78f, w * 0.4f, h * 0.65f, w * 0.5f, h * 0.55f)
            close()
        }
        drawPath(path = innerPath, color = tint)
    }
}

@Composable
fun SoundwaveIcon(tint: Color = PureWhite, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()

        val barX = listOf(0.15f, 0.32f, 0.50f, 0.68f, 0.85f)
        val barHeights = listOf(0.40f, 0.75f, 0.95f, 0.65f, 0.35f)

        barX.forEachIndexed { index, xRatio ->
            val barH = h * barHeights[index]
            val topY = (h - barH) / 2f
            drawLine(
                color = tint,
                start = Offset(w * xRatio, topY),
                end = Offset(w * xRatio, topY + barH),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun VinylIcon(tint: Color = PureWhite, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = (this.size.width / 2f) * 0.9f
        val strokeW = 1.6.dp.toPx()

        // Outer disc
        drawCircle(color = tint, radius = radius, center = center, style = Stroke(width = strokeW))

        // Middle groove
        drawCircle(color = tint.copy(alpha = 0.5f), radius = radius * 0.65f, center = center, style = Stroke(width = 1.dp.toPx()))

        // Center hub
        drawCircle(color = tint, radius = radius * 0.22f, center = center)
    }
}

@Composable
fun HeartPulseIcon(tint: Color = PureWhite, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.8.dp.toPx()

        // Heart contour
        val heart = Path().apply {
            moveTo(w * 0.5f, h * 0.88f)
            cubicTo(w * 0.1f, h * 0.60f, w * 0.05f, h * 0.25f, w * 0.28f, h * 0.15f)
            cubicTo(w * 0.40f, h * 0.10f, w * 0.48f, h * 0.22f, w * 0.5f, h * 0.30f)
            cubicTo(w * 0.52f, h * 0.22f, w * 0.60f, h * 0.10f, w * 0.72f, h * 0.15f)
            cubicTo(w * 0.95f, h * 0.25f, w * 0.90f, h * 0.60f, w * 0.5f, h * 0.88f)
            close()
        }
        drawPath(path = heart, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Pulse line inside
        val pulse = Path().apply {
            moveTo(w * 0.25f, h * 0.48f)
            lineTo(w * 0.42f, h * 0.48f)
            lineTo(w * 0.48f, h * 0.32f)
            lineTo(w * 0.54f, h * 0.64f)
            lineTo(w * 0.60f, h * 0.48f)
            lineTo(w * 0.75f, h * 0.48f)
        }
        drawPath(path = pulse, color = tint, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun SparkIcon(tint: Color = PureWhite, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        val star = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            cubicTo(w * 0.5f, h * 0.35f, w * 0.65f, h * 0.5f, w * 0.95f, h * 0.5f)
            cubicTo(w * 0.65f, h * 0.5f, w * 0.5f, h * 0.65f, w * 0.5f, h * 0.95f)
            cubicTo(w * 0.5f, h * 0.65f, w * 0.35f, h * 0.5f, w * 0.05f, h * 0.5f)
            cubicTo(w * 0.35f, h * 0.5f, w * 0.5f, h * 0.35f, w * 0.5f, h * 0.05f)
            close()
        }
        drawPath(path = star, color = tint)
    }
}

@Composable
fun DiamondIcon(tint: Color = PureWhite, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = 1.6.dp.toPx()

        // Contorno diamante
        val topY = h * 0.22f
        val botY = h * 0.86f
        val midLeftX = w * 0.12f
        val midRightX = w * 0.88f
        val topLeftX = w * 0.28f
        val topRightX = w * 0.72f
        val tipX = w * 0.50f
        val midY = h * 0.44f

        val outline = Path().apply {
            moveTo(topLeftX, topY)
            lineTo(topRightX, topY)
            lineTo(midRightX, midY)
            lineTo(tipX, botY)
            lineTo(midLeftX, midY)
            close()
        }
        drawPath(
            path = outline,
            color = tint,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Linee sfaccettature interne
        val innerLines = Path().apply {
            // Cintura orizzontale
            moveTo(midLeftX, midY)
            lineTo(midRightX, midY)

            // Sfaccettatura superiore sinistra
            moveTo(topLeftX, topY)
            lineTo(w * 0.38f, midY)
            lineTo(tipX, botY)

            // Sfaccettatura superiore destra
            moveTo(topRightX, topY)
            lineTo(w * 0.62f, midY)
            lineTo(tipX, botY)

            // Triangolo centrale superiore
            moveTo(topLeftX, topY)
            lineTo(tipX, midY)
            lineTo(topRightX, topY)
        }
        drawPath(
            path = innerLines,
            color = tint.copy(alpha = 0.85f),
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun StarIcon(tint: Color = PureWhite, size: Dp = 24.dp, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerR = (w / 2f) * 0.90f
        val innerR = outerR * 0.42f
        val starPath = Path()
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = Math.toRadians((i * 36.0 - 90.0))
            val x = cx + (r * kotlin.math.cos(angle)).toFloat()
            val y = cy + (r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) starPath.moveTo(x, y) else starPath.lineTo(x, y)
        }
        starPath.close()
        drawPath(path = starPath, color = tint)
    }
}
