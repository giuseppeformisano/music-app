package com.example.ui.components

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val DISMISS_THRESHOLD = 240f

/**
 * Contenitore comune per TUTTE le dialog:
 * - backdrop a tutto schermo, GIÀ fullscreen dietro status bar/barra di navigazione
 *   sin dal primo frame (flag impostati in composizione, niente "scatto")
 * - chiusura con swipe verso il basso DA QUALSIASI PUNTO, o tap sullo sfondo
 * - niente pulsante X; contenuto centrato verticalmente e orizzontalmente
 */
@Composable
private fun ImmersiveScaffold(
    onDismiss: () -> Unit,
    backdrop: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        // Fullscreen sopra la barra di sistema impostato SUBITO in composizione
        // (prima del primo draw) così non si vede il salto dal sotto-barra a fullscreen.
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
            }
            true
        }

        val scope = rememberCoroutineScope()
        val offsetY = remember { Animatable(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                // Swipe verso il basso da QUALSIASI punto della dialog
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (offsetY.value > DISMISS_THRESHOLD) onDismiss()
                            else scope.launch { offsetY.animateTo(0f) }
                        },
                        onVerticalDrag = { _, dy ->
                            scope.launch { offsetY.snapTo((offsetY.value + dy).coerceAtLeast(0f)) }
                        }
                    )
                }
                // Tap sullo sfondo (non sul contenuto) chiude
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onDismiss() })
                },
            contentAlignment = Alignment.Center
        ) {
            // Backdrop statico (non trasla con lo swipe)
            backdrop()

            // Contenuto: trasla con lo swipe; i tap qui NON propagano allo sfondo
            Column(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .pointerInput(Unit) { detectTapGestures { /* consuma i tap sul contenuto */ } },
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

/**
 * Dialog di UTILITÀ (ricerca, notifiche, modifica profilo, collega account, follower/following).
 * Backdrop quasi completamente nero, nessuno spazio delimitato da bordi, contenuto centrato.
 */
@Composable
fun UtilityDialog(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ImmersiveScaffold(
        onDismiss = onDismiss,
        backdrop = {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xF7000000)))
        },
        content = content
    )
}

/**
 * Dialog dei BRANI (dettaglio brano feed/live, condivisione). Backdrop = copertina
 * dell'album sfocata + velo scuro, coerente col dettaglio live.
 */
@Composable
fun TrackDialog(
    coverUrl: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ImmersiveScaffold(
        onDismiss = onDismiss,
        backdrop = {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(42.dp)
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f)))
        },
        content = content
    )
}
