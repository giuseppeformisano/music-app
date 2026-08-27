package com.example.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import kotlin.math.sqrt

/**
 * Registratore per il "Pulse vocale": cattura dal microfono e permette di leggere il livello
 * istantaneo della voce (per costruire l'inviluppo che pilota vibrazione + onda).
 * NB: il file audio in Fase 1 serve solo alla registrazione (viene scartato); in Fase 2 verrà
 * conservato per la riproduzione della voce.
 */
class PulseRecorder {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(context: Context): Boolean = try {
        val f = File(context.cacheDir, "pulse_rec_${System.currentTimeMillis()}.3gp")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        r.setOutputFile(f.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = f
        true
    } catch (_: Exception) {
        stop()
        false
    }

    /** Livello istantaneo 0..1 (percettivo, radice del picco dal microfono). */
    fun level(): Float = try {
        val a = recorder?.maxAmplitude ?: 0 // 0..32767, picco dall'ultima chiamata
        sqrt((a / 32767f).coerceIn(0f, 1f))
    } catch (_: Exception) {
        0f
    }

    fun stop() {
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        try { file?.delete() } catch (_: Exception) {}
        file = null
    }
}
