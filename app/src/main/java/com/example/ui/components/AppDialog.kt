package com.example.ui.components

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Modifier di trascinamento-per-chiudere esposto dal contenitore della dialog.
 * Ogni dialog con scroll interno lo attacca al PROPRIO header (titolo ± barra di ricerca):
 * così lo swipe up/down sull'header chiude, mentre il tap sulla barra continua a scrivere e
 * le liste interne restano libere di scrollare. È `Modifier` (no-op) fuori da una dialog o
 * quando la dialog non è chiudibile.
 */
val LocalDialogDragHandle = compositionLocalOf<Modifier> { Modifier }

// Distanza (px) di trascinamento a cui il pannello è considerato "chiuso" e a cui il
// backdrop è completamente dissolto: la transizione è LINEARE con la discesa.
private const val DISMISS_DISTANCE = 620f
private const val DISMISS_THRESHOLD = 240f

/**
 * Contenitore comune per TUTTE le dialog:
 * - backdrop a tutto schermo, GIÀ fullscreen dietro status bar/barra di navigazione
 *   sin dal primo frame (niente "scatto")
 * - chiusura con swipe verso il basso O verso l'alto DA QUALSIASI PUNTO (anche sopra le
 *   liste, via nested scroll), o tap sullo sfondo
 * - durante la discesa il backdrop si dissolve/sfoca in modo LINEARE, rivelando la
 *   schermata sottostante
 * - niente pulsante X; contenuto centrato
 *
 * `backdrop` riceve `dragFraction` (0 = chiuso/coperto, 1 = trascinato a fondo/rivelato).
 */
@Composable
private fun ImmersiveScaffold(
    onDismiss: () -> Unit,
    swipeAnywhere: Boolean = false,
    dismissible: Boolean = true,
    backdrop: @Composable (dragFraction: Float, offsetY: Float) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            decorFitsSystemWindows = false,
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        // Fullscreen nativo edge-to-edge dietro la barra di stato sin dal frame zero
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        LaunchedEffect(window) {
            window?.let { w ->
                WindowCompat.setDecorFitsSystemWindows(w, false)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    w.attributes.layoutInDisplayCutoutMode =
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
                w.setFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                )
                w.statusBarColor = android.graphics.Color.TRANSPARENT
                w.navigationBarColor = android.graphics.Color.TRANSPARENT
                w.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            }
        }

        val scope = rememberCoroutineScope()
        val offsetY = remember { Animatable(0f) }

        // Settle: spring fisico con velocità — se il pannello è spostato abbastanza O il fling
        // è abbastanza veloce nella direzione giusta, chiude; altrimenti torna a riposo con spring.
        fun settle(velocity: Float = 0f) {
            scope.launch {
                val shouldDismiss = when {
                    offsetY.value > DISMISS_THRESHOLD -> true
                    offsetY.value < -DISMISS_THRESHOLD -> true
                    offsetY.value > 0f && velocity > 1200f -> true
                    offsetY.value < 0f && velocity < -1200f -> true
                    else -> false
                }
                if (shouldDismiss) onDismiss()
                else offsetY.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                    initialVelocity = velocity
                )
            }
        }

        // Stato drag condiviso: applica resistenza rubber-band oltre 200px per un feel fisico.
        val draggableState = rememberDraggableState { rawDelta ->
            val damped = if (kotlin.math.abs(offsetY.value) > 200f) rawDelta * 0.4f else rawDelta
            scope.launch { offsetY.snapTo(offsetY.value + damped) }
        }

        // Nested scroll: consente lo swipe-per-chiudere (giù O su) anche partendo SOPRA una
        // lista scrollabile (quando la lista è a un estremo e non può scrollare oltre).
        val nested = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                    val dy = available.y
                    if (offsetY.value > 0f && dy < 0f) {
                        val target = (offsetY.value + dy).coerceAtLeast(0f)
                        val consumed = target - offsetY.value
                        scope.launch { offsetY.snapTo(target) }
                        return Offset(0f, consumed)
                    }
                    if (offsetY.value < 0f && dy > 0f) {
                        val target = (offsetY.value + dy).coerceAtMost(0f)
                        val consumed = target - offsetY.value
                        scope.launch { offsetY.snapTo(target) }
                        return Offset(0f, consumed)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): Offset {
                    // La dialog non assorbe mai lo scroll residuo della lista — né fling né drag
                    // continuo. Si muove solo con un gesto nuovo (dito sollevato + nuova swipe)
                    // intercettato dall'header o dal box swipeAnywhere.
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (offsetY.value != 0f) { settle(available.y); return available }
                    return Velocity.Zero
                }
            }
        }

        val dragFraction = if (dismissible) (kotlin.math.abs(offsetY.value) / DISMISS_DISTANCE).coerceIn(0f, 1f) else 0f

        val boxModifier = when {
            dismissible && swipeAnywhere -> Modifier
                .fillMaxSize()
                .nestedScroll(nested)
                .draggable(
                    state = draggableState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity -> settle(velocity) }
                )
            dismissible -> Modifier.fillMaxSize().nestedScroll(nested)
            else -> Modifier.fillMaxSize()
        }

        // Handle esposto ai contenuti: le dialog con scroll interno lo attaccano al proprio
        // header (titolo ± barra di ricerca). Fuori dai casi chiudibili è un no-op.
        val dragHandle: Modifier = if (dismissible)
            Modifier.draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> settle(velocity) }
            )
        else Modifier

        Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
            backdrop(dragFraction, offsetY.value)

            Column(
                modifier = Modifier.offset { IntOffset(0, offsetY.value.roundToInt()) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val columnScope = this
                CompositionLocalProvider(LocalDialogDragHandle provides dragHandle) {
                    with(columnScope) { content() }
                }
            }
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
    swipeAnywhere: Boolean = false,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ImmersiveScaffold(
        onDismiss = onDismiss,
        swipeAnywhere = swipeAnywhere,
        dismissible = dismissible,
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
    swipeAnywhere: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    ImmersiveScaffold(
        onDismiss = onDismiss,
        swipeAnywhere = swipeAnywhere,
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
                        .graphicsLayer { scaleX = 1.05f; scaleY = 1.05f }
                        .blur(12.dp)
                )
                // Velo leggero per la leggibilità del testo (ridotto per mostrare i colori reali)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f))
                )
            }
        },
        content = content
    )
}
