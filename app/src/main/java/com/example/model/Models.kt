package com.example.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val coverUrl: String,
    val durationText: String = "3:45",
    val accentColorHex: Long = 0xFF1DB954, // Hex color extracted from album art
    val genre: String = "Alternative / Electronic",
    val releaseYear: String = "2024"
)

data class User(
    val id: String,
    val name: String,
    val username: String,
    val avatarUrl: String,
    val coverUrl: String? = null,
    val email: String = "",
    val isCurrentUser: Boolean = false,
    val isLiveNow: Boolean = true,
    val currentTrack: Track? = null,
    val sharedTracks: List<Track> = emptyList(),
    val stats: UserStats = UserStats(),
    val followerIds: List<String> = emptyList(),
    val followingIds: List<String> = emptyList()
)

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
