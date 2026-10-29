package com.example.ui.components

import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.PI
import kotlin.math.sin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.example.ui.theme.BlackPitch
import kotlinx.coroutines.launch

/**
 * Modifier di trascinamento-per-chiudere esposto dal contenitore della dialog.
 * Ogni dialog con scroll interno lo attacca al PROPRIO header (titolo ± barra di ricerca):
 * così lo swipe up/down sull'header chiude, mentre il tap sulla barra continua a scrivere e
 * le liste interne restano libere di scrollare. È `Modifier` (no-op) fuori da una dialog o
 * quando la dialog non è chiudibile.
 */
val LocalDialogDragHandle = compositionLocalOf<Modifier> { Modifier }

/**
 * NestedScrollConnection esposto dalla dialog ai contenuti interni.
 * Le LazyColumn nelle utility dialog con swipeAnywhere=true lo applicano via
 * `.nestedScroll(LocalDialogScrollConnection.current)` per propagare il dismiss
 * anche quando la lista è vuota/corta (onPostScroll riceve il delta residuo).
 */
val LocalDialogScrollConnection = compositionLocalOf<NestedScrollConnection?> { null }

/**
 * Copertina atmosferica condivisa con i pannelli (notifiche, impostazioni, follower,
 * lista chat, ecc.): quando [enabled] è true e c'è una [coverUrl], il backdrop dei pannelli
 * mostra la stessa copertina sfocata di profilo/feed/live invece del nero pieno. Impostata
 * una volta a livello root; [enabled] riflette l'impostazione "applica copertina" dell'utente.
 */
data class PanelCover(val coverUrl: String?, val enabled: Boolean)
val LocalPanelCover = compositionLocalOf { PanelCover(null, false) }

/**
 * Azioni globali della barra di navigazione in basso, condivise con TUTTI i pannelli overlay
 * (notifiche, ricerca, dettaglio, ecc.). Il tasto indietro di ogni pannello usa il proprio
 * onDismiss; queste sono le altre icone. Impostata una volta a livello root.
 */
data class PanelNav(
    val unreadMessages: Int = 0,
    val unreadNotifications: Int = 0,
    val onSearch: () -> Unit = {},
    val onMessages: () -> Unit = {},
    val onNotifications: () -> Unit = {},
    val onSettings: () -> Unit = {}
)
val LocalPanelNav = compositionLocalOf { PanelNav() }

/**
 * Chiusura animata del pannello corrente, esposta ai contenuti (es. back arrow nell'header
 * di chat/notifiche) così da avviare lo stesso slide-out orizzontale di swipe/back di sistema.
 */
val LocalPanelDismiss = compositionLocalOf<() -> Unit> { {} }

/**
 * Backdrop condiviso dei pannelli: copertina sfocata + scrim quando attiva, altrimenti nero.
 * In entrambi i casi si dissolve/trasla con lo swipe (letto in draw, nessuna ricomposizione).
 */
@Composable
private fun PanelBackdrop(offset: () -> Float, fraction: () -> Float) {
    val cover = LocalPanelCover.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            // Consuma i tap sullo sfondo: rende l'overlay modale (niente click-through
            // alla schermata sotto, come faceva la finestra Dialog).
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { }
    ) {
    if (cover.enabled && !cover.coverUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = offset()
                    alpha = (1f - fraction()).coerceIn(0f, 1f)
                }
        ) {
            Box(modifier = Modifier.fillMaxSize().background(BlackPitch))
            AsyncImage(
                model = cover.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { scaleX = 1.15f; scaleY = 1.15f }
                    .blur(12.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                BlackPitch.copy(alpha = 0.60f),
                                BlackPitch.copy(alpha = 0.80f),
                                BlackPitch.copy(alpha = 0.94f)
                            )
                        )
                    )
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = offset() }
                .drawBehind {
                    drawRect(Color.Black.copy(alpha = 0.965f * (1f - fraction())))
                }
        )
    }
    }
}

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
    // Provider differiti: le lambda leggono offset/frazione in fase di draw (graphicsLayer/
    // drawBehind), MAI in composizione → niente ricomposizione durante slide e settle.
    backdrop: @Composable (offset: () -> Float, fraction: () -> Float) -> Unit,
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
                w.setBackgroundDrawable(
                    android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
                )
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

        // nested: applicato sull'outer Box. Gestisce SOLO lo snap-back quando il dialog
        // è già spostato (onPreScroll) e il dismiss per dialog con swipeAnywhere=true
        // che NON applicano LocalDialogScrollConnection sulla propria LazyColumn.
        val nested = remember(swipeAnywhere) {
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
                    // Solo per swipeAnywhere=true: dialog con lista vuota/corta che NON applicano
                    // childConnection. Le dialog con childConnection consumano prima di qui.
                    if (swipeAnywhere && available.y != 0f) {
                        val damped = if (kotlin.math.abs(offsetY.value) > 200f) available.y * 0.4f else available.y
                        scope.launch { offsetY.snapTo(offsetY.value + damped) }
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (offsetY.value != 0f) { settle(available.y); return available }
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (swipeAnywhere && available.y != 0f) { settle(available.y); return available }
                    return Velocity.Zero
                }
            }
        }

        // childConnection: applicato via LocalDialogScrollConnection DIRETTAMENTE sulle LazyColumn
        // delle dialog. Separato da nested per evitare doppia elaborazione del delta.
        //
        // Con swipeAnywhere=false (es. ChatScreen): dead zone di 60px — i primi 60px di overscroll
        // sono assorbiti silenziosamente; solo dopo una spinta deliberata il dialog si muove.
        // I Fling non contano (solo Drag deliberati). Impedisce che uno scroll veloce che arriva
        // al bordo chiuda accidentalmente la dialog.
        //
        // Con swipeAnywhere=true: nessuna dead zone, comportamento diretto.
        val childConnection = remember(swipeAnywhere) {
            object : NestedScrollConnection {
                var overscrollAccum = 0f
                private val DEAD_ZONE = 10f

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
                    if (available.y == 0f) return Offset.Zero
                    if (!swipeAnywhere) {
                        // Blocca i Fling: solo Drag deliberati al bordo spostano il dialog
                        if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.Fling) return Offset.Zero
                        // Dead zone: reset se la direzione cambia
                        if ((overscrollAccum > 0f && available.y < 0f) || (overscrollAccum < 0f && available.y > 0f)) {
                            overscrollAccum = 0f
                        }
                        overscrollAccum += available.y
                        if (kotlin.math.abs(overscrollAccum) < DEAD_ZONE) return Offset.Zero
                    }
                    val damped = if (kotlin.math.abs(offsetY.value) > 200f) available.y * 0.4f else available.y
                    scope.launch { offsetY.snapTo(offsetY.value + damped) }
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    overscrollAccum = 0f
                    if (offsetY.value != 0f) { settle(available.y); return available }
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    overscrollAccum = 0f
                    if (available.y != 0f && (swipeAnywhere || kotlin.math.abs(offsetY.value) > 20f)) {
                        settle(available.y)
                        return available
                    }
                    return Velocity.Zero
                }
            }
        }

        // Provider differiti (vedi firma backdrop): chiusi su offsetY, letti solo in draw.
        val offsetProvider = remember { { offsetY.value } }
        val fractionProvider = remember(dismissible) {
            {
                if (dismissible) (kotlin.math.abs(offsetY.value) / DISMISS_DISTANCE).coerceIn(0f, 1f) else 0f
            }
        }

        // swipeAnywhere: il draggable sull'outer Box cattura gesti verticali su aree
        // non-scrollabili (empty state, testo, spaziatori). Per le LazyColumn vuote,
        // onPostScroll riceve il delta residuo non consumato e muove la dialog.
        // Per le LazyColumn CON contenuto, onPreScroll/onPostScroll gestiscono
        // il dismiss quando la lista raggiunge i propri estremi.
        val swipeAnywhereModifier: Modifier = if (dismissible && swipeAnywhere) {
            Modifier.draggable(
                state = draggableState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> settle(velocity) }
            )
        } else Modifier

        val boxModifier = when {
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

        Box(modifier = boxModifier.then(swipeAnywhereModifier), contentAlignment = Alignment.Center) {
            backdrop(offsetProvider, fractionProvider)

            Column(
                modifier = Modifier
                    .graphicsLayer {
                        // translationY invece di offset{}: sposta in fase di draw, nessun
                        // re-layout per frame durante slide/settle.
                        translationY = offsetY.value
                        // Blur progressivo del contenuto durante lo swipe (0→6px), letto in draw.
                        val contentBlurPx = if (dismissible) (fractionProvider() * 6f).coerceAtLeast(0f) else 0f
                        if (contentBlurPx >= 0.5f &&
                            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                            renderEffect = android.graphics.RenderEffect
                                .createBlurEffect(contentBlurPx, contentBlurPx, android.graphics.Shader.TileMode.DECAL)
                                .asComposeRenderEffect()
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val columnScope = this
                CompositionLocalProvider(
                    LocalDialogDragHandle provides dragHandle,
                    LocalDialogScrollConnection provides if (dismissible) childConnection else null,
                    LocalPanelDismiss provides onDismiss
                ) {
                    with(columnScope) { content() }
                }
            }
        }
    }
}

/**
 * Variante IN-TREE dello scaffold immersivo: stesso gesto/backdrop/blur di [ImmersiveScaffold],
 * ma renderizzata come overlay fullscreen DENTRO la finestra dell'Activity (niente `Dialog` =
 * niente seconda finestra). Condivide così l'edge-to-edge e gli insets della schermata
 * principale: il background sta dietro status/nav bar senza riconfigurare flag di finestra,
 * e la tastiera può essere gestita con WindowInsets.ime dal contenuto. Il back chiude via
 * BackHandler. Va posizionata dai chiamanti come ultimo figlio del Box root, sopra il resto.
 */
@Composable
private fun ImmersiveOverlayScaffold(
    onDismiss: () -> Unit,
    swipeAnywhere: Boolean = false,
    dismissible: Boolean = true,
    backdrop: @Composable (offset: () -> Float, fraction: () -> Float) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetY = remember { Animatable(0f) }

    // Transizione d'ingresso orizzontale (slide + fade), uguale a profilo/lista chat.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(420, easing = FastOutSlowInEasing)) }

    // Chiusura con slide-out orizzontale INVERSO (back / tasto indietro della barra):
    // anima appear 1→0, poi chiude davvero.
    fun dismissWithExit() {
        scope.launch {
            appear.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
            onDismiss()
        }
    }

    BackHandler(enabled = dismissible) { dismissWithExit() }

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

    val draggableState = rememberDraggableState { rawDelta ->
        val damped = if (kotlin.math.abs(offsetY.value) > 200f) rawDelta * 0.4f else rawDelta
        scope.launch { offsetY.snapTo(offsetY.value + damped) }
    }

    val nested = remember(swipeAnywhere) {
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
                if (swipeAnywhere && available.y != 0f) {
                    val damped = if (kotlin.math.abs(offsetY.value) > 200f) available.y * 0.4f else available.y
                    scope.launch { offsetY.snapTo(offsetY.value + damped) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetY.value != 0f) { settle(available.y); return available }
                return Velocity.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (swipeAnywhere && available.y != 0f) { settle(available.y); return available }
                return Velocity.Zero
            }
        }
    }

    val childConnection = remember(swipeAnywhere) {
        object : NestedScrollConnection {
            var overscrollAccum = 0f
            private val DEAD_ZONE = 10f
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
                if (available.y == 0f) return Offset.Zero
                if (!swipeAnywhere) {
                    if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.Fling) return Offset.Zero
                    if ((overscrollAccum > 0f && available.y < 0f) || (overscrollAccum < 0f && available.y > 0f)) {
                        overscrollAccum = 0f
                    }
                    overscrollAccum += available.y
                    if (kotlin.math.abs(overscrollAccum) < DEAD_ZONE) return Offset.Zero
                }
                val damped = if (kotlin.math.abs(offsetY.value) > 200f) available.y * 0.4f else available.y
                scope.launch { offsetY.snapTo(offsetY.value + damped) }
                return Offset(0f, available.y)
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                overscrollAccum = 0f
                if (offsetY.value != 0f) { settle(available.y); return available }
                return Velocity.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                overscrollAccum = 0f
                if (available.y != 0f && (swipeAnywhere || kotlin.math.abs(offsetY.value) > 20f)) {
                    settle(available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val offsetProvider = remember { { offsetY.value } }
    val fractionProvider = remember(dismissible) {
        {
            if (dismissible) (kotlin.math.abs(offsetY.value) / DISMISS_DISTANCE).coerceIn(0f, 1f) else 0f
        }
    }

    val swipeAnywhereModifier: Modifier = if (dismissible && swipeAnywhere) {
        Modifier.draggable(
            state = draggableState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity -> settle(velocity) }
        )
    } else Modifier

    val boxModifier = when {
        dismissible -> Modifier.fillMaxSize().nestedScroll(nested)
        else -> Modifier.fillMaxSize()
    }

    val dragHandle: Modifier = if (dismissible)
        Modifier.draggable(
            state = draggableState,
            orientation = Orientation.Vertical,
            onDragStopped = { velocity -> settle(velocity) }
        )
    else Modifier

    Box(
        modifier = boxModifier
            .then(swipeAnywhereModifier)
            // Slide-in/out orizzontale + fade (appear 0↔1). Niente blur offscreen qui:
            // leggero e fluido anche su pannelli pesanti come la chat.
            .graphicsLayer {
                val p = appear.value
                alpha = p
                translationX = (1f - p) * size.width * 0.18f
            },
        contentAlignment = Alignment.Center
    ) {
        backdrop(offsetProvider, fractionProvider)

        // Niente barra di navigazione negli overlay: la navigazione vive solo nelle pagine
        // top-level (feed/live, profilo). Qui si chiude con swipe o back di sistema.
        Column(
            modifier = Modifier
                .graphicsLayer {
                    translationY = offsetY.value
                    val contentBlurPx = if (dismissible) (fractionProvider() * 6f).coerceAtLeast(0f) else 0f
                    if (contentBlurPx >= 0.5f &&
                        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen
                        renderEffect = android.graphics.RenderEffect
                            .createBlurEffect(contentBlurPx, contentBlurPx, android.graphics.Shader.TileMode.DECAL)
                            .asComposeRenderEffect()
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val columnScope = this
            CompositionLocalProvider(
                LocalDialogDragHandle provides dragHandle,
                LocalDialogScrollConnection provides if (dismissible) childConnection else null,
                LocalPanelDismiss provides { dismissWithExit() }
            ) {
                with(columnScope) { content() }
            }
        }
    }
}

/**
 * Versione overlay in-tree di [UtilityDialog] (stesso backdrop nero). Usala per i pannelli
 * resi come figli del Box root dell'app (es. chat), così il background copre la status bar
 * come le schermate normali e la tastiera è gestibile dal contenuto.
 */
@Composable
fun UtilityOverlay(
    onDismiss: () -> Unit,
    swipeAnywhere: Boolean = false,
    dismissible: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    ImmersiveOverlayScaffold(
        onDismiss = onDismiss,
        swipeAnywhere = swipeAnywhere,
        dismissible = dismissible,
        backdrop = { offset, fraction -> PanelBackdrop(offset, fraction) },
        content = content
    )
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
        backdrop = { offset, fraction -> PanelBackdrop(offset, fraction) },
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
        backdrop = { offset, fraction ->
            // La copertina è lo sfondo del dialog e SCENDE + si dissolve INSIEME al
            // contenuto durante lo swipe (translationY = offset, alpha = 1 - frazione),
            // rivelando linearmente l'app sottostante. A riposo copre tutto (anche la
            // status bar); la base nera evita bordi trasparenti dello sfocato.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = offset()
                        alpha = (1f - fraction()).coerceIn(0f, 1f)
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

/**
 * Versione overlay in-tree di [TrackDialog] (backdrop = copertina del brano sfocata).
 * Usala per i pannelli di brano/live resi come figli del Box root dell'app (es. dettaglio
 * live), così condividono l'edge-to-edge e il background copre bene la barra di stato.
 */
@Composable
fun TrackOverlay(
    coverUrl: String,
    onDismiss: () -> Unit,
    swipeAnywhere: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    ImmersiveOverlayScaffold(
        onDismiss = onDismiss,
        swipeAnywhere = swipeAnywhere,
        backdrop = { offset, fraction ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Modale: consuma i tap sullo sfondo.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { }
                    .graphicsLayer {
                        translationY = offset()
                        alpha = (1f - fraction()).coerceIn(0f, 1f)
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
