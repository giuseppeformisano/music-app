package com.example.data

import android.util.Log
import com.example.model.Track
import com.example.model.User
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Gestore ultra-ottimizzato per Firebase Firestore.
 * Progettato per ridurre al minimo assoluto le letture e le scritture (Zero Quota Waste):
 * 1. Cache persistente attiva per default (legge da disco senza consumare letture).
 * 2. Singola collezione 'users' con array 'sharedTracks' incorporato (1 lettura = profilo + stories live + brani condivisi).
 * 3. Scritture mirate 'merge' solo al reale cambio traccia o condivisione (mai a intervalli periodici).
 * 4. Query con limit(15) e ascolto deltas.
 */
object FirebaseRepository {

    private const val TAG = "FirebaseRepository"
    private const val USERS_COLLECTION = "users"

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(
                    PersistentCacheSettings.newBuilder()
                        .setSizeBytes(100L * 1024L * 1024L) // 100MB cache
                        .build()
                )
                .build()
            db.firestoreSettings = settings
            db
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase Firestore non inizializzato o Play Services assenti: ${e.message}")
            null
        }
    }

    /**
     * Sincronizza il profilo dell'utente loggato e lo stato di ascolto Live (1 sola scrittura per aggiornamento).
     */
    fun syncCurrentUser(user: User) {
        val db = firestore ?: return
        try {
            val userMap = hashMapOf<String, Any?>(
                "id" to user.id,
                "name" to user.name,
                "username" to user.username,
                "avatarUrl" to user.avatarUrl,
                "coverUrl" to user.coverUrl,
                "isLiveNow" to user.isLiveNow,
                "currentTrack" to user.currentTrack?.let { trackToMap(it) },
                "sharedCount" to user.stats.sharedCount,
                "topArtist" to user.stats.topArtist,
                "genres" to user.stats.totalMinutesOrGenres,
                "updatedAt" to System.currentTimeMillis()
            )

            db.collection(USERS_COLLECTION)
                .document(user.id)
                .set(userMap, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "Profilo/Stato utente aggiornato su Firebase.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Errore sync Firebase: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante syncCurrentUser: ${e.message}")
        }
    }

    /**
     * Condivide un brano nel feed dell'utente (1 singola scrittura con arrayUnion).
     */
    fun shareTrack(userId: String, track: Track) {
        val db = firestore ?: return
        try {
            val trackMap = trackToMap(track)
            db.collection(USERS_COLLECTION)
                .document(userId)
                .update(
                    "sharedTracks", FieldValue.arrayUnion(trackMap),
                    "updatedAt", System.currentTimeMillis()
                )
                .addOnFailureListener {
                    // Se il documento non esiste ancora, fa un merge
                    db.collection(USERS_COLLECTION)
                        .document(userId)
                        .set(
                            mapOf(
                                "sharedTracks" to listOf(trackMap),
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        )
                }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante shareTrack: ${e.message}")
        }
    }

    /**
     * Ascolta in tempo reale gli altri utenti con limit(15) e cache attiva.
     */
    fun observeOtherUsers(currentUserId: String): Flow<List<User>> = callbackFlow {
        val db = firestore
        if (db == null) {
            channel.close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection(USERS_COLLECTION)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(15)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val users = snapshot.documents.mapNotNull { doc ->
                            val id = doc.getString("id") ?: doc.id
                            if (id == currentUserId) null // Escludi se stesso
                            else mapDocToUser(doc.data, id)
                        }
                        trySend(users)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Errore osservatore Firebase: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    private fun trackToMap(track: Track): Map<String, Any?> {
        return mapOf(
            "id" to track.id,
            "title" to track.title,
            "artist" to track.artist,
            "album" to track.album,
            "coverUrl" to track.coverUrl,
            "durationText" to track.durationText,
            "accentColorHex" to track.accentColorHex,
            "genre" to track.genre,
            "releaseYear" to track.releaseYear
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDocToUser(data: Map<String, Any?>?, id: String): User? {
        if (data == null) return null
        val name = data["name"] as? String ?: "Utente"
        val username = data["username"] as? String ?: id
        val avatarUrl = data["avatarUrl"] as? String ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
        val coverUrl = data["coverUrl"] as? String
        val isLiveNow = data["isLiveNow"] as? Boolean ?: false

        val currentTrackMap = data["currentTrack"] as? Map<String, Any?>
        val currentTrack = currentTrackMap?.let { mapToTrack(it) }

        val sharedTracksList = (data["sharedTracks"] as? List<Map<String, Any?>>) ?: emptyList()
        val sharedTracks = sharedTracksList.mapNotNull { mapToTrack(it) }

        val sharedCount = (data["sharedCount"] as? Number)?.toInt() ?: sharedTracks.size
        val topArtist = data["topArtist"] as? String ?: "Artista"
        val genres = data["genres"] as? String ?: "Alternative"

        return User(
            id = id,
            name = name,
            username = username,
            avatarUrl = avatarUrl,
            coverUrl = coverUrl,
            isLiveNow = isLiveNow,
            currentTrack = currentTrack,
            sharedTracks = sharedTracks,
            stats = com.example.model.UserStats(
                sharedCount = sharedCount,
                topArtist = topArtist,
                totalMinutesOrGenres = genres
            ),
            isCurrentUser = false
        )
    }

    private fun mapToTrack(map: Map<String, Any?>): Track? {
        val title = map["title"] as? String ?: return null
        val artist = map["artist"] as? String ?: "Artista"
        val album = map["album"] as? String ?: "Singolo"
        val coverUrl = map["coverUrl"] as? String ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600"
        val durationText = map["durationText"] as? String ?: "3:30"
        val accentColorHex = (map["accentColorHex"] as? Number)?.toLong() ?: 0xFF1DB954
        val genre = map["genre"] as? String ?: "Musica"
        val releaseYear = map["releaseYear"] as? String ?: "2024"

        return Track(
            id = map["id"] as? String ?: (title + artist).hashCode().toString(),
            title = title,
            artist = artist,
            album = album,
            coverUrl = coverUrl,
            durationText = durationText,
            accentColorHex = accentColorHex,
            genre = genre,
            releaseYear = releaseYear
        )
    }
}
