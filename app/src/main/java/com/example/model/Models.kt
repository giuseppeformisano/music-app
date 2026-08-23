package com.example.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String,
    val durationText: String = "3:45",
    val durationMs: Long = 0L, // Durata reale in millisecondi (0 = sconosciuta)
    val accentColorHex: Long = 0xFF1DB954, // Hex color extracted from album art
    val genre: String = "Alternative / Electronic",
    val releaseYear: String = "2024",
    val source: String = "spotify"
)

data class User(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String,
    val coverUrl: String? = null,
    val bio: String = "",
    val email: String = "",
    val isCurrentUser: Boolean = false,
    val isOnline: Boolean = false,       // sta usando l'app (connesso), non necessariamente in ascolto
    val isLiveNow: Boolean = true,       // sta effettivamente ascoltando un brano in live
    val currentTrack: Track? = null,
    val trackProgressMs: Long = 0L,      // posizione di ascolto catturata (ms)
    val trackProgressAt: Long = 0L,      // wall-clock (ms) di quando è stata catturata la posizione
    val sharedTracks: List<Track> = emptyList(),
    val stats: UserStats = UserStats(),
    val followerIds: List<String> = emptyList(),
    val followingIds: List<String> = emptyList(),
    // Richieste di follow incorporate nel documento utente (niente collezione separata):
    // pendingRequests = ricevute (in attesa); sentRequestIds = inviate da me
    val pendingRequests: List<FriendRequest> = emptyList(),
    val sentRequestIds: List<String> = emptyList()
) {
    val isActuallyLive: Boolean
        get() {
            if (!isLiveNow || currentTrack == null) return false
            if (isCurrentUser) return true
            val now = System.currentTimeMillis()
            if (trackProgressAt > 0L) {
                val trackDuration = if (currentTrack.durationMs > 0L) currentTrack.durationMs else 240_000L
                val remainingMs = (trackDuration - trackProgressMs).coerceAtLeast(30_000L)
                if (now - trackProgressAt > remainingMs + 45_000L) {
                    return false
                }
            }
            return true
        }
}

data class UserStats(
    val sharedCount: Int = 142,
    val topArtist: String = "Daft Punk",
    val totalMinutesOrGenres: String = "Indie • Electronic"
)

enum class RequestStatus { PENDING, ACCEPTED, REJECTED }

data class FriendRequest(
    val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserUsername: String,
    val fromUserAvatarUrl: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: RequestStatus = RequestStatus.PENDING
)

data class ChatMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean,
    val attachedTrack: Track? = null
)
