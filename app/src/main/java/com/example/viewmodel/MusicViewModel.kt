package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseRepository
import com.example.data.MusicRepository
import com.example.model.ChatMessage
import com.example.model.Track
import com.example.model.User
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class SearchTab {
    TRACKS,
    USERS
}

data class MusicUiState(
    val isLoggedIn: Boolean = false,
    val isLoggingIn: Boolean = false,
    val isSpotifyConnected: Boolean = true,
    val connectedServices: Map<String, Boolean> = mapOf(
        "spotify" to true,
        "apple_music" to false,
        "amazon_music" to false,
        "youtube_music" to false
    ),
    val currentUser: User = MusicRepository.currentUser,
    val feedUsers: List<User> = MusicRepository.initialFeedUsers,
    val activeStoryUserIndex: Int? = null,
    val isShareSheetOpen: Boolean = false,
    val searchTab: SearchTab = SearchTab.TRACKS,
    val nowPlayingTrack: Track = MusicRepository.curatedTracks[3], // Daft Punk - Instant Crush
    val isPlaying: Boolean = true,
    val searchQuery: String = "",
    val searchResults: List<Track> = emptyList(),
    val isSearching: Boolean = false,
    val userSearchQuery: String = "",
    val userSearchResults: List<User> = MusicRepository.allDiscoverableUsers,
    val isSearchingUsers: Boolean = false,
    val activeProfileUser: User? = null,
    val activeChatUser: User? = null,
    val chatMessages: Map<String, List<ChatMessage>> = MusicRepository.defaultChats,
    val selectedTrackDetail: Pair<Track, User?>? = null,
    val showDesignSpec: Boolean = false,
    val feedbackToast: String? = null
)

class MusicViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var userSearchJob: Job? = null
    private var firebaseObserverJob: Job? = null
    private var liveSimulationJob: Job? = null

    init {
        startFirebaseListener()
        startLiveSimulation()
    }

    fun startLiveSimulation() {
        liveSimulationJob?.cancel()
        liveSimulationJob = viewModelScope.launch {
            while (isActive) {
                delay(6500) // Cambia automaticamente un brano live ogni 6.5s per mostrare la transizione gaussiana
                simulateLiveTrackChange()
            }
        }
    }

    fun stopLiveSimulation() {
        liveSimulationJob?.cancel()
    }

    private fun startFirebaseListener() {
        firebaseObserverJob?.cancel()
        firebaseObserverJob = viewModelScope.launch {
            FirebaseRepository.observeOtherUsers(_uiState.value.currentUser.id)
                .catch { /* Fallback silenzioso se offline o non configurato */ }
                .collect { remoteUsers ->
                    if (remoteUsers.isNotEmpty()) {
                        _uiState.update { current ->
                            // Unisci utenti remoti reali con quelli locali curati
                            val merged = (remoteUsers + current.feedUsers).distinctBy { it.id }
                            current.copy(
                                feedUsers = merged,
                                userSearchResults = if (current.userSearchQuery.isBlank()) merged else current.userSearchResults
                            )
                        }
                    }
                }
        }
    }

    fun setSearchTab(tab: SearchTab) {
        _uiState.update { it.copy(searchTab = tab) }
    }

    fun onUserSearchQueryChanged(query: String) {
        _uiState.update { it.copy(userSearchQuery = query) }
        userSearchJob?.cancel()

        userSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isSearchingUsers = true) }
            delay(150)
            val currentFeedUsers = _uiState.value.feedUsers
            val allPool = (currentFeedUsers + MusicRepository.allDiscoverableUsers).distinctBy { it.id }
            val results = if (query.isBlank()) {
                allPool
            } else {
                val clean = query.trim().removePrefix("@").lowercase()
                allPool.filter {
                    it.username.lowercase().contains(clean) ||
                    it.name.lowercase().contains(clean) ||
                    it.stats.topArtist.lowercase().contains(clean) ||
                    it.stats.totalMinutesOrGenres.lowercase().contains(clean)
                }
            }
            _uiState.update { it.copy(userSearchResults = results, isSearchingUsers = false) }
        }
    }

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

        // Sincronizza l'aggiornamento su Firebase
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun toggleFollowUser(targetUser: User) {
        val currentFeed = _uiState.value.feedUsers
        val isAlreadyInFeed = currentFeed.any { it.id == targetUser.id }

        val newFeed = if (isAlreadyInFeed) {
            currentFeed.filterNot { it.id == targetUser.id }
        } else {
            listOf(targetUser) + currentFeed
        }

        _uiState.update {
            it.copy(
                feedUsers = newFeed,
                feedbackToast = if (isAlreadyInFeed) "Rimosso dal tuo feed: @${targetUser.username}" else "Aggiunto al tuo feed: @${targetUser.username}"
            )
        }
    }

    fun loginWithGoogle() {
        loginWithAccount(
            name = "Tony Banks",
            email = "tonybanks989@gmail.com",
            username = "tonybanks",
            avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80"
        )
    }

    fun loginWithAccount(
        name: String = "Tony Banks",
        email: String = "tonybanks989@gmail.com",
        username: String = "tonybanks",
        avatarUrl: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingIn = true) }
            delay(400)
            val cleanUsername = username.trim().removePrefix("@").ifBlank { "tonybanks" }
            val updatedUser = _uiState.value.currentUser.copy(
                name = name.ifBlank { "Tony Banks" },
                email = email.ifBlank { "tonybanks989@gmail.com" },
                username = cleanUsername,
                avatarUrl = avatarUrl.ifBlank { _uiState.value.currentUser.avatarUrl }
            )
            _uiState.update {
                it.copy(
                    currentUser = updatedUser,
                    isLoggedIn = true,
                    isLoggingIn = false,
                    feedbackToast = null
                )
            }
            // Sincronizza il profilo reale con Firestore
            FirebaseRepository.syncCurrentUser(updatedUser)
            startFirebaseListener()
        }
    }

    fun toggleConnectedService(serviceKey: String) {
        val currentServices = _uiState.value.connectedServices.toMutableMap()
        val currentState = currentServices[serviceKey] ?: false
        val newState = !currentState
        currentServices[serviceKey] = newState

        val serviceName = when (serviceKey) {
            "spotify" -> "Spotify"
            "apple_music" -> "Apple Music"
            "amazon_music" -> "Amazon Music"
            "youtube_music" -> "YouTube Music"
            else -> serviceKey
        }

        if (serviceKey == "spotify") {
            if (newState) {
                connectSpotify()
            } else {
                disconnectSpotify()
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

    fun connectSpotify() {
        viewModelScope.launch {
            delay(300)
            val updatedUser = _uiState.value.currentUser.copy(
                isLiveNow = true,
                currentTrack = _uiState.value.nowPlayingTrack
            )
            val updatedServices = _uiState.value.connectedServices.toMutableMap().apply {
                put("spotify", true)
            }
            _uiState.update {
                it.copy(
                    isSpotifyConnected = true,
                    connectedServices = updatedServices,
                    currentUser = updatedUser,
                    feedbackToast = "Account Spotify collegato: Now Playing attivo"
                )
            }
            FirebaseRepository.syncCurrentUser(updatedUser)
        }
    }

    fun disconnectSpotify() {
        val updatedUser = _uiState.value.currentUser.copy(
            isLiveNow = false,
            currentTrack = null
        )
        val updatedServices = _uiState.value.connectedServices.toMutableMap().apply {
            put("spotify", false)
        }
        _uiState.update {
            it.copy(
                isSpotifyConnected = false,
                connectedServices = updatedServices,
                currentUser = updatedUser,
                feedbackToast = "Account Spotify disconnesso"
            )
        }
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun logout() {
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                activeProfileUser = null,
                activeChatUser = null,
                activeStoryUserIndex = null
            )
        }
    }

    fun openShareSheet() {
        _uiState.update {
            it.copy(
                isShareSheetOpen = true,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
    }

    fun closeShareSheet() {
        _uiState.update { it.copy(isShareSheetOpen = false) }
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
            delay(250) // Debounce
            val results = MusicRepository.searchTracks(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun shareTrack(track: Track) {
        val updatedUser = _uiState.value.currentUser.copy(
            currentTrack = track,
            sharedTracks = listOf(track) + _uiState.value.currentUser.sharedTracks.filterNot { it.id == track.id },
            stats = _uiState.value.currentUser.stats.copy(
                sharedCount = _uiState.value.currentUser.stats.sharedCount + 1
            )
        )

        _uiState.update {
            it.copy(
                currentUser = updatedUser,
                nowPlayingTrack = track,
                isShareSheetOpen = false,
                feedbackToast = "Condiviso sul tuo feed: ${track.title}"
            )
        }

        // Singola operazione di scrittura atomica su Firebase
        FirebaseRepository.shareTrack(updatedUser.id, track)
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun simulateChangeNowPlaying() {
        val pool = MusicRepository.curatedTracks
        val current = _uiState.value.nowPlayingTrack
        val next = pool.filterNot { it.id == current.id }.random()
        val updatedUser = _uiState.value.currentUser.copy(
            currentTrack = next,
            isLiveNow = true
        )
        _uiState.update {
            it.copy(
                nowPlayingTrack = next,
                currentUser = updatedUser,
                feedbackToast = "Nuovo brano in riproduzione: ${next.title}"
            )
        }
        FirebaseRepository.syncCurrentUser(updatedUser)
    }

    fun simulateLiveTrackChange(userId: String? = null) {
        val pool = MusicRepository.curatedTracks
        val currentFeed = _uiState.value.feedUsers
        if (currentFeed.isEmpty()) return

        val target = if (userId != null) {
            currentFeed.firstOrNull { it.id == userId } ?: currentFeed.first()
        } else {
            currentFeed.filter { it.isLiveNow }.randomOrNull() ?: currentFeed.first()
        }

        val nextTrack = pool.filterNot { it.id == target.currentTrack?.id }.random()
        val updatedFeed = currentFeed.map { u ->
            if (u.id == target.id) {
                u.copy(currentTrack = nextTrack, isLiveNow = true)
            } else u
        }

        _uiState.update {
            it.copy(
                feedUsers = updatedFeed,
                feedbackToast = null
            )
        }
    }

    fun openStory(user: User) {
        val allStories = getStoriesList()
        val index = allStories.indexOfFirst { it.id == user.id }
        if (index != -1) {
            _uiState.update { it.copy(activeStoryUserIndex = index) }
        } else {
            val updatedFeed = if (_uiState.value.feedUsers.none { it.id == user.id } && !user.isCurrentUser) {
                listOf(user.copy(isLiveNow = true, currentTrack = user.currentTrack ?: MusicRepository.curatedTracks.random())) + _uiState.value.feedUsers
            } else {
                _uiState.value.feedUsers
            }
            val refreshedStories = if (user.isCurrentUser) {
                listOf(_uiState.value.currentUser.copy(isLiveNow = true, currentTrack = _uiState.value.currentUser.currentTrack ?: _uiState.value.nowPlayingTrack)) + updatedFeed
            } else {
                updatedFeed
            }
            val newIdx = refreshedStories.indexOfFirst { it.id == user.id }.coerceAtLeast(0)
            _uiState.update {
                it.copy(
                    feedUsers = updatedFeed,
                    activeStoryUserIndex = newIdx
                )
            }
        }
    }

    fun openStoryByIndex(index: Int) {
        val allStories = getStoriesList()
        if (index in allStories.indices) {
            _uiState.update { it.copy(activeStoryUserIndex = index) }
        }
    }

    fun nextStory() {
        val allStories = getStoriesList()
        val currentIndex = _uiState.value.activeStoryUserIndex ?: return
        if (currentIndex < allStories.size - 1) {
            _uiState.update { it.copy(activeStoryUserIndex = currentIndex + 1) }
        } else {
            closeStory()
        }
    }

    fun previousStory() {
        val currentIndex = _uiState.value.activeStoryUserIndex ?: return
        if (currentIndex > 0) {
            _uiState.update { it.copy(activeStoryUserIndex = currentIndex - 1) }
        }
    }

    fun closeStory() {
        _uiState.update { it.copy(activeStoryUserIndex = null) }
    }

    fun openProfile(user: User) {
        _uiState.update { it.copy(activeProfileUser = user) }
    }

    fun closeProfile() {
        _uiState.update { it.copy(activeProfileUser = null) }
    }

    fun openChat(user: User, initialTrack: Track? = null) {
        _uiState.update {
            it.copy(
                activeChatUser = user,
                activeStoryUserIndex = null
            )
        }

        if (initialTrack != null) {
            sendMessage(
                recipientId = user.id,
                text = "Ho visto che stavi ascoltando \"${initialTrack.title}\"!",
                attachedTrack = initialTrack
            )
        }
    }

    fun closeChat() {
        _uiState.update { it.copy(activeChatUser = null) }
    }

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

        val updatedMap = _uiState.value.chatMessages.toMutableMap().apply {
            put(recipientId, currentList)
        }

        _uiState.update { it.copy(chatMessages = updatedMap) }

        // Optional quick automatic ambient reply from interlocutor
        viewModelScope.launch {
            delay(1400)
            if (_uiState.value.activeChatUser?.id == recipientId) {
                val replies = listOf(
                    "Brano eccezionale, ha una produzione spaziale.",
                    "Sì! Lo tengo in loop da stamattina.",
                    "La linea di basso è clamorosa.",
                    "Ascoltalo a volume alto, merita davvero."
                )
                val reply = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    senderId = recipientId,
                    text = replies.random(),
                    timestamp = MusicRepository.getCurrentTimestamp(),
                    isFromMe = false
                )
                val refreshedList = _uiState.value.chatMessages[recipientId]?.toMutableList() ?: mutableListOf()
                refreshedList.add(reply)
                val newUpdatedMap = _uiState.value.chatMessages.toMutableMap().apply {
                    put(recipientId, refreshedList)
                }
                _uiState.update { it.copy(chatMessages = newUpdatedMap) }
            }
        }
    }

    fun inspectTrack(track: Track, user: User? = null) {
        _uiState.update { it.copy(selectedTrackDetail = Pair(track, user)) }
    }

    fun closeTrackInspector() {
        _uiState.update { it.copy(selectedTrackDetail = null) }
    }

    fun toggleDesignSpec(show: Boolean) {
        _uiState.update { it.copy(showDesignSpec = show) }
    }

    fun clearToast() {
        _uiState.update { it.copy(feedbackToast = null) }
    }

    fun getStoriesList(): List<User> {
        val state = _uiState.value
        val list = mutableListOf<User>()
        // Current user if has track
        if (state.currentUser.currentTrack != null) {
            list.add(state.currentUser)
        }
        list.addAll(state.feedUsers.filter { it.isLiveNow && it.currentTrack != null })
        return list
    }
}
