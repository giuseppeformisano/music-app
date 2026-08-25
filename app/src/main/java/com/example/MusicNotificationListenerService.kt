package com.example

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.google.firebase.auth.FirebaseAuth
import com.example.data.FirebaseRepository

class MusicNotificationListenerService : NotificationListenerService() {

    private var lastTrack = ""
    private var lastSource = ""
    private val handler = Handler(Looper.getMainLooper())
    private val recheck = Runnable { checkMediaSessions() }
    // Heartbeat: mentre un brano è live (anche in PAUSA), rinfresca periodicamente lo stato
    // così updatedAt resta fresco su Firestore. Se il processo viene ucciso, l'heartbeat si
    // ferma → dopo il TTL gli altri smettono di vederti live (UC8 anche da kill di sistema).
    private val heartbeat = Runnable { checkMediaSessions() }
    private val heartbeatMs = 20_000L

    override fun onCreate() {
        super.onCreate()
        loadPreferences()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        checkMediaSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == SPOTIFY_PACKAGE || sbn.packageName == AMAZON_MUSIC_PACKAGE) {
            handler.removeCallbacks(recheck)
            checkMediaSessions()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName == SPOTIFY_PACKAGE || sbn.packageName == AMAZON_MUSIC_PACKAGE) {
            handler.removeCallbacks(recheck)
            handler.postDelayed(recheck, 900)
        }
    }

    // Forza la ri-emissione del brano corrente: azzera lastTrack così il prossimo check
    // lo tratta come "nuovo" e scatta onTrackChanged (popola la live locale alla riapertura).
    private fun forceResync() {
        lastTrack = ""
        lastSource = ""
        checkMediaSessions()
    }

    private fun loadPreferences() {
        try {
            val prefs = getSharedPreferences("connected_services", Context.MODE_PRIVATE)
            // Default FALSE: la live via notifiche parte SOLO se l'utente ha collegato
            // esplicitamente Spotify Free / Amazon Music. Senza collegamento nessun tracking.
            isSpotifyFreeEnabled = prefs.getBoolean("spotify_free", false)
            isAmazonMusicEnabled = prefs.getBoolean("amazon_music", false)
        } catch (_: Exception) {}
    }

    private fun stopPlayback(source: String = "") {
        if (source.isEmpty() || lastSource == source) {
            handler.removeCallbacks(heartbeat)
            lastTrack = ""
            lastSource = ""
            pendingTrack = null
            if (onPlaybackStopped != null) {
                onPlaybackStopped?.invoke(source)
            } else {
                // Sveglia in background: aggiorna direttamente a DB se l'app è chiusa
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    FirebaseRepository.clearLiveTrack(userId)
                }
            }
        }
    }

    fun checkMediaSessions() {
        try {
            loadPreferences()
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val controllers = manager.getActiveSessions(
                ComponentName(this, MusicNotificationListenerService::class.java)
            )

            // Notifica media presente? In PAUSA la notifica resta (app aperta); alla CHIUSURA
            // sparisce. È il discriminatore affidabile tra pausa e chiusura del player:
            // così una sessione che "persiste in pausa" dopo la chiusura NON tiene live.
            val notifs = try { activeNotifications } catch (_: Exception) { null }
            fun hasNotif(pkg: String): Boolean = notifs?.any { it.packageName == pkg } ?: true

            val spotifyController = if (isSpotifyFreeEnabled && hasNotif(SPOTIFY_PACKAGE))
                controllers.firstOrNull { it.packageName == SPOTIFY_PACKAGE } else null
            val amazonController = if (isAmazonMusicEnabled && hasNotif(AMAZON_MUSIC_PACKAGE))
                controllers.firstOrNull { it.packageName == AMAZON_MUSIC_PACKAGE } else null

            // Priorità all'app che sta effettivamente suonando (STATE_PLAYING o STATE_BUFFERING).
            // A parità di esecuzione sul dispositivo, Amazon Music (o sorgente nativa) ha precedenza.
            val activeController: Pair<MediaController, String>? = when {
                amazonController != null && isPlaying(amazonController) -> amazonController to "amazon_music"
                spotifyController != null && isPlaying(spotifyController) -> spotifyController to "spotify"
                amazonController != null && isPausedOrActive(amazonController) -> amazonController to "amazon_music"
                spotifyController != null && isPausedOrActive(spotifyController) -> spotifyController to "spotify"
                else -> null
            }

            if (activeController == null) {
                if (lastSource.isNotEmpty()) {
                    stopPlayback(lastSource)
                }
                return
            }

            val (controller, source) = activeController
            val state = controller.playbackState?.state

            if (state == PlaybackState.STATE_STOPPED || state == PlaybackState.STATE_NONE) {
                stopPlayback(source)
                return
            }

            val meta = controller.metadata ?: return

            if (source == "spotify" && isAdvertisement(meta)) return

            val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim() ?: return
            val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim() ?: ""
            val durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION).coerceAtLeast(0L)
            val positionMs = controller.playbackState?.position?.coerceAtLeast(0L) ?: 0L

            val artUrl = listOf(
                MediaMetadata.METADATA_KEY_ALBUM_ART_URI,
                MediaMetadata.METADATA_KEY_ART_URI,
                MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI
            ).firstNotNullOfOrNull { key ->
                meta.getString(key)?.takeIf { it.startsWith("http", ignoreCase = true) }
            } ?: ""

            // Brano live valido: riarma l'heartbeat (anche in pausa) per tenere fresco updatedAt.
            handler.removeCallbacks(heartbeat)
            handler.postDelayed(heartbeat, heartbeatMs)

            if (title == lastTrack && source == lastSource) {
                if (onProgressChanged != null) {
                    onProgressChanged?.invoke(positionMs, durationMs, source)
                } else {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    if (userId != null) {
                        FirebaseRepository.touchLive(userId, positionMs)
                    }
                }
                return
            }

            lastTrack = title
            lastSource = source
            pendingTrack = Pending(title, artist, durationMs, positionMs, artUrl, source)

            if (onTrackChanged != null) {
                onTrackChanged?.invoke(title, artist, durationMs, positionMs, artUrl, source)
            } else {
                // Sveglia in background: aggiorna direttamente a DB solo se la UI dell'app è chiusa
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    FirebaseRepository.updateLiveTrack(userId, title, artist, durationMs, positionMs, artUrl, source)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Verifica se sul dispositivo è EFFETTIVAMENTE IN ESECUZIONE (STATE_PLAYING/BUFFERING)
     * una riproduzione da Amazon Music / sorgente nativa non-Spotify.
     */
    fun hasActiveNonSpotifyPlayback(): Boolean {
        try {
            loadPreferences()
            if (!isAmazonMusicEnabled) return false
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return false
            val controllers = manager.getActiveSessions(
                ComponentName(this, MusicNotificationListenerService::class.java)
            ) ?: return false
            val notifs = try { activeNotifications } catch (_: Exception) { null }
            fun hasNotif(pkg: String): Boolean = notifs?.any { it.packageName == pkg } ?: true
            val amazonController = if (hasNotif(AMAZON_MUSIC_PACKAGE))
                controllers.firstOrNull { it.packageName == AMAZON_MUSIC_PACKAGE } else null

            return amazonController != null && isPlaying(amazonController)
        } catch (_: Exception) {
            return false
        }
    }

    private fun isPlaying(controller: MediaController): Boolean {
        val state = controller.playbackState?.state
        return state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
    }

    private fun isPausedOrActive(controller: MediaController): Boolean {
        val state = controller.playbackState?.state
        return state == PlaybackState.STATE_PAUSED || state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING || state == PlaybackState.STATE_CONNECTING
    }

    private fun isAdvertisement(meta: MediaMetadata): Boolean {
        try {
            if (meta.getLong("android.media.metadata.ADVERTISEMENT") == 1L) return true
        } catch (_: Exception) {}
        val title = meta.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        val artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)?.trim().orEmpty()
        val duration = meta.getLong(MediaMetadata.METADATA_KEY_DURATION)
        if (duration <= 0L && artist.isBlank() && (title.equals("Advertisement", ignoreCase = true) || title.equals("Spotify", ignoreCase = true))) {
            return true
        }
        return false
    }

    data class Pending(
        val title: String,
        val artist: String,
        val durationMs: Long,
        val positionMs: Long,
        val artUrl: String,
        val source: String
    )

    companion object {
        const val SPOTIFY_PACKAGE = "com.spotify.music"
        const val AMAZON_MUSIC_PACKAGE = "com.amazon.mp3"

        @Volatile private var instance: MusicNotificationListenerService? = null
        @Volatile var isSpotifyFreeEnabled: Boolean = true
        @Volatile var isAmazonMusicEnabled: Boolean = true

        var onTrackChanged: ((title: String, artist: String, durationMs: Long, positionMs: Long, artUrl: String, source: String) -> Unit)? = null
        var onProgressChanged: ((positionMs: Long, durationMs: Long, source: String) -> Unit)? = null
        var onPlaybackStopped: ((source: String) -> Unit)? = null
        var pendingTrack: Pending? = null
            private set

        fun isNonSpotifyDevicePlaybackActive(): Boolean {
            return instance?.hasActiveNonSpotifyPlayback() ?: false
        }

        fun isEnabled(context: Context): Boolean {
            val cn = ComponentName(context, MusicNotificationListenerService::class.java)
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat?.contains(cn.flattenToString()) == true
        }

        fun startListening(context: Context) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    NotificationListenerService.requestRebind(ComponentName(context, MusicNotificationListenerService::class.java))
                }
            } catch (_: Exception) {}
            instance?.checkMediaSessions()
        }

        fun stopListening() {
            try { instance?.requestUnbind() } catch (_: Exception) {}
        }

        fun resyncCurrentTrack() {
            try { instance?.forceResync() } catch (_: Exception) {}
        }
    }
}
