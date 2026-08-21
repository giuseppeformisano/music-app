package com.example.data

import android.util.Log
import com.example.model.FriendRequest
import com.example.model.RequestStatus
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
    /**
     * Heartbeat live: rinfresca solo updatedAt (e isLiveNow) senza riscrivere l'intero
     * profilo. Mantiene il documento in cima alla query ordinata per updatedAt anche
     * quando il brano non cambia, così i live non spariscono dalla lista altrui.
     */
    fun touchLive(userId: String, progressMs: Long = 0L, progressAt: Long = System.currentTimeMillis()) {
        val db = firestore ?: return
        db.collection(USERS_COLLECTION).document(userId)
            .update(
                mapOf(
                    "updatedAt" to System.currentTimeMillis(),
                    "isLiveNow" to true,
                    "trackProgressMs" to progressMs,
                    "trackProgressAt" to progressAt
                )
            )
            .addOnFailureListener { /* documento non ancora presente: ignora */ }
    }

    /** Aggiorna solo lo stato di presenza (app aperta/connessa). */
    fun setOnline(userId: String, online: Boolean) {
        val db = firestore ?: return
        db.collection(USERS_COLLECTION).document(userId)
            .update(mapOf("isOnline" to online, "updatedAt" to System.currentTimeMillis()))
            .addOnFailureListener { }
    }

    fun saveFcmToken(userId: String, token: String) {
        val db = firestore ?: return
        db.collection(USERS_COLLECTION).document(userId)
            .update("fcmToken", token)
            .addOnFailureListener {
                // Document might not exist yet — use set with merge
                db.collection(USERS_COLLECTION).document(userId)
                    .set(mapOf("fcmToken" to token), SetOptions.merge())
            }
    }

    fun syncCurrentUser(user: User) {
        val db = firestore ?: return
        try {
            val userMap = hashMapOf<String, Any?>(
                "id" to user.id,
                "name" to user.name,
                "username" to user.username,
                "avatarUrl" to user.avatarUrl,
                "coverUrl" to user.coverUrl,
                "isOnline" to user.isOnline,
                "isLiveNow" to user.isLiveNow,
                "currentTrack" to user.currentTrack?.let { trackToMap(it) },
                "trackProgressMs" to user.trackProgressMs,
                "trackProgressAt" to user.trackProgressAt,
                "sharedCount" to user.stats.sharedCount,
                "topArtist" to user.stats.topArtist,
                "genres" to user.stats.totalMinutesOrGenres,
                "updatedAt" to System.currentTimeMillis()
                // followerIds/followingIds sono gestiti solo da acceptFollowRequest con arrayUnion — non sovrascrivere qui
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
                    "sharedCount", FieldValue.increment(1),
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
     * Ascolta in tempo reale gli altri utenti con limit(40) e cache attiva.
     * limit alto per non far cadere dalla lista i live che smettono di aggiornare
     * updatedAt (es. Premium in background sullo stesso brano).
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
                .limit(40)
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

    private const val REQUESTS_COLLECTION = "friend_requests"

    fun sendFollowRequest(from: User, to: User) {
        val db = firestore ?: return
        try {
            val requestId = "${from.id}_${to.id}"
            val data = hashMapOf<String, Any>(
                "id" to requestId,
                "fromUserId" to from.id,
                "fromUserName" to from.name,
                "fromUserUsername" to from.username,
                "fromUserAvatarUrl" to from.avatarUrl,
                "toUserId" to to.id,
                "timestamp" to System.currentTimeMillis(),
                "status" to "PENDING"
            )
            db.collection(REQUESTS_COLLECTION).document(requestId).set(data)
        } catch (e: Exception) {
            Log.e(TAG, "sendFollowRequest error: ${e.message}")
        }
    }

    fun acceptFollowRequest(requestId: String, currentUserId: String, fromUserId: String) {
        val db = firestore ?: return
        try {
            db.collection(REQUESTS_COLLECTION).document(requestId)
                .update("status", "ACCEPTED")
            db.collection(USERS_COLLECTION).document(currentUserId)
                .update("followerIds", com.google.firebase.firestore.FieldValue.arrayUnion(fromUserId))
            db.collection(USERS_COLLECTION).document(fromUserId)
                .update("followingIds", com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId))
        } catch (e: Exception) {
            Log.e(TAG, "acceptFollowRequest error: ${e.message}")
        }
    }

    fun rejectFollowRequest(requestId: String) {
        val db = firestore ?: return
        try {
            db.collection(REQUESTS_COLLECTION).document(requestId).delete()
        } catch (e: Exception) {
            Log.e(TAG, "rejectFollowRequest error: ${e.message}")
        }
    }

    fun getUsersByIds(ids: List<String>, onResult: (List<User>) -> Unit) {
        val db = firestore ?: run { onResult(emptyList()); return }
        if (ids.isEmpty()) { onResult(emptyList()); return }
        // Usa get() per document ID — più affidabile di whereIn("id",...) che richiede il campo "id" nel documento
        val allUsers = java.util.Collections.synchronizedList(mutableListOf<User>())
        val pending = java.util.concurrent.atomic.AtomicInteger(ids.size)
        for (id in ids) {
            db.collection(USERS_COLLECTION).document(id).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) mapDocToUser(doc.data, doc.id)?.let { allUsers.add(it) }
                    if (pending.decrementAndGet() == 0) onResult(allUsers)
                }
                .addOnFailureListener {
                    Log.w(TAG, "getUsersByIds: failed to fetch $id")
                    if (pending.decrementAndGet() == 0) onResult(allUsers)
                }
        }
    }

    fun removeFollowing(currentUserId: String, targetUserId: String) {
        val db = firestore ?: return
        try {
            db.collection(USERS_COLLECTION).document(currentUserId)
                .update("followingIds", FieldValue.arrayRemove(targetUserId))
            db.collection(USERS_COLLECTION).document(targetUserId)
                .update("followerIds", FieldValue.arrayRemove(currentUserId))
        } catch (e: Exception) {
            Log.e(TAG, "removeFollowing error: ${e.message}")
        }
    }

    fun removeFollower(currentUserId: String, followerId: String) {
        val db = firestore ?: return
        try {
            db.collection(USERS_COLLECTION).document(currentUserId)
                .update("followerIds", FieldValue.arrayRemove(followerId))
            db.collection(USERS_COLLECTION).document(followerId)
                .update("followingIds", FieldValue.arrayRemove(currentUserId))
        } catch (e: Exception) {
            Log.e(TAG, "removeFollower error: ${e.message}")
        }
    }

    /**
     * Ascolta il documento completo dell'utente corrente — necessario per ripristinare
     * sharedTracks, stats e info profilo al riavvio dell'app senza perdere dati.
     */
    fun observeCurrentUserDocument(userId: String): Flow<User> = callbackFlow {
        val db = firestore
        if (db == null) { channel.close(); return@callbackFlow }
        var reg: ListenerRegistration? = null
        try {
            reg = db.collection(USERS_COLLECTION).document(userId)
                .addSnapshotListener { snap, err ->
                    if (err != null) { Log.w(TAG, "observeCurrentUserDocument error: ${err.message}"); return@addSnapshotListener }
                    val data = snap?.data ?: return@addSnapshotListener
                    mapDocToUser(data, userId)?.let { trySend(it) }
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeCurrentUserDocument exception: ${e.message}")
        }
        awaitClose { reg?.remove() }
    }

    fun observeCurrentUserSocial(userId: String): Flow<Pair<List<String>, List<String>>> = callbackFlow {
        val db = firestore
        if (db == null) { channel.close(); return@callbackFlow }
        var reg: ListenerRegistration? = null
        try {
            reg = db.collection(USERS_COLLECTION).document(userId)
                .addSnapshotListener { snap, err ->
                    if (err != null) return@addSnapshotListener
                    val data = snap?.data ?: return@addSnapshotListener
                    val followerIds = (data["followerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val followingIds = (data["followingIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    trySend(Pair(followerIds, followingIds))
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeCurrentUserSocial error: ${e.message}")
        }
        awaitClose { reg?.remove() }
    }

    fun observePendingSentRequests(fromUserId: String): Flow<Set<String>> = callbackFlow {
        val db = firestore
        if (db == null) { channel.close(); return@callbackFlow }
        var reg: ListenerRegistration? = null
        try {
            // Query su singolo campo per evitare l'indice composito Firestore
            reg = db.collection(REQUESTS_COLLECTION)
                .whereEqualTo("fromUserId", fromUserId)
                .addSnapshotListener { snap, err ->
                    if (err != null) { Log.e(TAG, "observePendingSentRequests error: ${err.message}"); return@addSnapshotListener }
                    val ids = snap?.documents
                        ?.filter { (it.data?.get("status") as? String) == "PENDING" }
                        ?.mapNotNull { it.data?.get("toUserId") as? String }
                        ?.toSet() ?: emptySet()
                    trySend(ids)
                }
        } catch (e: Exception) {
            Log.e(TAG, "observePendingSentRequests error: ${e.message}")
        }
        awaitClose { reg?.remove() }
    }

    fun observeFriendRequests(currentUserId: String): Flow<List<FriendRequest>> = callbackFlow {
        val db = firestore
        if (db == null) { channel.close(); return@callbackFlow }
        var reg: ListenerRegistration? = null
        try {
            // Query su singolo campo per evitare l'indice composito Firestore
            reg = db.collection(REQUESTS_COLLECTION)
                .whereEqualTo("toUserId", currentUserId)
                .addSnapshotListener { snap, err ->
                    if (err != null) { Log.e(TAG, "observeFriendRequests error: ${err.message}"); return@addSnapshotListener }
                    val requests = snap?.documents?.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        if ((data["status"] as? String) != "PENDING") return@mapNotNull null
                        FriendRequest(
                            id = data["id"] as? String ?: doc.id,
                            fromUserId = data["fromUserId"] as? String ?: return@mapNotNull null,
                            fromUserName = data["fromUserName"] as? String ?: "Utente",
                            fromUserUsername = data["fromUserUsername"] as? String ?: "",
                            fromUserAvatarUrl = data["fromUserAvatarUrl"] as? String ?: "",
                            timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        )
                    } ?: emptyList()
                    trySend(requests)
                }
        } catch (e: Exception) {
            Log.e(TAG, "observeFriendRequests error: ${e.message}")
        }
        awaitClose { reg?.remove() }
    }

    private fun trackToMap(track: Track): Map<String, Any?> {
        return mapOf(
            "id" to track.id,
            "title" to track.title,
            "artist" to track.artist,
            "album" to track.album,
            "coverUrl" to track.coverUrl,
            "durationText" to track.durationText,
            "durationMs" to track.durationMs,
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
        val isOnline = data["isOnline"] as? Boolean ?: false
        val isLiveNow = data["isLiveNow"] as? Boolean ?: false
        val trackProgressMs = (data["trackProgressMs"] as? Number)?.toLong() ?: 0L
        val trackProgressAt = (data["trackProgressAt"] as? Number)?.toLong() ?: 0L

        val currentTrackMap = data["currentTrack"] as? Map<String, Any?>
        val currentTrack = currentTrackMap?.let { mapToTrack(it) }

        val sharedTracksList = (data["sharedTracks"] as? List<Map<String, Any?>>) ?: emptyList()
        val sharedTracks = sharedTracksList.mapNotNull { mapToTrack(it) }

        val sharedCount = (data["sharedCount"] as? Number)?.toInt() ?: sharedTracks.size
        val topArtist = data["topArtist"] as? String ?: "Artista"
        val genres = data["genres"] as? String ?: "Alternative"

        val followerIds = (data["followerIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val followingIds = (data["followingIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        return User(
            id = id,
            name = name,
            username = username,
            avatarUrl = avatarUrl,
            coverUrl = coverUrl,
            isOnline = isOnline,
            isLiveNow = isLiveNow,
            currentTrack = currentTrack,
            trackProgressMs = trackProgressMs,
            trackProgressAt = trackProgressAt,
            sharedTracks = sharedTracks,
            stats = com.example.model.UserStats(
                sharedCount = sharedCount,
                topArtist = topArtist,
                totalMinutesOrGenres = genres
            ),
            isCurrentUser = false,
            followerIds = followerIds,
            followingIds = followingIds
        )
    }

    private fun mapToTrack(map: Map<String, Any?>): Track? {
        val title = map["title"] as? String ?: return null
        val artist = map["artist"] as? String ?: "Artista"
        val album = map["album"] as? String ?: "Singolo"
        val coverUrl = map["coverUrl"] as? String ?: "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600"
        val durationText = map["durationText"] as? String ?: "3:30"
        val durationMs = (map["durationMs"] as? Number)?.toLong() ?: 0L
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
            durationMs = durationMs,
            accentColorHex = accentColorHex,
            genre = genre,
            releaseYear = releaseYear
        )
    }
}
