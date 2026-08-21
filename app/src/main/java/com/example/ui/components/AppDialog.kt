package com.example.ui.components

import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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

/**
 * Estende la finestra del Dialog a tutto lo schermo, DIETRO status bar e barra di
 * navigazione: il backdrop copre anche il pannello notifiche/barra di stato Android.
 */
@Composable
private fun ImmersiveDialogWindow() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }
}

private const val DISMISS_THRESHOLD = 260f

/**
 * Contenitore comune: backdrop a tutto schermo, chiusura con swipe verso il basso
 * o tap sul backdrop. Niente pulsante X. Contenuto centrato verticalmente/orizzontalmente.
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
        ImmersiveDialogWindow()
        val scope = rememberCoroutineScope()
        val offsetY = remember { Animatable(0f) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            backdrop()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    // Consuma i tap così toccare il contenuto non chiude la dialog
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    // Swipe verso il basso per chiudere
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
                    },
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
