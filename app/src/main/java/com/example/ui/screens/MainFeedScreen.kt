package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.components.NavigationPage
import com.example.ui.components.UniversalHeader
import com.example.ui.components.liveNameVibration
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc900
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Transizione a Veneziana Orizzontale con BLUR dinamico tra Feed e Live
 */
private fun Modifier.venetianBlindBlurTransition(page: Int, pagerState: PagerState): Modifier {
    val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(-1f, 1f)
    val absOffset = abs(pageOffset)
    val blurRadius = (absOffset * 24f).dp

    return this
        .blur(radius = blurRadius)
        .graphicsLayer {
            if (pageOffset != 0f) {
                cameraDistance = 20f * density
                rotationY = pageOffset * -36f
                alpha = (1f - (absOffset * 0.45f)).coerceIn(0.05f, 1f)
                scaleX = 1f - (absOffset * 0.08f)
                scaleY = 1f - (absOffset * 0.08f)
                translationX = pageOffset * (size.width * 0.14f)
            }
        }
}

/**
 * Schermata Principale con Navigazione a 2 Pagine (Feed e Live)
 */
@Composable
fun MainFeedScreen(
    currentUser: User,
    feedUsers: List<User>,
    stories: List<User>,
    onOpenLiveDetail: (User) -> Unit,
    onOpenProfile: (User) -> Unit,
    onSelectTrack: (Track, User) -> Unit,
    onOpenShareSheet: () -> Unit,
    onOpenPeopleSearch: () -> Unit,
    feedbackToast: String?,
    onClearToast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    val currentPageEnum = if (pagerState.currentPage == 0) NavigationPage.LIVE else NavigationPage.FEED

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BlackPitch)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("main_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ================= 1. HEADER SUPERIORE UNIVERSALE =================
            // Centrato e minimale: logo "m" cerchiata + "live" in corsivo minuscolo al centro,
            // lente di ingrandimento a sinistra, avatar profilo a destra.
            UniversalHeader(
                currentPage = currentPageEnum,
                currentUser = currentUser,
                onSearchClick = onOpenPeopleSearch,
                onProfileClick = { onOpenProfile(currentUser) },
                onHeaderCenterClick = {
                    coroutineScope.launch {
                        val targetPage = if (pagerState.currentPage == 0) 1 else 0
                        pagerState.animateScrollToPage(targetPage, animationSpec = tween(380))
                    }
                }
            )

            // ================= 2. PAGER A VENEZIANA CON BLUR =================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .venetianBlindBlurTransition(page = page, pagerState = pagerState)
                ) {
                    if (page == 0) {
                        // PAGINA 0: SCHERMATA LIVE CON VINYL GLOW
                        LivePageContent(
                            currentUser = currentUser,
                            liveUsers = stories.filter { it.currentTrack != null && !it.isCurrentUser },
                            onSelectLiveUser = onOpenLiveDetail,
                            onOpenProfile = onOpenProfile
                        )
                    } else {
                        // PAGINA 1: FEED CONDIVISIONI
                        FeedPageContent(
                            currentUser = currentUser,
                            feedUsers = feedUsers,
                            onSelectTrack = onSelectTrack,
                            onOpenProfile = onOpenProfile
                        )
                    }
                }
            }
        }

        // ================= 3. FAB (+) SOLO NEL FEED =================
        AnimatedVisibility(
            visible = pagerState.currentPage == 1,
            enter = scaleIn(animationSpec = tween(250)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 24.dp)
        ) {
            FloatingActionButton(
                onClick = onOpenShareSheet,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("fab_add_post"),
                shape = CircleShape,
                containerColor = PureWhite,
                contentColor = BlackPitch,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Condividi un brano",
                    modifier = Modifier.size(28.dp),
                    tint = BlackPitch
                )
            }
        }
    }
}

/**
 * Contenuto Schermata Feed: Colonna verticale di copertine fusa nel nero.
 */
@Composable
private fun FeedPageContent(
    currentUser: User,
    feedUsers: List<User>,
    onSelectTrack: (Track, User) -> Unit,
    onOpenProfile: (User) -> Unit
) {
    val allShares = remember(feedUsers) {
        val list = mutableListOf<Pair<User, Track>>()
        feedUsers.forEach { user ->
            user.sharedTracks.forEach { trk -> list.add(Pair(user, trk)) }
        }
        list
    }

    if (allShares.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "nessun brano condiviso nel feed",
                color = SubtitleGray,
                fontSize = 14.sp,
                letterSpacing = 0.3.sp
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        items(allShares, key = { (user, track) -> "${user.id}_${track.id}" }) { (user, track) ->
            FeedMinimalPost(
                user = user,
                track = track,
                onCardClick = { onSelectTrack(track, user) },
                onUserClick = { onOpenProfile(user) }
            )
        }
    }
}

/**
 * Singolo Post Feed Minimalista Borderless
 */
@Composable
private fun FeedMinimalPost(
    user: User,
    track: Track,
    onCardClick: () -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCardClick
            )
            .testTag("feed_post_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onUserClick
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar ${user.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
            )

            Column {
                Text(
                    text = user.name.lowercase(),
                    color = PureWhite,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                )
                Text(
                    text = "@${user.username}",
                    color = SubtitleGray,
                    fontSize = 11.sp,
                    letterSpacing = 0.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Zinc900)
        ) {
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
                            colors = listOf(
                                Color.Transparent,
                                BlackPitch.copy(alpha = 0.85f)
                            ),
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

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Mi piace",
                        tint = SubtitleGray,
                        modifier = Modifier.size(17.dp)
                    )
                    Text("condividi", color = SubtitleGray, fontSize = 12.sp, letterSpacing = 0.2.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Commenta",
                        tint = SubtitleGray,
                        modifier = Modifier.size(17.dp)
                    )
                    Text("discuti", color = SubtitleGray, fontSize = 12.sp, letterSpacing = 0.2.sp)
                }
            }

            Text(
                text = track.genre.lowercase(),
                color = SubtitleGray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.6.sp
            )
        }
    }
}

/**
 * Contenuto Schermata Live:
 * 1. Sfondo Nero Puro (#000000) minimale e senza distrazioni.
 * 2. Colonna verticale di righe Live con vinile animato a pulsazione d'alone (Vinyl Aura Pulse) e micro-interazione social.
 * 3. Mini-player nero nativo in stile footer in basso con pallino rosso pulsante.
 */
@Composable
private fun LivePageContent(
    currentUser: User,
    liveUsers: List<User>,
    onSelectLiveUser: (User) -> Unit,
    onOpenProfile: (User) -> Unit
) {
    val myTrack = currentUser.currentTrack
    val iAmLive = myTrack != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackPitch)
    ) {
        if (!iAmLive && liveUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "nessun ascolto live in questo momento",
                    color = SubtitleGray,
                    fontSize = 14.sp,
                    letterSpacing = 0.3.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BlackPitch),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── La mia live ───────────────────────────────────────────
                if (iAmLive && myTrack != null) {
                    item(key = "my_live_header") {
                        LiveSectionHeader(text = "LA TUA LIVE")
                    }
                    item(key = "my_live") {
                        LiveUserMinimalItem(
                            user = currentUser,
                            track = myTrack,
                            onClick = { onSelectLiveUser(currentUser) },
                            onProfileClick = { onOpenProfile(currentUser) }
                        )
                    }
                }
                // ─── Live degli amici ──────────────────────────────────────
                if (liveUsers.isNotEmpty()) {
                    item(key = "friends_live_header") {
                        LiveSectionHeader(
                            text = "FRIENDS LIVE",
                            topPadding = if (iAmLive) 12.dp else 0.dp
                        )
                    }
                    items(liveUsers, key = { it.id }) { user ->
                        val track = user.currentTrack ?: return@items
                        LiveUserMinimalItem(
                            user = user,
                            track = track,
                            onClick = { onSelectLiveUser(user) },
                            onProfileClick = { onOpenProfile(user) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSectionHeader(
    text: String,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    Text(
        text = text,
        color = PureWhite.copy(alpha = 0.35f),
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.5.sp,
        textAlign = TextAlign.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding, bottom = 6.dp, end = 6.dp)
    )
}

/**
 * Copertina Circolare con CD Compact Disc Ottico Riflettente e CD Aura Pulse:
 * - Superficie metallica argentata/lucida in policarbonato con spettacolari riflessi iridescenti/arcobaleno a cono speculare
 *   tipici della superficie inferiore di un Compact Disc audio che ruota sotto la luce.
 * - Copertina album al centro sensibilmente ingrandita (~36dp su 52dp), con finitura glassmorphism semi-trasparente (alpha ~0.84)
 *   che fonde dolcemente i colori dell'artwork con i bagliori iridescenti del CD sottostante.
 * - Anello di bloccaggio centrale in plastica trasparente (Clamping Ring) e foro centrale (Spindle Hole) con riflessi di vetro.
 * - Effetto "CD Aura Pulse": anello cromatico diffuso che respira lentamente a ritmo live.
 */
@Composable
private fun CdGlowCircularCover(
    coverUrl: String,
    title: String,
    trackId: String,
    primaryColor: Color,
    secondaryColor: Color,
    glowFactor: Float = 1f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cd_glow_anim")

    // Rotazione continua e fluida del CD ottico e della copertina
    val baseCdRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "base_cd_rotation"
    )

    // Rotazione accentuata al cambio brano
    var rollSpinTarget by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(trackId) {
        rollSpinTarget += 720f
    }
    val animatedRollSpin by animateFloatAsState(
        targetValue = rollSpinTarget,
        animationSpec = tween(
            durationMillis = 900,
            easing = CubicBezierEasing(0.16f, 1f, 0.30f, 1f)
        ),
        label = "cd_track_change_spin"
    )

    // CD Aura Pulse: Pulsazione e respirazione lenta dell'alone cromatico
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cd_aura_pulse_scale"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cd_aura_pulse_alpha"
    )

    // Onda di espansione dell'alone per indicare l'ascolto live
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.34f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cd_wave_pulse_scale"
    )

    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cd_wave_pulse_alpha"
    )

    val currentRotation = baseCdRotation + animatedRollSpin
    val effectiveAuraAlpha = auraAlpha * glowFactor
    val effectiveWaveAlpha = waveAlpha * glowFactor

    Box(
        modifier = modifier.size(58.dp),
        contentAlignment = Alignment.Center
    ) {
        // 1. CD AURA PULSE: ALONE SOFFUSO CHE RESPIRA (Espansione e contrazione ciclica)
        if (effectiveAuraAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        scaleX = auraScale
                        scaleY = auraScale
                        alpha = effectiveAuraAlpha
                    }
                    .blur(radius = 10.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.85f),
                                secondaryColor.copy(alpha = 0.45f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Onda esterna ad anello che si espande (Aura Ripple)
            Canvas(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        scaleX = waveScale
                        scaleY = waveScale
                        alpha = effectiveWaveAlpha
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f
                drawCircle(
                    color = primaryColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            // Anello di contorno luminoso dinamico (Live Chromatic Ring)
            Canvas(
                modifier = Modifier
                    .size(54.dp)
                    .graphicsLayer {
                        scaleX = 0.98f + (auraScale - 0.94f) * 0.25f
                        scaleY = 0.98f + (auraScale - 0.94f) * 0.25f
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) - 0.5.dp.toPx()
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = effectiveAuraAlpha * 0.95f),
                            secondaryColor.copy(alpha = effectiveAuraAlpha * 0.60f),
                            PureWhite.copy(alpha = effectiveAuraAlpha * 0.90f),
                            primaryColor.copy(alpha = effectiveAuraAlpha * 0.95f)
                        ),
                        center = center
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        // 2. COMPACT DISC OTTICO ROTANTE (Superficie dominata dall'Artwork con sottile bordo metallico lucido)
        Box(
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer { rotationZ = currentRotation },
            contentAlignment = Alignment.Center
        ) {
            // Sottile anello metallico / policarbonato esterno del CD (Outer Metallic Rim)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension / 2f

                // Bordo metallico argentato satinato sobrio e semi-trasparente
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFE2E8F0).copy(alpha = 0.95f),
                            Color(0xFF94A3B8).copy(alpha = 0.85f),
                            Color(0xFF475569).copy(alpha = 0.90f),
                            Color(0xFF1E293B).copy(alpha = 0.95f)
                        ),
                        center = center,
                        radius = outerRadius
                    ),
                    radius = outerRadius,
                    center = center
                )

                // Riflesso speculare discreto sul bordo metallico
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            PureWhite.copy(alpha = 0.55f),
                            Color(0xFF94A3B8).copy(alpha = 0.20f),
                            PureWhite.copy(alpha = 0.60f),
                            Color(0xFF64748B).copy(alpha = 0.15f),
                            PureWhite.copy(alpha = 0.55f)
                        ),
                        center = center
                    ),
                    radius = outerRadius,
                    center = center
                )

                // Profilatura esterna ad alta precisione
                drawCircle(
                    color = PureWhite.copy(alpha = 0.50f),
                    radius = outerRadius - 0.5.dp.toPx(),
                    center = center,
                    style = Stroke(width = 0.75.dp.toPx())
                )
            }

            // 3. TOP-SURFACE PRINTED ART: L'artwork dell'album occupa quasi interamente la superficie del disco (~47dp su 52dp)
            Box(
                modifier = Modifier
                    .size(47.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Cover $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )

                // Patina lucida/ottica diagonale (Glossy Optical Disc Sheen)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                PureWhite.copy(alpha = 0.28f),
                                PureWhite.copy(alpha = 0.08f),
                                Color.Transparent,
                                PureWhite.copy(alpha = 0.12f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        ),
                        radius = size.minDimension / 2f,
                        center = Offset(w / 2f, h / 2f)
                    )
                    // Bordo di giunzione tra artwork e rim
                    drawCircle(
                        color = PureWhite.copy(alpha = 0.35f),
                        radius = (size.minDimension / 2f) - 0.5.dp.toPx(),
                        center = Offset(w / 2f, h / 2f),
                        style = Stroke(width = 0.6.dp.toPx())
                    )
                }

                // 4. ANELLO CENTRALE DI BLOCCAGGIO (CLEAR CLAMPING RING) & FORO SPINDLE
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(Color(0xFF0F1115).copy(alpha = 0.78f), CircleShape)
                        .border(0.7.dp, PureWhite.copy(alpha = 0.55f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Sottile anello argentato interno
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(
                            color = Color(0xFFE2E8F0).copy(alpha = 0.50f),
                            radius = 4.8.dp.toPx(),
                            center = center,
                            style = Stroke(width = 0.5.dp.toPx())
                        )
                    }

                    // Foro centrale del CD (Center Spindle Hole nero assoluto con bordo metallico)
                    Box(
                        modifier = Modifier
                            .size(5.5.dp)
                            .background(Color(0xFF000000), CircleShape)
                            .border(0.7.dp, PureWhite.copy(alpha = 0.85f), CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Estrazione dinamica in tempo reale della palette cromatica per il glow pulsante del vinile:
 * - Estrae il colore base dalla palette della copertina/traccia (accentColorHex).
 * - Genera una sfumatura secondaria ricca ed elegante per un alone tridimensionale e mai monocromatico.
 */
private fun extractDynamicTrackGlowColors(track: Track): Pair<Color, Color> {
    val baseHex = track.accentColorHex
    val primary = if (baseHex != 0L && baseHex != 0xFF1DB954) {
        Color(baseHex)
    } else {
        when (track.genre.lowercase()) {
            "progressive rock", "rock" -> Color(0xFFE040FB) // Magenta Violet
            "ambient", "synthwave" -> Color(0xFF00E5FF) // Electric Cyan
            "r&b", "soul" -> Color(0xFFFF9100) // Vibrant Amber
            "dream pop / synthpop" -> Color(0xFF7928CA) // Cyber Violet
            "r&b / experimental" -> Color(0xFF00DF89) // Emerald Mint
            "french house" -> Color(0xFF0070F3) // Cobalt Blue
            "darkwave / synthpop" -> Color(0xFFE000FF) // Vivid Purple
            "electro house" -> Color(0xFFFFBE0B) // Golden Glow
            "psychedelic rock", "indie" -> Color(0xFFFF5252) // Sunset Coral
            else -> {
                val hash = kotlin.math.abs((track.title + track.artist + track.id).hashCode())
                when (hash % 8) {
                    0 -> Color(0xFFFF0055)
                    1 -> Color(0xFF7928CA)
                    2 -> Color(0xFF00E5FF)
                    3 -> Color(0xFFFF9900)
                    4 -> Color(0xFF0070F3)
                    5 -> Color(0xFFE000FF)
                    6 -> Color(0xFFFFBE0B)
                    else -> Color(0xFF00E676)
                }
            }
        }
    }

    val secondary = when (primary) {
        Color(0xFFFF0055) -> Color(0xFF9333EA) // Magenta -> Purple
        Color(0xFF7928CA) -> Color(0xFF00E5FF) // Cyber Violet -> Cyan
        Color(0xFF00E5FF) -> Color(0xFF0070F3) // Cyan -> Cobalt
        Color(0xFFFF9100) -> Color(0xFFFF3366) // Amber -> Coral Pink
        Color(0xFF0070F3) -> Color(0xFF00E5FF) // Cobalt -> Cyan
        Color(0xFFE000FF) -> Color(0xFF4F46E5) // Purple -> Indigo
        Color(0xFFFFBE0B) -> Color(0xFFF97316) // Gold -> Orange
        Color(0xFF00DF89) -> Color(0xFF10B981) // Mint -> Emerald
        Color(0xFFFF5252) -> Color(0xFF9C27B0) // Coral -> Violet
        else -> {
            val r = (primary.red * 0.75f + primary.blue * 0.25f).coerceIn(0f, 1f)
            val g = (primary.green * 0.45f + primary.red * 0.55f).coerceIn(0f, 1f)
            val b = (primary.blue * 0.85f + primary.green * 0.15f).coerceIn(0f, 1f)
            Color(r, g, b, 1f)
        }
    }

    return Pair(primary, secondary)
}

/**
 * Scia luminosa altamente irregolare, organica e fortemente blurrata (Organic Blurred CD Trail):
 * - Genera una scia eterea liquida e ondulante che segue il CD con variazioni organiche di ampiezza.
 * - Triplo strato: aura fluida diffusa con sfumatura d'onda, filamenti plasmatici sinusoidali e bagliore vivo.
 */
@Composable
private fun OrganicBlurredCdTrail(
    currentHeadX: Float,
    trailAlpha: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    if (currentHeadX <= 8f || trailAlpha <= 0.01f) return

    val density = LocalDensity.current
    val trailWidthDp = with(density) { currentHeadX.toDp() }

    Box(
        modifier = modifier
            .width(trailWidthDp)
            .height(58.dp)
            .graphicsLayer { alpha = trailAlpha }
            .blur(radius = 12.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val h = size.height
            val w = size.width
            val midY = h / 2f
            if (w < 4f) return@Canvas

            // 1. Onda Organica Superiore & Inferiore (Ethereal Plasma Cloud)
            val topPath = Path()
            val bottomPath = Path()
            topPath.moveTo(0f, midY)
            bottomPath.moveTo(0f, midY)

            val step = 12f
            var x = 0f
            while (x <= w) {
                val progressFrac = (x / w).coerceIn(0f, 1f)
                val envelope = kotlin.math.sin(progressFrac * Math.PI).toFloat() // Più spessa al centro
                val wave1 = kotlin.math.sin((x * 0.08f).toDouble()).toFloat() * 7.dp.toPx() * envelope
                val wave2 = kotlin.math.cos((x * 0.05f + 1.2).toDouble()).toFloat() * 5.dp.toPx() * envelope

                val topY = midY - (14.dp.toPx() * envelope) + wave1
                val botY = midY + (14.dp.toPx() * envelope) + wave2

                topPath.lineTo(x, topY)
                bottomPath.lineTo(x, botY)
                x += step
            }
            topPath.lineTo(w, midY)
            bottomPath.lineTo(w, midY)

            // Chiudi il perimetro per il riempimento organico
            val cloudPath = Path().apply {
                addPath(topPath)
                // Percorri all'indietro il bordo inferiore
                lineTo(w, midY)
                var backX = w
                while (backX >= 0f) {
                    val progressFrac = (backX / w).coerceIn(0f, 1f)
                    val envelope = kotlin.math.sin(progressFrac * Math.PI).toFloat()
                    val wave2 = kotlin.math.cos((backX * 0.05f + 1.2).toDouble()).toFloat() * 5.dp.toPx() * envelope
                    val botY = midY + (14.dp.toPx() * envelope) + wave2
                    lineTo(backX, botY)
                    backX -= step
                }
                close()
            }

            drawPath(
                path = cloudPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.12f),
                        secondaryColor.copy(alpha = 0.35f),
                        primaryColor.copy(alpha = 0.75f),
                        PureWhite.copy(alpha = 0.90f)
                    ),
                    startX = 0f,
                    endX = w
                )
            )

            // 2. Nucleo energetico organico sinusoidale (Bright Inner Fluid Stream)
            val streamPath = Path().apply {
                moveTo(0f, midY)
                var sx = 0f
                while (sx <= w) {
                    val progressFrac = (sx / w).coerceIn(0f, 1f)
                    val envelope = kotlin.math.sin(progressFrac * Math.PI * 0.5).toFloat()
                    val streamWave = kotlin.math.sin((sx * 0.12f).toDouble()).toFloat() * 4.dp.toPx() * envelope
                    lineTo(sx, midY + streamWave)
                    sx += 8f
                }
            }

            drawPath(
                path = streamPath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        secondaryColor.copy(alpha = 0.20f),
                        primaryColor.copy(alpha = 0.70f),
                        PureWhite.copy(alpha = 0.95f)
                    ),
                    startX = (w * 0.2f).coerceAtLeast(0f),
                    endX = w
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            )
        }
    }
}

/**
 * Custodia CD a Libro Fotorealistica 3D Speculare (Reversed Hinge 3D CD Jewel Case):
 * - Cerniera / spina dorsale verticale sul lato DESTRO della custodia.
 * - Backplate rigido (fondo custodia in acrilico trasparente fumé con tray circolare per il disco a sinistra del dorso).
 * - Anta frontale (Front Book Cover) incernierata rigidamente a DESTRA (transformOrigin = (1f, 0.5f)):
 *   si apre ruotando da sinistra verso destra verso l'esterno con rotazione 3D prospettica sull'asse Y (rotationY da 0° a +65°).
 * - Mapping Copertina in tempo reale: Sull'anta frontale aperta compare nitida la copertina dell'album del brano precedente (Artwork Booklet).
 * - Il CD lucido scorre da sinistra, entra al 100% e interamente all'interno dello scomparto della custodia aperta.
 * - Solo dopo che il CD è completamente dentro e nascosto dai bordi, l'anta frontale si richiude da destra verso sinistra (rotationY -> 0°).
 * - Solo a custodia completamente sigillata, l'intero oggetto esegue il fade-out finale.
 */
/**
 * Fondo Custodia CD (Back Case Chassis & Disc Tray Bed - Hinge a Destra):
 * - Base rigida in acrilico trasparente fumé con cerniera verticale sul lato DESTRO.
 * - Tray circolare centrale sagomato con alloggiamento disco e rosetta centrale di bloccaggio (Spindle Rosette).
 */
@Composable
private fun JewelCaseBackPlate(
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.01f) return

    Canvas(
        modifier = modifier
            .width(54.dp)
            .height(52.dp)
            .graphicsLayer { this.alpha = alpha }
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 3.dp, bottomEnd = 3.dp),
                ambientColor = Color.Black.copy(alpha = 0.8f),
                spotColor = Color.Black.copy(alpha = 0.95f)
            )
    ) {
        val w = size.width
        val h = size.height

        // Base rigida della custodia (Nero opaco grafite con bordo squadrato smussato)
        val backPlatePath = Path().apply {
            moveTo(3.dp.toPx(), 0f)
            lineTo(w - 3.dp.toPx(), 0f)
            quadraticTo(w, 0f, w, 3.dp.toPx())
            lineTo(w, h - 3.dp.toPx())
            quadraticTo(w, h, w - 3.dp.toPx(), h)
            lineTo(3.dp.toPx(), h)
            quadraticTo(0f, h, 0f, h - 3.dp.toPx())
            lineTo(0f, 3.dp.toPx())
            quadraticTo(0f, 0f, 3.dp.toPx(), 0f)
            close()
        }

        drawPath(
            path = backPlatePath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1D1D26),
                    Color(0xFF101015),
                    Color(0xFF08080C)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // Bordo esterno rifinito in acrilico trasparente/grafite
        drawPath(
            path = backPlatePath,
            color = Color(0xFF323242),
            style = Stroke(width = 1.dp.toPx())
        )

        // Cerniera / Spina dorsale verticale sul lato DESTRO della custodia (Spine Hinge at Right)
        val spineWidth = 5.dp.toPx()
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF363648),
                    Color(0xFF181822),
                    Color(0xFF2C2C3A)
                ),
                startX = w - spineWidth,
                endX = w
            ),
            topLeft = Offset(w - spineWidth, 0f),
            size = Size(spineWidth, h)
        )
        drawLine(
            color = PureWhite.copy(alpha = 0.25f),
            start = Offset(w - spineWidth, 0f),
            end = Offset(w - spineWidth, h),
            strokeWidth = 0.9.dp.toPx()
        )

        // Tray circolare interno (Alloggiamento sagomato per il disco - posizionato a sinistra rispetto alla cerniera)
        val trayCenterX = w * 0.46f
        val trayCenterY = h * 0.50f
        val trayRadius = 21.5.dp.toPx()

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF060608),
                    Color(0xFF0F0F14),
                    Color(0xFF1A1A24)
                ),
                center = Offset(trayCenterX, trayCenterY),
                radius = trayRadius
            ),
            radius = trayRadius,
            center = Offset(trayCenterX, trayCenterY)
        )
        drawCircle(
            color = Color(0xFF2E2E3E),
            radius = trayRadius,
            center = Offset(trayCenterX, trayCenterY),
            style = Stroke(width = 0.8.dp.toPx())
        )

        // Rosetta / Hub centrale di bloccaggio del disco (Center Spindle / Rosette Teeth)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF303040),
                    Color(0xFF16161E),
                    Color(0xFF09090D)
                ),
                center = Offset(trayCenterX, trayCenterY),
                radius = 6.5.dp.toPx()
            ),
            radius = 6.5.dp.toPx(),
            center = Offset(trayCenterX, trayCenterY)
        )
        drawCircle(
            color = PureWhite.copy(alpha = 0.40f),
            radius = 2.2.dp.toPx(),
            center = Offset(trayCenterX, trayCenterY)
        )

        // Denti della rosetta (fessure di presa a 60 gradi)
        for (angle in listOf(0f, 60f, 120f, 180f, 240f, 300f)) {
            val rad = Math.toRadians(angle.toDouble())
            val x1 = trayCenterX + (3.dp.toPx() * kotlin.math.cos(rad)).toFloat()
            val y1 = trayCenterY + (3.dp.toPx() * kotlin.math.sin(rad)).toFloat()
            val x2 = trayCenterX + (6.dp.toPx() * kotlin.math.cos(rad)).toFloat()
            val y2 = trayCenterY + (6.dp.toPx() * kotlin.math.sin(rad)).toFloat()
            drawLine(
                color = Color(0xFF424256),
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 0.8.dp.toPx()
            )
        }
    }
}

/**
 * Anta Frontale Custodia CD a Libro 3D Speculare (Hinge a Destra, transformOrigin X = 1f):
 * - Incernierata sul lato destro, ruota verso l'esterno da sinistra a destra (rotationY 0° -> +68°).
 * - Mostra l'artwork del brano uscente (Artwork Booklet) protetto da profilo acrilico e riflesso in vetro.
 * - Si richiude a libro (rotationY 68° -> 0°) SOPRA al CD dopo che questo si è completamente inserito nel tray.
 */
@Composable
private fun JewelCaseFrontLid(
    coverUrl: String,
    title: String,
    openAmount: Float, // 0f = Chiuso, 1f = Aperto a +68°
    alpha: Float,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.01f) return

    val density = LocalDensity.current
    val coverRotationY = openAmount * 68f
    val perspectiveSkewX = openAmount * 4f

    Box(
        modifier = modifier
            .width(54.dp)
            .height(52.dp)
            .graphicsLayer {
                this.alpha = alpha
                rotationY = coverRotationY
                rotationZ = perspectiveSkewX
                cameraDistance = 16f * density.density
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f)
            }
            .shadow(
                elevation = if (openAmount > 0.15f) 8.dp else 2.dp,
                shape = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 3.dp, bottomEnd = 3.dp),
                ambientColor = Color.Black.copy(alpha = 0.75f),
                spotColor = Color.Black.copy(alpha = 0.95f)
            )
    ) {
        // Strato base acrilico dell'anta frontale
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val flapPath = Path().apply {
                moveTo(3.dp.toPx(), 0f)
                lineTo(w - 3.dp.toPx(), 0f)
                quadraticTo(w, 0f, w, 3.dp.toPx())
                lineTo(w, h - 3.dp.toPx())
                quadraticTo(w, h, w - 3.dp.toPx(), h)
                lineTo(3.dp.toPx(), h)
                quadraticTo(0f, h, 0f, h - 3.dp.toPx())
                lineTo(0f, 3.dp.toPx())
                quadraticTo(0f, 0f, 3.dp.toPx(), 0f)
                close()
            }

            // Corpo acrilico solido fumé della copertina (100% opaco e coprente)
            drawPath(
                path = flapPath,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF242432),
                        Color(0xFF161622),
                        Color(0xFF0C0C14)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            // Profilatura perimetrale acrilica
            drawPath(
                path = flapPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PureWhite.copy(alpha = 0.40f),
                        Color(0xFF3E3E50),
                        PureWhite.copy(alpha = 0.12f)
                    )
                ),
                style = Stroke(width = 0.9.dp.toPx())
            )

            // Linguetta di chiusura a scatto sul lato SINISTRO
            drawRect(
                color = PureWhite.copy(alpha = 0.35f),
                topLeft = Offset(0.5.dp.toPx(), (h * 0.5f) - 4.dp.toPx()),
                size = Size(1.5.dp.toPx(), 8.dp.toPx())
            )
        }

        // Inserto Grafico: Copertina dell'Album Mappata in Tempo Reale (Artwork Booklet)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 4.dp, top = 4.dp, end = 5.5.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF16161E))
                .border(0.6.dp, Color(0xFF383848).copy(alpha = 0.8f), RoundedCornerShape(2.dp))
        ) {
            if (coverUrl.isNotBlank()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = "Artwork $title",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Riflesso Speculare Diagonale Acrilico / Vetro Lucido (Glass Gloss Glare)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                PureWhite.copy(alpha = 0.28f),
                                PureWhite.copy(alpha = 0.08f),
                                Color.Transparent,
                                PureWhite.copy(alpha = 0.05f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(80f, 80f)
                        )
                    )
            )

            // Micro-linguette / fermagli ad aletta del libretto negli angoli destri
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bw = size.width
                val bh = size.height
                // Fermaglio alto a destra
                drawRect(
                    color = PureWhite.copy(alpha = 0.45f),
                    topLeft = Offset(bw - 2.dp.toPx(), 3.dp.toPx()),
                    size = Size(2.dp.toPx(), 1.5.dp.toPx())
                )
                // Fermaglio basso a destra
                drawRect(
                    color = PureWhite.copy(alpha = 0.45f),
                    topLeft = Offset(bw - 2.dp.toPx(), bh - 4.5.dp.toPx()),
                    size = Size(2.dp.toPx(), 1.5.dp.toPx())
                )
            }
        }
    }
}

/**
 * Elemento Lista Live (Layout Minimalista & Motion Design Digitale Ottico):
 * - Sfondo rigorosamente nero puro (#000000).
 * - A sinistra: CD Compact Disc ottico rotante con riflessi metallici iridescenti e Dynamic Glow.
 * - Copertina centrale ingrandita con trasparenza glassmorphic che fonde i colori con i bagliori del disco.
 * - Dati brano e utente (@nickname + fotina profilo circolare) posizionati a sinistra.
 * - Transizione Cinematografica Accelerata (2.8s):
 *   1. Il CD ottico scorre verso destra lasciando una scia luminosa fluida e blurrata.
 *   2. I testi del vecchio brano sfumano progressivamente (fade-out).
 *   3. Contemporaneamente, dal lato sinistro entrano con fade-in il nuovo brano e il nuovo CD in rotazione.
 *   4. A destra, la custodia CD Jewel Case 3D (cerniera a destra) si apre a libro mostrando l'artwork del brano uscente.
 *   5. Il CD plana e viene ingerito al 100% all'interno del tray della custodia.
 *   6. Solo dopo l'inserimento totale, la custodia si richiude a libro e svanisce in fade-out.
 */
@Composable
private fun LiveUserMinimalItem(
    user: User,
    track: Track,
    onClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previousTrackState = remember { mutableStateOf<Track?>(null) }
    val currentDisplayTrackState = remember { mutableStateOf<Track>(track) }
    val isTransitioningState = remember { mutableStateOf(false) }
    val transitionProgress = remember { Animatable(0f) }

    val currentDisplayTrack = currentDisplayTrackState.value
    val previousTrack = previousTrackState.value
    val isTransitioning = isTransitioningState.value

    // Gestione unificata e atomica della transizione: ZERO-LAG E ZERO SCATTI INIZIALI (2.8s Accelerated Timeline)
    LaunchedEffect(track.id) {
        if (track.id != currentDisplayTrackState.value.id) {
            previousTrackState.value = currentDisplayTrackState.value
            currentDisplayTrackState.value = track
            // Reset immediato del progresso prima di attivare lo stato di transizione
            transitionProgress.snapTo(0f)
            isTransitioningState.value = true
            // Animazione cinematografica accelerata a 2.8s con flusso continuo senza sobbalzi
            transitionProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 2800,
                    easing = LinearEasing
                )
            )
            isTransitioningState.value = false
            previousTrackState.value = null
        }
    }

    // Palette cromatica dinamica estratta in tempo reale dal brano specifico
    val (primaryColor, secondaryColor) = remember(currentDisplayTrack.id, currentDisplayTrack.accentColorHex, currentDisplayTrack.genre) {
        extractDynamicTrackGlowColors(currentDisplayTrack)
    }

    val (prevPrimaryColor, prevSecondaryColor) = remember(previousTrack?.id, previousTrack?.accentColorHex, previousTrack?.genre) {
        previousTrack?.let { extractDynamicTrackGlowColors(it) } ?: Pair(primaryColor, secondaryColor)
    }

    val progress = if (isTransitioning) transitionProgress.value else 0f
    val density = LocalDensity.current
    val formattedUsername = if (user.username.startsWith("@")) user.username else "@${user.username}"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BlackPitch)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = PureWhite.copy(alpha = 0.08f)),
                onClick = onClick
            )
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag("live_item_${user.id}")
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val availableWidthPx = constraints.maxWidth.toFloat()
            val cdSizePx = with(density) { 58.dp.toPx() }
            val jewelCaseWidthPx = with(density) { 54.dp.toPx() }
            // Distanza di scorrimento millimetrica: posiziona il centro del CD esattamente al centro del piatto/tray della custodia CD a destra
            val travelDistance = (availableWidthPx - jewelCaseWidthPx + (jewelCaseWidthPx * 0.46f) - (cdSizePx / 2f)).coerceAtLeast(0f)

            // ================= 1. DATI DEL VECCHIO BRANO (SFUMATURA VERSO LA SINISTRA) =================
            val prevT = previousTrack
            val oldTextAlpha = if (!isTransitioning || prevT == null) 0f else (1f - (progress / 0.26f)).coerceIn(0f, 1f)

            if (isTransitioning && prevT != null && oldTextAlpha > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp)
                        .zIndex(1f)
                        .graphicsLayer {
                            alpha = oldTextAlpha
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = prevT.title,
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = prevT.artist,
                            color = Zinc400,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        // Nickname statico e non cliccabile, fuso sullo sfondo nero #000000
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clip(CircleShape)
                            )
                            Text(
                                text = formattedUsername.lowercase(),
                                color = PureWhite.copy(alpha = 0.65f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal,
                                letterSpacing = 0.1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // ================= 2. DATI DEL NUOVO BRANO & NUOVO CD OTTICO (FADE-IN PULITO DALLA SINISTRA) =================
            val newTrackAlpha = if (!isTransitioning) 1f else ((progress - 0.10f) / 0.26f).coerceIn(0f, 1f)
            val newCdScale = if (!isTransitioning) 1f else (0.80f + 0.20f * newTrackAlpha).coerceIn(0.80f, 1f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
                    .graphicsLayer {
                        alpha = newTrackAlpha
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // CD OTTICO COMPACT DISC ROTANTE CON COPERTINA ALBUM PROMINENTE
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .graphicsLayer {
                            scaleX = newCdScale
                            scaleY = newCdScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    CdGlowCircularCover(
                        coverUrl = currentDisplayTrack.coverUrl,
                        title = currentDisplayTrack.title,
                        trackId = currentDisplayTrack.id,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // COLONNA TITOLO + ARTISTA + @NICKNAME UTENTE (STATICO E FUSO CON SFONDO NERO #000000)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentDisplayTrack.title,
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentDisplayTrack.artist,
                        color = Zinc400,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.1.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Nickname e avatar completamente statici, non cliccabili, senza box né bordi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(15.dp)
                                .clip(CircleShape)
                        )
                        Text(
                            text = formattedUsername.lowercase(),
                            color = PureWhite.copy(alpha = 0.65f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.1.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ================= 3. SCIA LUMINOSA ORGANICA E FORTEMENTE BLURRATA (ORGANIC BLURRED TRAIL) =================
            if (isTransitioning && prevT != null) {
                val currentRollFraction = (progress / 0.58f).coerceIn(0f, 1f)
                val currentHeadX = currentRollFraction * travelDistance + (cdSizePx / 2f)

                // La scia è vivace durante la corsa e sfuma delicatamente all'ingresso della custodia CD
                val trailAlpha = when {
                    progress < 0.04f -> (progress / 0.04f)
                    progress < 0.44f -> 1f
                    progress < 0.58f -> (1f - ((progress - 0.44f) / 0.14f)).coerceIn(0f, 1f)
                    else -> 0f
                }

                OrganicBlurredCdTrail(
                    currentHeadX = currentHeadX,
                    trailAlpha = trailAlpha,
                    primaryColor = prevPrimaryColor,
                    secondaryColor = prevSecondaryColor,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .zIndex(5f)
                )
            }

            // ================= 4. TRANSIZIONE MECCANICA RIGOROSA: FONDO CUSTODIA + CD + ANTA RICHIUSA SOPRA =================
            if (isTransitioning && prevT != null) {
                // Sincronizzazione Temporale Rigorosa:
                // 1. Apertura Speculare a Libro (0.04f .. 0.28f)
                // 2. Il CD scorre e atterra al 100% nel tray della custodia (0.00f .. 0.58f) con OPACITÀ 100%
                // 3. L'anta con l'artwork si richiude a libro SOPRA al CD inserito (0.58f .. 0.78f) - zIndex 12f
                // 4. Nel momento esatto in cui l'anta è chiusa (progress >= 0.78f), il CD sparisce ISTANTANEAMENTE
                // 5. SOLO ED ESCLUSIVAMENTE DOPO la chiusura totale e la scomparsa del CD, la custodia sfuma in fade-out (0.78f .. 1.00f)
                val caseAlpha = when {
                    progress < 0.10f -> (progress / 0.10f).coerceIn(0f, 1f)
                    progress < 0.78f -> 1f
                    else -> (1f - ((progress - 0.78f) / 0.22f)).coerceIn(0f, 1f)
                }

                val openAmount = when {
                    progress < 0.04f -> 0f
                    progress < 0.28f -> ((progress - 0.04f) / 0.24f).coerceIn(0f, 1f)
                    progress < 0.58f -> 1f
                    progress < 0.78f -> (1f - ((progress - 0.58f) / 0.20f)).coerceIn(0f, 1f)
                    else -> 0f
                }

                // 4a. FONDO DELLA CUSTODIA (Back Case & Tray Bed) a zIndex(6f)
                JewelCaseBackPlate(
                    alpha = caseAlpha,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .zIndex(6f)
                )

                // 4b. IL CD IN MOVIMENTO E INSERIMENTO (zIndex 8f, sopra il fondo e SOTTO l'anta richiudibile a zIndex 12f)
                // MANDATORIO: Opacità al 100% fino alla chiusura dell'anta.
                // Nel momento esatto in cui la custodia è completamente sigillata (progress >= 0.78f),
                // il CD sparisce ISTANTANEAMENTE (nessun fade-out sul CD all'interno della custodia).
                val isCdInsideAndVisible = progress < 0.78f

                if (isCdInsideAndVisible) {
                    val rollProgress = (progress / 0.58f).coerceIn(0f, 1f)
                    val currentX = rollProgress * travelDistance
                    // La rotazione si ferma con precisione al locking nel tray a 0.58f
                    val rollAngle = if (progress < 0.58f) progress * 1600f else (0.58f * 1600f)

                    // Planata di inserimento millimetrica all'interno del tray
                    val landingProgress = ((progress - 0.38f) / 0.20f).coerceIn(0f, 1f)
                    val glideOffsetY = if (progress in 0.38f..0.58f) {
                        kotlin.math.sin(landingProgress * Math.PI.toFloat()) * with(density) { 1.5.dp.toPx() }
                    } else 0f

                    // Riduzione di scala durante la planata per combaciare perfettamente con il diametro del tray
                    val rollingScale = 1f - (landingProgress * 0.26f)
                    val cdGlowFactor = (1f - landingProgress).coerceIn(0f, 1f)

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .zIndex(8f)
                            .graphicsLayer {
                                translationX = currentX
                                translationY = glideOffsetY
                                rotationZ = rollAngle
                                scaleX = rollingScale
                                scaleY = rollingScale
                                alpha = 1f // Nessun fade out: 100% opaco fino alla scomparsa istantanea a 0.78f
                            }
                            .size(58.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CdGlowCircularCover(
                            coverUrl = prevT.coverUrl,
                            title = prevT.title,
                            trackId = prevT.id,
                            primaryColor = prevPrimaryColor,
                            secondaryColor = prevSecondaryColor,
                            glowFactor = cdGlowFactor
                        )
                    }
                }

                // 4c. ANTA FRONTALE A LIBRO 3D SPECULARE (zIndex 12f, chiude fisicamente SOPRA al CD oscurandolo al 100%)
                JewelCaseFrontLid(
                    coverUrl = prevT.coverUrl,
                    title = prevT.title,
                    openAmount = openAmount,
                    alpha = caseAlpha,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .zIndex(12f)
                )
            }
        }
    }
}
