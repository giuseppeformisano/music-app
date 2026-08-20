package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.model.User
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SubtitleGray
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc900
import kotlin.math.abs

// Preset fotografici ad alta risoluzione per cambio rapido Avatar
private val AVATAR_PRESETS = listOf(
    "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80"
)

// Preset estetici per copertina di sfondo atmosferico
private val COVER_PRESETS = listOf(
    "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800&auto=format&fit=crop&q=80",
    "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800&auto=format&fit=crop&q=80"
)

/**
 * Schermata Profilo Utente dell'app MUSIC
 * 1. Sfondo #000000 con blur atmosferico profondo (artwork o custom cover).
 * 2. Header Superiore: ricerca a sx, solo "m" al centro, profilo a dx.
 * 3. Identità Utente: Grande avatar con alone soft luminoso, Nome in bianco grassetto e @username.
 * 4. Pulsanti d'Azione Minimali: "Modifica profilo" e "Collega account" posizionati tra identità e shared tracks.
 * 5. Carosello Condivisioni: 3 elementi a vista con effetto prospettiva 3D ricurva.
 * 6. Sezione Statistiche: Top Artists, Total Shared, Listening Time.
 * 7. Player Live a Scomparsa: Barra inferiore fluttuante con copertina, titolo/artista e pallino rosso pulsante.
 */
@Composable
fun ProfileScreen(
    user: User,
    isCurrentUser: Boolean,
    isFollowing: Boolean = false,
    isSpotifyConnected: Boolean = true,
    connectedServices: Map<String, Boolean> = mapOf(
        "spotify" to isSpotifyConnected,
        "amazon_music" to false
    ),
    onToggleService: (String) -> Unit = {},
    spotifyError: String? = null,
    onConnectSpotify: (android.content.Context) -> Unit = {},
    onDisconnectSpotify: () -> Unit = {},
    onUpdateProfile: (name: String, username: String, avatarUrl: String, coverUrl: String?) -> Unit = { _, _, _, _ -> },
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    notificationCount: Int = 0,
    onOpenChat: (User) -> Unit,
    onSelectTrack: (Track, User) -> Unit,
    onOpenLiveDetail: (User) -> Unit = {},
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showConnectAccountsSheet by remember { mutableStateOf(false) }

    // Copertina di sfondo atmosferico: custom cover > track in riproduzione > prima condivisione > default
    val atmosphericCoverUrl = user.coverUrl
        ?: user.currentTrack?.coverUrl
        ?: user.sharedTracks.firstOrNull()?.coverUrl
        ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BlackPitch)
            .testTag("profile_screen_${user.id}")
    ) {
        // ================= 1. SFONDO E ATMOSFERA FULLSCREEN (MENO BLURRED) =================
        AsyncImage(
            model = atmosphericCoverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.15f
                    scaleY = 1.15f
                }
                .blur(radius = 16.dp)
        )

        // Scrim scuro sfumato per contrasto e leggibilità perfetta
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BlackPitch.copy(alpha = 0.45f),
                            BlackPitch.copy(alpha = 0.70f),
                            BlackPitch.copy(alpha = 0.90f)
                        )
                    )
                )
        )

        // ================= STRUTTURA EQUIDISTANTE SENZA SCROLL VERTICALE =================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            // Header superiore fisso
            ProfileTopHeader(
                onBack = onBack,
                onNotificationsClick = onOpenNotifications,
                notificationCount = notificationCount,
                isCurrentUser = isCurrentUser,
                onLogout = onLogout
            )

            // Tutti i blocchi della pagina equidistanziati verticalmente entro lo spazio disponibile (Zero Scrolling)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // 1. Blocco unificato: Identità + Contatori sociali + Pulsanti azione
                UserIdentityBlock(
                    user = user,
                    isCurrentUser = isCurrentUser,
                    onEditClick = { showEditProfileSheet = true },
                    onConnectAccountClick = { showConnectAccountsSheet = true }
                )

                val canSeeContent = isCurrentUser || isFollowing

                // 2. Carosello Brani Condivisi — visibile solo se si segue l'utente o è il profilo proprio
                if (canSeeContent) {
                    SharedTracks3DCarousel(
                        tracks = user.sharedTracks,
                        onTrackClick = { track -> onSelectTrack(track, user) }
                    )
                }

                // 3. Sezione Statistiche — visibile solo se si segue l'utente o è il profilo proprio
                if (canSeeContent) {
                    UserStatsSection(
                        totalShared = user.sharedTracks.size,
                        topArtistsCount = remember(user.sharedTracks) {
                            user.sharedTracks.map { it.artist }.distinct().size.coerceAtLeast(6)
                        },
                        listeningHours = 65
                    )
                }

                // 4. Blocco Live Footer — visibile solo se si segue l'utente o è il profilo proprio
                if (canSeeContent && user.currentTrack != null) {
                    FloatingLiveBar(
                        track = user.currentTrack,
                        onClick = { onOpenLiveDetail(user) },
                        onShare = { onSelectTrack(user.currentTrack, user) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ================= DIALOG: MODIFICA PROFILO =================
        if (showEditProfileSheet) {
            EditProfileDialog(
                user = user,
                currentCoverUrl = atmosphericCoverUrl,
                onDismiss = { showEditProfileSheet = false },
                onSave = { newName, newUsername, newAvatar, newCover ->
                    onUpdateProfile(newName, newUsername, newAvatar, newCover)
                    showEditProfileSheet = false
                }
            )
        }

        // ================= DIALOG: COLLEGA ACCOUNT STREAMING =================
        if (showConnectAccountsSheet) {
            ConnectAccountsDialog(
                connectedServices = connectedServices,
                spotifyError = spotifyError,
                onConnectSpotify = { onConnectSpotify(context) },
                onDisconnectSpotify = onDisconnectSpotify,
                onToggleService = onToggleService,
                onDismiss = { showConnectAccountsSheet = false }
            )
        }
    }
}

@Composable
private fun ProfileTopHeader(
    onBack: () -> Unit,
    onNotificationsClick: () -> Unit,
    notificationCount: Int,
    isCurrentUser: Boolean,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("profile_top_header"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sinistra: torna al feed
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .testTag("profile_back_button")
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Torna al feed",
                tint = PureWhite,
                modifier = Modifier.size(22.dp)
            )
        }

        // Centro: logo "m"
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(PureWhite),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "m",
                color = BlackPitch,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Destra: notifiche + logout (solo utente corrente)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNotificationsClick
                    )
                    .testTag("profile_notifications_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifiche",
                    tint = PureWhite,
                    modifier = Modifier.size(22.dp)
                )
                if (notificationCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF3B30))
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                            color = PureWhite,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 10.sp
                        )
                    }
                }
            }

            if (isCurrentUser) {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("profile_logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = PureWhite.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Blocco unificato: Avatar + Nome + @Username a sinistra, Follower/Following a destra.
 * Il tutto centrato orizzontalmente. Pulsanti azione direttamente sotto.
 */
@Composable
private fun UserIdentityBlock(
    user: User,
    isCurrentUser: Boolean,
    onEditClick: () -> Unit,
    onConnectAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Sinistra: Avatar + Nome + @username
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = "Avatar ${user.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = user.name,
                    color = PureWhite,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "@${user.username.removePrefix("@")}",
                    color = PureWhite.copy(alpha = 0.65f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.1.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Separatore verticale
            Spacer(modifier = Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(56.dp)
                    .background(PureWhite.copy(alpha = 0.12f))
            )
            Spacer(modifier = Modifier.width(20.dp))

            // Destra: Follower / Following
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatItem(label = "Follower", value = user.followerIds.size.toString())
                Spacer(modifier = Modifier.height(14.dp))
                StatItem(label = "Following", value = user.followingIds.size.toString())
            }
        }

        if (isCurrentUser) {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileActionButtons(
                isCurrentUser = true,
                onEditClick = onEditClick,
                onConnectAccountClick = onConnectAccountClick
            )
        }
    }
}

/**
 * Pulsanti d'Azione Minimali (Modifica Profilo & Collega Account):
 * - Posizionati tra il nome profilo e Shared Tracks.
 * - Il pulsante dice sempre "Collega account" e apre il pannello di gestione.
 */
@Composable
private fun ProfileActionButtons(
    isCurrentUser: Boolean,
    onEditClick: () -> Unit,
    onConnectAccountClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pulsante 1: Modifica Profilo
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, PureWhite.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = PureWhite.copy(alpha = 0.15f)),
                    onClick = onEditClick
                )
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("btn_edit_profile"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Modifica profilo",
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Pulsante 2: Collega Account (Apre impostazione per collegare account)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, PureWhite.copy(alpha = 0.22f), RoundedCornerShape(20.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(color = PureWhite.copy(alpha = 0.15f)),
                    onClick = onConnectAccountClick
                )
                .padding(horizontal = 14.dp, vertical = 7.dp)
                .testTag("btn_connect_account"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                tint = PureWhite,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "Collega account",
                color = PureWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.2.sp
            )
        }
    }
}

/**
 * Carosello in Stile Jukebox per i Brani Condivisi ("Shared Tracks")
 * - Titolo "SHARED TRACKS" centrato orizzontalmente con font stilizzato e raffinato.
 * - Brani distanziati con generoso spazio/respiro per valorizzare la prospettiva 3D.
 * - Nessun indicatore o numero di paginazione.
 */
@Composable
private fun SharedTracks3DCarousel(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label "SHARED TRACKS"
        Text(
            text = "SHARED TRACKS",
            color = PureWhite.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.5.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "nessun brano condiviso ancora",
                    color = PureWhite.copy(alpha = 0.25f),
                    fontSize = 13.sp,
                    letterSpacing = 0.3.sp,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val pagerState = rememberPagerState(
            initialPage = (tracks.size / 2).coerceAtLeast(0),
            pageCount = { tracks.size }
        )

        // Carosello con brani ben distanziati e prospettiva 3D evidenziata
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 104.dp),
            pageSpacing = 32.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
        ) { page ->
            val track = tracks[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val absOffset = abs(pageOffset)
            val isSelected = absOffset < 0.35f

            // Inclinazione 3D morbida e fluida con elementi distanziati
            val yRotation = (-pageOffset.coerceIn(-1.2f, 1.2f) * 24f)
            val itemScale = (1.0f - (absOffset * 0.12f)).coerceIn(0.85f, 1.0f)
            val itemAlpha = (1f - (absOffset * 0.25f)).coerceIn(0.65f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f - absOffset)
                    .graphicsLayer {
                        cameraDistance = 8f * density
                        rotationY = yRotation
                        scaleX = itemScale
                        scaleY = itemScale
                        alpha = itemAlpha
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onTrackClick(track) }
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Cover con finitura riflettente sottile
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .shadow(
                                elevation = if (isSelected) 14.dp else 4.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = PureWhite.copy(alpha = 0.20f),
                                spotColor = PureWhite.copy(alpha = 0.35f)
                            )
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (isSelected) 1.2.dp else 0.5.dp,
                                brush = if (isSelected) {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            PureWhite.copy(alpha = 0.85f),
                                            PureWhite.copy(alpha = 0.20f),
                                            PureWhite.copy(alpha = 0.75f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        colors = listOf(
                                            PureWhite.copy(alpha = 0.30f),
                                            PureWhite.copy(alpha = 0.08f)
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = "Cover ${track.title}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Riflesso satinato leggero
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            PureWhite.copy(alpha = 0.15f),
                                            Color.Transparent,
                                            Color.Transparent,
                                            PureWhite.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(7.dp))

                    Text(
                        text = track.title,
                        color = if (isSelected) PureWhite else PureWhite.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    Text(
                        text = track.artist,
                        color = if (isSelected) Zinc400 else Zinc400.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Sezione Statistiche ("YOUR STATS"):
 * - Titolo "YOUR STATS" centrato orizzontalmente con font stilizzato e raffinato.
 * - Zero Riquadri Grigio/Solidi: poggia in totale trasparenza direttamente sullo sfondo #000000.
 */
@Composable
private fun UserStatsSection(
    totalShared: Int,
    topArtistsCount: Int,
    listeningHours: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Label "YOUR STATS"
        Text(
            text = "YOUR STATS",
            color = PureWhite.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.5.sp,
            fontFamily = FontFamily.SansSerif,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Music stats
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(label = "Top Artists", value = topArtistsCount.toString())
            Box(modifier = Modifier.width(1.dp).height(22.dp).background(PureWhite.copy(alpha = 0.12f)))
            StatItem(label = "Total Shared", value = totalShared.toString())
            Box(modifier = Modifier.width(1.dp).height(22.dp).background(PureWhite.copy(alpha = 0.12f)))
            StatItem(label = "Listening Time", value = "${listeningHours}h")
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = PureWhite,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.2).sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            color = SubtitleGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Player Live in Footer (Fondo Pagina):
 * - Completamente FUSO con il background: zero box grigio alle spalle, trasparenza totale sul nero (#000000).
 * - Copertina quadrata, Titolo e Artista a sinistra, pallino rosso pulsante a destra.
 */
@Composable
private fun FloatingLiveBar(
    track: Track,
    onClick: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_dot_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("floating_live_bar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.coverUrl,
            contentDescription = "Cover ${track.title}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = PureWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = track.artist,
                color = PureWhite.copy(alpha = 0.65f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Pulsante condivisione
        IconButton(
            onClick = onShare,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Condividi nel feed",
                tint = PureWhite.copy(alpha = 0.70f),
                modifier = Modifier.size(16.dp)
            )
        }

        // Pallino live verde pulsante
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotAlpha * 0.6f
                    }
                    .background(Color(0xFF1DB954), shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF1DB954), shape = CircleShape)
            )
        }
    }
}

@Composable
private fun EditProfileDialog(
    user: User,
    currentCoverUrl: String,
    onDismiss: () -> Unit,
    onSave: (newName: String, newUsername: String, newAvatar: String, newCover: String) -> Unit
) {
    var nameInput by remember { mutableStateOf(user.name) }
    var usernameInput by remember { mutableStateOf(user.username.removePrefix("@")) }
    var avatarUrlInput by remember { mutableStateOf(user.avatarUrl) }
    var coverUrlInput by remember { mutableStateOf(user.coverUrl ?: currentCoverUrl) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, PureWhite.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { }
            ) {
                // Header fisso
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Modifica Profilo",
                        color = PureWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = Zinc400, modifier = Modifier.size(20.dp))
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(PureWhite.copy(alpha = 0.06f)))

                // Contenuto scrollabile
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 20.dp, bottom = 8.dp)
                ) {
                    Text("NOME", color = SubtitleGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    MinimalTextInputField(value = nameInput, onValueChange = { nameInput = it }, placeholder = "Il tuo nome", testTag = "input_edit_name")

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("NICKNAME", color = SubtitleGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    MinimalTextInputField(value = usernameInput, onValueChange = { usernameInput = it.removePrefix("@").trim() }, placeholder = "username (es. marco_rossi)", prefix = "@", testTag = "input_edit_username")

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("IMMAGINE PROFILO (AVATAR)", color = SubtitleGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(model = avatarUrlInput, contentDescription = "Anteprima Avatar", contentScale = ContentScale.Crop, modifier = Modifier.size(54.dp).clip(CircleShape).border(1.5.dp, PureWhite.copy(alpha = 0.6f), CircleShape))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(AVATAR_PRESETS) { preset ->
                                val isSelected = avatarUrlInput == preset
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                        .border(if (isSelected) 2.dp else 0.dp, if (isSelected) PureWhite else Color.Transparent, CircleShape)
                                        .clickable { avatarUrlInput = preset },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(model = preset, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MinimalTextInputField(value = avatarUrlInput, onValueChange = { avatarUrlInput = it }, placeholder = "Oppure incolla URL immagine...", testTag = "input_edit_avatar_url")

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("IMMAGINE COPERTINA (SFONDO ATMOSFERICO)", color = SubtitleGray, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(model = coverUrlInput, contentDescription = "Anteprima Cover", contentScale = ContentScale.Crop, modifier = Modifier.size(width = 72.dp, height = 48.dp).clip(RoundedCornerShape(8.dp)).border(1.5.dp, PureWhite.copy(alpha = 0.6f), RoundedCornerShape(8.dp)))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                            items(COVER_PRESETS) { preset ->
                                val isSelected = coverUrlInput == preset
                                Box(
                                                    modifier = Modifier
                                        .size(width = 56.dp, height = 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 2.dp else 0.dp, if (isSelected) PureWhite else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { coverUrlInput = preset },
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(model = preset, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    MinimalTextInputField(value = coverUrlInput, onValueChange = { coverUrlInput = it }, placeholder = "Oppure incolla URL copertina...", testTag = "input_edit_cover_url")

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Pulsante Salva fisso in fondo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0A0A0A))
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Button(
                        onClick = {
                            onSave(
                                nameInput.trim().ifBlank { user.name },
                                usernameInput.trim().ifBlank { user.username },
                                avatarUrlInput.trim().ifBlank { user.avatarUrl },
                                coverUrlInput.trim()
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_save_profile"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = BlackPitch)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Salva modifiche", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

/**
 * Campo di testo minimale con bordo soft semitrasparente
 */
@Composable
private fun MinimalTextInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    prefix: String? = null,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PureWhite.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (prefix != null) {
            Text(
                text = prefix,
                color = Zinc400,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(
                color = PureWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(PureWhite),
            singleLine = true,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = SubtitleGray,
                        fontSize = 14.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

@Composable
private fun ConnectAccountsDialog(
    connectedServices: Map<String, Boolean>,
    spotifyError: String?,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onToggleService: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0A0A0A))
                    .border(1.dp, PureWhite.copy(alpha = 0.09f), RoundedCornerShape(24.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { }
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Collega Account",
                            color = PureWhite,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.4).sp
                        )
                        Text(
                            text = "Scegli i tuoi servizi di streaming",
                            color = SubtitleGray,
                            fontSize = 13.sp,
                            letterSpacing = 0.2.sp
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Chiudi", tint = SubtitleGray)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StreamingAccountItem(
                    serviceName = "Spotify",
                    serviceDesc = "Ascolti live in tempo reale & top tracks",
                    isConnected = connectedServices["spotify"] == true,
                    brandColor = Color(0xFF1DB954),
                    iconComposable = { SpotifyBrandLogo() },
                    onToggle = {
                        if (connectedServices["spotify"] == true) onDisconnectSpotify()
                        else onConnectSpotify()
                    },
                    testTag = "service_spotify"
                )

                if (spotifyError != null) {
                    Text(
                        text = "Errore: $spotifyError",
                        color = Color(0xFFFF6B6B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp, end = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                StreamingAccountItem(
                    serviceName = "Amazon Music",
                    serviceDesc = "Sincronizzazione Unlimited & HD",
                    isConnected = connectedServices["amazon_music"] == true,
                    brandColor = Color(0xFF00A8E1),
                    iconComposable = { AmazonMusicBrandLogo() },
                    onToggle = { onToggleService("amazon_music") },
                    testTag = "service_amazon_music"
                )

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

/**
 * Singola riga di servizio streaming musicale
 */
@Composable
private fun StreamingAccountItem(
    serviceName: String,
    serviceDesc: String,
    isConnected: Boolean,
    brandColor: Color,
    iconComposable: @Composable () -> Unit,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PureWhite.copy(alpha = 0.05f))
            .border(
                width = if (isConnected) 1.dp else 0.5.dp,
                color = if (isConnected) brandColor.copy(alpha = 0.5f) else PureWhite.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icona Servizio
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            iconComposable()
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Info Nome e Descrizione
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = serviceName,
                    color = PureWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(brandColor.copy(alpha = 0.20f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "COLLEGATO",
                            color = brandColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = serviceDesc,
                color = SubtitleGray,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Pulsante Connetti / Disconnetti
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isConnected) brandColor.copy(alpha = 0.15f) else PureWhite.copy(alpha = 0.10f)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isConnected) "Disconnetti" else "Collega",
                color = if (isConnected) brandColor else PureWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Icona Vettoriale Spotify
 */
@Composable
private fun SpotifyBrandLogo() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(40.dp)) {
        val radius = size.minDimension / 2f
        drawCircle(color = Color(0xFF1DB954), radius = radius)

        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = radius * 0.18f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        // Tre onde curve bianche
        val w = size.width
        val h = size.height

        val path1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.28f, h * 0.38f)
            cubicTo(w * 0.45f, h * 0.32f, w * 0.62f, h * 0.34f, w * 0.74f, h * 0.42f)
        }
        drawPath(path1, color = Color.White, style = stroke)

        val path2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.32f, h * 0.52f)
            cubicTo(w * 0.47f, h * 0.47f, w * 0.60f, h * 0.49f, w * 0.70f, h * 0.55f)
        }
        drawPath(
            path2,
            color = Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = radius * 0.16f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        val path3 = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.36f, h * 0.65f)
            cubicTo(w * 0.48f, h * 0.61f, w * 0.58f, h * 0.63f, w * 0.66f, h * 0.68f)
        }
        drawPath(
            path3,
            color = Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = radius * 0.14f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

/**
 * Icona Vettoriale Amazon Music
 */
@Composable
private fun AmazonMusicBrandLogo() {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(40.dp)) {
        val corner = 10.dp.toPx()
        drawRoundRect(
            color = Color(0xFF00A8E1),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner)
        )

        val w = size.width
        val h = size.height

        // Sorriso / freccia Amazon stilizzata
        val smilePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.25f, h * 0.58f)
            cubicTo(w * 0.42f, h * 0.74f, w * 0.60f, h * 0.74f, w * 0.75f, h * 0.58f)
        }
        drawPath(
            smilePath,
            color = Color.White,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = w * 0.09f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )

        // Onde audio superiori
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(
            width = w * 0.08f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.42f), androidx.compose.ui.geometry.Offset(w * 0.38f, h * 0.30f), stroke.width)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.46f), androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.22f), stroke.width)
        drawLine(Color.White, androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.42f), androidx.compose.ui.geometry.Offset(w * 0.62f, h * 0.28f), stroke.width)
    }
}


