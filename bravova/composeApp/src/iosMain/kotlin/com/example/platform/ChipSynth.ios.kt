package com.example.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.AVFAudio.AVAudioPlayer
import platform.Foundation.NSData
import platform.Foundation.create

/** Se guarda para que el reproductor no se libere antes de terminar de sonar. */
private var currentPlayer: AVAudioPlayer? = null

/**
 * iPhone: arma un archivo WAV en memoria y lo toca con AVAudioPlayer.
 * No se cambia la categoría de audio: la predeterminada respeta el interruptor de silencio.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun playChipPcm(samples: ShortArray, sampleRate: Int) {
    val wav = wavBytes(samples, sampleRate)
    val data = wav.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = wav.size.toULong())
    }
    val player = AVAudioPlayer(data = data, error = null)
    player.volume = 1f
    player.prepareToPlay()
    player.play()
    currentPlayer = player
}

/** Cabecera WAV estándar (PCM 16 bits, mono) + las muestras. */
private fun wavBytes(samples: ShortArray, rate: Int): ByteArray {
    val dataLen = samples.size * 2
    val out = ByteArray(44 + dataLen)
    fun str(at: Int, s: String) = s.forEachIndexed { i, c -> out[at + i] = c.code.toByte() }
    fun int32(at: Int, v: Int) { for (i in 0 until 4) out[at + i] = (v shr (8 * i)).toByte() }
    fun int16(at: Int, v: Int) { out[at] = v.toByte(); out[at + 1] = (v shr 8).toByte() }
    str(0, "RIFF"); int32(4, 36 + dataLen); str(8, "WAVE")
    str(12, "fmt "); int32(16, 16); int16(20, 1); int16(22, 1)
    int32(24, rate); int32(28, rate * 2); int16(32, 2); int16(34, 16)
    str(36, "data"); int32(40, dataLen)
    samples.forEachIndexed { i, s -> int16(44 + i * 2, s.toInt()) }
    return out
}
