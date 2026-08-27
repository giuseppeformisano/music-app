package com.example.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import java.io.File
import kotlin.math.sqrt

/**
 * Registratore per il "Pulse vocale": cattura la voce dal microfono (AAC/m4a mono a basso
 * bitrate), espone il livello istantaneo per costruire l'inviluppo (vibrazione + onda) e a fine
 * registrazione restituisce l'audio in Base64 (memorizzato su Firestore, per ora inline in un doc).
 */
class PulseRecorder {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(context: Context): Boolean = try {
        val f = File(context.cacheDir, "pulse_rec_${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioChannels(1)
        r.setAudioSamplingRate(22050)
        r.setAudioEncodingBitRate(24000)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = f
        true
    } catch (_: Exception) {
        cleanup()
        false
    }

    /** Livello istantaneo 0..1 (percettivo, radice del picco dal microfono). */
    fun level(): Float = try {
        val a = recorder?.maxAmplitude ?: 0 // 0..32767, picco dall'ultima chiamata
        sqrt((a / 32767f).coerceIn(0f, 1f))
    } catch (_: Exception) {
        0f
    }

    /** Ferma la registrazione e restituisce l'audio in Base64 (null se fallita/vuota). */
    fun stopAndGetBase64(): String? {
        val f = file
        var result: String? = null
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        try {
            if (f != null && f.exists() && f.length() > 0L) {
                result = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
            }
        } catch (_: Exception) {}
        try { f?.delete() } catch (_: Exception) {}
        file = null
        return result
    }

    fun cleanup() {
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        try { file?.delete() } catch (_: Exception) {}
        file = null
    }
}
