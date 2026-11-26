package com.example.platform

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/** Android: AudioTrack estático con volumen de "sonidos del sistema" (respeta el modo silencio/vibración). */
actual fun playChipPcm(samples: ShortArray, sampleRate: Int) {
    Thread {
        runCatching {
            val bytes = samples.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            Thread.sleep(samples.size * 1000L / sampleRate + 60)
            track.stop()
            track.release()
        }
    }.start()
}
