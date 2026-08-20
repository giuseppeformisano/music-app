package com.example.data

import com.example.model.Track
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
}
