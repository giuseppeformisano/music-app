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
    val liveTick: Long = 0L
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

    init {
        SpotifyAuthRepository.loadTokens(appContext)
        checkForUpdate()
        val existingUser = AuthRepository.currentFirebaseUser
        if (existingUser != null) {
            viewModelScope.launch {
                val rawUser = existingUser.toAppUser()
                val user = applyUserLocalPrefs(rawUser)
                val spotifyConnected = SpotifyAuthRepository.isAuthorized
                val services = mapOf("spotify" to spotifyConnected) + loadPersistedServices()
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        isLoggedIn = true,
                        isSpotifyConnected = spotifyConnected,
                        connectedServices = services
                    )
                }
                FirebaseRepository.ensureUserProfile(user)
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
            result.onSuccess { rawUser ->
                val user = applyUserLocalPrefs(rawUser)
                val spotifyConnected = SpotifyAuthRepository.isAuthorized
                val services = mapOf("spotify" to spotifyConnected) + loadPersistedServices()
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
                // PRIMO login: crea il profilo dai metadati account; se esiste già non
                // sovrascrive i dati persistiti (nome/avatar modificati dall'utente).
                FirebaseRepository.ensureUserProfile(user)
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
                val updatedUser = _uiState.value.currentUser.copy(currentTrack = null, isLiveNow = false)
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
            currentTrack = liveTrack, isLiveNow = true,
            trackProgressMs = progressMs, trackProgressAt = now
        )
        _uiState.update { it.copy(nowPlayingTrack = liveTrack, currentUser = updatedUser) }
        FirebaseRepository.syncCurrentUser(updatedUser)
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

    // Lo stato LIVE dipende dalla music-app (Spotify/Amazon), NON dal fatto che la nostra
    // app (MUSIC) sia in primo piano: chiudendo/mettendo in background MUSIC non si esce
    // dalla live (si resta live finché la music-app riproduce/è in pausa).
    fun onAppForeground() {
        // no-op: mantenuto per compatibilità con MainActivity.onResume
    }

    fun onAppStopped() {
        setOnline(false)
    }

    fun onAppDestroyed() {
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
                durationMs = durationMs,
                source = source,
                // Sorgente via notifiche (Spotify Free / Amazon Music): sempre dal telefono.
                deviceType = "Smartphone",
                deviceName = ""
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

    fun clearNowPlayingFromBroadcast(source: String = "") {
        val currentTrack = _uiState.value.currentUser.currentTrack ?: _uiState.value.nowPlayingTrack
        if (currentTrack == null) return
        if (source.isNotBlank() && !currentTrack.source.equals(source, ignoreCase = true)) {
            return
        }
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

    private fun startFirebaseListener() {
        val userId = _uiState.value.currentUser.id
        // Ticker: forza la riValutazione del TTL dei live anche in assenza di eventi Firestore
        // (es. tutti i live vengono uccisi contemporaneamente e nessun altro aggiorna il DB).
        liveStaleTickerJob?.cancel()
        liveStaleTickerJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(30_000L)
                _uiState.update { it.copy(liveTick = System.currentTimeMillis()) }
            }
        }
        firebaseObserverJob?.cancel()
        firebaseObserverJob = viewModelScope.launch {
            FirebaseRepository.observeOtherUsers(userId)
                .catch { }
                .collect { remoteUsers ->
                    val processed = withContext(Dispatchers.IO) {
                        remoteUsers.map { processUserImages(it) }
                    }
                    _uiState.update { current ->
                        // `processed` è la lista autorevole dal server. Gli utenti non più
                        // presenti (cancellati) NON vengono resuscitati: manteniamo dalla cache
                        // locale solo quelli che seguiamo (per non perderli se escono dai 40
                        // più recenti). Così la ricerca persone non mostra profili cancellati.
                        val freshIds = processed.map { it.id }.toSet()
                        val followed = current.currentUser.followingIds.toSet()
                        val retained = current.feedUsers.filter { it.id !in freshIds && it.id in followed }
                        val merged = (processed + retained).distinctBy { it.id }
                        val updatedActiveProfile = if (current.activeProfileUser != null && !current.activeProfileUser.isCurrentUser) {
                            processed.find { it.id == current.activeProfileUser.id } ?: current.activeProfileUser
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
                    // Notifica solo le NUOVE richieste ricevute (dopo il primo snapshot)
                    val newIds = fresh.pendingRequests.map { it.id }.toSet()
                    if (friendRequestsInitialized) {
                        fresh.pendingRequests.filter { it.id !in knownFriendRequestIds }
                            .forEach { showFriendRequestNotification(it) }
                    }
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
        val now = System.currentTimeMillis()
        val list = mutableListOf<User>()
        // Il proprio stato è locale e sempre "fresco": nessun TTL su noi stessi.
        if (state.currentUser.currentTrack != null && state.currentUser.isLiveNow) list.add(state.currentUser)
        // Gli altri sono live solo se il documento è stato aggiornato di recente: se il loro
        // processo viene ucciso l'heartbeat si ferma e dopo il TTL spariscono (UC8).
        list.addAll(state.feedUsers.filter {
            followingIds.contains(it.id) && it.isActuallyLive && (now - it.updatedAt) < LIVE_STALE_TTL_MS
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
                isLiveNow = false,
                currentTrack = null,
                sharedTracks = emptyList(),
                stats = UserStats(sharedCount = 0, topArtist = "", totalMinutesOrGenres = "")
            )
        }
    }
}
