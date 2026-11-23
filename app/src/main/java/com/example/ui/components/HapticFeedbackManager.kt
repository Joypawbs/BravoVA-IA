package com.example.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class HapticStyle {
    TICK,
    CONFIRM,
    HEAVY,
    ADD_TO_CART,
    NAVIGATION,
    ASSISTANT
}

object HapticFeedbackManager {

    private val soundExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "BravoDynamicAudio").apply { isDaemon = true }
    }

    fun perform(view: View, style: HapticStyle = HapticStyle.TICK) {
        when (style) {
            HapticStyle.TICK -> onSelectionTick(view)
            HapticStyle.NAVIGATION -> onNavigationClick(view)
            HapticStyle.CONFIRM -> onConfirmAction(view)
            HapticStyle.ADD_TO_CART -> onAddToCart(view)
            HapticStyle.ASSISTANT -> onAssistantTrigger(view)
            HapticStyle.HEAVY -> onDecrementOrRemove(view, isFullRemove = true)
        }
    }

    fun onAddToCart(view: View) {
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            view.performHapticFeedback(constant)
            vibratePattern(
                context = view.context,
                timings = longArrayOf(0L, 12L, 28L, 20L),
                amplitudes = intArrayOf(0, 145, 0, 225),
                fallbackMs = 20L
            )
            playDynamicSynthChime(
                frequenciesHz = doubleArrayOf(587.33, 880.0),
                noteDurationsMs = intArrayOf(36, 52),
                gain = 0.22
            )
        }
    }

    fun onDecrementOrRemove(view: View, isFullRemove: Boolean = false) {
        runCatching {
            if (isFullRemove) {
                val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    HapticFeedbackConstants.REJECT
                } else {
                    HapticFeedbackConstants.LONG_PRESS
                }
                view.performHapticFeedback(constant)
                vibratePattern(
                    context = view.context,
                    timings = longArrayOf(0L, 26L, 34L, 16L),
                    amplitudes = intArrayOf(0, 230, 0, 130),
                    fallbackMs = 28L
                )
                playDynamicSynthChime(
                    frequenciesHz = doubleArrayOf(440.0, 311.13),
                    noteDurationsMs = intArrayOf(42, 58),
                    gain = 0.20
                )
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                vibratePredefinedOrOneShot(
                    context = view.context,
                    effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else -1,
                    fallbackMs = 10L,
                    amplitude = 95
                )
                playDynamicSynthChime(
                    frequenciesHz = doubleArrayOf(493.88),
                    noteDurationsMs = intArrayOf(26),
                    gain = 0.14
                )
            }
        }
    }

    fun onNavigationClick(view: View) {
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                HapticFeedbackConstants.KEYBOARD_PRESS
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            }
            view.performHapticFeedback(constant)
            view.playSoundEffect(SoundEffectConstants.CLICK)
            vibratePredefinedOrOneShot(
                context = view.context,
                effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else -1,
                fallbackMs = 11L,
                amplitude = 115
            )
            playDynamicSynthChime(
                frequenciesHz = doubleArrayOf(659.25),
                noteDurationsMs = intArrayOf(22),
                gain = 0.12
            )
        }
    }

    fun onAssistantTrigger(view: View) {
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.GESTURE_START
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
            view.performHapticFeedback(constant)
            vibratePattern(
                context = view.context,
                timings = longArrayOf(0L, 10L, 22L, 15L, 22L, 20L),
                amplitudes = intArrayOf(0, 115, 0, 170, 0, 240),
                fallbackMs = 24L
            )
            playDynamicSynthChime(
                frequenciesHz = doubleArrayOf(523.25, 659.25, 1046.50),
                noteDurationsMs = intArrayOf(34, 38, 64),
                gain = 0.20
            )
        }
    }

    fun onCheckoutConfirm(view: View) {
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(constant)
            vibratePattern(
                context = view.context,
                timings = longArrayOf(0L, 14L, 26L, 18L, 28L, 32L),
                amplitudes = intArrayOf(0, 135, 0, 185, 0, 255),
                fallbackMs = 34L
            )
            playDynamicSynthChime(
                frequenciesHz = doubleArrayOf(523.25, 659.25, 783.99, 1046.50),
                noteDurationsMs = intArrayOf(45, 45, 50, 95),
                gain = 0.25
            )
        }
    }

    fun onOrderCelebration(view: View) {
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
            view.performHapticFeedback(constant)
            vibratePattern(
                context = view.context,
                timings = longArrayOf(0L, 22L, 35L, 28L, 40L, 45L, 55L, 70L),
                amplitudes = intArrayOf(0, 160, 0, 200, 0, 235, 0, 255),
                fallbackMs = 65L
            )
            // Celebratory ascending major arpeggio fanfare (C5 - E5 - G5 - C6 - E6 - G6)
            playDynamicSynthChime(
                frequenciesHz = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51, 1567.98),
                noteDurationsMs = intArrayOf(65, 65, 75, 90, 95, 220),
                gain = 0.30
            )
        }
    }

    fun onBarcodeScanned(view: View) {
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CONTEXT_CLICK
            }
            view.performHapticFeedback(constant)
            vibratePredefinedOrOneShot(
                context = view.context,
                effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_DOUBLE_CLICK else -1,
                fallbackMs = 18L,
                amplitude = 195
            )
            playDynamicSynthChime(
                frequenciesHz = doubleArrayOf(1760.0, 2093.0),
                noteDurationsMs = intArrayOf(35, 45),
                gain = 0.22
            )
        }
    }

    fun onSelectionTick(view: View) {
        runCatching {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            vibratePredefinedOrOneShot(
                context = view.context,
                effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_TICK else -1,
                fallbackMs = 8L,
                amplitude = 80
            )
            playDynamicSynthChime(
                frequenciesHz = doubleArrayOf(740.0),
                noteDurationsMs = intArrayOf(18),
                gain = 0.10
            )
        }
    }

    fun onConfirmAction(view: View) {
        runCatching {
            val constant = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.KEYBOARD_TAP
            }
            view.performHapticFeedback(constant)
            vibratePredefinedOrOneShot(
                context = view.context,
                effectId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) VibrationEffect.EFFECT_CLICK else -1,
                fallbackMs = 14L,
                amplitude = 150
            )
            playDynamicSynthChime(
                frequenciesHz = doubleArrayOf(659.25, 880.0),
                noteDurationsMs = intArrayOf(28, 42),
                gain = 0.16
            )
        }
    }

    private fun playDynamicSynthChime(
        frequenciesHz: DoubleArray,
        noteDurationsMs: IntArray,
        gain: Double
    ) {
        soundExecutor.execute {
            runCatching {
                val sampleRate = 24000
                val totalMs = noteDurationsMs.sum().coerceAtLeast(15)
                val totalSamples = (sampleRate * totalMs) / 1000
                val pcm = ShortArray(totalSamples)
                var writeIdx = 0

                for (i in frequenciesHz.indices) {
                    val freq = frequenciesHz[i]
                    val durMs = noteDurationsMs.getOrElse(i) { 30 }
                    val noteSamples = ((sampleRate * durMs) / 1000).coerceAtLeast(1)

                    for (s in 0 until noteSamples) {
                        if (writeIdx >= totalSamples) break
                        val t = s.toDouble() / sampleRate
                        val attack = (s.toDouble() / (noteSamples * 0.12)).coerceAtMost(1.0)
                        val decay = exp(-4.8 * s.toDouble() / noteSamples)
                        val envelope = attack * decay
                        val wave = sin(2.0 * PI * freq * t) + 0.25 * sin(4.0 * PI * freq * t)
                        val sampleVal = (wave * envelope * gain * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        pcm[writeIdx++] = sampleVal.toShort()
                    }
                }

                val byteCount = totalSamples * 2
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(byteCount)

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
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(pcm, 0, totalSamples)
                track.play()
                Thread.sleep((totalMs + 25).toLong())
                track.stop()
                track.release()
            }
        }
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }.getOrNull()
    }

    private fun vibratePredefinedOrOneShot(
        context: Context,
        effectId: Int,
        fallbackMs: Long,
        amplitude: Int
    ) {
        val vibrator = resolveVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectId >= 0) {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, amplitude.coerceIn(1, 255)))
        }
    }

    private fun vibratePattern(
        context: Context,
        timings: LongArray,
        amplitudes: IntArray,
        fallbackMs: Long
    ) {
        val vibrator = resolveVibrator(context) ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (vibrator.hasAmplitudeControl()) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(fallbackMs, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}

fun performCrispHaptic(view: View, style: HapticStyle = HapticStyle.TICK) {
    HapticFeedbackManager.perform(view, style)
}
