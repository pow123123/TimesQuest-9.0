package com.selfuse.timesquest.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure procedural PCM sound-effect synthesizer (zero external audio files, zero latency).
 * Plays pentatonic ascending combo chimes, crisp spell impacts, crit chords, shield shatters,
 * soft mistake bounces, and victory fanfare.
 */
object SoundEffectEngine {
    private const val SAMPLE_RATE = 22050
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cheerful C-major pentatonic scale ascending across combo steps 1..8
    private val COMBO_FREQS = doubleArrayOf(
        523.25, // C5
        587.33, // D5
        659.25, // E5
        783.99, // G5
        880.00, // A5
        1046.50, // C6
        1174.66, // D6
        1318.51  // E6
    )

    fun playComboHit(enabled: Boolean, combo: Int, isCrit: Boolean) {
        if (!enabled) return
        val idx = (combo - 1).coerceIn(0, COMBO_FREQS.lastIndex)
        val base = COMBO_FREQS[idx]
        scope.launch {
            val pcm = if (isCrit) {
                synthesizeChord(
                    freqs = doubleArrayOf(base, base * 1.25, base * 1.5),
                    durationMs = 240,
                    volume = 0.42
                )
            } else {
                synthesizeChime(
                    startFreq = base,
                    endFreq = base * 1.12,
                    durationMs = 170,
                    volume = 0.36
                )
            }
            playPcm(pcm)
        }
    }

    fun playShieldBreak(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            val part1 = synthesizeChime(880.0, 1480.0, 90, 0.35)
            val part2 = synthesizeChord(doubleArrayOf(1046.5, 1318.5, 1567.98), 210, 0.38)
            playPcm(part1 + part2)
        }
    }

    fun playUltimateBlast(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            val sweep = synthesizeChime(330.0, 1320.0, 180, 0.36)
            val chord = synthesizeChord(doubleArrayOf(523.25, 659.25, 783.99, 1046.5), 340, 0.45)
            playPcm(sweep + chord)
        }
    }

    fun playSoftMiss(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            // Gentle downward wooden marimba bounce (friendly, never harsh!)
            val pcm = synthesizeChime(340.0, 230.0, 180, 0.28)
            playPcm(pcm)
        }
    }

    fun playBubblePop(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            val pcm = synthesizeChime(480.0, 980.0, 85, 0.32)
            playPcm(pcm)
        }
    }

    fun playVictoryFanfare(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            val notes = listOf(
                523.25 to 110,
                659.25 to 110,
                783.99 to 110,
                1046.50 to 300
            )
            val chunks = notes.map { (f, ms) -> synthesizeChime(f, f, ms, 0.36) }
            val totalLen = chunks.sumOf { it.size }
            val merged = ShortArray(totalLen)
            var pos = 0
            for (c in chunks) {
                System.arraycopy(c, 0, merged, pos, c.size)
                pos += c.size
            }
            playPcm(merged)
        }
    }

    fun playChestOpen(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            val c1 = synthesizeChime(587.33, 880.0, 120, 0.34)
            val c2 = synthesizeChord(doubleArrayOf(880.0, 1108.73, 1318.51), 280, 0.40)
            playPcm(c1 + c2)
        }
    }

    fun playButtonTap(enabled: Boolean) {
        if (!enabled) return
        scope.launch {
            val pcm = synthesizeChime(620.0, 720.0, 45, 0.20)
            playPcm(pcm)
        }
    }

    fun hapticTap(context: Context, enabled: Boolean, strong: Boolean = false) {
        if (!enabled) return
        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return
            val duration = if (strong) 55L else 22L
            val amp = if (strong) 180 else 95
            vibrator.vibrate(VibrationEffect.createOneShot(duration, amp))
        } catch (_: Exception) {
        }
    }

    private fun synthesizeChime(
        startFreq: Double,
        endFreq: Double,
        durationMs: Int,
        volume: Double
    ): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startFreq + (endFreq - startFreq) * t
            phase += 2.0 * PI * freq / SAMPLE_RATE
            val attack = (i / (SAMPLE_RATE * 0.008)).coerceAtMost(1.0)
            val decay = exp(-4.2 * t)
            val wave = sin(phase) + 0.25 * sin(2.0 * phase)
            out[i] = (wave * attack * decay * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeChord(
        freqs: DoubleArray,
        durationMs: Int,
        volume: Double
    ): ShortArray {
        val count = (SAMPLE_RATE * durationMs) / 1000
        val out = ShortArray(count)
        val scale = 1.0 / freqs.size
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val attack = (i / (SAMPLE_RATE * 0.01)).coerceAtMost(1.0)
            val decay = exp(-3.5 * t)
            var sum = 0.0
            for (f in freqs) {
                val angle = 2.0 * PI * f * i / SAMPLE_RATE
                sum += (sin(angle) + 0.18 * sin(2.0 * angle)) * scale
            }
            out[i] = (sum * attack * decay * volume * Short.MAX_VALUE).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun playPcm(samples: ShortArray) {
        if (samples.isEmpty()) return
        var track: AudioTrack? = null
        try {
            val byteLen = samples.size * 2
            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteLen.coerceAtLeast(2048))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            val waitMs = (samples.size * 1000L) / SAMPLE_RATE + 60L
            Thread.sleep(waitMs)
        } catch (_: Exception) {
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Exception) {
            }
        }
    }
}
