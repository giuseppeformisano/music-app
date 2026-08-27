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
import com.example.model.Conversation
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
        "spotify" to false,       // Premium via OAuth Web API
        "spotify_free" to false,  // Free via notifiche (scelta esplicita, ricordata)
        "amazon_music" to false   // via notifiche (scelta esplicita, ricordata)
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
    val conversations: List<Conversation> = emptyList(),
    val isChatListOpen: Boolean = false,
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
    val isNotificationListenerEnabled: Boolean = false,
    // Aumenta periodicamente per forzare la riValutazione del TTL di staleness dei live
    // anche quando non arrivano nuovi eventi da Firestore.
    val liveTick: Long = 0L,
    val applyCoverToFeed: Boolean = false,
    val liveNotificationsEnabled: Boolean = true,
    val showChangelog: Boolean = false,
    val activePulse: com.example.model.ActivePulse? = null
)

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext: Context = app.applicationContext

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var userSearchJob: Job? = null
    private var firebaseObserverJob: Job? = null
    private var liveStaleTickerJob: Job? = null
    private var spotifyPollingJob: Job? = null
    // Debounce: evita che un singolo poll vuoto (204 tra brani / errore rete transitorio)
    // faccia sparire e riapparire la live agli altri
    private var emptyPollCount = 0
    // Heartbeat: rinfresca updatedAt ogni ~25s mentre si ascolta lo stesso brano, così
    // il proprio documento resta in cima alla query e la live non sparisce agli altri
    private var lastLiveHeartbeat = 0L
    private val httpClient by lazy { OkHttpClient() }

    private var isAppInForeground = true

    // Chat listeners
    private var chatMessagesListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var conversationsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        val userSettingsPrefs = appContext.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
        val initialApplyCover = userSettingsPrefs.getBoolean("apply_cover_to_feed", false)
        val initialLiveNotifs = userSettingsPrefs.getBoolean("live_notifications_enabled", true)
        // Changelog: mostralo una sola volta dopo un aggiornamento (non al primissimo avvio).
        val lastChangelogCode = userSettingsPrefs.getInt("last_changelog_code", 0)
        val showChangelog = BuildConfig.VERSION_CODE > lastChangelogCode
        _uiState.update { it.copy(applyCoverToFeed = initialApplyCover, liveNotificationsEnabled = initialLiveNotifs, showChangelog = showChangelog) }

        SpotifyAuthRepository.loadTokens(appContext)
        checkForUpdate()
        val existingUser = AuthRepository.currentFirebaseUser
        if (existingUser != null) {
            viewModelScope.launch {
                val rawUser = existingUser.toAppUser()
                val user = applyUserLocalPrefs(rawUser)
                val spotifyConnected = SpotifyAuthRepository.isAuthorized
                val services = mapOf("spotify" to spotifyConnected) + loadPersistedServices()
                val initialUser = user.copy(presenceState = if (isAppInForeground) com.example.model.UserPresenceState.ONLINE else com.example.model.UserPresenceState.OFFLINE)
                _uiState.update {
                    it.copy(
                        currentUser = initialUser,
                        isLoggedIn = true,
                        isSpotifyConnected = spotifyConnected,
                        connectedServices = services
                    )
                }
                FirebaseRepository.ensureUserProfile(initialUser)
                if (isAppInForeground && initialUser.id.isNotBlank()) {
                    FirebaseRepository.setOnline(initialUser.id, true)
                }
                startFirebaseListener()
                saveFcmToken(initialUser.id)
                startConversationsListener(initialUser.id)
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

    fun dismissChangelog() {
        appContext.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            .edit().putInt("last_changelog_code", BuildConfig.VERSION_CODE).apply()
        _uiState.update { it.copy(showChangelog = false) }
    }

    // ===================== AUTH =====================

    fun loginWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true, loginError = null) }
            val result = AuthRepository.signInWithGoogle(context)
            result.onSuccess { rawUser ->
                val user = applyUserLocalPrefs(rawUser)
                val spotifyConnected = SpotifyAuthRepository.isAuthorized
                val services = mapOf("spotify" to spotifyConnected) + loadPersistedServices()
                val loggedUser = user.copy(presenceState = if (isAppInForeground) com.example.model.UserPresenceState.ONLINE else com.example.model.UserPresenceState.OFFLINE)
                _uiState.update {
                    it.copy(
                        currentUser = loggedUser,
                        isLoggedIn = true,
                        isLoggingIn = false,
                        loginError = null,
                        isSpotifyConnected = spotifyConnected,
                        connectedServices = services
                    )
                }
                // PRIMO login: crea il profilo dai metadati account; se esiste già non
                // sovrascrive i dati persistiti (nome/avatar modificati dall'utente).
                FirebaseRepository.ensureUserProfile(loggedUser)
                if (isAppInForeground && loggedUser.id.isNotBlank()) {
                    FirebaseRepository.setOnline(loggedUser.id, true)
                }
                startFirebaseListener()
                saveFcmToken(loggedUser.id)
                startConversationsListener(loggedUser.id)
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
        liveStaleTickerJob?.cancel()
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
                // Premium collegato → è la sorgente attiva: azzera la scelta Free (mutua esclusione)
                persistServiceState("spotify_free", false)
                val services = _uiState.value.connectedServices.toMutableMap().apply {
                    put("spotify", true)
                    put("spotify_free", false)
                }
                val updatedUser = _uiState.value.currentUser.copy(presenceState = com.example.model.UserPresenceState.LIVE)
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
        wasLive = false
        val updatedUser = _uiState.value.currentUser.copy(
            presenceState = if (isAppInForeground) com.example.model.UserPresenceState.ONLINE else com.example.model.UserPresenceState.OFFLINE,
            currentTrack = null
        )
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
        // La sorgente segue la SCELTA esplicita: il Web API si usa solo in modalità Premium.
        if (_uiState.value.connectedServices["spotify"] != true) return
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
        // Se sul dispositivo è EFFETTIVAMENTE in esecuzione un'altra sorgente (es. Amazon Music),
        // questa ha precedenza assoluta sullo streaming via API di Spotify Premium.
        if (com.example.MusicNotificationListenerService.isNonSpotifyDevicePlaybackActive()) {
            return
        }

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
                val currentTrack = _uiState.value.currentUser.currentTrack ?: _uiState.value.nowPlayingTrack
                if (currentTrack == null) { emptyPollCount = 0; return }
                // Se la live attiva è di un'altra piattaforma (es. Amazon Music), Spotify NON deve cancellarla
                if (!currentTrack.source.equals("spotify", ignoreCase = true)) {
                    emptyPollCount = 0
                    return
                }
                // 2 conferme consecutive (~6s) prima di azzerare: assorbe i 204 tra brani
                emptyPollCount++
                if (emptyPollCount < 2) return
                emptyPollCount = 0
                val updatedUser = _uiState.value.currentUser.copy(
                    currentTrack = null,
                    presenceState = if (isAppInForeground) com.example.model.UserPresenceState.ONLINE else com.example.model.UserPresenceState.OFFLINE
                )
                _uiState.update { it.copy(nowPlayingTrack = null, currentUser = updatedUser) }
                FirebaseRepository.syncCurrentUser(updatedUser)
            }

            is SpotifyWebApiRepository.PlaybackResult.Playing ->
                applyLiveTrack(result.track, result.progressMs)

            // In PAUSA: resta live e mostra il brano in pausa. Esce solo con NotPlaying (204).
            is SpotifyWebApiRepository.PlaybackResult.Paused ->
                applyLiveTrack(result.track, result.progressMs)
        }
    }

    private fun applyLiveTrack(track: Track, progressMs: Long) {
        val currentTrackId = _uiState.value.nowPlayingTrack?.id
        val liveTrack = if (track.source.isBlank()) track.copy(source = "spotify") else track
        emptyPollCount = 0
        if (_uiState.value.spotifyError != null) {
            _uiState.update { it.copy(spotifyError = null) }
        }
        val now = System.currentTimeMillis()
        if (liveTrack.id == currentTrackId) {
            // Stesso brano: heartbeat + riallineo la posizione reale (assorbe seek/pausa)
            if (now - lastLiveHeartbeat >= 25_000L) {
                lastLiveHeartbeat = now
                val u = _uiState.value.currentUser.copy(
                    trackProgressMs = progressMs, trackProgressAt = now
                )
                _uiState.update { it.copy(currentUser = u) }
                FirebaseRepository.touchLive(u.id, progressMs, now)
            }
            return
        }
        lastLiveHeartbeat = now
        val updatedUser = _uiState.value.currentUser.copy(
            currentTrack = liveTrack,
            presenceState = com.example.model.UserPresenceState.LIVE,
            trackProgressMs = progressMs,
            trackProgressAt = now
        )
        _uiState.update { it.copy(nowPlayingTrack = liveTrack, currentUser = updatedUser) }
        checkAndSendLiveNotification(updatedUser)
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun clearSpotifyError() {
        _uiState.update { it.copy(spotifyError = null) }
    }

    fun setApplyCoverToFeed(enabled: Boolean) {
        _uiState.update { it.copy(applyCoverToFeed = enabled) }
        appContext.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("apply_cover_to_feed", enabled)
            .apply()
    }

    fun setLiveNotificationsEnabled(enabled: Boolean) {
        _uiState.update {
            it.copy(
                liveNotificationsEnabled = enabled,
                currentUser = it.currentUser.copy(liveNotificationsEnabled = enabled)
            )
        }
        appContext.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("live_notifications_enabled", enabled)
            .apply()
        val userId = _uiState.value.currentUser.id
        if (userId.isNotBlank()) {
            FirebaseRepository.setLiveNotificationsEnabled(userId, enabled)
        }
    }

    /** Presenza: l'utente sta usando l'app (connesso) — distinta dall'essere in live. */
    fun setOnline(online: Boolean) {
        val id = _uiState.value.currentUser.id
        if (id.isNotBlank()) {
            val currentPresence = _uiState.value.currentUser.presenceState
            val newPresence = if (currentPresence == com.example.model.UserPresenceState.LIVE) {
                com.example.model.UserPresenceState.LIVE
            } else if (online) {
                com.example.model.UserPresenceState.ONLINE
            } else {
                com.example.model.UserPresenceState.OFFLINE
            }
            _uiState.update { it.copy(currentUser = it.currentUser.copy(presenceState = newPresence)) }
            FirebaseRepository.setOnline(id, online)
        }
    }

    // Lo stato LIVE dipende dalla music-app (Spotify/Amazon), NON dal fatto che la nostra
    // app (MUSIC) sia in primo piano: chiudendo/mettendo in background MUSIC non si esce
    // dalla live (si resta live finché la music-app riproduce/è in pausa).
    fun onAppForeground() {
        isAppInForeground = true
        // Il resync/polling che parte subito dopo il rientro non deve generare notifiche live.
        suppressLivePushUntilMs = System.currentTimeMillis() + 5000L
        setOnline(true)
    }

    fun onAppStopped() {
        isAppInForeground = false
        setOnline(false)
    }

    fun onAppDestroyed() {
        isAppInForeground = false
        setOnline(false)
    }

    override fun onCleared() {
        super.onCleared()
        // Il ViewModel muore (app chiusa): sgancia i callback così il listener continua a
        // pubblicare la live scrivendo DIRETTAMENTE su Firestore (UC5/UC9: app MUSIC chiusa
        // ma streaming attivo → l'amico continua a vederti live).
        com.example.MusicNotificationListenerService.onTrackChanged = null
        com.example.MusicNotificationListenerService.onProgressChanged = null
        com.example.MusicNotificationListenerService.onPlaybackStopped = null
    }

    fun checkNotificationListenerEnabled() {
        val enabled = com.example.MusicNotificationListenerService.isEnabled(appContext)
        _uiState.update { it.copy(isNotificationListenerEnabled = enabled) }
        registerMusicNotificationListenerCallbacks()
        syncNotificationListenerServiceFlags()
        if (enabled) {
            com.example.MusicNotificationListenerService.startListening(appContext)
            // Riapertura app con musica già in corso: forza il re-emit del brano corrente
            // (altrimenti il service lo vede come "stesso brano" e non popola la live locale
            // finché non cambi traccia).
            com.example.MusicNotificationListenerService.resyncCurrentTrack()
        }
    }

    private fun syncNotificationListenerServiceFlags() {
        com.example.MusicNotificationListenerService.isSpotifyFreeEnabled = _uiState.value.connectedServices["spotify_free"] ?: false
        com.example.MusicNotificationListenerService.isAmazonMusicEnabled = _uiState.value.connectedServices["amazon_music"] ?: false
    }

    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(
            android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun registerMusicNotificationListenerCallbacks() {
        com.example.MusicNotificationListenerService.onTrackChanged = { trackName, artist, durationMs, positionMs, artUrl, source ->
            updateNowPlayingFromBroadcast("", trackName, artist, "", durationMs, positionMs, artUrl, source = source)
        }
        com.example.MusicNotificationListenerService.onProgressChanged = { positionMs, durationMs, source ->
            updateLiveProgress(positionMs, durationMs)
        }
        com.example.MusicNotificationListenerService.onPlaybackStopped = { source ->
            clearNowPlayingFromBroadcast(source = source)
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
        durationMs: Long = 0L, positionMs: Long = 0L, artUrl: String = "",
        source: String = "spotify"
    ) {
        if (trackName.isBlank()) return
        val cleanArtist = sanitizeSpotifyContext(artistName)
        val cleanAlbum = sanitizeSpotifyContext(albumName)
        viewModelScope.launch {
            val coverUrl = com.example.data.CoverResolver.resolveCoverUrl(cleanArtist, trackName, artUrl)
            val now = System.currentTimeMillis()
            lastLiveHeartbeat = now
            val track = Track(
                id = trackId.ifBlank { "$cleanArtist-$trackName".hashCode().toString() },
                title = trackName,
                artist = cleanArtist,
                album = cleanAlbum,
                coverUrl = coverUrl,
                durationText = if (durationMs > 0) formatMs(durationMs) else "3:45",
                durationMs = durationMs,
                source = source,
                // Sorgente via notifiche (Spotify Free / Amazon Music): sempre dal telefono.
                deviceType = "Smartphone",
                deviceName = ""
            )
            val updatedUser = _uiState.value.currentUser.copy(
                currentTrack = track,
                presenceState = com.example.model.UserPresenceState.LIVE,
                trackProgressMs = positionMs, trackProgressAt = now
            )
            _uiState.update { it.copy(nowPlayingTrack = track, currentUser = updatedUser) }
            checkAndSendLiveNotification(updatedUser)
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

    fun clearNowPlayingFromBroadcast(source: String = "") {
        val currentTrack = _uiState.value.currentUser.currentTrack ?: _uiState.value.nowPlayingTrack
        if (currentTrack == null) return
        if (source.isNotBlank() && !currentTrack.source.equals(source, ignoreCase = true)) {
            return
        }
        wasLive = false
        val updatedUser = _uiState.value.currentUser.copy(
            currentTrack = null,
            presenceState = if (isAppInForeground) com.example.model.UserPresenceState.ONLINE else com.example.model.UserPresenceState.OFFLINE
        )
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

    private fun processUserImages(user: User): User {
        val localAvatar = if (user.avatarUrl.startsWith("data:image", ignoreCase = true) ||
            (user.avatarUrl.length > 200 && !user.avatarUrl.startsWith("http", ignoreCase = true) && !user.avatarUrl.startsWith("file:", ignoreCase = true))
        ) {
            com.example.data.ImageUtils.base64ToLocalFile(appContext, user.avatarUrl, "avatar", user.id) ?: user.avatarUrl
        } else {
            user.avatarUrl
        }

        val localCover = if (!user.coverUrl.isNullOrBlank() && (user.coverUrl!!.startsWith("data:image", ignoreCase = true) ||
            (user.coverUrl!!.length > 200 && !user.coverUrl!!.startsWith("http", ignoreCase = true) && !user.coverUrl!!.startsWith("file:", ignoreCase = true)))
        ) {
            com.example.data.ImageUtils.base64ToLocalFile(appContext, user.coverUrl, "cover", user.id) ?: user.coverUrl
        } else {
            user.coverUrl
        }

        return user.copy(avatarUrl = localAvatar, coverUrl = localCover)
    }

    private suspend fun resolveLiveTrackCovers(user: User): User {
        val u = processUserImages(user)
        val tr = u.currentTrack ?: return u
        if (u.isLiveNow && (tr.coverUrl.isBlank() || tr.coverUrl.contains("unsplash.com") || !tr.coverUrl.startsWith("http", ignoreCase = true))) {
            val resolved = com.example.data.CoverResolver.resolveCoverUrl(tr.artist, tr.title, tr.coverUrl)
            return u.copy(currentTrack = tr.copy(coverUrl = resolved))
        }
        return u
    }

    private fun startFirebaseListener() {
        val userId = _uiState.value.currentUser.id
        // Ticker: forza la riValutazione del TTL dei live anche in assenza di eventi Firestore
        // (es. tutti i live vengono uccisi contemporaneamente e nessun altro aggiorna il DB).
        liveStaleTickerJob?.cancel()
        liveStaleTickerJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(30_000L)
                // Heartbeat presenza: in foreground rinfresca updatedAt del proprio documento,
                // così NON si viene declassati per staleness (e gli online idle restano online).
                val uid = _uiState.value.currentUser.id
                if (isAppInForeground && uid.isNotBlank()) {
                    FirebaseRepository.setOnline(uid, true)
                }
                // Declassa gli stati stantii: chi non aggiorna da oltre il TTL torna OFFLINE
                // ovunque (presenceState è l'unica fonte di verità).
                _uiState.update { current ->
                    val ref = presenceRef(current.feedUsers)
                    current.copy(
                        feedUsers = current.feedUsers.map { demoteIfStale(it, ref) },
                        activeProfileUser = current.activeProfileUser?.let {
                            if (it.isCurrentUser) it else demoteIfStale(it, ref)
                        },
                        liveTick = System.currentTimeMillis()
                    )
                }
            }
        }
        firebaseObserverJob?.cancel()
        firebaseObserverJob = viewModelScope.launch {
            FirebaseRepository.observeOtherUsers(userId)
                .catch { }
                .collect { remoteUsers ->
                    val processed = withContext(Dispatchers.IO) {
                        remoteUsers.map { resolveLiveTrackCovers(it) }
                    }
                    _uiState.update { current ->
                        // `processed` è la lista autorevole dal server. Gli utenti non più
                        // presenti (cancellati) NON vengono resuscitati: manteniamo dalla cache
                        // locale solo quelli che seguiamo (per non perderli se escono dai 40
                        // più recenti). Così la ricerca persone non mostra profili cancellati.
                        val freshIds = processed.map { it.id }.toSet()
                        val followed = current.currentUser.followingIds.toSet()
                        val retained = current.feedUsers.filter { it.id !in freshIds && it.id in followed }
                        val mergedRaw = (processed + retained).distinctBy { it.id }
                        // Declassa gli stati stantii (presenceState = unica fonte di verità).
                        val ref = presenceRef(mergedRaw)
                        val merged = mergedRaw.map { demoteIfStale(it, ref) }
                        val updatedActiveProfile = if (current.activeProfileUser != null && !current.activeProfileUser.isCurrentUser) {
                            val fresh = processed.find { it.id == current.activeProfileUser.id } ?: current.activeProfileUser
                            demoteIfStale(fresh, ref)
                        } else {
                            current.activeProfileUser
                        }
                        current.copy(
                            feedUsers = merged,
                            activeProfileUser = updatedActiveProfile,
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
                    val newIds = fresh.pendingRequests.map { it.id }.toSet()
                    // FCM Push via Render gestisce la notifica reale in tempo reale su qualsiasi stato (aperta, background, chiusa).
                    friendRequestsInitialized = true
                    knownFriendRequestIds = newIds

                    _uiState.update { current ->
                        // Se fresh contiene un'immagine in base64, convertila in un file locale di cache
                        val localAvatar = if (fresh.avatarUrl.startsWith("data:image", ignoreCase = true) ||
                            (fresh.avatarUrl.length > 200 && !fresh.avatarUrl.startsWith("http", ignoreCase = true) && !fresh.avatarUrl.startsWith("file:", ignoreCase = true))
                        ) {
                            com.example.data.ImageUtils.base64ToLocalFile(appContext, fresh.avatarUrl, "avatar", userId) ?: fresh.avatarUrl
                        } else {
                            fresh.avatarUrl
                        }

                        val localCover = if (!fresh.coverUrl.isNullOrBlank() && (fresh.coverUrl.startsWith("data:image", ignoreCase = true) ||
                            (fresh.coverUrl.length > 200 && !fresh.coverUrl.startsWith("http", ignoreCase = true) && !fresh.coverUrl.startsWith("file:", ignoreCase = true)))
                        ) {
                            com.example.data.ImageUtils.base64ToLocalFile(appContext, fresh.coverUrl, "cover", userId) ?: fresh.coverUrl
                        } else {
                            fresh.coverUrl
                        }

                        // Preserva il file locale se già esistente su disco per evitare flash o sovrascritture stantie
                        val curAvatar = current.currentUser.avatarUrl
                        val finalAvatar = if (curAvatar.startsWith("file://") && File(curAvatar.removePrefix("file://")).exists()) {
                            curAvatar
                        } else if (localAvatar.isNotBlank()) {
                            localAvatar
                        } else {
                            curAvatar
                        }

                        val curCover = current.currentUser.coverUrl
                        val finalCover = if (!curCover.isNullOrBlank() && curCover.startsWith("file://") && File(curCover.removePrefix("file://")).exists()) {
                            curCover
                        } else if (!localCover.isNullOrBlank()) {
                            localCover
                        } else {
                            curCover
                        }

                        appContext.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
                            .edit()
                            .putString("last_user_id", fresh.id)
                            .apply()

                        val updatedCurrentUser = current.currentUser.copy(
                            name = fresh.name.ifBlank { current.currentUser.name },
                            username = fresh.username.ifBlank { current.currentUser.username },
                            avatarUrl = finalAvatar,
                            coverUrl = finalCover,
                            bio = fresh.bio,
                            sharedTracks = fresh.sharedTracks,
                            stats = fresh.stats,
                            followerIds = fresh.followerIds,
                            followingIds = fresh.followingIds
                            // isLiveNow e currentTrack restano gestiti localmente da Spotify
                        )
                        saveUserLocalPrefs(updatedCurrentUser)
                        current.copy(
                            currentUser = updatedCurrentUser,
                            activeProfileUser = if (current.activeProfileUser?.isCurrentUser == true || current.activeProfileUser?.id == updatedCurrentUser.id) updatedCurrentUser else current.activeProfileUser,
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

    private val servicesPrefs
        get() = appContext.getSharedPreferences("connected_services", Context.MODE_PRIVATE)

    // Stato "collegato" esplicito e RICORDATO per i servizi basati su notifiche
    // (Spotify Free, Amazon Music). È fittizio a livello funzionale — dipendono tutti
    // dal listener notifiche — ma rispetta la scelta dell'utente che ci ha cliccato.
    private fun loadPersistedServices(): Map<String, Boolean> = mapOf(
        "spotify_free" to servicesPrefs.getBoolean("spotify_free", false),
        "amazon_music" to servicesPrefs.getBoolean("amazon_music", false)
    )

    private fun persistServiceState(serviceKey: String, connected: Boolean) {
        servicesPrefs.edit().putBoolean(serviceKey, connected).apply()
    }

    fun toggleConnectedService(serviceKey: String) {
        val currentServices = _uiState.value.connectedServices.toMutableMap()
        val newState = !(currentServices[serviceKey] ?: false)
        currentServices[serviceKey] = newState
        persistServiceState(serviceKey, newState)

        syncNotificationListenerServiceFlags()

        when (serviceKey) {
            "spotify_free" -> {
                if (newState) {
                    com.example.MusicNotificationListenerService.startListening(appContext)
                    stopSpotifyPolling() // sorgente = notifiche, non API
                }
            }
            "amazon_music" -> {
                if (newState) {
                    com.example.MusicNotificationListenerService.startListening(appContext)
                }
            }
        }
        // Disconnessione di un servizio a notifiche -> la live si spegne subito
        if (!newState && (serviceKey == "spotify_free" || serviceKey == "amazon_music")) {
            val src = if (serviceKey == "spotify_free") "spotify" else "amazon_music"
            clearNowPlayingFromBroadcast(source = src)
        }

        val serviceName = when (serviceKey) {
            "amazon_music" -> "Amazon Music"
            "spotify_free" -> "Spotify Free"
            else -> serviceKey.replaceFirstChar { it.uppercase() }
        }

        val listenerEnabled = com.example.MusicNotificationListenerService.isEnabled(appContext)
        val isNotificationService = (serviceKey == "spotify_free" || serviceKey == "amazon_music")
        if (newState && isNotificationService && !listenerEnabled) {
            openNotificationListenerSettings(appContext)
            _uiState.update {
                it.copy(
                    connectedServices = currentServices,
                    feedbackToast = "Abilita l'accesso alle notifiche per $serviceName"
                )
            }
        } else {
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

    private fun saveUserLocalPrefs(user: User) {
        try {
            val prefKey = if (user.id.isNotBlank()) "user_profile_prefs_${user.id}" else "user_profile_prefs_default"
            val prefs = appContext.getSharedPreferences(prefKey, Context.MODE_PRIVATE)
            prefs.edit()
                .putString("name", user.name)
                .putString("username", user.username)
                .putString("avatarUrl", user.avatarUrl)
                .putString("coverUrl", user.coverUrl ?: "")
                .putString("bio", user.bio)
                .apply()
        } catch (e: Exception) {
            android.util.Log.e("MusicViewModel", "Errore salvataggio locale profilo: ${e.message}")
        }
    }

    private fun applyUserLocalPrefs(user: User): User {
        try {
            val prefKey = if (user.id.isNotBlank()) "user_profile_prefs_${user.id}" else "user_profile_prefs_default"
            val prefs = appContext.getSharedPreferences(prefKey, Context.MODE_PRIVATE)
            val savedName = prefs.getString("name", null)
            val savedUsername = prefs.getString("username", null)
            val savedAvatarUrl = prefs.getString("avatarUrl", null)
            val savedCoverUrl = prefs.getString("coverUrl", null)
            val savedBio = prefs.getString("bio", null)

            if (savedName == null && savedUsername == null && savedAvatarUrl == null && savedCoverUrl == null && savedBio == null) {
                return user
            }

            val validAvatar = savedAvatarUrl?.takeIf {
                if (it.startsWith("file://")) File(it.removePrefix("file://")).exists() else it.isNotBlank()
            } ?: user.avatarUrl

            val validCover = savedCoverUrl?.takeIf {
                if (it.startsWith("file://")) File(it.removePrefix("file://")).exists() else it.isNotBlank()
            } ?: user.coverUrl

            return user.copy(
                name = savedName?.ifBlank { user.name } ?: user.name,
                username = savedUsername?.ifBlank { user.username } ?: user.username,
                avatarUrl = validAvatar,
                coverUrl = validCover,
                bio = savedBio ?: user.bio
            )
        } catch (e: Exception) {
            return user
        }
    }

    fun updateProfile(name: String, username: String, avatarUrl: String, coverUrl: String? = null, bio: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanUsername = username.trim().removePrefix("@")
            val cleanAvatar = avatarUrl.trim()
            val cleanCover = coverUrl?.trim()
            val updatedUser = _uiState.value.currentUser.copy(
                name = name.trim().ifBlank { _uiState.value.currentUser.name },
                username = cleanUsername.ifBlank { _uiState.value.currentUser.username },
                avatarUrl = if (cleanAvatar.isNotBlank()) cleanAvatar else _uiState.value.currentUser.avatarUrl,
                coverUrl = if (!cleanCover.isNullOrBlank()) cleanCover else _uiState.value.currentUser.coverUrl,
                bio = bio.trim(),
                isCurrentUser = true
            )
            saveUserLocalPrefs(updatedUser)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        currentUser = updatedUser,
                        activeProfileUser = updatedUser,
                        feedbackToast = "Profilo aggiornato: @${updatedUser.username}"
                    )
                }
            }
            // Per Firestore, se l'immagine è un file locale, convertila in Base64 per inviarla al cloud
            val firestoreAvatar = com.example.data.ImageUtils.fileUriToBase64(updatedUser.avatarUrl) ?: updatedUser.avatarUrl
            val firestoreCover = com.example.data.ImageUtils.fileUriToBase64(updatedUser.coverUrl) ?: updatedUser.coverUrl
            val firestoreUser = updatedUser.copy(avatarUrl = firestoreAvatar, coverUrl = firestoreCover)
            FirebaseRepository.syncCurrentUser(firestoreUser)
        }
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

    fun deleteSharedTrack(track: Track) {
        val currentUser = _uiState.value.currentUser
        val updatedTracks = currentUser.sharedTracks.filterNot { it.id == track.id }
        val updatedUser = currentUser.copy(sharedTracks = updatedTracks)
        _uiState.update {
            it.copy(
                currentUser = updatedUser,
                activeProfileUser = if (it.activeProfileUser?.isCurrentUser == true || it.activeProfileUser?.id == currentUser.id) updatedUser else it.activeProfileUser,
                feedbackToast = "Canzone rimossa dal feed"
            )
        }
        FirebaseRepository.deleteSharedTrack(currentUser.id, track.id)
    }

    private var wasLive = false
    // Finestra dopo il rientro in foreground in cui NON si notifica la live: il resync/polling
    // iniziale ri-emette la traccia già in corso, ma NON è un nuovo ascolto → niente push.
    private var suppressLivePushUntilMs = 0L

    private fun checkAndSendLiveNotification(user: User) {
        val isLive = user.presenceState == com.example.model.UserPresenceState.LIVE
        if (!isLive) {
            wasLive = false
            return
        }
        if (wasLive) return // già live in questa sessione → nessuna re-notifica
        wasLive = true
        // Se è solo il resync all'apertura dell'app (traccia già in riproduzione), non notificare:
        // la notifica live è SOLO per l'inizio effettivo di un brano.
        if (System.currentTimeMillis() < suppressLivePushUntilMs) return
        FirebaseRepository.sendLiveNotificationToFollowers(user)
    }

    fun openLiveFromNotification(hostUserId: String) {
        if (hostUserId.isBlank()) return
        FirebaseRepository.getUsersByIds(listOf(hostUserId)) { users ->
            val hostUser = users.firstOrNull()
            if (hostUser != null) {
                openStory(hostUser)
            }
        }
    }

    fun openStory(user: User) {
        if (!user.isCurrentUser) {
            // Assicura che l'utente sia presente in feedUsers come LIVE (upsert), così comparirà
            // in getStoriesList() — la lista effettivamente usata per renderizzare il dettaglio.
            val liveUser = user.copy(presenceState = com.example.model.UserPresenceState.LIVE)
            val feed = _uiState.value.feedUsers
            val newFeed = if (feed.any { it.id == user.id })
                feed.map { if (it.id == user.id) liveUser else it }
            else
                listOf(liveUser) + feed
            // La push "live" arriva solo ai follower: assicura il follow noto localmente così
            // l'utente compare in getStoriesList() anche all'avvio a freddo da notifica, prima
            // che il listener del documento profilo abbia caricato followingIds.
            val cu = _uiState.value.currentUser
            val ensuredFollowing = if (cu.followingIds.contains(user.id)) cu.followingIds
                                   else cu.followingIds + user.id
            _uiState.update {
                it.copy(feedUsers = newFeed, currentUser = cu.copy(followingIds = ensuredFollowing))
            }
        }
        // L'indice DEVE riferirsi a getStoriesList(): è ciò che il dettaglio live indicizza.
        val stories = getStoriesList()
        val index = stories.indexOfFirst { it.id == user.id }
        if (index != -1) {
            _uiState.update { it.copy(activeStoryUserIndex = index) }
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
        val isMe = user.isCurrentUser || (user.id.isNotBlank() && user.id == _uiState.value.currentUser.id)
        val target = if (isMe) _uiState.value.currentUser.copy(isCurrentUser = true) else processUserImages(user)
        _uiState.update { it.copy(activeProfileUser = target) }
        if (isMe) {
            // Usa sempre i dati freschi dallo stato (non l'oggetto passato che potrebbe essere stale)
            loadSocialDetails(_uiState.value.currentUser)
        } else {
            loadSocialDetails(target)
            loadFriendProfile(target.id)
        }
    }

    private fun loadFriendProfile(userId: String) {
        if (userId.isBlank()) return
        FirebaseRepository.getUsersByIds(listOf(userId)) { users ->
            val freshUser = users.firstOrNull() ?: return@getUsersByIds
            viewModelScope.launch(Dispatchers.IO) {
                val processed = processUserImages(freshUser)
                withContext(Dispatchers.Main) {
                    _uiState.update { state ->
                        val updatedFeed = (listOf(processed) + state.feedUsers).distinctBy { it.id }
                        state.copy(
                            activeProfileUser = if (state.activeProfileUser?.id == processed.id) processed else state.activeProfileUser,
                            feedUsers = updatedFeed
                        )
                    }
                }
            }
        }
    }

    fun loadSocialDetails(user: User) {
        val ids = (user.followerIds + user.followingIds).distinct()
        if (ids.isEmpty()) return
        FirebaseRepository.getUsersByIds(ids) { allUsers ->
            viewModelScope.launch(Dispatchers.IO) {
                val processed = allUsers.map { processUserImages(it) }
                withContext(Dispatchers.Main) {
                    val followerSet = user.followerIds.toSet()
                    val followingSet = user.followingIds.toSet()
                    _uiState.update { state ->
                        state.copy(
                            followerDetails = processed.filter { it.id in followerSet },
                            followingDetails = processed.filter { it.id in followingSet }
                        )
                    }
                }
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
        // Segnala al servizio FCM che stai guardando questa chat → niente notifica per i suoi messaggi.
        com.example.AppFirebaseMessagingService.openChatUserId = user.id
        _uiState.update { it.copy(activeChatUser = user, activeStoryUserIndex = null, isChatListOpen = false) }

        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) return

        val convId = FirebaseRepository.getConversationId(currentUserId, user.id)

        // Rimuovi il listener precedente se presente
        chatMessagesListener?.remove()
        chatMessagesListener = FirebaseRepository.listenToMessages(convId, currentUserId) { messages ->
            val updatedMap = _uiState.value.chatMessages.toMutableMap().apply { put(user.id, messages) }
            _uiState.update { it.copy(chatMessages = updatedMap) }
        }

        // Se c'è una traccia iniziale, invia subito un messaggio con la traccia allegata
        if (initialTrack != null) {
            sendMessage(user.id, "Ho visto che stavi ascoltando \"${initialTrack.title}\"!", initialTrack)
        }
    }

    fun closeChat() {
        com.example.AppFirebaseMessagingService.openChatUserId = null
        chatMessagesListener?.remove()
        chatMessagesListener = null
        _uiState.update { it.copy(activeChatUser = null) }
    }

    fun sendMessage(recipientId: String, text: String, attachedTrack: Track? = null) {
        if (text.isBlank()) return
        val currentUserId = _uiState.value.currentUser.id
        if (currentUserId.isBlank()) return

        // 1. Aggiornamento ottimistico locale per feedback immediato a 0ms
        val localMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            senderId = currentUserId,
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            attachedTrack = attachedTrack
        )
        val currentList = _uiState.value.chatMessages[recipientId]?.toMutableList() ?: mutableListOf()
        currentList.add(localMessage)
        val updatedMap = _uiState.value.chatMessages.toMutableMap().apply { put(recipientId, currentList) }
        _uiState.update { it.copy(chatMessages = updatedMap) }

        // 2. Persistenza atomica su Firestore
        val convId = FirebaseRepository.getConversationId(currentUserId, recipientId)
        FirebaseRepository.sendChatMessage(
            conversationId = convId,
            senderId = currentUserId,
            senderName = _uiState.value.currentUser.name,
            senderAvatarUrl = _uiState.value.currentUser.avatarUrl,
            recipientId = recipientId,
            text = text,
            attachedTrack = attachedTrack
        )
    }

    /** Invia un Pulse tattile (registrazione di 5s come stringa di campioni). */
    fun sendPulse(recipientId: String, samples: String, audioBase64: String? = null) {
        if (!com.example.data.PulseHaptics.hasContent(samples)) return
        val me = _uiState.value.currentUser
        if (me.id.isBlank() || recipientId.isBlank()) return
        val convId = FirebaseRepository.getConversationId(me.id, recipientId)
        FirebaseRepository.sendChatMessage(
            conversationId = convId,
            senderId = me.id,
            senderName = me.name,
            senderAvatarUrl = me.avatarUrl,
            recipientId = recipientId,
            text = "",
            attachedTrack = null,
            pulse = samples,
            pulseAudioBase64 = audioBase64
        )
        _uiState.update { it.copy(feedbackToast = "Pulse inviato") }
    }

    fun openPulseFromNotification(senderId: String, senderName: String, avatarUrl: String, samples: String, audioId: String? = null) {
        if (samples.isBlank()) return
        _uiState.update {
            it.copy(activePulse = com.example.model.ActivePulse(senderId, senderName, avatarUrl, samples, audioId))
        }
    }

    fun dismissPulse() {
        _uiState.update { it.copy(activePulse = null) }
    }

    fun openChatFromNotification(senderId: String) {
        if (senderId.isBlank()) return
        FirebaseRepository.getUsersByIds(listOf(senderId)) { users ->
            users.firstOrNull()?.let { openChat(it) }
        }
    }

    /**
     * Azzera TUTTI gli overlay/dialog aperti, così la navigazione da notifica porta davvero
     * al punto di riferimento indipendentemente da dove ci si trova nell'app.
     */
    fun clearOverlaysForNavigation() {
        com.example.AppFirebaseMessagingService.openChatUserId = null
        _uiState.update {
            it.copy(
                activeProfileUser = null,
                isChatListOpen = false,
                activeChatUser = null,
                activeStoryUserIndex = null,
                isShareSheetOpen = false,
                showPeopleSearch = false,
                showNotifications = false,
                selectedTrackDetail = null
            )
        }
    }

    fun openChatList() {
        _uiState.update { it.copy(isChatListOpen = true, userSearchQuery = "", userSearchResults = emptyList()) }
    }

    fun closeChatList() {
        _uiState.update { it.copy(isChatListOpen = false, userSearchQuery = "", userSearchResults = emptyList()) }
    }

    private fun startConversationsListener(userId: String) {
        conversationsListener?.remove()
        conversationsListener = FirebaseRepository.listenToConversations(userId) { rawConvList ->
            viewModelScope.launch {
                val missingUserIds = mutableListOf<String>()
                val initialConversations = rawConvList.mapNotNull { (convId, data) ->
                    val participants = (data["participants"] as? List<*>)?.filterIsInstance<String>() ?: return@mapNotNull null
                    val otherUserId = participants.firstOrNull { it != userId } ?: return@mapNotNull null

                    val recipientUser = _uiState.value.feedUsers.firstOrNull { it.id == otherUserId }
                        ?: _uiState.value.followerDetails.firstOrNull { it.id == otherUserId }
                        ?: _uiState.value.followingDetails.firstOrNull { it.id == otherUserId }

                    if (recipientUser == null) {
                        missingUserIds.add(otherUserId)
                    }

                    val lastText = data["lastMessageText"] as? String ?: ""
                    val lastAt = (data["lastMessageAt"] as? Number)?.toLong() ?: 0L
                    val lastSenderId = data["lastMessageSenderId"] as? String ?: ""
                    val trackData = data["lastAttachedTrack"] as? Map<*, *>
                    val lastTrack = trackData?.let {
                        Track(
                            id = it["id"] as? String ?: "",
                            title = it["title"] as? String ?: "",
                            artist = it["artist"] as? String ?: "",
                            coverUrl = it["coverUrl"] as? String ?: ""
                        )
                    }

                    Conversation(
                        id = convId,
                        recipientUser = recipientUser ?: User(id = otherUserId, name = "Utente", username = "", avatarUrl = ""),
                        lastMessageText = lastText,
                        lastMessageAt = lastAt,
                        lastMessageSenderId = lastSenderId,
                        lastAttachedTrack = lastTrack
                    )
                }
                _uiState.update { it.copy(conversations = initialConversations) }

                // Se ci sono utenti non ancora in cache locale, caricali da Firestore
                if (missingUserIds.isNotEmpty()) {
                    FirebaseRepository.getUsersByIds(missingUserIds.distinct()) { fetchedUsers ->
                        if (fetchedUsers.isNotEmpty()) {
                            val userMap = fetchedUsers.associateBy { it.id }
                            _uiState.update { state ->
                                val updatedConv = state.conversations.map { conv ->
                                    val fetched = userMap[conv.recipientUser.id]
                                    if (fetched != null) conv.copy(recipientUser = fetched) else conv
                                }
                                state.copy(conversations = updatedConv)
                            }
                        }
                    }
                }
            }
        }
    }

    fun inspectTrack(track: Track, user: User? = null) {
        _uiState.update { it.copy(selectedTrackDetail = Pair(track, user)) }
    }

    fun closeTrackInspector() { _uiState.update { it.copy(selectedTrackDetail = null) } }

    fun clearToast() { _uiState.update { it.copy(feedbackToast = null) } }

    fun clearLoginError() { _uiState.update { it.copy(loginError = null) } }

    /**
     * Riferimento temporale robusto allo sfasamento di orologio: il timestamp più recente
     * scritto da chiunque (ripiega sull'orologio locale se nessuno aggiorna da oltre il TTL).
     */
    private fun presenceRef(users: List<User>): Long {
        val localNow = System.currentTimeMillis()
        val maxUpdated = users.maxOfOrNull { it.updatedAt } ?: 0L
        return if (localNow - maxUpdated > LIVE_STALE_TTL_MS) localNow else maxUpdated
    }

    /** Se il documento è stantio (nessun aggiornamento entro il TTL) l'utente torna OFFLINE. */
    private fun demoteIfStale(u: User, ref: Long): User =
        if (u.updatedAt > 0L && (ref - u.updatedAt) > LIVE_STALE_TTL_MS &&
            u.presenceState != com.example.model.UserPresenceState.OFFLINE
        ) {
            u.copy(presenceState = com.example.model.UserPresenceState.OFFLINE, currentTrack = null)
        } else u

    fun getStoriesList(): List<User> {
        val state = _uiState.value
        val followingIds = state.currentUser.followingIds
        val localNow = System.currentTimeMillis()
        // Riferimento temporale ROBUSTO allo sfasamento di orologio tra dispositivi: usiamo il
        // timestamp più recente scritto da chiunque (≈ "ora" nel dominio di chi scrive), così un
        // amico realmente live non sparisce se l'orologio di questo device è avanti. Ripieghiamo
        // sull'orologio locale solo se nessuno aggiorna da oltre il TTL, così i processi uccisi
        // scadono comunque (UC8).
        val maxUpdatedAt = state.feedUsers.maxOfOrNull { it.updatedAt } ?: 0L
        val referenceNow = if (localNow - maxUpdatedAt > LIVE_STALE_TTL_MS) localNow else maxUpdatedAt
        val list = mutableListOf<User>()
        // Il proprio stato è locale e sempre "fresco": nessun TTL su noi stessi.
        if (state.currentUser.currentTrack != null && state.currentUser.isLiveNow) list.add(state.currentUser)
        // Gli altri sono live solo se il documento è stato aggiornato di recente.
        list.addAll(state.feedUsers.filter {
            followingIds.contains(it.id) && it.isActuallyLive && (referenceNow - it.updatedAt) < LIVE_STALE_TTL_MS
        })
        return list
    }

    companion object {
        const val FRIEND_REQUEST_CHANNEL_ID = "friend_requests_channel"
        // Un utente è considerato live solo se il suo documento è stato aggiornato entro questo
        // tempo. L'heartbeat del listener rinfresca updatedAt ~ogni 20s; 90s tollera qualche
        // ping perso ma fa sparire chi ha il processo ucciso (UC8).
        private const val LIVE_STALE_TTL_MS = 90_000L
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
                presenceState = com.example.model.UserPresenceState.OFFLINE,
                currentTrack = null,
                sharedTracks = emptyList(),
                stats = UserStats(sharedCount = 0, topArtist = "", totalMinutesOrGenres = "")
            )
        }
    }
}
