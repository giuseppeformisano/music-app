package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AuthRepository
import com.example.data.FirebaseRepository
import com.example.data.MusicRepository
import com.example.data.SpotifyAuthRepository
import com.example.data.SpotifyWebApiRepository
import com.example.data.UpdateRepository
import com.example.data.VersionInfo
import java.io.File
import com.example.model.ChatMessage
import com.example.model.FriendRequest
import com.example.model.Track
import com.example.model.User
import com.example.model.UserStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID

enum class SearchTab { TRACKS, USERS }

data class MusicUiState(
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val loginError: String? = null,
    val isSpotifyConnected: Boolean = false,
    val connectedServices: Map<String, Boolean> = mapOf(
        "spotify" to false,
        "amazon_music" to false
    ),
    val currentUser: User = User(
        id = "",
        name = "",
        username = "",
        avatarUrl = "",
        isCurrentUser = true
    ),
    val feedUsers: List<User> = emptyList(),
    val activeStoryUserIndex: Int? = null,
    val isShareSheetOpen: Boolean = false,
    val searchTab: SearchTab = SearchTab.TRACKS,
    val nowPlayingTrack: Track? = null,
    val searchQuery: String = "",
    val searchResults: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val userSearchQuery: String = "",
    val userSearchResults: List<User> = emptyList(),
    val isSearchingUsers: Boolean = false,
    val activeProfileUser: User? = null,
    val activeChatUser: User? = null,
    val chatMessages: Map<String, List<ChatMessage>> = emptyMap(),
    val selectedTrackDetail: Pair<Track, User?>? = null,
    val feedbackToast: String? = null,
    val spotifyError: String? = null,
    val availableUpdate: VersionInfo? = null,
    val updateDownloadProgress: Int? = null,
    val updateReadyFile: File? = null,
    val pendingFriendRequests: List<FriendRequest> = emptyList(),
    val showPeopleSearch: Boolean = false,
    val showNotifications: Boolean = false,
    val peopleSearchQuery: String = "",
    val peopleSearchResults: List<User> = emptyList(),
    val sentRequestIds: Set<String> = emptySet(),
    val followerDetails: List<User> = emptyList(),
    val followingDetails: List<User> = emptyList(),
    val isNotificationListenerEnabled: Boolean = false
)

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext: Context = app.applicationContext

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var userSearchJob: Job? = null
    private var firebaseObserverJob: Job? = null
    private var spotifyPollingJob: Job? = null
    // Debounce: evita che un singolo poll vuoto (204 tra brani / errore rete transitorio)
    // faccia sparire e riapparire la live agli altri
    private var emptyPollCount = 0
    // Heartbeat: rinfresca updatedAt ogni ~25s mentre si ascolta lo stesso brano, così
    // il proprio documento resta in cima alla query e la live non sparisce agli altri
    private var lastLiveHeartbeat = 0L
    private val httpClient by lazy { OkHttpClient() }

    init {
        SpotifyAuthRepository.loadTokens(appContext)
        checkForUpdate()
        val existingUser = AuthRepository.currentFirebaseUser
        if (existingUser != null) {
            viewModelScope.launch {
                val user = existingUser.toAppUser()
                val spotifyConnected = SpotifyAuthRepository.isAuthorized
                val services = mapOf("spotify" to spotifyConnected, "amazon_music" to false)
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        isLoggedIn = true,
                        isSpotifyConnected = spotifyConnected,
                        connectedServices = services
                    )
                }
                FirebaseRepository.syncCurrentUser(user)
                startFirebaseListener()
                saveFcmToken(user.id)
                if (spotifyConnected) startSpotifyPolling()
            }
        }
    }

    private fun saveFcmToken(userId: String) {
        if (userId.isBlank()) return
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                FirebaseRepository.saveFcmToken(userId, token)
            }
    }

    // ===================== UPDATE =====================

    private fun checkForUpdate() {
        viewModelScope.launch {
            val update = UpdateRepository.checkUpdate(BuildConfig.VERSION_CODE)
            if (update != null) _uiState.update { it.copy(availableUpdate = update) }
        }
    }

    fun downloadAndInstallUpdate(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(updateDownloadProgress = 0) }
            val file = UpdateRepository.downloadApk(context) { progress ->
                _uiState.update { it.copy(updateDownloadProgress = progress) }
            }
            if (file != null) {
                _uiState.update { it.copy(updateDownloadProgress = null, updateReadyFile = file, availableUpdate = null) }
                UpdateRepository.installApk(context, file)
            } else {
                _uiState.update { it.copy(updateDownloadProgress = null, feedbackToast = "Download aggiornamento fallito") }
            }
        }
    }

    fun dismissUpdate() {
        _uiState.update { it.copy(availableUpdate = null) }
    }

    // ===================== AUTH =====================

    fun loginWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            val result = AuthRepository.signInWithGoogle(context)
            result.onSuccess { user ->
                val spotifyConnected = SpotifyAuthRepository.isAuthorized
                val services = mapOf("spotify" to spotifyConnected, "amazon_music" to false)
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        isLoggedIn = true,
                        isLoggingIn = false,
                        loginError = null,
                        isSpotifyConnected = spotifyConnected,
                        connectedServices = services
                    )
                }
                FirebaseRepository.syncCurrentUser(user)
                startFirebaseListener()
                saveFcmToken(user.id)
                if (spotifyConnected) startSpotifyPolling()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoggingIn = false,
                        loginError = error.message ?: "Errore durante il login"
                    )
                }
            }
        }
    }

    fun logout() {
        stopSpotifyPolling()
        AuthRepository.signOut()
        firebaseObserverJob?.cancel()
        _uiState.value = MusicUiState()
    }

    // ===================== SPOTIFY =====================

    fun launchSpotifyAuth() {
        SpotifyAuthRepository.launchAuthFlow(appContext)
    }

    fun handleSpotifyCallback(code: String) {
        viewModelScope.launch {
            val success = SpotifyAuthRepository.handleCallback(appContext, code)
            if (success) {
                val services = _uiState.value.connectedServices.toMutableMap().apply { put("spotify", true) }
                val updatedUser = _uiState.value.currentUser.copy(isLiveNow = true)
                _uiState.update {
                    it.copy(
                        isSpotifyConnected = true,
                        connectedServices = services,
                        currentUser = updatedUser,
                        spotifyError = null,
                        feedbackToast = "Spotify collegato"
                    )
                }
                FirebaseRepository.syncCurrentUser(updatedUser)
                startSpotifyPolling()
            } else {
                _uiState.update { it.copy(spotifyError = "Autorizzazione fallita") }
            }
        }
    }

    fun disconnectSpotify() {
        stopSpotifyPolling()
        SpotifyAuthRepository.clearTokens(appContext)
        val services = _uiState.value.connectedServices.toMutableMap().apply { put("spotify", false) }
        val updatedUser = _uiState.value.currentUser.copy(isLiveNow = false, currentTrack = null)
        _uiState.update {
            it.copy(
                isSpotifyConnected = false,
                connectedServices = services,
                currentUser = updatedUser,
                nowPlayingTrack = null,
                feedbackToast = "Spotify disconnesso"
            )
        }
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun startSpotifyPolling() {
        if (!SpotifyAuthRepository.isAuthorized) return
        if (spotifyPollingJob?.isActive == true) return
        spotifyPollingJob = viewModelScope.launch {
            while (isActive) {
                // Ogni fetch è isolato: un errore transitorio NON deve uccidere il loop
                // (altrimenti la live si "congela" e non si aggiorna più al cambio brano)
                try {
                    fetchCurrentlyPlaying()
                } catch (_: Exception) {
                    // errore transitorio ignorato: si riprova al prossimo giro
                }
                delay(3_000) // 3s: più reattivo, rileva Spotify già in play rapidamente
            }
        }
    }

    fun stopSpotifyPolling() {
        spotifyPollingJob?.cancel()
        spotifyPollingJob = null
    }

    private suspend fun fetchCurrentlyPlaying() {
        val result = SpotifyWebApiRepository.getCurrentlyPlaying(appContext)
        val currentTrackId = _uiState.value.nowPlayingTrack?.id

        when (result) {
            // Errore/rete/token: stato ignoto → non toccare la live (evita che sparisca
            // in background per un timeout o doze). Se c'è un errore di auth/permessi
            // (es. 403) lo mostro all'utente invece di fallire in silenzio.
            is SpotifyWebApiRepository.PlaybackResult.Unknown -> {
                result.error?.let { msg ->
                    if (_uiState.value.spotifyError != msg) {
                        _uiState.update { it.copy(spotifyError = msg) }
                    }
                }
                return
            }

            // Spotify conferma che non suona nulla: azzera con debounce
            is SpotifyWebApiRepository.PlaybackResult.NotPlaying -> {
                val hasLive = currentTrackId != null || _uiState.value.currentUser.currentTrack != null
                if (!hasLive) { emptyPollCount = 0; return }
                // 2 conferme consecutive (~6s) prima di azzerare: assorbe i 204 tra brani
                emptyPollCount++
                if (emptyPollCount < 2) return
                emptyPollCount = 0
                val updatedUser = _uiState.value.currentUser.copy(currentTrack = null, isLiveNow = false)
                _uiState.update { it.copy(nowPlayingTrack = null, currentUser = updatedUser) }
                FirebaseRepository.syncCurrentUser(updatedUser)
            }

            is SpotifyWebApiRepository.PlaybackResult.Playing -> {
                val track = result.track
                emptyPollCount = 0
                if (_uiState.value.spotifyError != null) {
                    _uiState.update { it.copy(spotifyError = null) }
                }
                val now = System.currentTimeMillis()
                if (track.id == currentTrackId) {
                    // Stesso brano: heartbeat + riallineo la posizione reale (assorbe seek/pausa)
                    if (now - lastLiveHeartbeat >= 25_000L) {
                        lastLiveHeartbeat = now
                        val u = _uiState.value.currentUser.copy(
                            trackProgressMs = result.progressMs, trackProgressAt = now
                        )
                        _uiState.update { it.copy(currentUser = u) }
                        FirebaseRepository.touchLive(u.id, result.progressMs, now)
                    }
                    return
                }
                lastLiveHeartbeat = now
                val updatedUser = _uiState.value.currentUser.copy(
                    currentTrack = track, isLiveNow = true,
                    trackProgressMs = result.progressMs, trackProgressAt = now
                )
                _uiState.update { it.copy(nowPlayingTrack = track, currentUser = updatedUser) }
                FirebaseRepository.syncCurrentUser(updatedUser)
            }
        }
    }

    fun clearSpotifyError() {
        _uiState.update { it.copy(spotifyError = null) }
    }

    /** Presenza: l'utente sta usando l'app (connesso) — distinta dall'essere in live. */
    fun setOnline(online: Boolean) {
        val id = _uiState.value.currentUser.id
        if (id.isBlank()) return
        _uiState.update { it.copy(currentUser = it.currentUser.copy(isOnline = online)) }
        FirebaseRepository.setOnline(id, online)
    }

    fun checkNotificationListenerEnabled() {
        val spotifyEnabled = com.example.SpotifyNotificationListenerService.isEnabled(appContext)
        val amazonEnabled = com.example.AmazonMusicNotificationListenerService.isEnabled(appContext)
        val enabled = spotifyEnabled || amazonEnabled
        _uiState.update { it.copy(isNotificationListenerEnabled = enabled) }
        if (spotifyEnabled) registerSpotifyNotificationListenerCallbacks()
        if (amazonEnabled) registerAmazonMusicNotificationListenerCallbacks()
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(
            android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun registerSpotifyNotificationListenerCallbacks() {
        com.example.SpotifyNotificationListenerService.onTrackChanged = { trackName, artist, durationMs, positionMs, artUrl ->
            // Usa solo come fallback se il Web API non sta già fornendo dati
            if (!SpotifyAuthRepository.isAuthorized || spotifyPollingJob?.isActive != true) {
                updateNowPlayingFromBroadcast("", trackName, artist, "", durationMs, positionMs, artUrl)
            }
        }
        com.example.SpotifyNotificationListenerService.onProgressChanged = { positionMs, durationMs ->
            if (!SpotifyAuthRepository.isAuthorized || spotifyPollingJob?.isActive != true) {
                updateLiveProgress(positionMs, durationMs)
            }
        }
        com.example.SpotifyNotificationListenerService.onPlaybackStopped = {
            if (!SpotifyAuthRepository.isAuthorized || spotifyPollingJob?.isActive != true) {
                clearNowPlayingFromBroadcast()
            }
        }
    }

    private fun registerAmazonMusicNotificationListenerCallbacks() {
        com.example.AmazonMusicNotificationListenerService.onTrackChanged = { trackName, artist, durationMs, positionMs, artUrl ->
            updateNowPlayingFromBroadcast("", trackName, artist, "", durationMs, positionMs, artUrl)
        }
        com.example.AmazonMusicNotificationListenerService.onProgressChanged = { positionMs, durationMs ->
            updateLiveProgress(positionMs, durationMs)
        }
        com.example.AmazonMusicNotificationListenerService.onPlaybackStopped = {
            clearNowPlayingFromBroadcast()
        }
    }

    // Riallinea la posizione reale del brano corrente (Free), con heartbeat ~25s
    private fun updateLiveProgress(positionMs: Long, durationMs: Long) {
        if (_uiState.value.currentUser.currentTrack == null) return
        val now = System.currentTimeMillis()
        if (now - lastLiveHeartbeat < 25_000L) return
        lastLiveHeartbeat = now
        val u = _uiState.value.currentUser.copy(trackProgressMs = positionMs, trackProgressAt = now)
        _uiState.update { it.copy(currentUser = u) }
        FirebaseRepository.touchLive(u.id, positionMs, now)
    }

    fun updateNowPlayingFromBroadcast(
        trackId: String, trackName: String, artistName: String, albumName: String,
        durationMs: Long = 0L, positionMs: Long = 0L, artUrl: String = ""
    ) {
        if (trackName.isBlank()) return
        val currentId = _uiState.value.nowPlayingTrack?.id
        if (trackId.isNotBlank() && trackId == currentId) return
        val cleanArtist = sanitizeSpotifyContext(artistName)
        val cleanAlbum = sanitizeSpotifyContext(albumName)
        viewModelScope.launch {
            // Preferisci l'artwork reale della MediaSession; altrimenti cerca su iTunes
            val coverUrl = if (artUrl.startsWith("http", ignoreCase = true)) artUrl
                           else fetchItunesCover(cleanArtist, trackName)
            val now = System.currentTimeMillis()
            lastLiveHeartbeat = now
            val track = Track(
                id = trackId.ifBlank { "$cleanArtist-$trackName".hashCode().toString() },
                title = trackName,
                artist = cleanArtist,
                album = cleanAlbum,
                coverUrl = coverUrl,
                durationText = if (durationMs > 0) formatMs(durationMs) else "3:45",
                durationMs = durationMs
            )
            val updatedUser = _uiState.value.currentUser.copy(
                currentTrack = track, isLiveNow = true,
                trackProgressMs = positionMs, trackProgressAt = now
            )
            _uiState.update { it.copy(nowPlayingTrack = track, currentUser = updatedUser) }
            FirebaseRepository.syncCurrentUser(updatedUser)
        }
    }

    private fun formatMs(ms: Long): String {
        val secs = ms / 1000
        return "${secs / 60}:${(secs % 60).toString().padStart(2, '0')}"
    }

    /**
     * Spotify (soprattutto Free) a volte riporta il nome del contesto/playlist
     * ("Consigliati per te", "Fatto per te", …) al posto dell'artista. Match ESATTO
     * (mai substring) per non toccare artisti o brani reali che contengono quelle parole.
     */
    private fun sanitizeSpotifyContext(value: String): String {
        val v = value.trim()
        val contexts = setOf(
            "consigliato per te", "consigliati per te",
            "fatto per te", "made for you",
            "radio", "mix del giorno", "daily mix"
        )
        return if (v.lowercase() in contexts) "" else v
    }

    fun clearNowPlayingFromBroadcast() {
        if (_uiState.value.nowPlayingTrack == null && _uiState.value.currentUser.currentTrack == null) return
        val updatedUser = _uiState.value.currentUser.copy(currentTrack = null, isLiveNow = false)
        _uiState.update { it.copy(nowPlayingTrack = null, currentUser = updatedUser) }
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    private suspend fun fetchItunesCover(artist: String, track: String): String =
        withContext(Dispatchers.IO) {
            try {
                // entity=song per matchare il brano esatto (non album/video); artista+titolo
                // migliora la precisione. Se manca l'artista uso solo il titolo.
                val term = listOf(artist, track).filter { it.isNotBlank() }.joinToString(" ")
                if (term.isBlank()) return@withContext DEFAULT_COVER
                val query = java.net.URLEncoder.encode(term, "UTF-8")
                val response = httpClient.newCall(
                    Request.Builder()
                        .url("https://itunes.apple.com/search?term=$query&media=music&entity=song&limit=1")
                        .build()
                ).execute()
                val body = response.body?.string() ?: return@withContext DEFAULT_COVER
                val results = JSONObject(body).optJSONArray("results")
                if (results == null || results.length() == 0) return@withContext DEFAULT_COVER
                // artworkUrl100 → richiedo 600x600 per la copertina ad alta risoluzione
                results.getJSONObject(0).optString("artworkUrl100", DEFAULT_COVER)
                    .replace("100x100bb", "600x600bb")
            } catch (e: Exception) {
                DEFAULT_COVER
            }
        }

    // ===================== FIREBASE =====================

    private fun startFirebaseListener() {
        val userId = _uiState.value.currentUser.id
        firebaseObserverJob?.cancel()
        firebaseObserverJob = viewModelScope.launch {
            FirebaseRepository.observeOtherUsers(userId)
                .catch { }
                .collect { remoteUsers ->
                    _uiState.update { current ->
                        val merged = (remoteUsers + current.feedUsers).distinctBy { it.id }
                        current.copy(
                            feedUsers = merged,
                            userSearchResults = if (current.userSearchQuery.isBlank()) merged else current.userSearchResults
                        )
                    }
                }
        }
        // UNICO listener sul documento utente: profilo, sharedTracks, stats, social
        // (follower/following), richieste ricevute e inviate. Prima erano 4 listener.
        friendRequestsInitialized = false
        viewModelScope.launch {
            FirebaseRepository.observeCurrentUserDocument(userId)
                .catch { }
                .collect { fresh ->
                    // Notifica solo le NUOVE richieste ricevute (dopo il primo snapshot)
                    val newIds = fresh.pendingRequests.map { it.id }.toSet()
                    if (friendRequestsInitialized) {
                        fresh.pendingRequests.filter { it.id !in knownFriendRequestIds }
                            .forEach { showFriendRequestNotification(it) }
                    }
                    friendRequestsInitialized = true
                    knownFriendRequestIds = newIds

                    _uiState.update { current ->
                        current.copy(
                            currentUser = current.currentUser.copy(
                                name = fresh.name.ifBlank { current.currentUser.name },
                                username = fresh.username.ifBlank { current.currentUser.username },
                                avatarUrl = fresh.avatarUrl.ifBlank { current.currentUser.avatarUrl },
                                coverUrl = fresh.coverUrl ?: current.currentUser.coverUrl,
                                sharedTracks = fresh.sharedTracks,
                                stats = fresh.stats,
                                followerIds = fresh.followerIds,
                                followingIds = fresh.followingIds
                                // isLiveNow e currentTrack restano gestiti localmente da Spotify
                            ),
                            pendingFriendRequests = fresh.pendingRequests,
                            sentRequestIds = fresh.sentRequestIds.toSet()
                        )
                    }
                    // Se il profilo proprio è aperto, aggiorna i dettagli social
                    if (_uiState.value.activeProfileUser?.isCurrentUser == true) {
                        loadSocialDetails(_uiState.value.currentUser)
                    }
                }
        }
    }

    // ===================== SERVICES =====================

    fun toggleConnectedService(serviceKey: String) {
        val currentServices = _uiState.value.connectedServices.toMutableMap()
        val newState = !(currentServices[serviceKey] ?: false)
        
        if (serviceKey == "amazon_music" && newState) {
            // Per Amazon Music, apri le impostazioni del Notification Listener
            openNotificationListenerSettings(appContext)
            // Il servizio verrà attivato quando l'utente abiliterà il permesso
            // e tornerà nell'app (onResume -> checkNotificationListenerEnabled)
            currentServices[serviceKey] = true
            _uiState.update {
                it.copy(
                    connectedServices = currentServices,
                    feedbackToast = "Abilita l'accesso alle notifiche per Amazon Music"
                )
            }
        } else {
            currentServices[serviceKey] = newState
            val serviceName = when (serviceKey) {
                "amazon_music" -> "Amazon Music"
                else -> serviceKey.replaceFirstChar { it.uppercase() }
            }
            _uiState.update {
                it.copy(
                    connectedServices = currentServices,
                    feedbackToast = if (newState) "$serviceName collegato" else "$serviceName disconnesso"
                )
            }
        }
    }

    // ===================== SEARCH =====================

    fun setSearchTab(tab: SearchTab) {
        _uiState.update { it.copy(searchTab = tab) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = MusicRepository.searchTracks(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun onUserSearchQueryChanged(query: String) {
        _uiState.update { it.copy(userSearchQuery = query) }
        userSearchJob?.cancel()
        userSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUsers = true) }
            val pool = _uiState.value.feedUsers
            val results = if (query.isBlank()) pool else {
                val clean = query.trim().removePrefix("@").lowercase()
                pool.filter {
                    it.username.lowercase().contains(clean) ||
                    it.name.lowercase().contains(clean) ||
                    it.stats.topArtist.lowercase().contains(clean)
                }
            }
            _uiState.update { it.copy(userSearchResults = results, isSearchingUsers = false) }
        }
    }

    // ===================== PROFILE =====================

    fun updateProfile(name: String, username: String, avatarUrl: String, coverUrl: String? = null) {
        val cleanUsername = username.trim().removePrefix("@")
        val updatedUser = _uiState.value.currentUser.copy(
            name = name.trim().ifBlank { _uiState.value.currentUser.name },
            username = cleanUsername.ifBlank { _uiState.value.currentUser.username },
            avatarUrl = avatarUrl.trim().ifBlank { _uiState.value.currentUser.avatarUrl },
            coverUrl = if (!coverUrl.isNullOrBlank()) coverUrl.trim() else _uiState.value.currentUser.coverUrl
        )
        _uiState.update {
            it.copy(
                currentUser = updatedUser,
                activeProfileUser = if (it.activeProfileUser?.id == updatedUser.id) updatedUser else it.activeProfileUser,
                feedbackToast = "Profilo aggiornato: @${updatedUser.username}"
            )
        }
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    // ===================== NAVIGATION =====================

    fun toggleFollowUser(targetUser: User) {
        val currentFeed = _uiState.value.feedUsers
        val isAlreadyInFeed = currentFeed.any { it.id == targetUser.id }
        val newFeed = if (isAlreadyInFeed) currentFeed.filterNot { it.id == targetUser.id }
                      else listOf(targetUser) + currentFeed
        _uiState.update {
            it.copy(
                feedUsers = newFeed,
                feedbackToast = if (isAlreadyInFeed) "Rimosso dal feed: @${targetUser.username}"
                                else "Aggiunto al feed: @${targetUser.username}"
            )
        }
    }

    fun openShareSheet() {
        _uiState.update { it.copy(isShareSheetOpen = true, searchQuery = "", searchResults = emptyList()) }
    }

    fun closeShareSheet() {
        _uiState.update { it.copy(isShareSheetOpen = false) }
    }

    // ===================== SOCIAL — PEOPLE SEARCH =====================

    fun openPeopleSearch() {
        val pool = _uiState.value.feedUsers
        _uiState.update { it.copy(showPeopleSearch = true, peopleSearchQuery = "", peopleSearchResults = pool) }
    }

    fun closePeopleSearch() {
        _uiState.update { it.copy(showPeopleSearch = false) }
    }

    fun onPeopleSearchQueryChanged(query: String) {
        _uiState.update { it.copy(peopleSearchQuery = query) }
        val pool = _uiState.value.feedUsers
        val results = if (query.isBlank()) pool else {
            val clean = query.trim().removePrefix("@").lowercase()
            pool.filter {
                it.username.lowercase().contains(clean) || it.name.lowercase().contains(clean)
            }
        }
        _uiState.update { it.copy(peopleSearchResults = results) }
    }

    fun sendFollowRequest(targetUser: User) {
        val already = _uiState.value.sentRequestIds.contains(targetUser.id)
        if (already) return
        _uiState.update {
            it.copy(
                sentRequestIds = it.sentRequestIds + targetUser.id,
                feedbackToast = "Richiesta inviata a @${targetUser.username}"
            )
        }
        FirebaseRepository.sendFollowRequest(from = _uiState.value.currentUser, to = targetUser)
    }

    // ===================== SOCIAL — NOTIFICATIONS =====================

    fun openNotifications() {
        _uiState.update { it.copy(showNotifications = true) }
    }

    fun closeNotifications() {
        _uiState.update { it.copy(showNotifications = false) }
    }

    fun acceptFriendRequest(request: FriendRequest) {
        val updated = _uiState.value.pendingFriendRequests.filter { it.id != request.id }
        val currentId = _uiState.value.currentUser.id
        _uiState.update {
            it.copy(
                pendingFriendRequests = updated,
                feedbackToast = "Ora segui @${request.fromUserUsername}"
            )
        }
        FirebaseRepository.acceptFollowRequest(currentId, request.fromUserId)
    }

    fun rejectFriendRequest(request: FriendRequest) {
        val updated = _uiState.value.pendingFriendRequests.filter { it.id != request.id }
        val currentId = _uiState.value.currentUser.id
        _uiState.update { it.copy(pendingFriendRequests = updated) }
        FirebaseRepository.rejectFollowRequest(currentId, request.fromUserId)
    }

    // Diffing per notificare solo le NUOVE richieste (usato dal listener unico del doc utente)
    private var knownFriendRequestIds = emptySet<String>()
    private var friendRequestsInitialized = false

    private fun showFriendRequestNotification(request: FriendRequest) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        }
        val intent = android.content.Intent(appContext, com.example.MainActivity::class.java).apply {
            putExtra(com.example.MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
            flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            appContext,
            request.id.hashCode(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notif = androidx.core.app.NotificationCompat.Builder(appContext, FRIEND_REQUEST_CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_stat_notification)
            .setContentTitle("Nuova richiesta di follow")
            .setContentText("@${request.fromUserUsername} vuole seguirti")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setDefaults(androidx.core.app.NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        androidx.core.app.NotificationManagerCompat.from(appContext)
            .notify(request.id.hashCode(), notif)
    }

    fun shareTrack(track: Track) {
        // Condivisione nel FEED: aggiorna solo i brani condivisi + stats.
        // NON tocca currentTrack/isLiveNow (che sono lo stato LIVE) — feed e live
        // sono due sezioni distinte e la condivisione non deve mandarti in live.
        val updatedUser = _uiState.value.currentUser.copy(
            sharedTracks = listOf(track) + _uiState.value.currentUser.sharedTracks.filterNot { it.id == track.id },
            stats = _uiState.value.currentUser.stats.copy(sharedCount = _uiState.value.currentUser.stats.sharedCount + 1)
        )
        _uiState.update {
            it.copy(
                currentUser = updatedUser,
                isShareSheetOpen = false,
                feedbackToast = "Condiviso: ${track.title}"
            )
        }
        FirebaseRepository.shareTrack(updatedUser.id, track)
        // shareTrack scrive già sharedTracks su Firestore; syncCurrentUser riscriverebbe
        // l'intero profilo (incluso currentTrack live) — non serve qui.
    }

    fun openStory(user: User) {
        val allStories = getStoriesList()
        val index = allStories.indexOfFirst { it.id == user.id }
        if (index != -1) {
            _uiState.update { it.copy(activeStoryUserIndex = index) }
        } else {
            val updatedFeed = if (_uiState.value.feedUsers.none { it.id == user.id } && !user.isCurrentUser) {
                listOf(user.copy(isLiveNow = true)) + _uiState.value.feedUsers
            } else _uiState.value.feedUsers
            val refreshedStories = if (user.isCurrentUser) listOf(_uiState.value.currentUser) + updatedFeed
                                   else updatedFeed
            val newIdx = refreshedStories.indexOfFirst { it.id == user.id }.coerceAtLeast(0)
            _uiState.update { it.copy(feedUsers = updatedFeed, activeStoryUserIndex = newIdx) }
        }
    }

    fun nextStory() {
        val allStories = getStoriesList()
        val currentIndex = _uiState.value.activeStoryUserIndex ?: return
        if (currentIndex < allStories.size - 1) _uiState.update { it.copy(activeStoryUserIndex = currentIndex + 1) }
        else closeStory()
    }

    fun previousStory() {
        val currentIndex = _uiState.value.activeStoryUserIndex ?: return
        if (currentIndex > 0) _uiState.update { it.copy(activeStoryUserIndex = currentIndex - 1) }
    }

    fun closeStory() { _uiState.update { it.copy(activeStoryUserIndex = null) } }

    fun openProfile(user: User) {
        _uiState.update { it.copy(activeProfileUser = user) }
        if (user.isCurrentUser) {
            // Usa sempre i dati freschi dallo stato (non l'oggetto passato che potrebbe essere stale)
            loadSocialDetails(_uiState.value.currentUser)
        }
    }

    fun loadSocialDetails(user: User) {
        val ids = (user.followerIds + user.followingIds).distinct()
        if (ids.isEmpty()) return
        FirebaseRepository.getUsersByIds(ids) { allUsers ->
            val followerSet = user.followerIds.toSet()
            val followingSet = user.followingIds.toSet()
            _uiState.update { state ->
                state.copy(
                    followerDetails = allUsers.filter { it.id in followerSet },
                    followingDetails = allUsers.filter { it.id in followingSet }
                )
            }
        }
    }

    fun unfollow(target: User) {
        val currentId = _uiState.value.currentUser.id
        FirebaseRepository.removeFollowing(currentId, target.id)
        val updatedUser = _uiState.value.currentUser.copy(
            followingIds = _uiState.value.currentUser.followingIds.filter { it != target.id }
        )
        _uiState.update {
            it.copy(
                currentUser = updatedUser,
                followingDetails = it.followingDetails.filter { u -> u.id != target.id }
            )
        }
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun removeFollower(follower: User) {
        val currentId = _uiState.value.currentUser.id
        FirebaseRepository.removeFollower(currentId, follower.id)
        val updatedUser = _uiState.value.currentUser.copy(
            followerIds = _uiState.value.currentUser.followerIds.filter { it != follower.id }
        )
        _uiState.update {
            it.copy(
                currentUser = updatedUser,
                followerDetails = it.followerDetails.filter { u -> u.id != follower.id }
            )
        }
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun closeProfile() { _uiState.update { it.copy(activeProfileUser = null) } }

    fun openChat(user: User, initialTrack: Track? = null) {
        _uiState.update { it.copy(activeChatUser = user, activeStoryUserIndex = null) }
        if (initialTrack != null) {
            sendMessage(user.id, "Ho visto che stavi ascoltando \"${initialTrack.title}\"!", initialTrack)
        }
    }

    fun closeChat() { _uiState.update { it.copy(activeChatUser = null) } }

    fun sendMessage(recipientId: String, text: String, attachedTrack: Track? = null) {
        if (text.isBlank()) return
        val newMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = "me",
            text = text.trim(),
            timestamp = MusicRepository.getCurrentTimestamp(),
            isFromMe = true,
            attachedTrack = attachedTrack
        )
        val currentList = _uiState.value.chatMessages[recipientId]?.toMutableList() ?: mutableListOf()
        currentList.add(newMessage)
        val updatedMap = _uiState.value.chatMessages.toMutableMap().apply { put(recipientId, currentList) }
        _uiState.update { it.copy(chatMessages = updatedMap) }
    }

    fun inspectTrack(track: Track, user: User? = null) {
        _uiState.update { it.copy(selectedTrackDetail = Pair(track, user)) }
    }

    fun closeTrackInspector() { _uiState.update { it.copy(selectedTrackDetail = null) } }

    fun clearToast() { _uiState.update { it.copy(feedbackToast = null) } }

    fun clearLoginError() { _uiState.update { it.copy(loginError = null) } }

    fun getStoriesList(): List<User> {
        val state = _uiState.value
        val followingIds = state.currentUser.followingIds
        val list = mutableListOf<User>()
        if (state.currentUser.currentTrack != null) list.add(state.currentUser)
        list.addAll(state.feedUsers.filter {
            followingIds.contains(it.id) && it.isLiveNow && it.currentTrack != null
        })
        return list
    }

    companion object {
        const val FRIEND_REQUEST_CHANNEL_ID = "friend_requests_channel"
        private const val DEFAULT_COVER = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80"

        private fun com.google.firebase.auth.FirebaseUser.toAppUser(): User {
            val username = email
                ?.substringBefore("@")
                ?.replace(".", "_")
                ?.replace("-", "_")
                ?: uid.take(8)
            return User(
                id = uid,
                name = displayName ?: "Utente",
                username = username,
                email = email ?: "",
                avatarUrl = photoUrl?.toString() ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400",
                isCurrentUser = true,
                isLiveNow = false,
                currentTrack = null,
                sharedTracks = emptyList(),
                stats = UserStats(sharedCount = 0, topArtist = "", totalMinutesOrGenres = "")
            )
        }
    }
}
