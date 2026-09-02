package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite
import com.example.ui.theme.Zinc400
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

// ─── Modello dati ────────────────────────────────────────────────────────────

data class FeedWeekGroup(
    val user: User,
    val weekKey: String,       // es. "2025-W35"
    val weekLabel: String,     // es. "Questa settimana", "Settimana scorsa", "26 ago – 1 set"
    val tracks: List<Track>,   // ordinati per sharedAt desc
    val lastSharedAt: Long     // timestamp del brano più recente (per ordinare i gruppi)
)

fun buildFeedWeekGroups(feedUsers: List<User>): List<FeedWeekGroup> {
    val now = Calendar.getInstance()
    val currentYear = now.get(Calendar.YEAR)
    val currentWeek = now.get(Calendar.WEEK_OF_YEAR)
    val result = mutableListOf<FeedWeekGroup>()

    feedUsers.forEach { user ->
        if (user.sharedTracks.isEmpty()) return@forEach
        val byWeek = mutableMapOf<String, MutableList<Track>>()
        user.sharedTracks.forEach { track ->
            val ts = if (track.sharedAt > 0L) track.sharedAt else System.currentTimeMillis()
            val cal = Calendar.getInstance().apply { timeInMillis = ts }
            val key = "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
            byWeek.getOrPut(key) { mutableListOf() }.add(track)
        }
        byWeek.forEach { (key, tracks) ->
            val sorted = tracks.sortedByDescending { it.sharedAt }
            val lastAt = sorted.maxOf { it.sharedAt }
            result.add(FeedWeekGroup(user, key, weekLabel(key, currentYear, currentWeek), sorted, lastAt))
        }
    }
    return result.sortedByDescending { it.lastSharedAt }
}

private fun weekLabel(key: String, currentYear: Int, currentWeek: Int): String {
    val parts = key.split("-W")
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return key
    val week = parts.getOrNull(1)?.toIntOrNull() ?: return key
    return when {
        year == currentYear && week == currentWeek -> "Questa settimana"
        year == currentYear && week == currentWeek - 1 -> "Settimana scorsa"
        else -> {
            val italian = Locale("it")
            val cal = Calendar.getInstance(italian).apply {
                set(Calendar.YEAR, year)
                set(Calendar.WEEK_OF_YEAR, week)
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            }
            val startDay = cal.get(Calendar.DAY_OF_MONTH)
            val startMonth = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, italian) ?: ""
            cal.add(Calendar.DAY_OF_MONTH, 6)
            val endDay = cal.get(Calendar.DAY_OF_MONTH)
            val endMonth = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, italian) ?: ""
            "$startDay $startMonth – $endDay $endMonth"
        }
    }
}

// ─── Easing helpers ───────────────────────────────────────────────────────────

private fun easeOutCubic(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u
}

private fun easeInOutCubic(t: Float): Float {
    return if (t < 0.5f) 4f * t * t * t
    else {
        val v = -2f * t + 2f
        1f - v * v * v / 2f
    }
}

// ─── Composable carosello ─────────────────────────────────────────────────────

/**
 * Carosello a scorrimento orizzontale per i brani di un gruppo settimanale.
 * Parametri animazione confermati dall'utente:
 *   - durata: 402ms
 *   - blur orizzontale max: 40px (API 31+)
 *   - traslazione: 18% della larghezza della card
 *   - scala: 100% (invariata)
 *   - picco blur: 90% del progresso animazione
 */
@Composable
fun FeedCarousel(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tracks.isEmpty()) return
    if (tracks.size == 1) {
        CarouselCard(
            track = tracks[0],
            onClick = { onTrackClick(tracks[0]) },
            modifier = modifier
        )
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var pendingIndex by remember { mutableIntStateOf(0) }
    var swipeDir by remember { mutableIntStateOf(1) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var dragAccumulated by remember { mutableFloatStateOf(0f) }
    var autoPlayVersion by remember { mutableIntStateOf(0) }

    fun navigate(dir: Int, isManual: Boolean = false) {
        if (isManual) autoPlayVersion++  // resetta sempre il timer anche se animazione in corso
        if (progress.isRunning) return
        val next = (currentIndex + dir + tracks.size) % tracks.size
        pendingIndex = next
        swipeDir = dir
        scope.launch {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(402, easing = FastOutSlowInEasing))
            currentIndex = next
            progress.snapTo(0f)
        }
    }

    // Auto-avanzamento ogni 3s; si resetta ad ogni swipe manuale
    LaunchedEffect(autoPlayVersion) {
        while (true) {
            delay(3000L)
            navigate(1)
        }
    }

    val p = progress.value
    val eased = easeOutCubic(p)
    val blurFraction = sin((p / 0.9f) * PI).coerceIn(0.0, 1.0).toFloat()
    val blurPx = 40f * blurFraction

    BoxWithConstraints(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulated = 0f },
                    onDragEnd = {
                        when {
                            dragAccumulated < -60f -> navigate(1, isManual = true)
                            dragAccumulated > 60f -> navigate(-1, isManual = true)
                            else -> autoPlayVersion++  // reset timer anche su swipe annullato
                        }
                        dragAccumulated = 0f
                    },
                    onDragCancel = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, delta -> dragAccumulated += delta }
                )
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!progress.isRunning) onTrackClick(tracks[currentIndex])
            }
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }

        if (p > 0f) {
            CarouselCard(
                track = tracks[currentIndex],
                onClick = {},
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = -swipeDir * 0.18f * eased * widthPx
                        alpha = 1f - easeInOutCubic(p)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx >= 0.5f) {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(blurPx, 0.5f, android.graphics.Shader.TileMode.DECAL)
                                .asComposeRenderEffect()
                        }
                    }
            )
            CarouselCard(
                track = tracks[pendingIndex],
                onClick = {},
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = swipeDir * 0.18f * (1f - eased) * widthPx
                        alpha = easeInOutCubic(p)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx >= 0.5f) {
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(blurPx, 0.5f, android.graphics.Shader.TileMode.DECAL)
                                .asComposeRenderEffect()
                        }
                    }
            )
        } else {
            CarouselCard(
                track = tracks[currentIndex],
                onClick = {},
                modifier = Modifier.fillMaxSize()
            )
        }

        // Indicatori dot
        val displayedIndex = if (p > 0.5f) pendingIndex else currentIndex
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tracks.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .size(if (i == displayedIndex) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (i == displayedIndex) PureWhite
                            else PureWhite.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

@Composable
internal fun CarouselCard(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = "Copertina ${track.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, BlackPitch.copy(alpha = 0.85f)),
                        startY = 210f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                text = track.title,
                color = PureWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.4).sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist.uppercase(),
                color = Zinc400,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
