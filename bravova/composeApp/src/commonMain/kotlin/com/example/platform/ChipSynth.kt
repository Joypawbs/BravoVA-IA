package com.example.platform

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sintetizador de 8 bits para los huevos de pascua.
 *
 * Para qué: dar a cada huevo un sonido "de videojuego viejo" sin usar audio de nadie.
 * Cómo: cada sonido es una lista de notas escrita aquí mismo (melodías originales) y se
 * convierte en muestras de audio con matemáticas: onda cuadrada, triangular, de sierra o ruido.
 * Cada sistema solo recibe las muestras y las reproduce ([playChipPcm]).
 */
enum class Wave { SQUARE, PULSE, TRIANGLE, SAW, NOISE }

/** Una nota: [note] en notación inglesa ("C5", "F#4", "-" = silencio) o una frecuencia fija en [hz]. */
data class ChipNote(
    val note: String,
    val ms: Int,
    val wave: Wave = Wave.SQUARE,
    val volume: Float = 1f,
    /** Cuánto sube (positivo) o baja (negativo) la nota en semitonos mientras suena: barridos tipo "pew". */
    val slide: Float = 0f,
    val hz: Double? = null
)

object ChipSynth {
    const val SAMPLE_RATE = 22_050
    /** Volumen general bajo: es un detalle divertido, no debe asustar a nadie en el súper. */
    private const val MASTER = 0.28

    private val NOTE_INDEX = mapOf(
        "C" to 0, "C#" to 1, "DB" to 1, "D" to 2, "D#" to 3, "EB" to 3, "E" to 4, "F" to 5,
        "F#" to 6, "GB" to 6, "G" to 7, "G#" to 8, "AB" to 8, "A" to 9, "A#" to 10, "BB" to 10, "B" to 11
    )

    /** "A4" → 440 Hz. Devuelve null para silencios. */
    fun frequency(note: String): Double? {
        val n = note.trim().uppercase()
        if (n.isEmpty() || n == "-") return null
        val name = n.takeWhile { !it.isDigit() && it != '-' }
        val octave = n.drop(name.length).toIntOrNull() ?: 4
        val idx = NOTE_INDEX[name] ?: return null
        val midi = (octave + 1) * 12 + idx
        return 440.0 * 2.0.pow((midi - 69) / 12.0)
    }

    /** Convierte las notas en muestras de 16 bits, mono, a [SAMPLE_RATE]. */
    fun render(notes: List<ChipNote>): ShortArray {
        val total = notes.sumOf { it.ms } * SAMPLE_RATE / 1000
        val out = ShortArray(total + SAMPLE_RATE / 20) // un poco de cola en silencio
        var pos = 0
        val rnd = Random(7)
        var noiseValue = 0.0
        for (n in notes) {
            val count = n.ms * SAMPLE_RATE / 1000
            val base = n.hz ?: frequency(n.note)
            var phase = 0.0
            for (i in 0 until count) {
                if (base == null) { pos++; continue }
                val progress = i.toDouble() / count
                val freq = base * 2.0.pow(n.slide * progress / 12.0)
                phase += freq / SAMPLE_RATE
                val p = phase - kotlin.math.floor(phase)
                val raw = when (n.wave) {
                    Wave.SQUARE -> if (p < 0.5) 1.0 else -1.0
                    Wave.PULSE -> if (p < 0.25) 1.0 else -1.0
                    Wave.TRIANGLE -> if (p < 0.5) 4 * p - 1 else 3 - 4 * p
                    Wave.SAW -> 2 * p - 1
                    Wave.NOISE -> {
                        // Ruido que cambia según la "frecuencia": grave = retumbo, agudo = siseo
                        if (i % (SAMPLE_RATE / freq.coerceAtLeast(60.0)).toInt().coerceAtLeast(1) == 0) {
                            noiseValue = rnd.nextDouble(-1.0, 1.0)
                        }
                        noiseValue
                    }
                }
                // Envolvente: entra rápido (5 ms) y se apaga suave, para que no haga "clic"
                val attack = (i / (SAMPLE_RATE * 0.005)).coerceAtMost(1.0)
                val release = exp(-3.2 * progress)
                val v = raw * attack * release * n.volume * MASTER
                if (pos < out.size) {
                    out[pos] = (v * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
                }
                pos++
            }
        }
        return out
    }

    /** Toca una melodía. No hace nada si el sistema no puede reproducir audio. */
    fun play(notes: List<ChipNote>) {
        runCatching { playChipPcm(render(notes), SAMPLE_RATE) }
    }

    /** Atajo para escribir melodías: "C5 E5 G5 - C6" con la misma duración y onda. */
    fun seq(notes: String, ms: Int, wave: Wave = Wave.SQUARE, volume: Float = 1f): List<ChipNote> =
        notes.trim().split(Regex("\\s+")).map { ChipNote(it, ms, wave, volume) }
}

/** Reproduce muestras PCM de 16 bits mono. Android: AudioTrack. iPhone: AVAudioPlayer (respeta el modo silencio). */
expect fun playChipPcm(samples: ShortArray, sampleRate: Int)
