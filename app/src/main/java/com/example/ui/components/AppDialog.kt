package com.example.ui.components

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Distanza (px) di trascinamento a cui il pannello è considerato "chiuso" e a cui il
// backdrop è completamente dissolto: la transizione è LINEARE con la discesa.
private const val DISMISS_DISTANCE = 620f
private const val DISMISS_THRESHOLD = 240f

/**
 * Contenitore comune per TUTTE le dialog:
 * - backdrop a tutto schermo, GIÀ fullscreen dietro status bar/barra di navigazione
 *   sin dal primo frame (niente "scatto")
 * - chiusura con swipe verso il basso DA QUALSIASI PUNTO (anche sopra le liste, via
 *   nested scroll), o tap sullo sfondo
 * - durante la discesa il backdrop si dissolve/sfoca in modo LINEARE, rivelando la
 *   schermata sottostante
 * - niente pulsante X; contenuto centrato
 *
 * `backdrop` riceve `dragFraction` (0 = chiuso/coperto, 1 = trascinato a fondo/rivelato).
 */
@Composable
private fun ImmersiveScaffold(
    onDismiss: () -> Unit,
    backdrop: @Composable (dragFraction: Float, offsetY: Float) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        // Fullscreen sopra la barra di sistema impostato SUBITO in composizione
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        remember(window) {
            window?.apply {
                setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                // Il contenuto (es. campo "rispondi in live") sale con la tastiera
                setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            true
        }

        val scope = rememberCoroutineScope()
        val offsetY = remember { Animatable(0f) }

        fun settle() {
            scope.launch {
                if (offsetY.value > DISMISS_THRESHOLD) onDismiss()
                else offsetY.animateTo(0f, tween(220))
            }
        }

        // Nested scroll: consente lo swipe-giù-per-chiudere anche partendo SOPRA una lista
        // scrollabile (quando la lista è in cima e non può più scrollare verso l'alto).
        val nested = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                    // Se il pannello è già sceso e si trascina verso l'alto, prima lo si riporta su
                    val dy = available.y
                    if (dy < 0f && offsetY.value > 0f) {
                        val target = (offsetY.value + dy).coerceAtLeast(0f)
                        val consumed = target - offsetY.value
                        scope.launch { offsetY.snapTo(target) }
                        return Offset(0f, consumed)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                    // La lista non ha consumato lo scroll verso il basso -> abbassa il pannello
                    if (available.y > 0f) {
                        scope.launch { offsetY.snapTo(offsetY.value + available.y) }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (offsetY.value > 0f) { settle(); return available }
                    return Velocity.Zero
                }
            }
        }

        val dragFraction = (offsetY.value / DISMISS_DISTANCE).coerceIn(0f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nested)
                // Swipe verso il basso da QUALSIASI punto (aree non scrollabili)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = { settle() },
                        onDragCancel = { settle() },
                        onVerticalDrag = { _, dy ->
                            scope.launch { offsetY.snapTo((offsetY.value + dy).coerceAtLeast(0f)) }
                        }
                    )
                }
                // Tap sullo sfondo chiude
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            backdrop(dragFraction, offsetY.value)

            Column(
                modifier = Modifier.offset { IntOffset(0, offsetY.value.roundToInt()) },
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

/**
 * Dialog di UTILITÀ (ricerca, notifiche, modifica profilo, collega account, follower,
 * aggiornamento). Backdrop quasi nero che si dissolve linearmente con la discesa.
 */
@Composable
fun UtilityDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ImmersiveScaffold(
        onDismiss = onDismiss,
        backdrop = { frac, _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.965f * (1f - frac)))
            )
        },
        content = content
    )
}

/**
 * Dialog dei BRANI (dettaglio brano, condivisione). Backdrop = copertina sfocata + velo;
 * lo sfocato e il velo si riducono linearmente con la discesa, rivelando la schermata sotto.
 */
@Composable
fun TrackDialog(
    coverUrl: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ImmersiveScaffold(
        onDismiss = onDismiss,
        backdrop = { frac, off ->
            // La copertina è lo sfondo del dialog e SCENDE + si dissolve INSIEME al
            // contenuto durante lo swipe (translationY = offset, alpha = 1 - frazione),
            // rivelando linearmente l'app sottostante. A riposo copre tutto (anche la
            // status bar); la base nera evita bordi trasparenti dello sfocato.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = off
                        alpha = (1f - frac).coerceIn(0f, 1f)
                    }
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = 1.12f; scaleY = 1.12f }
                        .blur(30.dp)
                )
                // Velo leggero per la leggibilità del testo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.38f))
                )
            }
        },
        content = content
    )
}
