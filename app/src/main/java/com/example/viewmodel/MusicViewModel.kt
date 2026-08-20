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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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
    val sentRequestIds: Set<String> = emptySet()
)

class MusicViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext: Context = app.applicationContext

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var userSearchJob: Job? = null
    private var firebaseObserverJob: Job? = null
    private var spotifyPollingJob: Job? = null

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
                if (spotifyConnected) startSpotifyPolling()
            }
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
                fetchCurrentlyPlaying()
                delay(5_000)
            }
        }
    }

    fun stopSpotifyPolling() {
        spotifyPollingJob?.cancel()
        spotifyPollingJob = null
    }

    private suspend fun fetchCurrentlyPlaying() {
        val track = SpotifyWebApiRepository.getCurrentlyPlaying(appContext)
        val currentTrackId = _uiState.value.nowPlayingTrack?.id

        if (track == null) {
            if (currentTrackId != null) {
                val updatedUser = _uiState.value.currentUser.copy(currentTrack = null, isLiveNow = false)
                _uiState.update { it.copy(nowPlayingTrack = null, currentUser = updatedUser) }
                FirebaseRepository.syncCurrentUser(updatedUser)
            }
            return
        }

        if (track.id == currentTrackId) return
        val updatedUser = _uiState.value.currentUser.copy(currentTrack = track, isLiveNow = true)
        _uiState.update { it.copy(nowPlayingTrack = track, currentUser = updatedUser) }
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun clearSpotifyError() {
        _uiState.update { it.copy(spotifyError = null) }
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
        viewModelScope.launch {
            FirebaseRepository.observeCurrentUserSocial(userId)
                .catch { }
                .collect { (followerIds, followingIds) ->
                    _uiState.update { current ->
                        current.copy(
                            currentUser = current.currentUser.copy(
                                followerIds = followerIds,
                                followingIds = followingIds
                            )
                        )
                    }
                }
        }
        viewModelScope.launch {
            FirebaseRepository.observePendingSentRequests(userId)
                .catch { }
                .collect { sentIds ->
                    _uiState.update { it.copy(sentRequestIds = sentIds) }
                }
        }
        startFriendRequestListener()
    }

    // ===================== SERVICES =====================

    fun toggleConnectedService(serviceKey: String) {
        val currentServices = _uiState.value.connectedServices.toMutableMap()
        val newState = !(currentServices[serviceKey] ?: false)
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
        FirebaseRepository.acceptFollowRequest(request.id, currentId, request.fromUserId)
    }

    fun rejectFriendRequest(request: FriendRequest) {
        val updated = _uiState.value.pendingFriendRequests.filter { it.id != request.id }
        _uiState.update { it.copy(pendingFriendRequests = updated) }
        FirebaseRepository.rejectFollowRequest(request.id)
    }

    private var knownFriendRequestIds = emptySet<String>()

    private fun startFriendRequestListener() {
        val userId = _uiState.value.currentUser.id
        if (userId.isBlank()) return
        viewModelScope.launch {
            FirebaseRepository.observeFriendRequests(userId)
                .catch { }
                .collect { requests ->
                    val newIds = requests.map { it.id }.toSet()
                    if (knownFriendRequestIds.isNotEmpty()) {
                        requests.filter { it.id !in knownFriendRequestIds }
                            .forEach { showFriendRequestNotification(it) }
                    }
                    knownFriendRequestIds = newIds
                    _uiState.update { it.copy(pendingFriendRequests = requests) }
                }
        }
    }

    private fun showFriendRequestNotification(request: FriendRequest) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (appContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        }
        val notif = androidx.core.app.NotificationCompat.Builder(appContext, FRIEND_REQUEST_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_my_calendar)
            .setContentTitle("Nuova richiesta di follow")
            .setContentText("@${request.fromUserUsername} vuole seguirti")
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        androidx.core.app.NotificationManagerCompat.from(appContext)
            .notify(request.id.hashCode(), notif)
    }

    fun shareTrack(track: Track) {
        val updatedUser = _uiState.value.currentUser.copy(
            currentTrack = track,
            sharedTracks = listOf(track) + _uiState.value.currentUser.sharedTracks.filterNot { it.id == track.id },
            stats = _uiState.value.currentUser.stats.copy(sharedCount = _uiState.value.currentUser.stats.sharedCount + 1)
        )
        _uiState.update {
            it.copy(
                currentUser = updatedUser,
                nowPlayingTrack = track,
                isShareSheetOpen = false,
                feedbackToast = "Condiviso: ${track.title}"
            )
        }
        FirebaseRepository.shareTrack(updatedUser.id, track)
        FirebaseRepository.syncCurrentUser(updatedUser)
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

    fun openProfile(user: User) { _uiState.update { it.copy(activeProfileUser = user) } }

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
