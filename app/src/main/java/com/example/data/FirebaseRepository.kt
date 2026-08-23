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
import java.io.File
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
            // Converte SEMPRE i percorsi file:/// locali in Base64 prima di scrivere su Firestore.
            // In questo modo nessun altro dispositivo riceverà percorsi file:/// inesistenti nel proprio sandbox.
            val cleanAvatar = when {
                user.avatarUrl.startsWith("file:", ignoreCase = true) || user.avatarUrl.startsWith("/") -> {
                    ImageUtils.fileUriToBase64(user.avatarUrl)
                }
                user.avatarUrl.isNotBlank() -> user.avatarUrl
                else -> null
            }

            val cleanCover = when {
                !user.coverUrl.isNullOrBlank() && (user.coverUrl!!.startsWith("file:", ignoreCase = true) || user.coverUrl!!.startsWith("/")) -> {
                    ImageUtils.fileUriToBase64(user.coverUrl)
                }
                !user.coverUrl.isNullOrBlank() -> user.coverUrl
                else -> null
            }

            val userMap = hashMapOf<String, Any?>(
                "id" to user.id,
                "name" to user.name,
                "username" to user.username,
                "bio" to user.bio,
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

            if (cleanAvatar != null) {
                userMap["avatarUrl"] = cleanAvatar
            }
            if (cleanCover != null) {
                userMap["coverUrl"] = cleanCover
            }

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

    fun sendFollowRequest(from: User, to: User) {
        val db = firestore ?: return
        try {
            val requestId = "${from.id}_${to.id}"
            val fromAvatar = if (from.avatarUrl.startsWith("file:", ignoreCase = true)) {
                ImageUtils.fileUriToBase64(from.avatarUrl) ?: from.avatarUrl
            } else {
                from.avatarUrl
            }
            val requestData = mapOf(
                "id" to requestId,
                "fromUserId" to from.id,
                "fromUserName" to from.name,
                "fromUserUsername" to from.username,
                "fromUserAvatarUrl" to fromAvatar,
                "timestamp" to System.currentTimeMillis()
            )
            // 1 batch atomico: richiesta incorporata nel doc del destinatario (mappa
            // pendingRequests.<fromId>) + id nell'array sentRequestIds del mittente.
            val batch = db.batch()
            batch.update(
                db.collection(USERS_COLLECTION).document(to.id),
                "pendingRequests.${from.id}", requestData,
                "updatedAt", System.currentTimeMillis()
            )
            batch.update(
                db.collection(USERS_COLLECTION).document(from.id),
                "sentRequestIds", FieldValue.arrayUnion(to.id)
            )
            batch.commit()
                .addOnSuccessListener { Log.d(TAG, "Richiesta inviata con successo a ${to.name}") }
                .addOnFailureListener { e -> Log.w(TAG, "Errore invio richiesta: ${e.message}") }
        } catch (e: Exception) {
            Log.e(TAG, "sendFollowRequest error: ${e.message}")
        }
    }

    // Accetta: 1 batch atomico. Il follow finisce in follower/following; la richiesta
    // viene RIMOSSA (niente doc "ACCEPTED" che si accumulano) da entrambi i lati.
    fun acceptFollowRequest(currentUserId: String, fromUserId: String) {
        val db = firestore ?: return
        try {
            db.batch().apply {
                update(
                    db.collection(USERS_COLLECTION).document(currentUserId),
                    mapOf(
                        "followerIds" to FieldValue.arrayUnion(fromUserId),
                        "pendingRequests.$fromUserId" to FieldValue.delete()
                    )
                )
                update(
                    db.collection(USERS_COLLECTION).document(fromUserId),
                    mapOf(
                        "followingIds" to FieldValue.arrayUnion(currentUserId),
                        "sentRequestIds" to FieldValue.arrayRemove(currentUserId)
                    )
                )
            }.commit()
        } catch (e: Exception) {
            Log.e(TAG, "acceptFollowRequest error: ${e.message}")
        }
    }

    // Rifiuta: 1 batch atomico. Rimuove la richiesta da entrambi i lati.
    fun rejectFollowRequest(currentUserId: String, fromUserId: String) {
        val db = firestore ?: return
        try {
            db.batch().apply {
                update(
                    db.collection(USERS_COLLECTION).document(currentUserId),
                    mapOf("pendingRequests.$fromUserId" to FieldValue.delete())
                )
                update(
                    db.collection(USERS_COLLECTION).document(fromUserId),
                    mapOf("sentRequestIds" to FieldValue.arrayRemove(currentUserId))
                )
            }.commit()
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

    // observeCurrentUserSocial / observePendingSentRequests / observeFriendRequests
    // RIMOSSI: profilo, social, richieste ricevute e inviate ora arrivano tutti da un
    // UNICO listener observeCurrentUserDocument (vedi mapDocToUser).

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
            "releaseYear" to track.releaseYear,
            "source" to track.source
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun mapDocToUser(data: Map<String, Any?>?, id: String): User? {
        if (data == null) return null
        val name = data["name"] as? String ?: "Utente"
        val username = data["username"] as? String ?: id
        val rawAvatar = data["avatarUrl"] as? String
        val rawCover = data["coverUrl"] as? String

        val cleanAvatar = when {
            rawAvatar.isNullOrBlank() -> "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
            rawAvatar.startsWith("file:", ignoreCase = true) -> {
                val path = rawAvatar.removePrefix("file://").removePrefix("file:")
                if (File(path).exists()) rawAvatar
                else "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400"
            }
            else -> rawAvatar
        }

        val cleanCover = when {
            rawCover.isNullOrBlank() -> null
            rawCover.startsWith("file:", ignoreCase = true) -> {
                val path = rawCover.removePrefix("file://").removePrefix("file:")
                if (File(path).exists()) rawCover else null
            }
            else -> rawCover
        }

        val bio = data["bio"] as? String ?: ""
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

        // Richieste incorporate nel documento (niente collezione separata / listener extra)
        val pendingMap = data["pendingRequests"] as? Map<String, Map<String, Any?>>
        val pendingRequests = pendingMap?.values?.mapNotNull { req ->
            val fromId = req["fromUserId"] as? String ?: return@mapNotNull null
            com.example.model.FriendRequest(
                id = req["id"] as? String ?: fromId,
                fromUserId = fromId,
                fromUserName = req["fromUserName"] as? String ?: "Utente",
                fromUserUsername = req["fromUserUsername"] as? String ?: "",
                fromUserAvatarUrl = req["fromUserAvatarUrl"] as? String ?: "",
                timestamp = (req["timestamp"] as? Number)?.toLong() ?: 0L
            )
        }?.sortedByDescending { it.timestamp } ?: emptyList()
        val sentRequestIds = (data["sentRequestIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

        return User(
            id = id,
            name = name,
            username = username,
            avatarUrl = cleanAvatar,
            coverUrl = cleanCover,
            bio = bio,
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
            followingIds = followingIds,
            pendingRequests = pendingRequests,
            sentRequestIds = sentRequestIds
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
        val source = map["source"] as? String ?: "spotify"

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
            releaseYear = releaseYear,
            source = source
        )
    }
}
