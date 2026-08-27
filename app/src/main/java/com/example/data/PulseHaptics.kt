package com.example.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * "Pulse" tattile: la registrazione è una stringa di [SAMPLE_COUNT] campioni '0'/'1'
 * (5 secondi campionati ogni [SAMPLE_MS] ms). '1' = dito premuto (vibra), '0' = pausa.
 * Da questa stringa si ricostruisce sia la vibrazione sia l'onda visiva → sempre sincronizzate.
 */
object PulseHaptics {
    const val SAMPLE_MS = 50L
    const val SAMPLE_COUNT = 100          // 5000ms / 50ms
    const val DURATION_MS = SAMPLE_MS * SAMPLE_COUNT

    private fun vibrator(context: Context): Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) { null }

    /** true se la stringa contiene almeno un tocco (evita l'invio di un Pulse "vuoto"). */
    fun hasContent(samples: String?): Boolean = samples?.any { it == '1' } == true

    /**
     * Converte i campioni in timings alternati per createWaveform:
     * l'indice 0 è sempre una PAUSA, poi si alterna vibrazione/pausa (run-length dei campioni).
     */
    fun toTimings(samples: String): LongArray {
        if (samples.isEmpty()) return longArrayOf(0L)
        val runs = ArrayList<Long>()
        var i = 0
        // Se inizia con vibrazione, anteponi una pausa di 0ms (l'indice 0 deve essere OFF).
        if (samples[0] == '1') runs.add(0L)
        while (i < samples.length) {
            val c = samples[i]
            var j = i
            while (j < samples.length && samples[j] == c) j++
            runs.add((j - i) * SAMPLE_MS)
            i = j
        }
        return runs.toLongArray()
    }

    fun play(context: Context, samples: String?) {
        if (!hasContent(samples)) return
        val vib = vibrator(context) ?: return
        val timings = toTimings(samples!!)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(timings, -1)
            }
        } catch (_: Exception) {}
    }

    fun cancel(context: Context) {
        try { vibrator(context)?.cancel() } catch (_: Exception) {}
    }
}
