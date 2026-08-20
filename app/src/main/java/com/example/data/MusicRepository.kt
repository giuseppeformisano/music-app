package com.example.data

import com.example.model.ChatMessage
import com.example.model.Track
import com.example.model.User
import com.example.model.UserStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object MusicRepository {

    private val apiService by lazy { ITunesApiService.create() }

    // Curated high quality tracks with dynamic accent colors
    val curatedTracks = listOf(
        Track(
            id = "trk_1",
            title = "Starboy",
            artist = "The Weeknd, Daft Punk",
            album = "Starboy",
            coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
            durationText = "3:50",
            accentColorHex = 0xFFFF0055,
            genre = "R&B / Synthwave",
            releaseYear = "2016"
        ),
        Track(
            id = "trk_2",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop&q=80",
            durationText = "4:03",
            accentColorHex = 0xFF7928CA,
            genre = "Dream Pop / Synthpop",
            releaseYear = "2011"
        ),
        Track(
            id = "trk_3",
            title = "Nikes",
            artist = "Frank Ocean",
            album = "Blonde",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
            durationText = "5:14",
            accentColorHex = 0xFF00DF89,
            genre = "R&B / Experimental",
            releaseYear = "2016"
        ),
        Track(
            id = "trk_4",
            title = "Instant Crush",
            artist = "Daft Punk ft. Julian Casablancas",
            album = "Random Access Memories",
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
            durationText = "5:37",
            accentColorHex = 0xFFFF9900,
            genre = "Nu-Disco / Electronic",
            releaseYear = "2013"
        ),
        Track(
            id = "trk_5",
            title = "Veridis Quo",
            artist = "Daft Punk",
            album = "Discovery",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
            durationText = "5:44",
            accentColorHex = 0xFF0070F3,
            genre = "French House",
            releaseYear = "2001"
        ),
        Track(
            id = "trk_6",
            title = "After Dark",
            artist = "Mr.Kitty",
            album = "Time",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
            durationText = "4:17",
            accentColorHex = 0xFFE000FF,
            genre = "Darkwave / Synthpop",
            releaseYear = "2014"
        ),
        Track(
            id = "trk_7",
            title = "Genesis",
            artist = "Justice",
            album = "Cross",
            coverUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=600&auto=format&fit=crop&q=80",
            durationText = "3:54",
            accentColorHex = 0xFFFFBE0B,
            genre = "Electro House",
            releaseYear = "2007"
        ),
        Track(
            id = "trk_8",
            title = "Borderline",
            artist = "Tame Impala",
            album = "The Slow Rush",
            coverUrl = "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?w=600&auto=format&fit=crop&q=80",
            durationText = "3:57",
            accentColorHex = 0xFFFF0080,
            genre = "Psychedelic Pop",
            releaseYear = "2020"
        ),
        Track(
            id = "trk_9",
            title = "Intro",
            artist = "The xx",
            album = "xx",
            coverUrl = "https://images.unsplash.com/photo-1447752875215-b2761acb3c5d?w=600&auto=format&fit=crop&q=80",
            durationText = "2:08",
            accentColorHex = 0xFFFFFFFF,
            genre = "Indie Pop / Minimal",
            releaseYear = "2009"
        ),
        Track(
            id = "trk_10",
            title = "resonance",
            artist = "HOME",
            album = "Odyssey",
            coverUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&auto=format&fit=crop&q=80",
            durationText = "3:32",
            accentColorHex = 0xFF00E5FF,
            genre = "Chillwave / Synthwave",
            releaseYear = "2014"
        )
    )

    // Current logged-in user
    val currentUser = User(
        id = "me",
        name = "Tony Banks",
        username = "tonybanks",
        email = "tonybanks989@gmail.com",
        avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=300&auto=format&fit=crop&q=80",
        isCurrentUser = true,
        isLiveNow = true,
        currentTrack = curatedTracks[3], // Daft Punk - Instant Crush
        sharedTracks = listOf(
            curatedTracks[3],
            curatedTracks[0],
            curatedTracks[7],
            curatedTracks[1]
        ),
        stats = UserStats(
            sharedCount = 38,
            topArtist = "Daft Punk",
            totalMinutesOrGenres = "Electro • Indie"
        )
    )

    // Feed users
    val initialFeedUsers = listOf(
        User(
            id = "usr_sofia",
            name = "Sofia Bianchi",
            username = "sofiabianchi",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=300&auto=format&fit=crop&q=80",
            isLiveNow = true,
            currentTrack = curatedTracks[1], // M83 - Midnight City
            sharedTracks = listOf(
                curatedTracks[1],
                curatedTracks[5],
                curatedTracks[7],
                curatedTracks[9]
            ),
            stats = UserStats(
                sharedCount = 74,
                topArtist = "M83",
                totalMinutesOrGenres = "Dream Pop • 4.2k min"
            )
        ),
        User(
            id = "usr_luca",
            name = "Luca Moretti",
            username = "lucamoretti",
            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80",
            isLiveNow = true,
            currentTrack = curatedTracks[2], // Frank Ocean - Nikes
            sharedTracks = listOf(
                curatedTracks[2],
                curatedTracks[0],
                curatedTracks[3],
                curatedTracks[8]
            ),
            stats = UserStats(
                sharedCount = 112,
                topArtist = "Frank Ocean",
                totalMinutesOrGenres = "R&B • 6.8k min"
            )
        ),
        User(
            id = "usr_elena",
            name = "Elena Ricci",
            username = "elenaricci",
            avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=300&auto=format&fit=crop&q=80",
            isLiveNow = true,
            currentTrack = curatedTracks[6], // Justice - Genesis
            sharedTracks = listOf(
                curatedTracks[6],
                curatedTracks[4],
                curatedTracks[1],
                curatedTracks[2]
            ),
            stats = UserStats(
                sharedCount = 59,
                topArtist = "Justice",
                totalMinutesOrGenres = "French Touch • 3.5k min"
            )
        ),
        User(
            id = "usr_marco",
            name = "Marco De Angelis",
            username = "marcodeangelis",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=300&auto=format&fit=crop&q=80",
            isLiveNow = true,
            currentTrack = curatedTracks[9], // HOME - resonance
            sharedTracks = listOf(
                curatedTracks[9],
                curatedTracks[8],
                curatedTracks[5],
                curatedTracks[3],
                curatedTracks[0]
            ),
            stats = UserStats(
                sharedCount = 168,
                topArtist = "HOME",
                totalMinutesOrGenres = "Chillwave • 8.1k min"
            )
        ),
        User(
            id = "usr_giulia",
            name = "Giulia Conti",
            username = "giuliaconti",
            avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=300&auto=format&fit=crop&q=80",
            isLiveNow = true,
            currentTrack = curatedTracks[7], // Tame Impala - Borderline
            sharedTracks = listOf(
                curatedTracks[7],
                curatedTracks[2],
                curatedTracks[0],
                curatedTracks[4]
            ),
            stats = UserStats(
                sharedCount = 89,
                topArtist = "Tame Impala",
                totalMinutesOrGenres = "Psychedelic • 5.3k min"
            )
        )
    )

    // Initial chats
    val defaultChats = mutableMapOf<String, MutableList<ChatMessage>>(
        "usr_sofia" to mutableListOf(
            ChatMessage("m1", "usr_sofia", "Stai ascoltando Instant Crush? Quel solo finale mi manda sempre fuori di testa.", "10:14", false),
            ChatMessage("m2", "me", "Assolutamente, la parte con Casablancas è pura magia. Tu invece su M83 non sbagli mai!", "10:16", true),
            ChatMessage("m3", "usr_sofia", "Ascoltalo con le cuffie buone, l'outro con il sax è pazzesco.", "10:18", false, attachedTrack = curatedTracks[1])
        ),
        "usr_luca" to mutableListOf(
            ChatMessage("m4", "usr_luca", "Nikes è il miglior opening track dell'ultimo decennio, non accetto repliche.", "Ieri", false, attachedTrack = curatedTracks[2]),
            ChatMessage("m5", "me", "Blonde è un capolavoro senza tempo. Quella cassa asciutta è perfezione.", "Ieri", true)
        )
    )

    suspend fun searchTracks(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val response = apiService.searchSongs(term = query, limit = 15)
            val networkResults = response.results?.mapNotNull { item ->
                val trackName = item.trackName ?: return@mapNotNull null
                val artistName = item.artistName ?: "Unknown Artist"
                val artwork = item.artworkUrl100?.replace("100x100bb.jpg", "600x600bb.jpg")
                    ?: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80"
                
                Track(
                    id = item.trackId?.toString() ?: UUID.randomUUID().toString(),
                    title = trackName,
                    artist = artistName,
                    album = item.collectionName ?: "Single",
                    coverUrl = artwork,
                    durationText = "3:30",
                    accentColorHex = getDeterministicAccent(trackName + artistName),
                    genre = item.primaryGenreName ?: "Music",
                    releaseYear = item.releaseDate?.take(4) ?: "2024"
                )
            } ?: emptyList()

            if (networkResults.isNotEmpty()) {
                return@withContext networkResults
            }
        } catch (_: Exception) {
            // Fallback to local catalog search
        }

        // Local search fallback
        curatedTracks.filter {
            it.title.contains(query, ignoreCase = true) ||
            it.artist.contains(query, ignoreCase = true) ||
            it.album.contains(query, ignoreCase = true) ||
            it.genre.contains(query, ignoreCase = true)
        }
    }

    private fun getDeterministicAccent(key: String): Long {
        val accents = listOf(
            0xFF1DB954, // Spotify Green
            0xFFFF0055, // Electric Pink
            0xFF7928CA, // Deep Violet
            0xFF0070F3, // Clean Cyan Blue
            0xFFFF9900, // Vibrant Amber
            0xFF00DF89, // Neon Emerald
            0xFFE000FF, // Magenta Purple
            0xFFFFBE0B  // Gold Sun
        )
        val hash = kotlin.math.abs(key.hashCode())
        return accents[hash % accents.size]
    }

    fun getCurrentTimestamp(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    val presetAvatars = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400&auto=format&fit=crop&q=80",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&auto=format&fit=crop&q=80"
    )

    val extraDiscoverableUsers = listOf(
        User(
            id = "usr_alex",
            name = "Alessandro Riva",
            username = "alex_sound",
            avatarUrl = "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?w=400&auto=format&fit=crop&q=80",
            isLiveNow = true,
            currentTrack = curatedTracks[4], // Fred again.. - Danielle
            sharedTracks = listOf(curatedTracks[4], curatedTracks[6], curatedTracks[1]),
            stats = UserStats(sharedCount = 95, topArtist = "Fred again..", totalMinutesOrGenres = "Club • Electronic")
        ),
        User(
            id = "usr_chloe",
            name = "Chloé Dubois",
            username = "chloevibe",
            avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=400&auto=format&fit=crop&q=80",
            isLiveNow = true,
            currentTrack = curatedTracks[8], // Peggy Gou - Nanana
            sharedTracks = listOf(curatedTracks[8], curatedTracks[0], curatedTracks[3]),
            stats = UserStats(sharedCount = 130, topArtist = "Peggy Gou", totalMinutesOrGenres = "House • Disco")
        ),
        User(
            id = "usr_davide",
            name = "Davide Sanna",
            username = "davide_synth",
            avatarUrl = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?w=400&auto=format&fit=crop&q=80",
            isLiveNow = false,
            currentTrack = null,
            sharedTracks = listOf(curatedTracks[9], curatedTracks[5], curatedTracks[2]),
            stats = UserStats(sharedCount = 64, topArtist = "Kavinsky", totalMinutesOrGenres = "Synthwave • Retro")
        ),
        User(
            id = "usr_valeria",
            name = "Valeria Rossi",
            username = "valeriawaves",
            avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=400&auto=format&fit=crop&q=80",
            isLiveNow = true,
            currentTrack = curatedTracks[5], // Arctic Monkeys - 505
            sharedTracks = listOf(curatedTracks[5], curatedTracks[2], curatedTracks[7]),
            stats = UserStats(sharedCount = 82, topArtist = "Arctic Monkeys", totalMinutesOrGenres = "Indie Rock")
        )
    )

    val allDiscoverableUsers: List<User>
        get() = (initialFeedUsers + extraDiscoverableUsers).distinctBy { it.id }

    fun searchUsers(query: String): List<User> {
        val cleanQuery = query.trim().removePrefix("@").lowercase()
        if (cleanQuery.isEmpty()) return allDiscoverableUsers
        return allDiscoverableUsers.filter {
            it.username.lowercase().contains(cleanQuery) ||
            it.name.lowercase().contains(cleanQuery) ||
            it.stats.topArtist.lowercase().contains(cleanQuery) ||
            it.stats.totalMinutesOrGenres.lowercase().contains(cleanQuery)
        }
    }
}
