package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.NotificationsDialog
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.PeopleSearchDialog
import com.example.ui.components.TrackDetailDialog
import com.example.ui.components.UpdateBanner
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.LiveDetailScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainFeedScreen
import com.example.ui.screens.NotificationOnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.BlackPitch
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MusicViewModel by viewModels()

    private val spotifyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.spotify.music.metadatachanged" -> {
                    // Broadcast ancora supportato da versioni Spotify più vecchie
                    val trackName = intent.getStringExtra("track") ?: return
                    val artist = intent.getStringExtra("artist") ?: ""
                    val album = intent.getStringExtra("album") ?: ""
                    val trackId = intent.getStringExtra("id") ?: ""
                    viewModel.updateNowPlayingFromBroadcast(trackId, trackName, artist, album, source = "spotify")
                }
                "com.spotify.music.playbackstatechanged" -> {
                    val playing = intent.getBooleanExtra("playing", false)
                    if (playing) {
                        // Spotify in play ma non abbiamo info brano: chiedi al Web API
                        viewModel.startSpotifyPolling()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(com.example.data.Base64Fetcher.Factory())
            }
            .crossfade(true)
            .allowHardware(false)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        }
        // Microfono per il Pulse vocale (registrazione della voce → inviluppo tattile/onda)
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 1002)
        }

        handleSpotifyCallback(intent)
        handleNotificationIntent(intent)

        setContent {
            // Cappa il font scale a 1.15× per evitare che testi troppo grandi
            // rompano i layout con accessibilità al 125-150%.
            val originalDensity = androidx.compose.ui.platform.LocalDensity.current
            val cappedDensity = androidx.compose.ui.unit.Density(
                density = originalDensity.density,
                fontScale = originalDensity.fontScale.coerceAtMost(1.15f)
            )
            androidx.compose.runtime.CompositionLocalProvider(
                coil.compose.LocalImageLoader provides imageLoader,
                androidx.compose.ui.platform.LocalDensity provides cappedDensity
            ) {
                MyApplicationTheme {
                    MusicApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyCallback(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null) return
        val openBool = intent.getBooleanExtra(EXTRA_OPEN_NOTIFICATIONS, false)
        val openString = intent.getStringExtra("open_notifications")
        val openLiveBool = intent.getBooleanExtra(EXTRA_OPEN_LIVE, false)
        val openLiveString = intent.getStringExtra("open_live")
        val openChatBool = intent.getBooleanExtra(EXTRA_OPEN_CHAT, false)
        val openChatString = intent.getStringExtra("open_chat")
        val openPulseBool = intent.getBooleanExtra(EXTRA_OPEN_PULSE, false)
        val openPulseString = intent.getStringExtra("open_pulse")
        val notifType = intent.getStringExtra("type")
        val hostUserId = intent.getStringExtra("hostUserId")
        val senderId = intent.getStringExtra("senderId")

        val isLiveNav = openLiveBool || openLiveString == "true" || notifType == "live_start"
        val isChatNav = openChatBool || openChatString == "true" || notifType == "new_message"
        val isPulseNav = openPulseBool || openPulseString == "true" || notifType == "new_pulse"
        val isFollowNav = openBool || openString == "true" || notifType == "follow_request"

        if (isPulseNav) {
            // Dialog dedicata al Pulse: non azzera gli overlay (è a sé, con swipe-down).
            val samples = intent.getStringExtra("pulse") ?: ""
            if (samples.isNotBlank()) {
                viewModel.openPulseFromNotification(
                    senderId = senderId ?: "",
                    senderName = intent.getStringExtra("senderName") ?: "",
                    avatarUrl = intent.getStringExtra("avatarUrl") ?: "",
                    samples = samples,
                    audioId = intent.getStringExtra("pulseAudioId")?.takeIf { it.isNotBlank() },
                    audioUrl = intent.getStringExtra("pulseAudioUrl")?.takeIf { it.isNotBlank() }
                )
            }
            return
        }

        // Porta SEMPRE al punto della notifica, da qualsiasi sezione: prima azzera gli overlay.
        if (isLiveNav || isChatNav || isFollowNav) {
            viewModel.clearOverlaysForNavigation()
        }

        if (isLiveNav) {
            if (!hostUserId.isNullOrBlank()) viewModel.openLiveFromNotification(hostUserId)
        } else if (isChatNav) {
            if (!senderId.isNullOrBlank()) viewModel.openChatFromNotification(senderId)
        } else if (isFollowNav) {
            viewModel.openNotifications()
        }
    }

    override fun onResume() {
        super.onResume()
        AppFirebaseMessagingService.isAppForeground = true
        viewModel.onAppForeground()
        viewModel.setOnline(true)
        viewModel.checkNotificationListenerEnabled()
        viewModel.startSpotifyPolling()
        val filter = IntentFilter().apply {
            addAction("com.spotify.music.metadatachanged")
            addAction("com.spotify.music.playbackstatechanged")
        }
        ContextCompat.registerReceiver(this, spotifyReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        AppFirebaseMessagingService.isAppForeground = false
        try { unregisterReceiver(spotifyReceiver) } catch (e: Exception) { /* receiver was not registered */ }
    }

    override fun onStop() {
        super.onStop()
        viewModel.onAppStopped()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onAppDestroyed()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MusicViewModel.FRIEND_REQUEST_CHANNEL_ID,
                "Richieste di Follow",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Nuove richieste di follow ricevute"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun handleSpotifyCallback(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "com.aistudio.music.livefeed" && uri.host == "callback") {
            val code = uri.getQueryParameter("code") ?: return
            viewModel.handleSpotifyCallback(code)
        }
    }

    companion object {
        const val EXTRA_OPEN_NOTIFICATIONS = "open_notifications"
        const val EXTRA_OPEN_LIVE = "open_live"
        const val EXTRA_OPEN_CHAT = "open_chat"
        const val EXTRA_OPEN_PULSE = "open_pulse"
    }
}

@Composable
fun MusicApp(viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Back handling for overlays/navigation
    BackHandler(
        enabled = uiState.activeStoryUserIndex != null ||
                uiState.activeProfileUser != null ||
                uiState.activeChatUser != null ||
                uiState.isChatListOpen ||
                uiState.selectedTrackDetail != null ||
                uiState.showPeopleSearch ||
                uiState.showNotifications
    ) {
        when {
            uiState.showPeopleSearch -> viewModel.closePeopleSearch()
            uiState.showNotifications -> viewModel.closeNotifications()
            uiState.selectedTrackDetail != null -> viewModel.closeTrackInspector()
            uiState.activeStoryUserIndex != null -> viewModel.closeStory()
            uiState.activeChatUser != null -> viewModel.closeChat()
            uiState.isChatListOpen -> viewModel.closeChatList()
            uiState.activeProfileUser != null -> viewModel.closeProfile()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlackPitch)
    ) {
        // Main Screen Switcher (Login vs Feed)
        AnimatedContent(
            targetState = uiState.isLoggedIn,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "auth_state"
        ) { loggedIn ->
            if (!loggedIn) {
                LoginScreen(
                    onLoginClick = { context -> viewModel.loginWithGoogle(context) },
                    isLoggingIn = uiState.isLoggingIn,
                    loginError = uiState.loginError
                )
            } else {
                if (uiState.showNotificationOnboarding) {
                    NotificationOnboardingScreen(
                        onEnable = { viewModel.completeNotificationOnboarding(openSettings = true, context = context) },
                        onSkip = { viewModel.completeNotificationOnboarding(openSettings = false, context = context) }
                    )
                } else {
                    // Solo gli utenti effettivamente seguiti appaiono nel feed e nelle live
                    val followingIds = uiState.currentUser.followingIds
                    MainFeedScreen(
                        currentUser = uiState.currentUser,
                        feedUsers = uiState.feedUsers.filter { followingIds.contains(it.id) },
                        stories = viewModel.getStoriesList(),
                        onOpenLiveDetail = { user -> viewModel.openStory(user) },
                        onOpenProfile = { user -> viewModel.openProfile(user) },
                        onSelectTrack = { track, user -> viewModel.inspectTrack(track, user) },
                        onOpenShareSheet = { viewModel.openShareSheet() },
                        onOpenPeopleSearch = { viewModel.openPeopleSearch() },
                        feedbackToast = uiState.feedbackToast,
                        onClearToast = { viewModel.clearToast() },
                        applyCoverToFeed = uiState.applyCoverToFeed,
                        isNotificationListenerEnabled = uiState.isNotificationListenerEnabled,
                        onEnableNotificationListener = { viewModel.openNotificationListenerSettings(context) }
                    )
                }
            }
        }

        // Overlay: Profile Screen (Flusso D)
        AnimatedVisibility(
            visible = uiState.activeProfileUser != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            uiState.activeProfileUser?.let { activeUser ->
                val isMe = activeUser.isCurrentUser || (uiState.currentUser.id.isNotBlank() && activeUser.id == uiState.currentUser.id)
                val displayUser = if (isMe) uiState.currentUser.copy(isCurrentUser = true) else activeUser
                if (isMe) {
                    LaunchedEffect(displayUser.id) { viewModel.loadSocialDetails(displayUser) }
                }
                ProfileScreen(
                    user = displayUser,
                    isCurrentUser = isMe,
                    isFollowing = uiState.currentUser.followingIds.contains(displayUser.id),
                    isSpotifyConnected = uiState.isSpotifyConnected,
                    connectedServices = uiState.connectedServices,
                    spotifyError = uiState.spotifyError,
                    onToggleService = { viewModel.toggleConnectedService(it) },
                    onConnectSpotify = { _ -> viewModel.launchSpotifyAuth() },
                    onDisconnectSpotify = { viewModel.disconnectSpotify() },
                    onUpdateProfile = { name, username, avatarUrl, coverUrl, bio ->
                        viewModel.updateProfile(name, username, avatarUrl, coverUrl, bio)
                    },
                    onBack = { viewModel.closeProfile() },
                    onOpenNotifications = { viewModel.openNotifications() },
                    notificationCount = uiState.pendingFriendRequests.size,
                    onOpenChat = { targetUser -> viewModel.openChat(targetUser) },
                    onSelectTrack = { track, owner -> viewModel.inspectTrack(track, owner) },
                    onOpenLiveDetail = { targetUser -> viewModel.openStory(targetUser) },
                    onLogout = { viewModel.logout() },
                    followers = uiState.followerDetails,
                    following = uiState.followingDetails,
                    onUnfollow = { user -> viewModel.unfollow(user) },
                    onRemoveFollower = { user -> viewModel.removeFollower(user) },
                    onSendFollowRequest = { viewModel.sendFollowRequest(displayUser) },
                    isSentRequest = uiState.sentRequestIds.contains(displayUser.id),
                    onOpenUserProfile = { u -> viewModel.openProfile(u) },
                    isNotificationListenerEnabled = uiState.isNotificationListenerEnabled,
                    onEnableNotificationListener = { viewModel.openNotificationListenerSettings(context) },
                    applyCoverToFeed = uiState.applyCoverToFeed,
                    onToggleApplyCoverToFeed = { viewModel.setApplyCoverToFeed(it) },
                    liveNotificationsEnabled = uiState.liveNotificationsEnabled,
                    onToggleLiveNotifications = { viewModel.setLiveNotificationsEnabled(it) },
                    onOpenChatList = { viewModel.openChatList() }
                )
            }
        }

        // Dialog: Chat List (Messaggi) — stile dialog immersivo con swipe-down per chiudere
        if (uiState.isChatListOpen) {
            ChatListScreen(
                conversations = uiState.conversations,
                currentUserId = uiState.currentUser.id,
                followingIds = uiState.currentUser.followingIds.toSet(),
                searchQuery = uiState.userSearchQuery,
                searchResults = uiState.userSearchResults,
                onSearchQueryChanged = { viewModel.onUserSearchQueryChanged(it) },
                onOpenChat = { user -> viewModel.openChat(user) },
                onDismiss = { viewModel.closeChatList() }
            )
        }

        // Dialog: Chat Screen (con swipe-down per chiudere)
        if (uiState.activeChatUser != null) {
            uiState.activeChatUser?.let { recipient ->
                val messages = uiState.chatMessages[recipient.id] ?: emptyList()
                ChatScreen(
                    recipient = recipient,
                    currentUser = uiState.currentUser,
                    messages = messages,
                    onSendMessage = { text -> viewModel.sendMessage(recipient.id, text) },
                    onDismiss = { viewModel.closeChat() },
                    onOpenProfile = { user -> viewModel.openProfile(user) },
                    applyCoverToFeed = uiState.applyCoverToFeed,
                    backgroundCoverUrl = uiState.currentUser.coverUrl
                )
            }
        }

        // Dettaglio Live (stesso scaffold immersivo del dettaglio feed, con contenuti live)
        if (uiState.activeStoryUserIndex != null) {
            val allStories = viewModel.getStoriesList()
            val activeIndex = uiState.activeStoryUserIndex
            val liveUser = if (activeIndex != null && activeIndex in allStories.indices) allStories[activeIndex] else null

            if (liveUser != null) {
                LiveDetailScreen(
                    user = liveUser,
                    onClose = { viewModel.closeStory() },
                    onSendLiveReply = { user, messageText, track ->
                        viewModel.sendMessage(user.id, messageText, track)
                        viewModel.openChat(user)
                    },
                    onOpenUserProfile = { user ->
                        viewModel.closeStory()
                        viewModel.openProfile(user)
                    },
                    onSendPulse = { u, samples, audio -> viewModel.sendPulse(u.id, samples, audio) },
                    onSetTrackAsCover = { trk -> viewModel.setTrackAsCover(trk) }
                )
            }
        }

        // Dialog: Condivisione / ricerca brano (unificata)
        if (uiState.isShareSheetOpen) {
            NowPlayingSheet(
                nowPlayingTrack = uiState.nowPlayingTrack,
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                isSearching = uiState.isSearching,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onShareTrack = { viewModel.shareTrack(it) },
                onDismiss = { viewModel.closeShareSheet() }
            )
        }

        // Dialog: Ricerca Persone
        if (uiState.showPeopleSearch) {
            PeopleSearchDialog(
                searchQuery = uiState.peopleSearchQuery,
                searchResults = uiState.peopleSearchResults,
                sentRequestIds = uiState.sentRequestIds,
                followingIds = uiState.currentUser.followingIds.toSet(),
                onQueryChanged = { viewModel.onPeopleSearchQueryChanged(it) },
                onSendRequest = { viewModel.sendFollowRequest(it) },
                onOpenProfile = { user -> viewModel.openProfile(user) },
                onDismiss = { viewModel.closePeopleSearch() }
            )
        }

        // Dialog: Notifiche e Richieste Amicizia
        if (uiState.showNotifications) {
            NotificationsDialog(
                pendingRequests = uiState.pendingFriendRequests,
                onAccept = { viewModel.acceptFriendRequest(it) },
                onReject = { viewModel.rejectFriendRequest(it) },
                onDismiss = { viewModel.closeNotifications() }
            )
        }

        UpdateBanner(
            update = uiState.availableUpdate,
            downloadProgress = uiState.updateDownloadProgress,
            onInstall = { viewModel.downloadAndInstallUpdate(context) },
            onDismiss = { viewModel.dismissUpdate() }
        )

        // Dialog: Track Detail Inspector
        uiState.selectedTrackDetail?.let { (track, owner) ->
            val isMyTrack = owner?.isCurrentUser == true || (owner?.id?.isNotBlank() == true && owner.id == uiState.currentUser.id)
            TrackDetailDialog(
                track = track,
                user = owner,
                onDismiss = { viewModel.closeTrackInspector() },
                onSendMessage = { user, trk -> viewModel.openChat(user, trk) },
                onSendTextMessage = { user, text, trk -> viewModel.sendMessage(user.id, text, trk) },
                onOpenUserProfile = { user -> viewModel.openProfile(user) },
                onShareToMyFeed = { trk -> viewModel.shareTrack(trk) },
                isMyTrack = isMyTrack,
                onDeleteTrack = { trk -> viewModel.deleteSharedTrack(trk) },
                onSetAsCover = { trk -> viewModel.setTrackAsCover(trk) }
            )
        }

        // Dialog: Pulse ricevuto (dedicata, swipe-down)
        uiState.activePulse?.let { pulse ->
            com.example.ui.components.PulseReceiveDialog(
                pulse = pulse,
                onDismiss = { viewModel.dismissPulse() }
            )
        }

        // Dialog: Changelog (mostrato una volta dopo un aggiornamento)
        if (uiState.isLoggedIn && uiState.showChangelog) {
            com.example.ui.components.ChangelogDialog(
                title = com.example.AppChangelog.TITLE,
                lines = com.example.AppChangelog.LINES,
                onDismiss = { viewModel.dismissChangelog() }
            )
        }
    }
}
