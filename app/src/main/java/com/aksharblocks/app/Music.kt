package com.aksharblocks.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Soft background music: a music-box tune made in code when first needed (nothing to license),
 * looped quietly, and quieter still while the voice is speaking. Parents can turn it off.
 */
object Music {

    /** Parents can turn the music off in the parent area. */
    @Volatile
    var enabled = true
        set(value) {
            field = value
            if (!value) pause()
        }

    private var track: AudioTrack? = null
    private var wanted = false
    private var ducked = false
    private var building = false

    /** Starts (or resumes) the music, if it's turned on. */
    fun play() {
        wanted = true
        if (!enabled) return
        val ready = track
        if (ready != null) {
            start(ready)
            return
        }
        if (building) return
        building = true
        // Making 20 seconds of music takes a moment: do it off the main thread.
        Thread {
            val made = try {
                build(MusicBox.loop())
            } catch (e: RuntimeException) {
                Log.w("Music", "Could not make the music", e)
                null
            }
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                building = false
                track = made
                if (made != null && wanted && enabled) start(made)
            }
        }.start()
    }

    fun pause() {
        wanted = false
        try {
            track?.takeIf { it.playState == AudioTrack.PLAYSTATE_PLAYING }?.pause()
        } catch (e: RuntimeException) {
            Log.w("Music", "Could not pause", e)
        }
    }

    /** Quieter while the voice speaks, so words stay clear. */
    fun duck(speaking: Boolean) {
        ducked = speaking
        track?.setVolume(volume())
    }

    fun release() {
        wanted = false
        track?.release()
        track = null
    }

    private fun volume() = if (ducked) DUCKED else NORMAL

    private fun start(track: AudioTrack) {
        try {
            track.setVolume(volume())
            if (track.playState != AudioTrack.PLAYSTATE_PLAYING) track.play()
        } catch (e: RuntimeException) {
            Log.w("Music", "Could not play", e)
        }
    }

    private fun build(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(MusicBox.RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.setLoopPoints(0, samples.size, -1)
        return track
    }

    /** Track volumes: the music sits well under the voice. */
    private const val NORMAL = 0.35f
    private const val DUCKED = 0.12f
}

/** The tune itself, in plain Kotlin so it can be checked in tests. */
object MusicBox {

    const val RATE = 22050

    /** Seconds per eighth note (about 96 beats a minute). */
    private const val EIGHTH = 0.3125f

    /** Chord tones (root, third, fifth, octave), music-box high. */
    private val C = floatArrayOf(523.3f, 659.3f, 784.0f, 1046.5f)
    private val AM = floatArrayOf(440.0f, 523.3f, 659.3f, 880.0f)
    private val F = floatArrayOf(349.2f, 440.0f, 523.3f, 698.5f)
    private val G = floatArrayOf(392.0f, 493.9f, 587.3f, 784.0f)

    /** Eight bars: C Am F G, twice; each bar an arpeggio up and down. */
    private val BARS = listOf(C, AM, F, G, C, AM, F, G)
    private val PATTERN = intArrayOf(0, 1, 2, 3, 2, 1, 2, 1)

    /** One loop of the tune; the end runs smoothly back into the start. */
    fun loop(): ShortArray {
        val barSamples = (EIGHTH * PATTERN.size * RATE).toInt()
        val total = barSamples * BARS.size
        val mix = FloatArray(total)
        BARS.forEachIndexed { bar, chord ->
            val barStart = bar * barSamples
            PATTERN.forEachIndexed { step, tone ->
                val start = barStart + (step * EIGHTH * RATE).toInt()
                // The first note of each bar is a little stronger, like a tune on top.
                bell(mix, start, chord[tone], if (step == 0) 0.32f else 0.2f, decay = 3.2f)
            }
            // A soft low note under each bar.
            bell(mix, barStart, chord[0] / 4f, 0.22f, decay = 1.2f)
        }
        // Scaled to a set level; the track volume (see Music) keeps it quiet.
        val peak = mix.maxOf { kotlin.math.abs(it) }.coerceAtLeast(0.0001f)
        return ShortArray(total) { (mix[it] / peak * 0.9f * Short.MAX_VALUE).toInt().toShort() }
    }

    /** Adds a ringing note into [mix], wrapping round the end so the loop has no seam. */
    private fun bell(mix: FloatArray, start: Int, hz: Float, volume: Float, decay: Float) {
        val length = (RATE * 2.5f).toInt()
        for (i in 0 until length) {
            val t = i / RATE.toFloat()
            val envelope = minOf(1f, t / 0.004f) * exp(-decay * t)
            // Stop once the note has faded (after its attack, which starts from silence).
            if (t > 0.01f && envelope < 0.002f) break
            val wave = sin(2 * PI * hz * t) * 0.85 + sin(4 * PI * hz * t) * 0.15
            val at = (start + i) % mix.size
            mix[at] += (wave * envelope * volume).toFloat()
        }
    }
}
