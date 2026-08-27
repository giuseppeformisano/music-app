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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import com.example.R
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
    onUpdateProfile: (name: String, username: String, avatarUrl: String, coverUrl: String?, bio: String) -> Unit = { _, _, _, _, _ -> },
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit = {},
    notificationCount: Int = 0,
    onOpenChat: (User) -> Unit,
    onSelectTrack: (Track, User) -> Unit,
    onOpenLiveDetail: (User) -> Unit = {},
    onLogout: () -> Unit,
    followers: List<User> = emptyList(),
    following: List<User> = emptyList(),
    onUnfollow: (User) -> Unit = {},
    onRemoveFollower: (User) -> Unit = {},
    onSendFollowRequest: () -> Unit = {},
    isSentRequest: Boolean = false,
    onOpenUserProfile: (User) -> Unit = {},
    isNotificationListenerEnabled: Boolean = false,
    onEnableNotificationListener: () -> Unit = {},
    applyCoverToFeed: Boolean = false,
    onToggleApplyCoverToFeed: (Boolean) -> Unit = {},
    liveNotificationsEnabled: Boolean = true,
    onToggleLiveNotifications: (Boolean) -> Unit = {},
    onOpenChatList: () -> Unit = {},
    onShareTrack: (Track) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showEditProfileSheet by remember { mutableStateOf(false) }
    var showConnectAccountsSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showFollowersDialog by remember { mutableStateOf(false) }
    var showFollowingDialog by remember { mutableStateOf(false) }

    // Copertina di sfondo atmosferico: custom cover > track in riproduzione > prima condivisione > default
    val atmosphericCoverUrl = user.coverUrl?.takeIf { it.isNotBlank() }
        ?: user.currentTrack?.coverUrl?.takeIf { it.isNotBlank() }
        ?: user.sharedTracks.firstOrNull()?.coverUrl?.takeIf { it.isNotBlank() }
        ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BlackPitch)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume touches — prevent click-through to feed */ }
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
                .blur(radius = 12.dp)
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
                .padding(top = 28.dp)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        ) {
            // Header superiore fisso
            ProfileTopHeader(
                onBack = onBack,
                onNotificationsClick = onOpenNotifications,
                notificationCount = notificationCount,
                isCurrentUser = isCurrentUser,
                onLogout = onLogout,
                onOpenSettings = { showSettingsSheet = true },
                onOpenChatList = onOpenChatList
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
                    isFollowing = isFollowing,
                    isSentRequest = isSentRequest,
                    onEditClick = { showEditProfileSheet = true },
                    onConnectAccountClick = { showConnectAccountsSheet = true },
                    onFollowersTap = { showFollowersDialog = true },
                    onFollowingTap = { showFollowingDialog = true },
                    onSendFollowRequest = onSendFollowRequest
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
                        onShare = { user.currentTrack?.let { onShareTrack(it) } },
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
                onSave = { newName, newUsername, newAvatar, newCover, newBio ->
                    onUpdateProfile(newName, newUsername, newAvatar, newCover, newBio)
                    showEditProfileSheet = false
                }
            )
        }

        // ================= DIALOG: COLLEGA ACCOUNT STREAMING =================
        if (showConnectAccountsSheet) {
            ConnectAccountsDialog(
                connectedServices = connectedServices,
                spotifyError = spotifyError,
                isNotificationListenerEnabled = isNotificationListenerEnabled,
                onConnectSpotify = { onConnectSpotify(context) },
                onDisconnectSpotify = onDisconnectSpotify,
                onToggleService = onToggleService,
                onEnableNotificationListener = onEnableNotificationListener,
                onDismiss = { showConnectAccountsSheet = false }
            )
        }

        // ================= DIALOG: FOLLOWER =================
        if (showFollowersDialog) {
            FollowersFollowingDialog(
                title = "Follower",
                users = followers,
                currentUserId = user.id,
                onOpenProfile = { u ->
                    showFollowersDialog = false
                    onOpenUserProfile(u)
                },
                onRemove = { u -> onRemoveFollower(u) },
                onDismiss = { showFollowersDialog = false }
            )
        }

        // ================= DIALOG: FOLLOWING =================
        if (showFollowingDialog) {
            FollowersFollowingDialog(
                title = "Following",
                users = following,
                currentUserId = user.id,
                onOpenProfile = { u ->
                    showFollowingDialog = false
                    onOpenUserProfile(u)
                },
                onRemove = { u -> onUnfollow(u) },
                onDismiss = { showFollowingDialog = false }
            )
        }

        // ================= DIALOG: IMPOSTAZIONI =================
        if (showSettingsSheet) {
            SettingsDialog(
                applyCoverToFeed = applyCoverToFeed,
                liveNotificationsEnabled = liveNotificationsEnabled,
                onSave = { newApplyCoverToFeed, newLiveNotifs ->
                    onToggleApplyCoverToFeed(newApplyCoverToFeed)
                    onToggleLiveNotifications(newLiveNotifs)
                    showSettingsSheet = false
                },
                onDismiss = { showSettingsSheet = false }
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
    onOpenSettings: () -> Unit = {},
    onOpenChatList: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("profile_top_header")
    ) {
        // Sinistra: torna al feed + impostazioni
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            if (isCurrentUser) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .testTag("profile_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Impostazioni",
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Centro: logo "m" — sempre centrato indipendentemente dalle icone a destra
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(PureWhite)
                .align(Alignment.Center),
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

        // Destra: chat + notifiche + logout (solo utente corrente)
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCurrentUser) {
                IconButton(
                    onClick = onOpenChatList,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("profile_chat_list_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = "Messaggi",
                        tint = PureWhite,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Box senza clip per permettere al badge di fuoriuscire senza essere tagliato
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (notificationCount > 9) "9+" else notificationCount.toString(),
                            color = Color(0xFF111111),
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 8.sp
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
    isFollowing: Boolean = false,
    isSentRequest: Boolean = false,
    onEditClick: () -> Unit,
    onConnectAccountClick: () -> Unit,
    onFollowersTap: () -> Unit = {},
    onFollowingTap: () -> Unit = {},
    onSendFollowRequest: () -> Unit = {},
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
                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFF1E1E24)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Avatar",
                            tint = PureWhite.copy(alpha = 0.8f),
                            modifier = Modifier.size(44.dp)
                        )
                        if (user.avatarUrl.isNotBlank()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = "Avatar ${user.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    com.example.ui.components.PresenceDot(
                        isOnline = user.isOnline,
                        isLive = user.currentTrack != null,
                        size = 20.dp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
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
                if (user.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = user.bio,
                        color = PureWhite.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.1.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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

            // Destra: Follower / Following (cliccabili)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StatItem(
                    label = "Follower",
                    value = user.followerIds.size.toString(),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFollowersTap() }
                )
                Spacer(modifier = Modifier.height(14.dp))
                StatItem(
                    label = "Following",
                    value = user.followingIds.size.toString(),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFollowingTap() }
                )
            }
        }

        if (isCurrentUser) {
            Spacer(modifier = Modifier.height(16.dp))
            ProfileActionButtons(
                isCurrentUser = true,
                onEditClick = onEditClick,
                onConnectAccountClick = onConnectAccountClick
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
            when {
                isFollowing -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PureWhite.copy(alpha = 0.06f))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Segui già",
                            color = SubtitleGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                isSentRequest -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(PureWhite.copy(alpha = 0.06f))
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Richiesta inviata",
                            color = SubtitleGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, PureWhite, RoundedCornerShape(20.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onSendFollowRequest() }
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Segui",
                            color = PureWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
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
        Text(
            text = "SHARED TRACKS",
            color = PureWhite.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 3.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "nessun brano condiviso",
                    color = PureWhite.copy(alpha = 0.18f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
            return@Column
        }

        val size = tracks.size
        // Infinite ring: start al centro di Int.MAX_VALUE allineato a size
        val startPage = (Int.MAX_VALUE / 2).let { it - it % size }
        val pagerState = rememberPagerState(initialPage = startPage) { Int.MAX_VALUE }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 68.dp),
            pageSpacing = (-16).dp,   // sovrapposizione lieve per effetto depth
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) { page ->
            val track = tracks[page % size]
            // rawOffset: 0=centro, +1=sinistra, -1=destra (continuo durante il drag)
            val rawOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                .coerceIn(-1f, 1f)
            val absOffset = abs(rawOffset)
            val isCenter = absOffset < 0.5f

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f - absOffset)
                    .graphicsLayer {
                        // CoverFlow 3D: rotationY + cameraDistance danno prospettiva reale
                        rotationY = rawOffset * 52f
                        cameraDistance = 7.5f * density
                        // Le card laterali scalano e retrocedono (depth)
                        val sc = androidx.compose.ui.util.lerp(0.78f, 1f, 1f - absOffset)
                        scaleX = sc; scaleY = sc
                        alpha = androidx.compose.ui.util.lerp(0.42f, 1f, 1f - absOffset)
                        // Spostamento verso il centro (le card si avvicinano ruotando)
                        translationX = rawOffset * 34f * density
                        translationY = absOffset * 18f * density
                    }
                    // Niente shadow: su sfondo scuro proiettava un alone nero
                    // semi-trasparente sotto la card. La profondità resta data da
                    // scala/rotazione/alpha del CoverFlow.
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onTrackClick(track) },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient scuro solo sulla card centrale
                if (isCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val displayTrack = tracks[pagerState.currentPage % size]
        Text(
            text = displayTrack.title,
            color = PureWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = displayTrack.artist,
            color = SubtitleGray.copy(alpha = 0.65f),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
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
private fun FollowersFollowingDialog(
    title: String,
    users: List<User>,
    currentUserId: String,
    onOpenProfile: (User) -> Unit,
    onRemove: (User) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(users, searchQuery) {
        if (searchQuery.isBlank()) users
        else users.filter {
            it.name.lowercase().contains(searchQuery.lowercase()) ||
            it.username.lowercase().contains(searchQuery.lowercase())
        }
    }

    com.example.ui.components.UtilityDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp, bottom = 20.dp)
        ) {
            // Header standardizzato in alto a sinistra
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            }

            // Campo di ricerca
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PureWhite.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = SolidColor(PureWhite),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(text = "Cerca...", color = SubtitleGray, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                )
            }

            // Lista utenti o empty state (senza riquadri grigi di sfondo)
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nessun utente",
                        color = SubtitleGray.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered) { u ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onOpenProfile(u) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = u.avatarUrl,
                                contentDescription = "Avatar ${u.name}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = u.name,
                                    color = PureWhite,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "@${u.username.removePrefix("@")}",
                                    color = SubtitleGray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PureWhite.copy(alpha = 0.08f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onRemove(u) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Rimuovi",
                                    color = SubtitleGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    user: User,
    currentCoverUrl: String,
    onDismiss: () -> Unit,
    onSave: (newName: String, newUsername: String, newAvatar: String, newCover: String, newBio: String) -> Unit
) {
    var nameInput by remember(user.name) { mutableStateOf(user.name) }
    var usernameInput by remember(user.username) { mutableStateOf(user.username.removePrefix("@")) }
    var avatarUrlInput by remember(user.avatarUrl) { mutableStateOf(user.avatarUrl) }
    var coverUrlInput by remember(user.coverUrl, currentCoverUrl) { mutableStateOf(user.coverUrl?.takeIf { it.isNotBlank() } ?: currentCoverUrl) }
    var bioInput by remember(user.bio) { mutableStateOf(user.bio) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = com.example.data.ImageUtils.saveImageLocally(context, it, "avatar", user.id, maxDimension = 320, quality = 80)
            if (localPath != null) {
                avatarUrlInput = localPath
            }
        }
    }

    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val localPath = com.example.data.ImageUtils.saveImageLocally(context, it, "cover", user.id, maxDimension = 640, quality = 75)
            if (localPath != null) {
                coverUrlInput = localPath
            }
        }
    }

    com.example.ui.components.UtilityDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header standardizzato in alto a sinistra
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Modifica Profilo",
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Contenuto scrollabile
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                // ================= COPERTINA E IMMAGINE PROFILO SOVRAPPOSTE (ESATTAMENTE COME IN FOTO) =================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Copertina (card rettangolare arrotondata in alto)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1E1E24))
                            .clickable { coverPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = coverUrlInput,
                            contentDescription = "Copertina",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Overlay scuro traslucido con fotocamera + "Cambia Copertina"
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cambia Copertina",
                                    tint = PureWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Cambia Copertina",
                                    color = PureWhite,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Foto profilo circolare sovrapposta al bordo inferiore della copertina
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(3.dp, BlackPitch, CircleShape)
                            .background(Color(0xFF141418))
                            .clickable { avatarPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = avatarUrlInput,
                            contentDescription = "Foto Profilo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Overlay circolare traslucido con fotocamera + "Cambia Foto"
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.50f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Cambia Foto",
                                    tint = PureWhite,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Cambia Foto",
                                    color = PureWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ================= CAMPI TESTO INSERIMENTO (NOME UTENTE, NOME VISUALIZZATO, BIO) =================
                // 1. Nome Utente (@nickname)
                Text(
                    text = "Nome Utente",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@",
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                    BasicTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it.removePrefix("@").trim() },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(PureWhite),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_username")
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PureWhite.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Nome Visualizzato (name)
                Text(
                    text = "Nome Visualizzato",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = PureWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(PureWhite),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_name")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PureWhite.copy(alpha = 0.12f))
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Bio
                Text(
                    text = "Bio",
                    color = SubtitleGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                BasicTextField(
                    value = bioInput,
                    onValueChange = { bioInput = it },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = PureWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(PureWhite),
                    minLines = 1,
                    maxLines = 4,
                    decorationBox = { innerTextField ->
                        if (bioInput.isEmpty()) {
                            Text(
                                text = "Aggiungi una breve biografia...",
                                color = SubtitleGray.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_bio")
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(PureWhite.copy(alpha = 0.12f))
                )
            }

            // Pulsante Salva fisso in fondo
            Button(
                onClick = {
                    onSave(
                        nameInput.trim().ifBlank { user.name },
                        usernameInput.trim().ifBlank { user.username },
                        avatarUrlInput.trim().ifBlank { user.avatarUrl },
                        coverUrlInput.trim(),
                        bioInput.trim()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_profile"),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = BlackPitch
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BlackPitch,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salva modifiche",
                    color = BlackPitch,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
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
    isNotificationListenerEnabled: Boolean = false,
    onConnectSpotify: () -> Unit,
    onDisconnectSpotify: () -> Unit,
    onToggleService: (String) -> Unit,
    onEnableNotificationListener: () -> Unit = {},
    onDismiss: () -> Unit
) {
    com.example.ui.components.UtilityDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp, bottom = 20.dp)
        ) {
            // Header standardizzato in alto a sinistra
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
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
            }

            // Spotify Premium e Spotify Free sono MUTUAMENTE ESCLUSIVI: collegato uno,
            // l'altro viene oscurato e reso non cliccabile.
            val premiumConnected = connectedServices["spotify"] == true
            val freeConnected = connectedServices["spotify_free"] == true

            StreamingAccountItem(
                serviceName = "Spotify Premium",
                serviceDesc = "Collega l'account: dati in tempo reale via API",
                isConnected = premiumConnected,
                enabled = !freeConnected,
                brandColor = Color(0xFF1DB954),
                iconComposable = { SpotifyBrandLogo() },
                onToggle = {
                    if (premiumConnected) onDisconnectSpotify()
                    else onConnectSpotify()
                },
                testTag = "service_spotify_premium"
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

            // Stato "collegato" RICORDATO (scelta esplicita dell'utente), anche se
            // funzionalmente dipende dalle notifiche.
            StreamingAccountItem(
                serviceName = "Spotify Free",
                serviceDesc = "Rileva l'ascolto dalle notifiche",
                isConnected = freeConnected,
                enabled = !premiumConnected,
                brandColor = Color(0xFF1DB954),
                iconComposable = { SpotifyBrandLogo() },
                onToggle = { onToggleService("spotify_free") },
                testTag = "service_spotify_free"
            )

            Spacer(modifier = Modifier.height(12.dp))

            StreamingAccountItem(
                serviceName = "Amazon Music",
                serviceDesc = "Rileva l'ascolto dalle notifiche",
                isConnected = connectedServices["amazon_music"] == true,
                brandColor = Color(0xFF00A8E1),
                iconComposable = { AmazonMusicBrandLogo() },
                onToggle = { onToggleService("amazon_music") },
                testTag = "service_amazon_music"
            )
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
    testTag: String,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isConnected) brandColor.copy(alpha = 0.12f) else PureWhite.copy(alpha = 0.05f))
            // Oscurato e non cliccabile quando disabilitato (mutua esclusione)
            .graphicsLayer { alpha = if (enabled) 1f else 0.35f }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
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
 * Icona Spotify
 */
@Composable
private fun SpotifyBrandLogo() {
    Image(
        painter = painterResource(id = R.drawable.ic_spotify),
        contentDescription = "Spotify",
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
    )
}

/**
 * Icona Amazon Music
 */
@Composable
private fun AmazonMusicBrandLogo() {
    Image(
        painter = painterResource(id = R.drawable.ic_amazon_music_app),
        contentDescription = "Amazon Music",
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}

/**
 * Dialog / Pannello Impostazioni
 */
@Composable
private fun SettingsDialog(
    applyCoverToFeed: Boolean,
    liveNotificationsEnabled: Boolean,
    onSave: (Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var localApplyCoverToFeed by remember(applyCoverToFeed) { mutableStateOf(applyCoverToFeed) }
    var localLiveNotificationsEnabled by remember(liveNotificationsEnabled) { mutableStateOf(liveNotificationsEnabled) }

    com.example.ui.components.UtilityDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 36.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header standardizzato in alto a sinistra
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Impostazioni",
                    color = PureWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contenuto scrollabile delle impostazioni (fuso direttamente con lo sfondo)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "ASPETTO E SFONDO",
                    color = SubtitleGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { localApplyCoverToFeed = !localApplyCoverToFeed }
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = "Copertina su Live e Feed",
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Applica la tua immagine di copertina profilo come sfondo atmosferico anche nelle sezioni Live e Feed",
                            color = SubtitleGray,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }

                    Switch(
                        checked = localApplyCoverToFeed,
                        onCheckedChange = { localApplyCoverToFeed = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = Color(0xFF383842),
                            uncheckedThumbColor = SubtitleGray,
                            uncheckedTrackColor = Color(0xFF1E1E24)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "NOTIFICHE",
                    color = SubtitleGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { localLiveNotificationsEnabled = !localLiveNotificationsEnabled }
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 16.dp)
                    ) {
                        Text(
                            text = "Notifiche Live dei seguiti",
                            color = PureWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ricevi una notifica push quando una persona che segui avvia una sessione Live",
                            color = SubtitleGray,
                            fontSize = 13.sp,
                            lineHeight = 17.sp
                        )
                    }

                    Switch(
                        checked = localLiveNotificationsEnabled,
                        onCheckedChange = { localLiveNotificationsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureWhite,
                            checkedTrackColor = Color(0xFF383842),
                            uncheckedThumbColor = SubtitleGray,
                            uncheckedTrackColor = Color(0xFF1E1E24)
                        )
                    )
                }
            }

            // Pulsante Salva modifiche coerente con Modifica Profilo
            Button(
                onClick = { onSave(localApplyCoverToFeed, localLiveNotificationsEnabled) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PureWhite,
                    contentColor = BlackPitch
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = BlackPitch,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salva modifiche",
                    color = BlackPitch,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}


