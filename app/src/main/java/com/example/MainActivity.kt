package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import com.example.ui.screens.LiveDetailScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainFeedScreen
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
                    val trackName = intent.getStringExtra("track") ?: return
                    val artist = intent.getStringExtra("artist") ?: ""
                    val album = intent.getStringExtra("album") ?: ""
                    val trackId = intent.getStringExtra("id") ?: ""
                    viewModel.updateNowPlayingFromBroadcast(trackId, trackName, artist, album)
                }
                "com.spotify.music.playbackstatechanged" -> {
                    if (!intent.getBooleanExtra("playing", false)) {
                        viewModel.clearNowPlayingFromBroadcast()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val imageLoader = ImageLoader.Builder(this)
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

        handleSpotifyCallback(intent)

        setContent {
            MyApplicationTheme {
                MusicApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSpotifyCallback(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.startSpotifyPolling()
        val filter = IntentFilter().apply {
            addAction("com.spotify.music.metadatachanged")
            addAction("com.spotify.music.playbackstatechanged")
        }
        ContextCompat.registerReceiver(this, spotifyReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopSpotifyPolling()
        try { unregisterReceiver(spotifyReceiver) } catch (e: Exception) { /* receiver was not registered */ }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MusicViewModel.FRIEND_REQUEST_CHANNEL_ID,
                "Richieste di Follow",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Nuove richieste di follow ricevute" }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicApp(viewModel: MusicViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val shareSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // Back handling for overlays/navigation
    BackHandler(
        enabled = uiState.activeStoryUserIndex != null ||
                uiState.activeProfileUser != null ||
                uiState.activeChatUser != null ||
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
                    onClearToast = { viewModel.clearToast() }
                )
            }
        }

        // Overlay: Profile Screen (Flusso D)
        AnimatedVisibility(
            visible = uiState.activeProfileUser != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            uiState.activeProfileUser?.let { activeUser ->
                val displayUser = if (activeUser.isCurrentUser) uiState.currentUser else activeUser
                ProfileScreen(
                    user = displayUser,
                    isCurrentUser = displayUser.isCurrentUser,
                    isFollowing = uiState.currentUser.followingIds.contains(displayUser.id),
                    isSpotifyConnected = uiState.isSpotifyConnected,
                    connectedServices = uiState.connectedServices,
                    spotifyError = uiState.spotifyError,
                    onToggleService = { viewModel.toggleConnectedService(it) },
                    onConnectSpotify = { _ -> viewModel.launchSpotifyAuth() },
                    onDisconnectSpotify = { viewModel.disconnectSpotify() },
                    onUpdateProfile = { name, username, avatarUrl, coverUrl ->
                        viewModel.updateProfile(name, username, avatarUrl, coverUrl)
                    },
                    onBack = { viewModel.closeProfile() },
                    onOpenNotifications = { viewModel.openNotifications() },
                    notificationCount = uiState.pendingFriendRequests.size,
                    onOpenChat = { targetUser -> viewModel.openChat(targetUser) },
                    onSelectTrack = { track, owner -> viewModel.inspectTrack(track, owner) },
                    onOpenLiveDetail = { targetUser -> viewModel.openStory(targetUser) },
                    onLogout = { viewModel.logout() }
                )
            }
        }

        // Overlay: Chat Screen (Flusso E)
        AnimatedVisibility(
            visible = uiState.activeChatUser != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
        ) {
            uiState.activeChatUser?.let { recipient ->
                val messages = uiState.chatMessages[recipient.id] ?: emptyList()
                ChatScreen(
                    recipient = recipient,
                    messages = messages,
                    onSendMessage = { text -> viewModel.sendMessage(recipient.id, text) },
                    onBack = { viewModel.closeChat() },
                    onOpenProfile = { user -> viewModel.openProfile(user) }
                )
            }
        }

        // Overlay: Live Detail Screen Fullscreen
        AnimatedVisibility(
            visible = uiState.activeStoryUserIndex != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(280, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(200)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(180))
        ) {
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
                    }
                )
            }
        }

        // Bottom Sheet: Sharing (Flusso B)
        if (uiState.isShareSheetOpen) {
            NowPlayingSheet(
                nowPlayingTrack = uiState.nowPlayingTrack,
                searchQuery = uiState.searchQuery,
                searchResults = uiState.searchResults,
                isSearching = uiState.isSearching,
                onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                onShareTrack = { viewModel.shareTrack(it) },
                onDismiss = { viewModel.closeShareSheet() },
                sheetState = shareSheetState
            )
        }

        // Dialog: Ricerca Persone
        if (uiState.showPeopleSearch) {
            PeopleSearchDialog(
                searchQuery = uiState.peopleSearchQuery,
                searchResults = uiState.peopleSearchResults,
                sentRequestIds = uiState.sentRequestIds,
                onQueryChanged = { viewModel.onPeopleSearchQueryChanged(it) },
                onSendRequest = { viewModel.sendFollowRequest(it) },
                onOpenProfile = { user -> viewModel.openProfile(user) },
                onDismiss = { viewModel.closePeopleSearch() }
            )
        }

        // Dialog: Notifiche & Richieste Amicizia
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
            TrackDetailDialog(
                track = track,
                user = owner,
                onDismiss = { viewModel.closeTrackInspector() },
                onSendMessage = { user, trk -> viewModel.openChat(user, trk) },
                onShareToMyFeed = { trk -> viewModel.shareTrack(trk) }
            )
        }
    }
}
