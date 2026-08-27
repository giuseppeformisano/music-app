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
    val source: String = "spotify",
    val deviceType: String = "",   // tipo device Spotify API (Smartphone/Computer/Speaker/Automobile/TV...)
    val deviceName: String = ""    // nome device (es. "Audi A3", "iPhone di Marco")
)

enum class UserPresenceState(val value: String) {
    OFFLINE("OFFLINE"), // App chiusa, nessuna musica attiva
    ONLINE("ONLINE"),   // App aperta in primo piano, nessuna musica attiva
    LIVE("LIVE");       // Musica in riproduzione/pausa attiva (app aperta o chiusa)

    companion object {
        fun fromString(value: String?): UserPresenceState {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: OFFLINE
        }
    }
}

data class User(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String,
    val coverUrl: String? = null,
    val bio: String = "",
    val email: String = "",
    val isCurrentUser: Boolean = false,
    val presenceState: UserPresenceState = UserPresenceState.OFFLINE,
    val currentTrack: Track? = null,
    val trackProgressMs: Long = 0L,      // posizione di ascolto catturata (ms)
    val trackProgressAt: Long = 0L,      // wall-clock (ms) di quando è stata catturata la posizione
    val updatedAt: Long = 0L,            // ultimo aggiornamento del documento (per TTL di staleness)
    val sharedTracks: List<Track> = emptyList(),
    val stats: UserStats = UserStats(),
    val followerIds: List<String> = emptyList(),
    val followingIds: List<String> = emptyList(),
    // Richieste di follow incorporate nel documento utente (niente collezione separata):
    // pendingRequests = ricevute (in attesa); sentRequestIds = inviate da me
    val pendingRequests: List<FriendRequest> = emptyList(),
    val sentRequestIds: List<String> = emptyList(),
    val liveNotificationsEnabled: Boolean = true
) {
    val isOnline: Boolean
        get() = presenceState == UserPresenceState.ONLINE || presenceState == UserPresenceState.LIVE

    val isLiveNow: Boolean
        get() = presenceState == UserPresenceState.LIVE

    val isActuallyLive: Boolean
        get() = presenceState == UserPresenceState.LIVE && currentTrack != null
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
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val attachedTrack: Track? = null
) {
    /** Formatta il timestamp in "HH:mm" per la UI */
    val formattedTime: String
        get() {
            val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }
}

data class Conversation(
    val id: String,
    val recipientUser: User,
    val lastMessageText: String = "",
    val lastMessageAt: Long = 0L,
    val lastMessageSenderId: String = "",
    val lastAttachedTrack: Track? = null
)
