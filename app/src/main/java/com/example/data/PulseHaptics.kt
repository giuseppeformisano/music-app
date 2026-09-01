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
    const val SAMPLE_COUNT = 200          // 10000ms / 50ms
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

    /**
     * Decodifica l'inviluppo in ampiezze 0..255 per campione.
     * - Vecchio formato "on/off": stringa di '0'/'1' (lunghezza ≤ SAMPLE_COUNT) → 0 o 255.
     * - Nuovo formato "voce": 2 cifre hex per campione (ampiezza 0..255).
     */
    fun decodeEnvelope(samples: String?): IntArray {
        if (samples.isNullOrEmpty()) return IntArray(0)
        if (samples.length <= SAMPLE_COUNT && samples.all { it == '0' || it == '1' }) {
            return IntArray(samples.length) { if (samples[it] == '1') 255 else 0 }
        }
        val n = samples.length / 2
        return IntArray(n) { i ->
            samples.substring(i * 2, i * 2 + 2).toIntOrNull(16)?.coerceIn(0, 255) ?: 0
        }
    }

    fun encodeEnvelope(amps: IntArray): String =
        amps.joinToString("") { "%02x".format(it.coerceIn(0, 255)) }

    /** true se l'inviluppo ha contenuto reale (evita l'invio di un Pulse "vuoto"). */
    fun hasContent(samples: String?): Boolean = decodeEnvelope(samples).any { it > 24 }

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

    /**
     * Sotto THRESHOLD il campione diventa 0 (pausa netta).
     * Sopra viene rimappato a MIN_OUT..255: anche una voce normale vibra forte,
     * creando un contrasto netto tra silenzio e vibrazione.
     */
    private fun remapAmplitude(amp: Int, threshold: Int = 70, minOut: Int = 140): Int {
        if (amp < threshold) return 0
        return (minOut + (amp - threshold).toFloat() / (255 - threshold) * (255 - minOut))
            .toInt().coerceIn(0, 255)
    }

    fun play(context: Context, samples: String?) {
        val raw = decodeEnvelope(samples)
        if (raw.isEmpty() || raw.none { it > 24 }) return
        val amps = IntArray(raw.size) { remapAmplitude(raw[it]) }
        val vib = vibrator(context) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vib.hasAmplitudeControl()) {
                    val timings = LongArray(amps.size) { SAMPLE_MS }
                    vib.vibrate(VibrationEffect.createWaveform(timings, amps, -1))
                } else {
                    val binary = amps.joinToString("") { if (it > 0) "1" else "0" }
                    vib.vibrate(VibrationEffect.createWaveform(toTimings(binary), -1))
                }
            } else {
                val binary = amps.joinToString("") { if (it > 0) "1" else "0" }
                @Suppress("DEPRECATION")
                vib.vibrate(toTimings(binary), -1)
            }
        } catch (_: Exception) {}
    }

    fun cancel(context: Context) {
        try { vibrator(context)?.cancel() } catch (_: Exception) {}
    }
}
